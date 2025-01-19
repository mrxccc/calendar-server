package com.calendar.credential;

public class FeishuCaldavCredential extends CaldavCredential {
    public FeishuCaldavCredential() {
        this.host = "caldav.feishu.cn";
        this.port = 443;
        this.protocol = "https";
        this.user = "u_hvjs0816";
        this.home = "/" + user + "/";
        this.password = "P2MTLwEoyE";
        this.collection = "";
    }
}
