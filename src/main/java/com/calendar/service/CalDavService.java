package com.calendar.service;

import com.calendar.model.CalendarDo;
import com.calendar.model.CalendarEvent;
import com.calendar.model.CalendarType;
import com.calendar.repository.CalendarRepository;
import com.calendar.repository.CalendarEventRepository;
import com.calendar.exception.CalendarSyncException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.property.*;
import org.osaf.caldav4j.CalDAVCollection;
import org.osaf.caldav4j.CalDAVConstants;
import org.osaf.caldav4j.methods.HttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.component.VEvent;
import org.apache.commons.httpclient.HostConfiguration;
import org.osaf.caldav4j.methods.CalDAV4JMethodFactory;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Date;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class CalDavService {

    private final CalendarRepository calendarRepository;
    private final CalendarEventRepository eventRepository;

    @Value("${caldav.server.protocol}")
    private String protocol;

    @Value("${caldav.server.host}")
    private String host;

    @Value("${caldav.server.port}")
    private int port;

    /**
     * 同步CalDAV日历
     */
    public void syncCalendar(String calendarUrl, String username, String password) {
        try {
            log.info("开始同步CalDAV日历: {}", calendarUrl);

            // 创建HTTP客户端
            HttpClient httpClient = new HttpClient();
            httpClient.getState().setCredentials(
                new org.apache.commons.httpclient.auth.AuthScope(host, port),
                new org.apache.commons.httpclient.UsernamePasswordCredentials(username, password)
            );

            // 创建CalDAV集合
            String fullUrl = String.format("%s://%s:%d%s", protocol, host, port, calendarUrl);
            HostConfiguration hostConfig = new HostConfiguration();
            hostConfig.setHost(host, port, protocol);
            CalDAV4JMethodFactory methodFactory = new CalDAV4JMethodFactory();
            CalDAVCollection collection = new CalDAVCollection(
                fullUrl,
                hostConfig,
                methodFactory,
                CalDAVConstants.PROC_ID_DEFAULT
            );

            // 获取或创建日历
            CalendarDo calendar = calendarRepository.findByExternalId(calendarUrl)
                .orElseGet(() -> {
                    CalendarDo newCalendar = new CalendarDo();
                    newCalendar.setExternalId(calendarUrl);
                    newCalendar.setName("CalDAV Calendar");
                    newCalendar.setType(CalendarType.GOOGLE);
                    newCalendar.setOwner(username);
                    newCalendar.setLastSync(LocalDateTime.now());
                    return calendarRepository.save(newCalendar);
                });

            // 获取所有事件
            List<Calendar> events = collection.multigetCalendarUris(httpClient, null);
          
            for (Calendar calEvent : events) {
                try {
                    for (Component component : calEvent.getComponents(Component.VEVENT)) {
                        processCalDavEvent((VEvent) component, calendar);
                    }
                } catch (Exception e) {
                    log.error("处理CalDAV事件失败: {}", e.getMessage());
                }
            }

            // 更新同步时间
            calendar.setLastSync(LocalDateTime.now());
            calendarRepository.save(calendar);
            
            log.info("CalDAV日历同步完成");

        } catch (Exception e) {
            log.error("同步CalDAV日历失败: {}", e.getMessage());
            throw new CalendarSyncException("同步CalDAV日历失败", e);
        }
    }

    /**
     * 处理单个CalDAV事件
     */
    private void processCalDavEvent(VEvent event, CalendarDo calendar) {
        if (event == null) {
            return;
        }

        String eventId = event.getUid().getValue();
        Optional<CalendarEvent> existingEvent = eventRepository.findByExternalId(eventId);

        // 转换事件时间
        Date startDate = event.getStartDate().getDate();
        Date endDate = event.getEndDate().getDate();
        
        LocalDateTime startTime = LocalDateTime.ofInstant(
            startDate.toInstant(),
            ZoneId.systemDefault()
        );
        LocalDateTime endTime = LocalDateTime.ofInstant(
            endDate.toInstant(),
            ZoneId.systemDefault()
        );

        if (existingEvent != null) {
            CalendarEvent calendarEvent = existingEvent.get();
            // 更新现有事件
            calendarEvent.setSummary(event.getSummary().getValue());
            calendarEvent.setDescription(
                event.getDescription() != null ? 
                event.getDescription().getValue() : null
            );
            calendarEvent.setLocation(
                event.getLocation() != null ? 
                event.getLocation().getValue() : null
            );
            calendarEvent.setStartTime(startTime);
            calendarEvent.setEndTime(endTime);
            eventRepository.save(calendarEvent);
            log.debug("更新事件: {}", eventId);
        } else {
            // 创建新事件
            CalendarEvent newEvent = new CalendarEvent();
            newEvent.setExternalId(eventId);
            newEvent.setCalendar(calendar);
            newEvent.setTitle(event.getSummary().getValue());
            newEvent.setDescription(
                event.getDescription() != null ? 
                event.getDescription().getValue() : null
            );
            newEvent.setLocation(
                event.getLocation() != null ? 
                event.getLocation().getValue() : null
            );
            newEvent.setStartTime(startTime);
            newEvent.setEndTime(endTime);
            eventRepository.save(newEvent);
            log.debug("创建新事件: {}", eventId);
        }
    }

    /**
     * 添加新事件到CalDAV日历
     */
    public void addEvent(String calendarUrl, String username, String password, CalendarEvent event) {
        try {
            // 创建HTTP客户端
            HttpClient httpClient = new HttpClient();
            httpClient.getState().setCredentials(
                    new org.apache.commons.httpclient.auth.AuthScope(host, port),
                    new org.apache.commons.httpclient.UsernamePasswordCredentials(username, password)
            );

            // 创建CalDAV集合
            String fullUrl = String.format("%s://%s:%d%s", protocol, host, port, calendarUrl);
            HostConfiguration hostConfig = new HostConfiguration();
            hostConfig.setHost(host, port, protocol);
            CalDAV4JMethodFactory methodFactory = new CalDAV4JMethodFactory();
            CalDAVCollection collection = new CalDAVCollection(
                fullUrl,
                hostConfig,
                methodFactory,
                CalDAVConstants.PROC_ID_DEFAULT
            );

            // 创建iCal事件
            VEvent vEvent = new VEvent();
            vEvent.getProperties().add(new Uid(event.getExternalId()));  // 使用Uid类
            vEvent.getProperties().add(new Summary(event.getTitle()));
            if (event.getDescription() != null) {
                vEvent.getProperties().add(new Description(event.getDescription()));
            }
            if (event.getLocation() != null) {
                vEvent.getProperties().add(new Location(event.getLocation()));
            }
            vEvent.getProperties().add(new DtStart(
                    new DateTime(Date.from(event.getStartTime().atZone(ZoneId.systemDefault()).toInstant()))
            ));
            vEvent.getProperties().add(new DtEnd(
                    new DateTime(Date.from(event.getEndTime().atZone(ZoneId.systemDefault()).toInstant()))
            ));

            // 创建日历对象并添加事件
            Calendar calendar = new Calendar();
            calendar.getComponents().add(vEvent);

            // 添加事件到CalDAV服务器
            collection.add(httpClient, calendar);
            log.info("CalDAV事件添加成功: {}", event.getExternalId());

        } catch (Exception e) {
            log.error("添加CalDAV事件失败: {}", e.getMessage());
            throw new CalendarSyncException("添加CalDAV事件失败", e);
        }
    }
} 