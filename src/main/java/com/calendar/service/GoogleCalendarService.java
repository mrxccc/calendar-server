package com.calendar.service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.calendar.exception.CalendarSyncException;
import com.calendar.model.CalendarDo;
import com.calendar.model.CalendarEvent;
import com.calendar.model.CalendarType;
import com.calendar.repository.CalendarEventRepository;
import com.calendar.repository.CalendarRepository;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.CalendarList;
import com.google.api.services.calendar.model.CalendarListEntry;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.Events;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

/**
 * Google日历服务
 */
@Service
@Slf4j
public class GoogleCalendarService {
    private Calendar service;
    private NetHttpTransport httpTransport;
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    @Autowired
    private CalendarRepository calendarRepository;

    @Autowired
    private CalendarEventRepository eventRepository;

    @Autowired
    private GoogleOAuth2Service oAuth2Service;

    @Value("${google.calendar.application-name}")
    private String applicationName;

    /**
     * 初始化Google Calendar服务
     */
    @PostConstruct
    public void init() throws Exception {
        try {
            this.httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            Calendar.Builder builder = new Calendar.Builder(httpTransport,
                    JSON_FACTORY,
                    null)
                    .setApplicationName(applicationName);
            this.service = builder.build();
            log.info("Google Calendar服务初始化成功");
        } catch (Exception e) {
            log.error("初始化Google Calendar服务失败", e);
            throw e;
        }
    }

    /**
     * 同步指定用户的Google日历事件
     */
    @Async
    public CompletableFuture<Void> syncCalendar(String userId, String calendarId) {
        try {
            // 如果是"primary"，则获取用户的主日历ID
            String targetCalendarId = "primary".equals(calendarId) ? 
                "primary" : calendarId;
            log.info("开始同步日历，用户ID: {}, 日历ID: {}", userId, targetCalendarId);

            Credential credential = oAuth2Service.getCredential(userId);
            if (credential == null) {
                throw new CalendarSyncException("用户未授权访问Google日历");
            }

            if (service == null) {
                throw new CalendarSyncException("Google Calendar服务未初始化");
            }

            // 先获取用户的日历列表
            try {
                // 使用带凭证的服务实例获取日历列表
                Calendar.Builder builder = new Calendar.Builder(
                        httpTransport,
                        JSON_FACTORY,
                        credential)
                        .setApplicationName(applicationName);
                Calendar authorizedService = builder.build();
                
                CalendarList calendarList = authorizedService
                    .calendarList()
                    .list()
//                    .setKey(apiKey)
                        .setOauthToken(credential.getAccessToken())
                    .execute();
                
                // 检查日历是否存在
                boolean calendarExists = calendarList.getItems().stream()
                    .anyMatch(cal -> cal.getId().equals(targetCalendarId));
                
                if (!calendarExists && !"primary".equals(targetCalendarId)) {
                    throw new CalendarSyncException("找不到指定的日历ID: " + targetCalendarId);
                }
                
                // 复用已创建的authorizedService
                this.service = authorizedService;
            } catch (Exception e) {
                log.error("获取日历列表失败", e);
                throw new CalendarSyncException("无法验证日历访问权限");
            }

            String syncToken = calendarRepository.findSyncTokenByExternalId(calendarId);
            Events events;
            
            if (syncToken == null) {
                // 全量同步
                events = service.events().list(targetCalendarId)
//                        .setKey(apiKey)
                        .setOauthToken(credential.getAccessToken())
                        .setMaxResults(2500)
                        .setShowDeleted(false)
                        .setTimeMin(new DateTime(System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000)) // 30天前
                        .setOrderBy("startTime")
                        .setSingleEvents(true)
                        .execute();
            } else {
                // 增量同步
                events = service.events().list(targetCalendarId)
//                        .setKey(apiKey)
                        .setSyncToken(syncToken)
                        .execute();
            }

            processEvents(events.getItems(), calendarId);
            updateSyncToken(calendarId, events.getNextSyncToken());
            
        } catch (Exception e) {
            log.error("同步Google日历失败", e);
            throw new CalendarSyncException("同步Google日历失败", e);
        }
        return CompletableFuture.completedFuture(null);
    }

    /**
     * 处理日历事件
     */
    private void processEvents(List<Event> events, String calendarId) {
        log.debug("开始处理 {} 个日历事件", events.size());
        
        // 获取或创建日历
        CalendarDo calendar = calendarRepository.findByExternalId(calendarId)
            .orElseGet(() -> {
                CalendarDo newCalendar = new CalendarDo();
                newCalendar.setExternalId(calendarId);
                newCalendar.setName("Google Calendar " + calendarId);
                newCalendar.setType(CalendarType.GOOGLE);
                newCalendar.setOwner("system");
                newCalendar.setLastSync(LocalDateTime.now());
                return calendarRepository.save(newCalendar);
            });

        for (Event event : events) {
            try {
                processEvent(event, calendar);
            } catch (Exception e) {
                log.error("处理事件 {} 失败: {}", event.getId(), e.getMessage());
            }
        }
    }

