package org.tine2k.accessLogParser;

import java.util.Date;

import org.apache.commons.lang.math.LongRange;

public class Session {

    private String user;
    private LongRange longRange;
    private Date min;
    private Date max;

    public Session(String user, LongRange longRange, Date min, Date max) {
        super();
        this.user = user;
        this.longRange = longRange;
        this.min = min;
        this.max = max;
    }

    public Date getMax() {
        return max;
    }

    public Date getMin() {
        return min;
    }

    public String getUser() {
        return user;
    }

    public LongRange getLongRange() {
        return longRange;
    }

    @Override
    public String toString() {
        return String.format("Session [user=%s, longRange=%s, min=%s, max=%s]", user, longRange, min, max);
    }

}