package com.calendar.service;

import com.calendar.credential.BedeworkCaldavCredential;
import com.calendar.credential.CaldavCredential;
import com.calendar.dialect.BedeworkCalDavDialect;
import com.calendar.dialect.CalDavDialect;
import com.calendar.dialect.ChandlerCalDavDialect;

import java.util.Arrays;
import java.util.Collection;

public class CalDAVCollectionService extends BaseCalDavService {

    public CalDAVCollectionService(CaldavCredential credential, CalDavDialect dialect) {
        this.caldavCredential = credential;
        this.caldavDialect = dialect;
    }

    public static Collection<Object[]> getCaldavCredentials() {
        return Arrays.asList(
                new Object[][] {
                        {new CaldavCredential(), new ChandlerCalDavDialect()}
                });
    }
}
