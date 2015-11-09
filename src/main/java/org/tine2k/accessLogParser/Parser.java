package org.tine2k.accessLogParser;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.time.DateUtils;

/**
 * Created by tine2k on 05/11/15.
 */
public class Parser {

    // Tomcat 8 default Pattern: %h %l %u %t &quot;%r&quot; %s %b

    private static final String DATE_PATTERN = "dd/MMM/yyyy:HH:mm:ss";
    // 0:0:0:0:0:0:0:1 - - [04/Nov/2015:16:12:47 +0100] "GET /helenos/gui/index.html HTTP/1.1" 404 1021
    private static final String REGEX = ".* - (.*) \\[(.*) \\+.*\\] \"(\\w*) (.*) .*\" (\\d*) (.*)";
    private static final Pattern P = Pattern.compile(REGEX);

    public List<LogEntry> parse(File location) {

        Locale.setDefault(Locale.ENGLISH);
        List<String> lines = null;
        try {
            lines = IOUtils.readLines(new FileInputStream(location));

            List<LogEntry> stream = lines.stream().map(s -> {
                Matcher m = P.matcher(s);
                if (!m.find()) {
                    throw new IllegalArgumentException("Row does not match: " + s);
                }
                return m;
            }).map(m -> {
                try {
                    Date date = DateUtils.parseDateStrictly(m.group(2), new String[] {DATE_PATTERN});
                    String user = m.group(1);
                    long length = Long.valueOf(m.group(6).equals("-") ? "0" : m.group(6));
                    String url = m.group(4);
                    String operation = m.group(3).equals("-") ? null : m.group(1);
                    int status = Integer.valueOf(m.group(5));

                    return new LogEntry(url, user, date, length, operation, status);
                }
                catch (ParseException e) {
                    System.out.println("Error parsing date " + m.group(2));
                    e.printStackTrace();
                    return null;
                }
            }).collect(Collectors.toList());

            return stream;

        }
        catch (IOException e) {
            throw new IllegalArgumentException("Error parsing file " + location.getName());
        }
    }
}
