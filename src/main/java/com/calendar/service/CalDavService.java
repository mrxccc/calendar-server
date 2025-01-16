package com.calendar.service;

import java.io.*;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import com.calendar.credential.CaldavCredential;

import com.calendar.model.CalendarCollection;
import com.github.caldav4j.CalDAVCollection;
import com.github.caldav4j.CalDAVConstants;
import com.github.caldav4j.methods.HttpCalDAVReportMethod;
import com.github.caldav4j.model.request.CalendarData;
import com.github.caldav4j.model.request.CalendarQuery;
import com.github.caldav4j.model.request.CompFilter;
import com.github.caldav4j.model.response.CalendarDataProperty;
import com.github.caldav4j.util.XMLUtils;
import net.fortuna.ical4j.model.Component;
import org.apache.http.HttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.jackrabbit.webdav.MultiStatusResponse;
import org.apache.jackrabbit.webdav.property.DavPropertyName;
import org.apache.jackrabbit.webdav.property.DavPropertyNameSet;
import org.aspectj.util.FileUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import com.calendar.repository.CalendarEventRepository;
import com.calendar.repository.CalendarRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.CalendarOutputter;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.DtEnd;
import net.fortuna.ical4j.model.property.DtStart;
import net.fortuna.ical4j.model.property.Location;
import net.fortuna.ical4j.model.property.ProdId;
import net.fortuna.ical4j.model.property.Summary;
import net.fortuna.ical4j.model.property.Uid;
import net.fortuna.ical4j.model.property.Version;
import org.springframework.web.bind.annotation.RequestParam;


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


    public void createCalendar(String username, String password, String calendarName) {
        try {
            log.info("开始创建CalDAV日历: {}", calendarName);

            // @todo 校验用户是否存在
            // @todo 鉴权：校验用户名密码
            // @todo 校验日历是否存在
            // 创建一个简单的iCalendar对象
            Calendar calendar = new Calendar();
            calendar.getProperties().add(new ProdId("-//My Company//iCal4j 1.0//EN"));
            calendar.getProperties().add(Version.VERSION_2_0);

            // 添加事件到日历
            VEvent event = new VEvent();
            calendar.getComponents().add(event);

            // 保存到.ics文件
            String filePath = calendarName + ".ics";
            ClassPathResource resource = new ClassPathResource("icalendar" + System.getProperty("file.separator") + filePath);
            try (FileOutputStream fout = new FileOutputStream(resource.getFile())) {
                CalendarOutputter outputter = new CalendarOutputter();
                outputter.output(calendar, fout);
                log.info("日历已保存到文件: {}", resource.getFile().getAbsolutePath());
                System.out.println("日历已保存到文件: " + resource.getFile().getAbsolutePath());
            }

        } catch (Exception e) {
            log.error("创建CalDAV日历失败: {}", e.getMessage());
            throw new RuntimeException("创建CalDAV日历失败", e);
        }
    }


    public void createCalendarEvent(String calendarUrl, String summary, String location, DateTime start, DateTime end) throws IOException {
        // 读取现有的.ics文件
        File file = new File(calendarUrl);
        Calendar calendar;
        try (FileInputStream fin = new FileInputStream(file)) {
            CalendarBuilder builder = new CalendarBuilder();
            calendar = builder.build(fin);
        } catch (ParserException e) {
            throw new RuntimeException(e);
        }

        // 创建事件
        VEvent event = new VEvent(start, end, summary);
        event.getProperties().add(new Location(location));
        event.getProperties().add(new Uid(UUID.randomUUID().toString()));

        // 添加事件到现有日历
        calendar.getComponents().add(event);

        // 保存更新后的日历到.ics文件
        try (FileOutputStream fout = new FileOutputStream(file)) {
            CalendarOutputter outputter = new CalendarOutputter();
            outputter.output(calendar, fout);
            log.info("事件已添加到日历文件: {}", file.getAbsolutePath());
        }
    }

    public void updateCalendarEvent(String calendarUrl, String eventUid, String newSummary, String newLocation, DateTime newStart, DateTime newEnd) throws IOException, ParseException {
        // 读取现有的.ics文件
        File file = new File(calendarUrl);
        Calendar calendar;
        try (FileInputStream fin = new FileInputStream(file)) {
            CalendarBuilder builder = new CalendarBuilder();
            calendar = builder.build(fin);
        } catch (ParserException e) {
            throw new RuntimeException(e);
        }

        // 查找并更新事件
        VEvent eventToUpdate = null;
        for (Object component : calendar.getComponents()) {
            if (component instanceof VEvent) {
                VEvent event = (VEvent) component;
                if (event.getUid().getValue().equals(eventUid)) {
                    eventToUpdate = event;
                    break;
                }
            }
        }

        if (eventToUpdate != null) {
            ((Summary) eventToUpdate.getProperty(Property.SUMMARY)).setValue(newSummary);
            ((Location) eventToUpdate.getProperty(Property.LOCATION)).setValue(newLocation);
            ((DtStart) eventToUpdate.getProperty(Property.DTSTART)).setValue(newStart.toString());
            ((DtEnd) eventToUpdate.getProperty(Property.DTEND)).setValue(newEnd.toString());
        } else {
            throw new IOException("事件未找到: " + eventUid);
        }

        // 保存更新后的日历到.ics文件
        try (FileOutputStream fout = new FileOutputStream(file)) {
            CalendarOutputter outputter = new CalendarOutputter();
            outputter.output(calendar, fout);
            log.info("事件已更新到日历文件: {}", file.getAbsolutePath());
        }
    }

