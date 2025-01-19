package com.calendar.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.springframework.stereotype.Service;

import com.calendar.credential.BedeworkCaldavCredential;
import com.calendar.dialect.BedeworkCalDavDialect;
import com.github.caldav4j.methods.HttpDeleteMethod;
import com.github.caldav4j.methods.HttpGetMethod;
import com.github.caldav4j.methods.HttpMkCalendarMethod;
import com.github.caldav4j.methods.HttpPutMethod;
import com.github.caldav4j.model.request.CalendarRequest;

import lombok.extern.slf4j.Slf4j;
import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.CalendarOutputter;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.CalScale;
import net.fortuna.ical4j.model.property.Location;
import net.fortuna.ical4j.model.property.ProdId;
import net.fortuna.ical4j.model.property.Uid;
import net.fortuna.ical4j.model.property.Version;

@Slf4j
@Service
public class BedeworkCalendarService extends BaseCalDavService {

    public BedeworkCalendarService() {
        super(new BedeworkCaldavCredential(), new BedeworkCalDavDialect());
    }

    public String getCalendar() throws IOException {
        HttpClient httpClient = createHttpClient();
        String fullUrl =  caldavCredential.protocol + "://"  + caldavCredential.host +  ":" + caldavCredential.port + caldavCredential.home;
        CalendarBuilder calendarBuilder = new CalendarBuilder();
        HttpGetMethod getMethod = new HttpGetMethod(fullUrl, calendarBuilder);

        HttpResponse response = httpClient.execute(getMethod);
        if (response.getStatusLine().getStatusCode() == 200) {
            try (InputStream content = response.getEntity().getContent()) {
                return new String(content.readAllBytes());
            }
        } else {
            log.error("获取日历失败: {}", response.getStatusLine().getReasonPhrase());
            throw new IOException("获取日历失败: " + response.getStatusLine().getReasonPhrase());
        }
    }

    public void createCalendarEvent(String calendarPath, String summary, String location, DateTime start, DateTime end) throws IOException {
        HttpClient httpClient = createHttpClient();
        String fullUrl = caldavCredential.protocol + "://" + caldavCredential.host + ":" + caldavCredential.port + caldavCredential.home + calendarPath;

        // 创建事件
        VEvent event = new VEvent(start, end, summary);
        event.getProperties().add(new Location(location));
        event.getProperties().add(new Uid(UUID.randomUUID().toString()));

        // 创建日历
        Calendar calendar = new Calendar();
        calendar.getProperties().add(new ProdId("-//My Company//iCal4j 1.0//EN"));
        calendar.getProperties().add(Version.VERSION_2_0);
        calendar.getProperties().add(CalScale.GREGORIAN);
        calendar.getComponents().add(event);

        // 创建CalendarRequest
        CalendarRequest calendarRequest = new CalendarRequest(calendar);

        // 使用PUT方法上传事件
        CalendarOutputter outputter = new CalendarOutputter();
        HttpPutMethod putMethod = new HttpPutMethod(fullUrl + "/" + event.getUid().getValue() + ".ics", calendarRequest, outputter);

        HttpResponse response = httpClient.execute(putMethod);
        if (response.getStatusLine().getStatusCode() == 201) {
            log.info("日历事件创建成功: {}", summary);
        } else {
            log.error("创建日历事件失败: {}", response.getStatusLine().getReasonPhrase());
            throw new IOException("创建日历事件失败: " + response.getStatusLine().getReasonPhrase());
        }
    }

    public void deleteCalendarEvent(String calendarPath, String eventUid) throws IOException {
        HttpClient httpClient = createHttpClient();
        String fullUrl = caldavCredential.protocol + "://" + caldavCredential.host + ":" + caldavCredential.port + caldavCredential.home + calendarPath + "/" + eventUid + ".ics";

        // 使用DELETE方法删除事件
        HttpDeleteMethod deleteMethod = new HttpDeleteMethod(fullUrl);

        HttpResponse response = httpClient.execute(deleteMethod);
        if (response.getStatusLine().getStatusCode() == 204) {
            log.info("日历事件删除成功: {}", eventUid);
        } else {
            log.error("删除日历事件失败: {}", response.getStatusLine().getReasonPhrase());
            throw new IOException("删除日历事件失败: " + response.getStatusLine().getReasonPhrase());
        }
    }
}
