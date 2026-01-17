import java.io.*;
import java.nio.file.*;
import java.util.*;

public class Main {
  static final List<String> FIELDS = Arrays.asList("file", "rule", "result");
  static final String NUMERIC_FIELD = null;
  static final String STORE_PATH = "data/store.txt";

  static Map<String, String> parseKv(String[] items) {
    Map<String, String> record = new LinkedHashMap<>();
    for (String item : items) {
      int idx = item.indexOf('=');
      if (idx < 0) throw new IllegalArgumentException("Invalid item: " + item);
      String key = item.substring(0, idx);
      String value = item.substring(idx + 1);
      if (!FIELDS.contains(key)) throw new IllegalArgumentException("Unknown field: " + key);
      if (value.contains("|")) throw new IllegalArgumentException("Value may not contain '|' ");
      record.put(key, value);
    }
    for (String f : FIELDS) record.putIfAbsent(f, "");
    return record;
  }

  static String formatRecord(Map<String, String> values) {
    List<String> parts = new ArrayList<>();
    for (String k : FIELDS) parts.add(k + "=" + values.getOrDefault(k, ""));
    return String.join("|", parts);
  }

  static Map<String, String> parseLine(String line) {
    Map<String, String> values = new LinkedHashMap<>();
    for (String part : line.trim().split("\|")) {
      if (part.isEmpty()) continue;
      int idx = part.indexOf('=');
      if (idx < 0) throw new IllegalArgumentException("Bad part: " + part);
      values.put(part.substring(0, idx), part.substring(idx + 1));
    }
    return values;
  }

  static List<Map<String, String>> loadRecords() throws IOException {
    Path p = Paths.get(STORE_PATH);
    if (!Files.exists(p)) return new ArrayList<>();
    List<Map<String, String>> records = new ArrayList<>();
    for (String line : Files.readAllLines(p)) {
      if (!line.trim().isEmpty()) records.add(parseLine(line));
    }
    return records;
  }

  static void appendRecord(Map<String, String> values) throws IOException {
    Files.createDirectories(Paths.get("data"));
    Files.write(Paths.get(STORE_PATH), Arrays.asList(formatRecord(values)), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
  }

  static String summary(List<Map<String, String>> records) {
    int count = records.size();
    if (NUMERIC_FIELD == null) return "count=" + count;
    long total = 0;
    for (Map<String, String> r : records) {
      try { total += Long.parseLong(r.getOrDefault(NUMERIC_FIELD, "0")); } catch (NumberFormatException ignored) {}
    }
    return "count=" + count + ", " + NUMERIC_FIELD + "_total=" + total;
  }

  static void usage() {
    System.out.println("Usage: init | add key=value... | list | summary");
  }

  public static void main(String[] args) throws Exception {
    if (args.length == 0) { usage(); return; }
    String cmd = args[0];
    String[] rest = Arrays.copyOfRange(args, 1, args.length);
    switch (cmd) {
      case "init":
        Files.createDirectories(Paths.get("data"));
        Files.write(Paths.get(STORE_PATH), new byte[0], StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        break;
      case "add":
        appendRecord(parseKv(rest));
        break;
      case "list":
        for (Map<String, String> r : loadRecords()) System.out.println(formatRecord(r));
        break;
      case "summary":
        System.out.println(summary(loadRecords()));
        break;
      default:
        System.err.println("Unknown command: " + cmd);
        usage();
    }
  }
}
