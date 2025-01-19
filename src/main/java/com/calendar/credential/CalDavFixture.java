/*
 * Copyright 2011 Open Source Applications Foundation
 * Copyright © 2018 Ankush Mishra, Mark Hobson, Roberto Polli
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.calendar.credential;

import com.calendar.dialect.CalDavDialect;
import com.calendar.support.HttpClientTestUtils;
import com.github.caldav4j.CalDAVConstants;
import com.github.caldav4j.methods.CalDAV4JMethodFactory;
import com.github.caldav4j.methods.HttpDeleteMethod;
import com.github.caldav4j.methods.HttpMkCalendarMethod;
import com.github.caldav4j.methods.HttpPutMethod;
import com.github.caldav4j.model.request.CalendarRequest;
import com.github.caldav4j.util.CalDAVStatus;
import com.github.caldav4j.util.UrlUtils;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.component.VEvent;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.routing.HttpRoutePlanner;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.DefaultRoutePlanner;
import org.apache.http.impl.conn.DefaultSchemePortResolver;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.apache.jackrabbit.webdav.client.methods.HttpMkcol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.calendar.support.HttpMethodCallbacks.nullCallback;

/**
 * Provides fixture support for CalDAV functional tests.
 *
 * @author <a href="mailto:markhobson@gmail.com">Mark Hobson</a>
 * @version $Id$
 */
public class CalDavFixture {
    // fields -----------------------------------------------------------------
    HttpClient httpClient;

    CalDAV4JMethodFactory methodFactory;

    String collectionPath;

    List<String> deleteOnTearDownPaths;

    CalDavDialect dialect;

    HttpHost hostConfig;

    // public methods ---------------------------------------------------------

    /**
     * Configure httpclient and eventually create the base calendar collection according to
     * CaldavDialect
     *
     * @param credential
     * @param dialect
     * @throws IOException
     */
    public void setUp(CaldavCredential credential, CalDavDialect dialect) {
        setUp(credential, dialect, false);
    }

    public void setUp(
            CaldavCredential credential, CalDavDialect dialect, boolean skipCreateCollection) {
        hostConfig = new HttpHost(credential.host, credential.port, credential.protocol);
        httpClient = configureHttpClient(credential);

        methodFactory = new CalDAV4JMethodFactory();
        collectionPath = UrlUtils.removeDoubleSlashes(credential.home + credential.collection);
        deleteOnTearDownPaths = new ArrayList<String>();
        this.dialect = dialect;
    }

    public void makeCalendar(String relativePath) throws IOException {
        // Note Google Calendar Doesn't support creating a calendar
        HttpMkCalendarMethod method = methodFactory.createMkCalendarMethod(relativePath);
        method.setHeader(
                CalDAVConstants.HEADER_CONTENT_TYPE, CalDAVConstants.CONTENT_TYPE_CALENDAR);

        executeMethod(CalDAVStatus.SC_CREATED, method, true);
    }

    public void makeCollection(String relativePath) throws IOException {
        HttpMkcol method = new HttpMkcol(relativePath);

        executeMethod(CalDAVStatus.SC_CREATED, method, true);
    }

    public void putEvent(String relativePath, VEvent event) throws IOException {
        CalendarRequest cr = new CalendarRequest();
        cr.setCalendar(event);
        HttpPutMethod method = methodFactory.createPutMethod(relativePath, cr);

        executeMethod(CalDAVStatus.SC_CREATED, method, true);
    }

    public void delete(String relativePath) throws IOException {
        HttpDeleteMethod method = new HttpDeleteMethod(relativePath);

        executeMethod(CalDAVStatus.SC_NO_CONTENT, method, false);
    }

    public void delete(String path, boolean isAbsolutePath) throws IOException {
        HttpDeleteMethod method = new HttpDeleteMethod(path);

        executeMethod(CalDAVStatus.SC_NO_CONTENT, method, false, nullCallback(), isAbsolutePath);
    }

    public HttpResponse executeMethod(
            int expectedStatus, HttpRequestBase method, boolean deleteOnTearDown)
            throws IOException {
        return executeMethod(expectedStatus, method, deleteOnTearDown, nullCallback());
    }

    public <R, M extends HttpRequestBase, E extends Exception> R executeMethod(
            int expectedStatus,
            M method,
            boolean deleteOnTearDown,
            HttpClientTestUtils.HttpMethodCallback<R, M, E> methodCallback)
            throws IOException, E {
        String relativePath = method.getURI().toString();

        // prefix path with collection path
        method.setURI(URI.create(collectionPath).resolve(method.getURI()));

        R response =
                HttpClientTestUtils.executeMethod(
                        expectedStatus, httpClient, method, methodCallback);

        if (deleteOnTearDown) {
            deleteOnTearDownPaths.add(relativePath);
        }

        return response;
    }

    public <R, M extends HttpRequestBase, E extends Exception> R executeMethod(
            int expectedStatus,
            M method,
            boolean deleteOnTearDown,
            HttpClientTestUtils.HttpMethodCallback<R, M, E> methodCallback,
            boolean absolutePath)
            throws IOException, E {
        String relativePath = method.getURI().toString();

        // prefix path with collection path
        if (!absolutePath) {
            method.setURI(URI.create(collectionPath + relativePath));
        }

        R response =
                HttpClientTestUtils.executeMethod(
                        expectedStatus, httpClient, method, methodCallback);

        if (deleteOnTearDown) {
            deleteOnTearDownPaths.add(relativePath);
        }

        return response;
    }

    // private methods --------------------------------------------------------

    private static HttpClient configureHttpClient(final CaldavCredential credential) {
        // HttpClient 4 requires a Cred providers, to be added during creation of client
        CredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(
                AuthScope.ANY,
                new UsernamePasswordCredentials(credential.user, credential.password));

        // Default Host setting
        HttpRoutePlanner routePlanner =
                new DefaultRoutePlanner(DefaultSchemePortResolver.INSTANCE) {

                    @Override
                    public HttpRoute determineRoute(
                            final HttpHost target,
                            final HttpRequest request,
                            final HttpContext context)
                            throws HttpException {
                        return super.determineRoute(
                                target != null
                                        ? target
                                        : new HttpHost(
                                                credential.host,
                                                credential.port,
                                                credential.protocol),
                                request,
                                context);
                    }
                };

        HttpClientBuilder builder =
                HttpClients.custom()
                        .setDefaultCredentialsProvider(credsProvider)
                        .setRoutePlanner(routePlanner);

        if (credential.getProxyHost() != null) {
            builder.setProxy(
                    new HttpHost(
                            credential.getProxyHost(),
                            (credential.getProxyPort() > 0) ? credential.getProxyPort() : 8080));
        }

        return builder.build();
    }

    public HttpClient getHttpClient() {
        return httpClient;
    }

    public CalDAV4JMethodFactory getMethodFactory() {
        return methodFactory;
    }

    public String getCollectionPath() {
        return collectionPath;
    }

    public List<String> getDeleteOnTearDownPaths() {
        return deleteOnTearDownPaths;
    }

    public CalDavDialect getDialect() {
        return dialect;
    }

    public HttpHost getHostConfig() {
        return hostConfig;
    }
}