    /**
     * 处理单个事件
     */
    private void processEvent(Event event, CalendarDo calendar) {
        String eventId = event.getId();
        Optional<CalendarEvent> existingEvent = eventRepository.findByExternalId(eventId);

        if (Boolean.TRUE.equals(event.getStatus().equals("cancelled"))) {
            // 处理删除的事件
            existingEvent.ifPresent(eventRepository::delete);
            log.debug("删除事件: {}", eventId);
            return;
        }

        // 转换事件时间
        LocalDateTime startTime = null;
        LocalDateTime endTime = null;

        if (event.getStart().getDateTime() != null) {
            // 具体时间的事件
            Date startDate = new Date(event.getStart().getDateTime().getValue());
            Date endDate = new Date(event.getEnd().getDateTime().getValue());
            startTime = LocalDateTime.ofInstant(
                startDate.toInstant(),
                ZoneId.systemDefault()
            );
            endTime = LocalDateTime.ofInstant(
                endDate.toInstant(),
                ZoneId.systemDefault()
            );
        } else if (event.getStart().getDate() != null) {
            // 全天事件
            Date startDate = new Date(event.getStart().getDate().getValue());
            Date endDate = new Date(event.getEnd().getDate().getValue());
            startTime = LocalDateTime.ofInstant(
                startDate.toInstant(),
                ZoneId.systemDefault()
            );
            endTime = LocalDateTime.ofInstant(
                endDate.toInstant(),
                ZoneId.systemDefault()
            );
        }

        if (existingEvent.isPresent()) {
            // 更新现有事件
            CalendarEvent updatedEvent = existingEvent.get();
            updatedEvent.setTitle(event.getSummary());
            updatedEvent.setDescription(event.getDescription());
            updatedEvent.setLocation(event.getLocation());
            updatedEvent.setStartTime(startTime);
            updatedEvent.setEndTime(endTime);
            eventRepository.save(updatedEvent);
            log.debug("更新事件: {}", eventId);
        } else {
            // 创建新事件
            CalendarEvent newEvent = new CalendarEvent();
            newEvent.setExternalId(eventId);
            newEvent.setCalendar(calendar);
            newEvent.setTitle(event.getSummary());
            newEvent.setDescription(event.getDescription());
            newEvent.setLocation(event.getLocation());
            newEvent.setStartTime(startTime);
            newEvent.setEndTime(endTime);
            eventRepository.save(newEvent);
            log.debug("创建新事件: {}", eventId);
        }
    }

    /**
     * 更新同步令牌
     */
    private void updateSyncToken(String calendarId, String nextSyncToken) {
        if (nextSyncToken == null) {
            log.warn("没有收到新的同步令牌");
            return;
        }

        try {
            CalendarDo calendar = calendarRepository.findByExternalId(calendarId)
                .orElseThrow(() -> new CalendarSyncException("找不到日历记录: " + calendarId));

            calendar.setSyncToken(nextSyncToken);
            calendar.setLastSync(LocalDateTime.now());
            calendarRepository.save(calendar);
            log.debug("更新同步令牌: {}", nextSyncToken);
        } catch (Exception e) {
            log.error("更新同步令牌失败: {}", e.getMessage());
            throw new CalendarSyncException("更新同步令牌失败", e);
        }
    }

    /**
     * 查看用户的Google日历列表
     * 
     * @param userId 用户ID
     * @return 日历列表
     * @throws CalendarSyncException 如果获取日历列表失败
     */
    public List<CalendarListEntry> listCalendars(String userId) throws CalendarSyncException {
        try {
            Credential credential = oAuth2Service.getCredential(userId);
            if (credential == null) {
                throw new CalendarSyncException("用户未授权访问Google日历");
            }

            Calendar.Builder builder = new Calendar.Builder(
                    httpTransport,
                    JSON_FACTORY,
                    credential)
                    .setApplicationName(applicationName);
            Calendar authorizedService = builder.build();

            CalendarList calendarList = authorizedService
                .calendarList()
                .list()
                .setOauthToken(credential.getAccessToken())
                .execute();

            return calendarList.getItems();
        } catch (Exception e) {
            log.error("获取日历列表失败", e);
            throw new CalendarSyncException("无法获取日历列表", e);
        }
    }

    /**
     * 查看指定日历的事件
     * 
     * @param userId 用户ID
     * @param calendarId 日历ID
     * @return 事件列表
     * @throws CalendarSyncException 如果获取事件失败
     */
    public List<Event> listEvents(String userId, String calendarId) throws CalendarSyncException {
        try {
            Credential credential = oAuth2Service.getCredential(userId);
            if (credential == null) {
                throw new CalendarSyncException("用户未授权访问Google日历");
            }

            Calendar.Builder builder = new Calendar.Builder(
                    httpTransport,
                    JSON_FACTORY,
                    credential)
                    .setApplicationName(applicationName);
            Calendar authorizedService = builder.build();

            Events events = authorizedService.events().list(calendarId)
                .setOauthToken(credential.getAccessToken())
                .setMaxResults(2500)
                .setOrderBy("startTime")
                .setSingleEvents(true)
                .execute();

            return events.getItems();
        } catch (Exception e) {
            log.error("获取日历事件失败", e);
            throw new CalendarSyncException("无法获取日历事件", e);
        }
    }

    /**
     * 删除指定日历中的事件
     * 
     * @param userId 用户ID
     * @param calendarId 日历ID
     * @param eventId 事件ID
     * @throws CalendarSyncException 如果删除事件失败
     */
    public void deleteEvent(String userId, String calendarId, String eventId) throws CalendarSyncException {
        try {
            Credential credential = oAuth2Service.getCredential(userId);
            if (credential == null) {
                throw new CalendarSyncException("用户未授权访问Google日历");
            }

            Calendar.Builder builder = new Calendar.Builder(
                    httpTransport,
                    JSON_FACTORY,
                    credential)
                    .setApplicationName(applicationName);
            Calendar authorizedService = builder.build();

            authorizedService.events().delete(calendarId, eventId)
                .setOauthToken(credential.getAccessToken())
                .execute();

            log.info("事件 {} 已从日历 {} 中删除", eventId, calendarId);
        } catch (Exception e) {
            log.error("删除日历事件失败", e);
            throw new CalendarSyncException("无法删除日历事件", e);
        }
    }
} 