/*
 * Copyright 2005 Open Source Applications Foundation
 * Copyright © 2018 Ankush Mishra, Bobby Rullo, Roberto Polli
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
package com.calendar.service;

import com.calendar.credential.CaldavCredential;
import com.calendar.dialect.CalDavDialect;
import com.calendar.dialect.ChandlerCalDavDialect;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BaseCalDavService {

    protected static final Logger log = LoggerFactory.getLogger(BaseCalDavService.class);

    protected CaldavCredential caldavCredential =
            new CaldavCredential(System.getProperty("caldav4jUri", null));

    protected CalDavDialect caldavDialect = new ChandlerCalDavDialect();

    // constructor
    public BaseCalDavService() {}

    public BaseCalDavService(CaldavCredential credential, CalDavDialect dialect) {
        this.caldavCredential = credential;
        this.caldavDialect = dialect;
    }

    public HttpClient createHttpClient() {
        return createHttpClient(this.caldavCredential);
    }

    public static HttpClient createHttpClient(CaldavCredential caldavCredential) {
        // HttpClient 4 requires a Cred providers, to be added during creation of client
        CredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(
                AuthScope.ANY,
                new UsernamePasswordCredentials(caldavCredential.user, caldavCredential.password));

        return HttpClients.custom().setDefaultCredentialsProvider(credsProvider).build();
    }
}
