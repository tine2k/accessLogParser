package org.tine2k.accessLogParser;

import java.util.Date;

/**
 * Created by tine2k on 05/11/15.
 */
public class LogEntry {

    private final String url;
    private final Date date;
    private final long contentLength;
    private final String operation;
    private final String user;
    private final int status;

    public LogEntry(String url, String user, Date date, long contentLength, String operation, int status) {
        this.url = url;
        this.user = user;
        this.date = date;
        this.contentLength = contentLength;
        this.operation = operation;
        this.status = status;
    }

    public int getStatus() {
        return status;
    }

    public String getUser() {
        return user;
    }

    public String getUrl() {
        return url;
    }

    public Date getDate() {
        return date;
    }

    public long getContentLength() {
        return contentLength;
    }

    public String getOperation() {
        return operation;
    }
}
