package com.calendar.credential;

public class GCaldavCredential extends CaldavCredential {

    public GCaldavCredential() {
        this.host = "apidata.google.com";
        this.port = 443;
        this.protocol = "https";
        this.user = "";
        this.home = "/caldav/v2";
        this.password = "caldav4j";
        this.collection = "events/";
    }
}
