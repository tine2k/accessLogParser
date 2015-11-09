package org.tine2k.accessLogParser;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.OptionalLong;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang.math.LongRange;
import org.apache.commons.lang.time.DateFormatUtils;

public class ReportMain {

    public static void main(String[] args) {

        File directory = new File(args[0]);
        System.out.println("Looking for file in directory " + directory.getAbsolutePath());
        File[] accessLogs = directory.listFiles();

        List<String> urlExceptions = Arrays.asList(args[1].split("\\|"));
        System.out.println("Using url exceptions: " + urlExceptions);

        List<String> userExceptions = Arrays.asList(args[2].split("\\|"));
        System.out.println("Using user exceptions: " + userExceptions);

        Parser parser = new Parser();
        List<LogEntry> entries =
                Arrays.stream(accessLogs).flatMap(f -> {
                    System.out.println("Parsing file " + f.getName());
                    return parser.parse(f).stream();
                }).filter(e -> urlExceptions.stream().filter(exc -> e.getUrl().contains(exc)).count() == 0)
                        .filter(e -> !userExceptions.contains(e.getUser())) //
                        .filter(e -> e.getStatus() == 200) //
                        .collect(Collectors.toList());

        printMetric(entries, "Entries", s -> s.count());
        printMetric(entries, "URLs", s -> s.map(e -> e.getUrl()).distinct().count());
        printMetric(entries, "Users", s -> s.map(e -> e.getUser()).distinct().count());

        Map<String, List<LogEntry>> dailyUsers =
                entries.stream()
                        .collect(Collectors.groupingBy(e -> DateFormatUtils.format(e.getDate(), "dd/MMM/yyyy")));
        printMetric(entries, "Max Daily Users", dailyUsers.entrySet().stream()
                .mapToInt(listE -> listE.getValue().stream().map(e -> e.getUser()).collect(Collectors.toSet()).size())
                .max().getAsInt());

        printMetric(entries, "Avg Daily Users", dailyUsers.entrySet().stream()
                .mapToInt(listE -> listE.getValue().stream().map(e -> e.getUser()).collect(Collectors.toSet()).size())
                .average().getAsDouble());

        Map<String, List<LogEntry>> entryByUser = entries.stream().collect(groupingBy(LogEntry::getUser));
        String mostActiveUser = "";
        int maxCount = 0;
        for (Entry<String, List<LogEntry>> logEntry : entryByUser.entrySet()) {
            if (logEntry.getValue().size() > maxCount) {
                maxCount = logEntry.getValue().size();
                mostActiveUser = logEntry.getKey();
            }
        }

        printMetric(entries, "Most active", mostActiveUser);

        entries = entries.stream().filter(e -> !e.getUser().equals("-")).collect(toList());

        Map<String, List<LogEntry>> perUser = entries.stream().collect(Collectors.groupingBy(e -> e.getUser()));
        Map<String, List<List<LogEntry>>> perUserSessions = new TreeMap<>();
        for (Map.Entry<String, List<LogEntry>> entry : perUser.entrySet()) {
            Collections.sort(entry.getValue(), Comparator.comparingLong(e -> e.getDate().getTime()));
            List<List<LogEntry>> sessions = new ArrayList<>();
            List<LogEntry> lastSession = new ArrayList<>();
            long lastDate = 0;
            for (LogEntry log : entry.getValue()) {
                long date = log.getDate().getTime();
                if (date - TimeUnit.MINUTES.toMillis(10) < lastDate) {
                    lastSession.add(log);
                } else {
                    lastSession = new ArrayList<>(Arrays.asList(log));
                    sessions.add(lastSession);
                }
                lastDate = date;
            }
            perUserSessions.put(entry.getKey(), sessions);
        }

        final List<LogEntry> sessionEntries = entries;
        perUserSessions.entrySet().forEach(e -> {
            printMetric(sessionEntries, String.format("Sessions (%s)", e.getKey()), e.getValue().size());
        });

        Collection<List<List<LogEntry>>> allValues = perUserSessions.values();
        List<List<LogEntry>> sessionList = allValues.stream().flatMap(e -> e.stream()).collect(Collectors.toList());

        List<Session> sessions = sessionList.stream().map((List<LogEntry> l) -> {
            OptionalLong min = l.stream().mapToLong(e -> e.getDate().getTime()).min();
            OptionalLong max = l.stream().mapToLong(e -> e.getDate().getTime()).max();
            LongRange longRange = new LongRange(min.getAsLong(), max.getAsLong());
            return new Session(l.get(0).getUser(), longRange, new Date(min.getAsLong()), new Date(max.getAsLong()));
        }).collect(Collectors.toList());

        int totalOverlaps = 0;
        for (Session l1 : sessions) {
            int minOverlapping = 1;
            int maxOverlapping = 1;

            // min
            for (Session l2 : sessions) {
                if (l1 != l2 && l2.getLongRange().containsLong(l1.getMin().getTime())) {
                    minOverlapping++;
                }
                if (l1 != l2 && l2.getLongRange().containsLong(l1.getMax().getTime())) {
                    maxOverlapping++;
                }
            }
            totalOverlaps = Math.max(totalOverlaps, minOverlapping);
            totalOverlaps = Math.max(totalOverlaps, maxOverlapping);
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
