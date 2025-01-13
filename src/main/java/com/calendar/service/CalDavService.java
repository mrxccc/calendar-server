package com.calendar.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.calendar.model.CalendarEntity;
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

@Service
@Slf4j
@RequiredArgsConstructor
public class CalDavService {

    private final CalendarRepository calendarRepository;
    private final CalendarEventRepository eventRepository;

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
            File file = new File(filePath);
            try (FileOutputStream fout = new FileOutputStream(file)) {
                CalendarOutputter outputter = new CalendarOutputter();
                outputter.output(calendar, fout);
                log.info("日历已保存到文件: {}", file.getAbsolutePath());
                System.out.println("日历已保存到文件: " + file.getAbsolutePath());
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

} 