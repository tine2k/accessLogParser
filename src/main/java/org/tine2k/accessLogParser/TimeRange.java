package org.tine2k.accessLogParser;

/**
 * Created by tine2k on 05/11/15.
 */
public class TimeRange {

    private long from;
    private long to;

    public TimeRange(long from, long to) {
        this.from = from;
        this.to = to;
    }

    public boolean contains(long l) {
        return l >= from && l <= to;
    }

    public long getFrom() {
        return from;
    }

    public void setFrom(long from) {
        this.from = from;
    }

    public long getTo() {
        return to;
    }

    public void setTo(long to) {
        this.to = to;
    }
}
