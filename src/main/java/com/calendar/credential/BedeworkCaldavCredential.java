package com.calendar.credential;

public class BedeworkCaldavCredential extends CaldavCredential {
    public BedeworkCaldavCredential() {
        this.host = "localhost";
        this.port = 8081;
        this.protocol = "http";
        this.user = "vbede";
        this.home = "/ucaldav/user/" + this.user + "/";
        this.password = "bedework";
        this.collection = "collection/";
    }
}
