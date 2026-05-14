package ua.sumdu.practice;

import java.io.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class CsvStorage {
    private final String fileName;

    public CsvStorage(String fileName) {
        this.fileName = fileName;
    }

    public List<Request> load() {
        List<Request> requests = new ArrayList<>();
        File file = new File(fileName);
        if (!file.exists()) return requests;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            boolean firstLine = true;

            while ((line = reader.readLine()) != null) {
                if (firstLine) { firstLine = false; continue; } // заголовок
                if (line.trim().isEmpty()) continue;

                Request r = parseCsvLineToRequest(line);
                if (r != null && !existsRequestNumber(requests, r.getRequestNumber())) {
                    requests.add(r);
                }
            }
        } catch (IOException e) {
            System.out.println("⚠ Помилка читання CSV: " + e.getMessage());
        }

        return requests;
    }

    public void save(List<Request> requests) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
            writer.write("requestNumber,date,vehicle,route");
            writer.newLine();

            for (Request r : requests) {
                String line = r.getRequestNumber() + ","
                        + r.getDate() + ","
                        + csvEscape(r.getVehicle()) + ","
                        + csvEscape(r.getRoute());
                writer.write(line);
                writer.newLine();
            }
        } catch (IOException e) {
            System.out.println("⚠ Помилка збереження CSV: " + e.getMessage());
        }
    }

    private static boolean existsRequestNumber(List<Request> requests, int requestNumber) {
        for (Request r : requests) {
            if (r.getRequestNumber() == requestNumber) return true;
        }
        return false;
    }

    private static String csvEscape(String text) {
        if (text == null) return "";
        boolean mustQuote = text.contains(",") || text.contains("\"") || text.contains("\n") || text.contains("\r");
        String escaped = text.replace("\"", "\"\"");
        return mustQuote ? "\"" + escaped + "\"" : escaped;
    }

    private static Request parseCsvLineToRequest(String line) {
        List<String> parts = splitCsv(line);
        if (parts.size() < 4) return null;

        try {
            int number = Integer.parseInt(parts.get(0).trim());
            LocalDate date = LocalDate.parse(parts.get(1).trim());
            String vehicle = parts.get(2);
            String route = parts.get(3);
            return new Request(number, vehicle, route, date);
        } catch (Exception e) {
            return null;
        }
    }

    private static List<String> splitCsv(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                result.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        result.add(current.toString());
        return result;
    }
}