//    public String queryCalendar(){
//        // Create a set of Dav Properties to query
//        DavPropertyNameSet properties = new DavPropertyNameSet();
//        properties.add(DavPropertyName.GETETAG);
//
//        // Create a Component filter for VCALENDAR and VEVENT
//        CompFilter vcalendar = new CompFilter(Calendar.VCALENDAR);
//        vcalendar.addCompFilter(new CompFilter(Component.VEVENT));
//
//        // Create a Query XML object with the above properties
//        CalendarQuery query = new CalendarQuery(properties, vcalendar, new CalendarData(), false, false);
//
//		/*
//		<C:calendar-query xmlns:C="urn:ietf:params:xml:ns:caldav">
//		  <D:prop xmlns:D="DAV:">
//		    <D:getetag/>
//		    <C:calendar-data/>
//		  </D:prop>
//		  <C:filter>
//		    <C:comp-filter name="VCALENDAR">
//		      <C:comp-filter name="VEVENT"/>
//		    </C:comp-filter>
//		  </C:filter>
//		</C:calendar-query>
//		*/
//        // Print to STDOUT the generated Query
//        System.out.println(XMLUtils.prettyPrint(query));
//
//        HttpCalDAVReportMethod method = null;
//
//        try {
//            method = new HttpCalDAVReportMethod("path://to/caldav/calendar", query, CalDAVConstants.DEPTH_1);
//            CloseableHttpClient client = HttpClients.createDefault();
//
//            // Execute the method
//            HttpResponse httpResponse = client.execute(method);
//
//            // If successful
//            if (method.succeeded(httpResponse)) {
//                // Retrieve all multistatus responses
//                MultiStatusResponse[] multiStatusResponses = method.getResponseBodyAsMultiStatus(httpResponse).getResponses();
//
//                // Iterate through all responses
//                for (MultiStatusResponse response : multiStatusResponses) {
//                    // If the individual calendar request was succesful
//                    if (response.getStatus()[0].getStatusCode() == SC_OK) {
//                        // Retrieve ETag and  Calendar from response
//                        String etag = CalendarDataProperty.getEtagfromResponse(response);
//                        Calendar ical = CalendarDataProperty.getCalendarfromResponse(response);
//
//                        // Print to output
//                        System.out.println("Calendar at " + response.getHref() + " with ETag: " + etag);
//                        System.out.println(ical);
//                    }
//                }
//            }
//
//        } catch (Exception e) {
//            log.info(e.getMessage(), e);
//            // No-op
//        } finally {
//            if (method != null) {
//                method.reset();
//            }
//        }
//        return "Response";
//    }

    public Calendar queryCalendar(String calendarName){
        // 读取现有的.ics文件
        // 使用 ClassPathResource 加载资源文件
        ClassPathResource resource = new ClassPathResource("icalendar" + System.getProperty("file.separator") + calendarName + ".ics");
        Calendar calendar;
        try (FileInputStream fin = new FileInputStream(resource.getFile())) {
            CalendarBuilder builder = new CalendarBuilder();
            calendar = builder.build(fin);
            System.out.println(calendar.toString());
        } catch (ParserException | FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return calendar;
    }

    public void syncCalendar(String calendarName, String username, String password) {
        try {
            log.info("开始同步CalDAV日历: {}", calendarName);
            List<CalendarCollection> calendarCollections = getCalendarCollections(username, password);
            for (CalendarCollection calendarCollection : calendarCollections) {
                // 多线程处理google 日历、icloud日历、qq日历等
                // @todo 校验用户是否存在
                // @todo 鉴权：校验用户名密码
                // @todo 校验日历是否存在
                // @todo 请求各个日历系统日历数据，保存到本地.ics文件或者数据库中
            }
        } catch (Exception e) {
            log.error("同步CalDAV日历失败: {}", e.getMessage());
            throw new RuntimeException("同步CalDAV日历失败", e);
        }
    }

    /**
     * 获取日历集合
     * @param username
     * @param password
     * @return
     * @throws IOException
     */
    public List<CalendarCollection> getCalendarCollections(String username, String password) throws IOException {
        List<CalendarCollection> calendarCollections = new ArrayList<>();
        // @todo 校验用户是否存在
        // @todo 鉴权：校验用户名密码
        // @todo 校验日历是否存在
        // @todo 两种方案获取.ics文件
        //  1..ics文件通过对象存储文件系统保存到数据库后，查询数据库获取用户所有日历系统日历集合，获取url集合
        //  2.这里为了简便先将.ics保存在本地，假设 .ics 文件存放在 resources 目录下
        String cid = "singleEvent";
        ClassPathResource resource = new ClassPathResource("icalendar");
        File icalendarDir = resource.getFile();

        if (icalendarDir != null && icalendarDir.exists()){
            File icalendarFile =  new File(icalendarDir.getAbsolutePath() + System.getProperty("file.separator"));
            File[] files = icalendarFile.listFiles();
            for (File file : files) {
                String fullUrl = String.format("%s://%s:%d/calendars/%s", protocol, host, port, file.getName());
                CalendarCollection collection = new CalendarCollection();
                collection.setDisplayName(cid);
                collection.setHref(fullUrl);
                calendarCollections.add(collection);
            }
        } else {
            throw new IOException("文件不存在");
        }
        return calendarCollections;
    }

    // 根据 UID 获取单个日历集合的详细信息
    public String getCalendarCollectionDetails(String cid) throws IOException {
        Calendar calendar = null;
        try {
            // 假设 .ics 文件存放在 resources 目录下
            ClassPathResource resource = new ClassPathResource("icalendar" + System.getProperty("file.separator") + cid);
            CalendarBuilder builder = new CalendarBuilder();
            calendar = builder.build(resource.getInputStream());
            System.out.println(calendar.toString());
        } catch (IOException e) {
            throw new IOException("文件不存在");
        } catch (ParserException e) {
            throw new RuntimeException(e);
        }
        return calendar.toString();
    }
}
