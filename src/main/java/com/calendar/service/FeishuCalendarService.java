package com.calendar.service;

import com.calendar.credential.FeishuCaldavCredential;
import com.calendar.dialect.FeishuCalDavDialect;
import com.github.caldav4j.methods.HttpPropFindMethod;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.property.DavPropertyName;
import org.apache.jackrabbit.webdav.property.DavPropertyNameSet;
import org.apache.jackrabbit.webdav.xml.DomUtil;
import org.apache.jackrabbit.webdav.xml.XmlSerializable;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.StringWriter;


@Slf4j
@Service
public class FeishuCalendarService extends BaseCalDavService {

    public FeishuCalendarService() {
        super(new FeishuCaldavCredential(), new FeishuCalDavDialect());
        super.setUp();
    }


    public String generatePropfindResponse() throws IOException, DavException, ParserConfigurationException, TransformerException {
        HttpClient http = fixture.getHttpClient();
        HttpHost hostConfig = fixture.getHostConfig();
        DavPropertyNameSet props = new DavPropertyNameSet();
        props.add(DavPropertyName.DISPLAYNAME);

        HttpPropFindMethod p = new HttpPropFindMethod(fixture.getCollectionPath() , props, DavConstants.DEPTH_1);

        HttpResponse response = http.execute(hostConfig, p);
        return print_Xml(p.getResponseBodyAsMultiStatus(response));

    }

    private String print_Xml(XmlSerializable ace)
            throws TransformerException, ParserConfigurationException {
        Document document = DomUtil.createDocument();
        return ElementoString(ace.toXml(document));
    }

    private String ElementoString(Element node) throws TransformerException {
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        StreamResult result = new StreamResult(new StringWriter());
        DOMSource source = new DOMSource(node);
        transformer.transform(source, result);

        String xmlString = result.getWriter().toString();
        System.out.println(xmlString);
        return xmlString;
    }
}
