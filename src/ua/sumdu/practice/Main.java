package ua.sumdu.practice;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;


public class Main {
    private static final String CSV_FILE = "requests.csv";

    public static void main(String[] args) {
        List<Request> requests = new ArrayList<>();
        Scanner scanner = new Scanner(System.in);

        loadFromCsv(requests);

        while (true) {
            System.out.println("\n=== МЕНЮ ===");
            System.out.println("1) Додати заявку");
            System.out.println("2) Показати всі заявки");
            System.out.println("3) Пошук");
            System.out.println("4) Видалити заявку");
            System.out.println("0) Вихід");
            System.out.print("Ваш вибір: ");

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1" -> addRequest(requests, scanner);
                case "2" -> printAllRequests(requests);
                case "3" -> searchMenu(requests, scanner);
                case "4" -> deleteRequestByNumber(requests, scanner);
                case "0" -> {
                    System.out.println("Вихід з програми.");
                    return;
                }
                default -> System.out.println("Невірний вибір. Спробуйте ще раз.");
            }
        }
    }

    // ===== ДОДАВАННЯ ЗАЯВКИ =====
    private static void addRequest(List<Request> requests, Scanner scanner) {
        System.out.println("\n--- Додавання заявки ---");

        int requestNumber;
        while (true) {
            requestNumber = readInt(scanner, "Введіть номер заявки (ціле число): ");
            if (existsRequestNumber(requests, requestNumber)) {
                System.out.println("Помилка: заявка з таким номером уже існує. Введіть інший номер.");
            } else {
                break;
            }
        }

        String vehicle = readNonEmpty(scanner, "Введіть авто (наприклад: DAF XF / Sprinter): ");
        String route = readNonEmpty(scanner, "Введіть маршрут (наприклад: Суми -> Київ): ");
        LocalDate date = readDate(scanner, "Введіть дату (yyyy-mm-dd), наприклад 2025-07-15: ");

        Request request = new Request(requestNumber, vehicle, route, date);
        requests.add(request);
        saveToCsv(requests);

        System.out.println("✅ Заявку додано успішно!");
    }

    private static boolean existsRequestNumber(List<Request> requests, int requestNumber) {
        for (Request r : requests) {
            if (r.getRequestNumber() == requestNumber) {
                return true;
            }
        }
        return false;
    }

    // ===== ВИВІД ТАБЛИЦЕЮ =====
    private static void printAllRequests(List<Request> requests) {
        System.out.println("\n--- Усі заявки ---");

        if (requests.isEmpty()) {
            System.out.println("Список порожній. Додайте хоча б одну заявку.");
            return;
        }

        // ширини колонок
        int wNum = 12;
        int wDate = 12;
        int wVehicle = 22;
        int wRoute = 40;

        String line = "+" + "-".repeat(wNum + 2)
                + "+" + "-".repeat(wDate + 2)
                + "+" + "-".repeat(wVehicle + 2)
                + "+" + "-".repeat(wRoute + 2)
                + "+";

        System.out.println(line);
        System.out.printf("| %-" + wNum + "s | %-" + wDate + "s | %-" + wVehicle + "s | %-" + wRoute + "s |%n",
                "Номер", "Дата", "Авто", "Маршрут");
        System.out.println(line);

        for (Request r : requests) {
            String num = String.valueOf(r.getRequestNumber());
            String date = String.valueOf(r.getDate());
            String vehicle = cut(r.getVehicle(), wVehicle);
            String route = cut(r.getRoute(), wRoute);

            System.out.printf("| %-" + wNum + "s | %-" + wDate + "s | %-" + wVehicle + "s | %-" + wRoute + "s |%n",
                    num, date, vehicle, route);
        }

        System.out.println(line);
    }

    private static String cut(String text, int maxLen) {
        if (text == null) return "";
        text = text.trim();
        if (text.length() <= maxLen) return text;
        return text.substring(0, Math.max(0, maxLen - 3)) + "...";
    }

    // ===== ПОШУК =====
    private static void searchMenu(List<Request> requests, Scanner scanner) {
        while (true) {
            System.out.println("\n--- Пошук ---");
            System.out.println("1) За номером заявки");
            System.out.println("2) За датою");
            System.out.println("3) За авто (частина назви)");
            System.out.println("0) Назад");
            System.out.print("Ваш вибір: ");

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1" -> searchByNumber(requests, scanner);
                case "2" -> searchByDate(requests, scanner);
                case "3" -> searchByVehicle(requests, scanner);
                case "0" -> {
                    return;
                }
                default -> System.out.println("Невірний вибір. Спробуйте ще раз.");
            }
        }
    }

    private static void searchByNumber(List<Request> requests, Scanner scanner) {
        System.out.println("\nПошук за номером заявки");
        int number = readInt(scanner, "Введіть номер: ");

        for (Request r : requests) {
            if (r.getRequestNumber() == number) {
                System.out.println("✅ Знайдено:");
                printSingle(r);
                return;
            }
        }

        System.out.println("❌ Заявку з таким номером не знайдено.");
    }

    private static void searchByDate(List<Request> requests, Scanner scanner) {
        System.out.println("\nПошук за датою");
        LocalDate date = readDate(scanner, "Введіть дату (yyyy-mm-dd): ");

        boolean found = false;
        for (Request r : requests) {
            if (r.getDate().equals(date)) {
                if (!found) {
                    System.out.println("✅ Знайдені заявки:");
                }
                found = true;
                printSingle(r);
            }
        }

        if (!found) {
            System.out.println("❌ На цю дату заявок не знайдено.");
        }
    }

    private static void searchByVehicle(List<Request> requests, Scanner scanner) {
        System.out.println("\nПошук за авто");
        String q = readNonEmpty(scanner, "Введіть частину назви авто: ").toLowerCase();

        boolean found = false;
        for (Request r : requests) {
            if (r.getVehicle().toLowerCase().contains(q)) {
                if (!found) {
                    System.out.println("✅ Знайдені заявки:");
                }
                found = true;
                printSingle(r);
            }
        }

        if (!found) {
            System.out.println("❌ За цим авто нічого не знайдено.");
        }
    }

    private static void printSingle(Request r) {
        System.out.println("------------------------------------");
        System.out.println("Номер:   " + r.getRequestNumber());
        System.out.println("Дата:    " + r.getDate());
        System.out.println("Авто:    " + r.getVehicle());
        System.out.println("Маршрут: " + r.getRoute());
        System.out.println("------------------------------------");
    }
    // ===== ВИДАЛЕННЯ ЗАПИСІВ ====
    private static void deleteRequestByNumber(List<Request> requests, Scanner scanner) {
        System.out.println("\n--- Видалення заявки ---");

        if (requests.isEmpty()) {
            System.out.println("Список порожній. Немає що видаляти.");
            return;
        }

        int number = readInt(scanner, "Введіть номер заявки для видалення: ");

        // знайдемо заявку
        Request target = null;
        for (Request r : requests) {
            if (r.getRequestNumber() == number) {
                target = r;
                break;
            }
        }

        if (target == null) {
            System.out.println("❌ Заявку з таким номером не знайдено.");
            return;
        }

        System.out.println("Знайдено заявку:");
        printSingle(target);

        while (true) {
            System.out.print("Видалити цю заявку? (Y/N): ");
            String answer = scanner.nextLine().trim();

            if (answer.equalsIgnoreCase("Y")) {
                requests.remove(target);
                saveToCsv(requests);
                System.out.println("✅ Заявку видалено.");
                return;
            } else if (answer.equalsIgnoreCase("N")) {
                System.out.println("Скасовано. Заявку НЕ видалено.");
                return;
            } else {
                System.out.println("Будь ласка, введіть тільки Y або N.");
            }
        }
    }
    // ===== МЕТОДИ ЗБЕРЕЖЕННЯВ CSV =====
    private static void saveToCsv(List<Request> requests) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(CSV_FILE))) {
            // заголовок
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
    // ===== МЕТОДИ ЗАВАНТАЖЕННЯ З CSV =====
    private static void loadFromCsv(List<Request> requests) {
        File file = new File(CSV_FILE);
        if (!file.exists()) {
            // якщо файлу нема — нічого не завантажуємо
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            boolean firstLine = true;

            while ((line = reader.readLine()) != null) {
                if (firstLine) {
                    firstLine = false; // пропускаємо заголовок
                    continue;
                }

                if (line.trim().isEmpty()) continue;

                Request r = parseCsvLineToRequest(line);
                if (r != null) {
                    // захист від дублікатів по номеру
                    if (!existsRequestNumber(requests, r.getRequestNumber())) {
                        requests.add(r);
                    }
                }
            }

            if (!requests.isEmpty()) {
                System.out.println("ℹ Завантажено заявок з CSV: " + requests.size());
            }
        } catch (IOException e) {
            System.out.println("⚠ Помилка читання CSV: " + e.getMessage());
        }
    }
    // ===== Допоміжні методи для CSV =====
    private static String csvEscape(String text) {
        if (text == null) return "";
        // якщо є коми/лапки/переноси — беремо в лапки і подвоюємо лапки
        boolean mustQuote = text.contains(",") || text.contains("\"") || text.contains("\n") || text.contains("\r");
        String escaped = text.replace("\"", "\"\"");
        return mustQuote ? "\"" + escaped + "\"" : escaped;
    }

    private static Request parseCsvLineToRequest(String line) {
        // Розбираємо CSV рядок на 4 поля з підтримкою лапок
        List<String> parts = splitCsv(line);
        if (parts.size() < 4) return null;

        try {
            int number = Integer.parseInt(parts.get(0).trim());
            LocalDate date = LocalDate.parse(parts.get(1).trim());
            String vehicle = parts.get(2);
            String route = parts.get(3);

            return new Request(number, vehicle, route, date);
        } catch (Exception e) {
            // якщо якийсь рядок кривий — пропускаємо
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
                // якщо подвійні лапки "" усередині quoted-рядка — це одна лапка
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

    // ===== ДОПОМІЖНІ ФУНКЦІЇ ВВОДУ =====
    private static String readNonEmpty(Scanner scanner, String prompt) {
        while (true) {
            System.out.print(prompt);
            String s = scanner.nextLine().trim();
            if (!s.isEmpty()) return s;
            System.out.println("Помилка: поле не може бути порожнім.");
        }
    }

    private static int readInt(Scanner scanner, String prompt) {
        while (true) {
            System.out.print(prompt);
            String s = scanner.nextLine().trim();
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException ex) {
                System.out.println("Помилка: введіть ціле число (наприклад 101).");
            }
        }
    }

    private static LocalDate readDate(Scanner scanner, String prompt) {
        while (true) {
            System.out.print(prompt);
            String s = scanner.nextLine().trim();
            try {
                return LocalDate.parse(s); // формат yyyy-mm-dd
            } catch (DateTimeParseException ex) {
                System.out.println("Помилка: дата має бути у форматі yyyy-mm-dd (наприклад 2025-07-15).");
            }
        }
    }
}
