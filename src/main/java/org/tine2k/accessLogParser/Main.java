package org.tine2k.accessLogParser;

import org.apache.commons.lang.math.LongRange;
import org.apache.commons.lang.time.DateFormatUtils;

import java.io.File;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Main {

    public static void main(String[] args) {

        File directory = new File(args[0]);
        System.out.println("Looking for file in directory " + directory.getAbsolutePath());
        File[] accessLogs = directory.listFiles((f, name) -> name.startsWith("access_log"));

        Parser parser = new Parser();
        List<LogEntry> entries = Arrays.stream(accessLogs).flatMap(f -> parser.parse(f).stream()).collect(Collectors.toList());

        printMetric(entries, "Einträge", s -> s.count());
        printMetric(entries, "URLs", s -> s.map(e -> e.getUrl()).distinct().count());
        printMetric(entries, "Users", s -> s.map(e -> e.getUser()).distinct().count());

        Map<String, List<LogEntry>> dailyUsers = entries.stream().collect(Collectors.groupingBy(e -> DateFormatUtils.format(e.getDate(), "dd/MMM/yyyy")));
        printMetric(entries, "Max Daily Users", dailyUsers.entrySet().stream().mapToInt(listE -> listE.getValue().stream().map(e -> e.getUser()).collect(Collectors.toSet()).size()).max().getAsInt());

        Map<String, List<LogEntry>> perUser = entries.stream().collect(Collectors.groupingBy(e -> e.getUser()));
        Map<String, List<List<LogEntry>>> perUserSessions = new HashMap<>();
        for (Map.Entry<String, List<LogEntry>> entry : perUser.entrySet()) {
            Collections.sort(entry.getValue(), Comparator.comparingLong(e -> e.getDate().getTime()));
            List<List<LogEntry>> sessions = new ArrayList<>();
            List<LogEntry> lastSession = new ArrayList<>();
            long lastDate = 0;
            for (LogEntry log : entry.getValue()) {
                long date = log.getDate().getTime();
                if (date - TimeUnit.MINUTES.toMillis(20) < lastDate) {
                    lastSession.add(log);
                } else {
                    lastSession = new ArrayList(Arrays.asList(log));
                    sessions.add(lastSession);
                }
                lastDate = log.getDate().getTime();
            }
            perUserSessions.put(entry.getKey(), sessions);
        }

        perUserSessions.entrySet().forEach(e -> {
            printMetric(entries, String.format("Sessions (%s)", e.getKey()), e.getValue().size());
        });

        Collection<List<List<LogEntry>>> allValues = perUserSessions.values();
        List<List<LogEntry>> sessionList = allValues.stream().flatMap(e -> e.stream()).collect(Collectors.toList());
        List<LongRange> sessions = sessionList.stream().map((List<LogEntry> l) -> {
            OptionalLong min = l.stream().mapToLong(e -> e.getDate().getTime()).min();
            OptionalLong max = l.stream().mapToLong(e -> e.getDate().getTime()).max();
            return new LongRange(min.getAsLong(), max.getAsLong());
        }).collect(Collectors.toList());

        int totalOverlaps = 0;
        for (LongRange l1 : sessions) {
            int overlapping = 1;
            for (LongRange l2 : sessions) {
                if (l1 != l2 && l1.overlapsRange(l2)) {
                    overlapping++;
                }
            }
            totalOverlaps = Math.max(totalOverlaps, overlapping);
        }

        printMetric(entries, "Max Overlapping", totalOverlaps);
    }

    private static <T> void printMetric(List<LogEntry> entries, String label, Function<Stream<LogEntry>, T> function) {
        printMetric(entries, label, function.apply(entries.stream()));
    }

    private static <T> void printMetric(List<LogEntry> entries, String label, Object value) {
        System.out.println(String.format("%10s: %s", label, "" + value));
    }
}
