package ua.sumdu.practice;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.util.List;

public class GuiApp {

    private static final String CSV_FILE = "requests.csv";

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) { }

        CsvStorage storage = new CsvStorage(CSV_FILE);

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("TransportRequests — облік заявок");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(950, 520);
            frame.setLocationRelativeTo(null);

            JPanel root = new JPanel(new BorderLayout(10, 10));
            root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            // ===== Верх: заголовок + пошук =====
            JPanel top = new JPanel(new BorderLayout(8, 8));

            JLabel title = new JLabel("Реєстр заявок на автоперевезення");
            title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
            top.add(title, BorderLayout.NORTH);

            JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            JComboBox<String> filterType = new JComboBox<>(new String[]{"Номер", "Дата", "Авто", "Маршрут"});
            JTextField queryField = new JTextField(25);
            JButton applyFilterBtn = new JButton("Застосувати");
            JButton clearFilterBtn = new JButton("Очистити");

            searchPanel.add(new JLabel("Пошук:"));
            searchPanel.add(queryField);
            searchPanel.add(new JLabel("по"));
            searchPanel.add(filterType);
            searchPanel.add(applyFilterBtn);
            searchPanel.add(clearFilterBtn);

            top.add(searchPanel, BorderLayout.SOUTH);
            root.add(top, BorderLayout.NORTH);

            // ===== Центр: таблиця =====
            DefaultTableModel model = new DefaultTableModel(
                    new Object[]{"Номер", "Дата", "Авто", "Маршрут"}, 0
            ) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };

            JTable table = new JTable(model);
            table.setAutoCreateRowSorter(true);
            table.setRowHeight(24);
            root.add(new JScrollPane(table), BorderLayout.CENTER);

            // ===== Низ: кнопки =====
            JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton addBtn = new JButton("Додати");
            JButton editBtn = new JButton("Редагувати");
            JButton deleteBtn = new JButton("Видалити");
            JButton refreshBtn = new JButton("Оновити");

            bottom.add(addBtn);
            bottom.add(editBtn);
            bottom.add(deleteBtn);
            bottom.add(refreshBtn);
            root.add(bottom, BorderLayout.SOUTH);

            // ===== Дані (стан) =====
            final List<Request>[] requestsHolder = new List[]{storage.load()};

            // ===== Перезавантаження таблиці з урахуванням фільтра =====
            Runnable reloadTable = () -> {
                requestsHolder[0] = storage.load();
                List<Request> requests = requestsHolder[0];

                model.setRowCount(0);

                String q = queryField.getText().trim().toLowerCase();
                String type = (String) filterType.getSelectedItem();

                for (Request r : requests) {
                    if (!q.isEmpty()) {
                        boolean matches = switch (type) {
                            case "Номер" -> String.valueOf(r.getRequestNumber()).contains(q);
                            case "Дата" -> String.valueOf(r.getDate()).contains(q);
                            case "Авто" -> r.getVehicle().toLowerCase().contains(q);
                            case "Маршрут" -> r.getRoute().toLowerCase().contains(q);
                            default -> true;
                        };
                        if (!matches) continue;
                    }

                    model.addRow(new Object[]{
                            r.getRequestNumber(),
                            String.valueOf(r.getDate()),
                            r.getVehicle(),
                            r.getRoute()
                    });
                }

                frame.setTitle("TransportRequests — облік заявок (завантажено: " + requests.size() + ")");
            };

            // ===== Обробники пошуку =====
            applyFilterBtn.addActionListener(ev -> reloadTable.run());
            clearFilterBtn.addActionListener(ev -> {
                queryField.setText("");
                reloadTable.run();
            });
            queryField.addActionListener(ev -> reloadTable.run()); // Enter

            // ===== Кнопка "Оновити" =====
            refreshBtn.addActionListener(ev -> reloadTable.run());

            // ===== Кнопка "Видалити" =====
            deleteBtn.addActionListener(ev -> {
                int selectedRow = table.getSelectedRow();
                if (selectedRow < 0) {
                    JOptionPane.showMessageDialog(frame, "Оберіть рядок для видалення.", "Увага", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                int requestNumber = (int) model.getValueAt(selectedRow, 0);

                Object[] options = {"Так", "Ні"};
                int confirm = JOptionPane.showOptionDialog(
                        frame,
                        "Видалити заявку №" + requestNumber + "?",
                        "Підтвердження",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        options,
                        options[1]
                );

                if (confirm != JOptionPane.YES_OPTION) return;

                List<Request> list = requestsHolder[0];
                list.removeIf(r -> r.getRequestNumber() == requestNumber);
                storage.save(list);
                reloadTable.run();
            });

            // ===== Кнопка "Додати" =====
            addBtn.addActionListener(ev -> {
                JTextField numberField = new JTextField();
                JTextField dateField = new JTextField("2026-04-13");
                JTextField vehicleField = new JTextField();
                JTextField routeField = new JTextField();

                JPanel panel = new JPanel(new GridLayout(0, 1, 6, 6));
                panel.add(new JLabel("Номер заявки (ціле число):"));
                panel.add(numberField);
                panel.add(new JLabel("Дата (yyyy-mm-dd):"));
                panel.add(dateField);
                panel.add(new JLabel("Авто:"));
                panel.add(vehicleField);
                panel.add(new JLabel("Маршрут:"));
                panel.add(routeField);

                int result = JOptionPane.showConfirmDialog(
                        frame,
                        panel,
                        "Додати заявку",
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.PLAIN_MESSAGE
                );

                if (result != JOptionPane.OK_OPTION) return;

                int number;
                try {
                    number = Integer.parseInt(numberField.getText().trim());
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(frame, "Номер заявки має бути цілим числом.", "Помилка", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                String dateText = dateField.getText().trim();
                String vehicle = vehicleField.getText().trim();
                String route = routeField.getText().trim();

                if (vehicle.isEmpty() || route.isEmpty() || dateText.isEmpty()) {
                    JOptionPane.showMessageDialog(frame, "Дата, авто та маршрут не можуть бути порожніми.", "Помилка", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                LocalDate date;
                try {
                    date = LocalDate.parse(dateText);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(frame, "Дата має бути у форматі yyyy-mm-dd.", "Помилка", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                List<Request> list = requestsHolder[0];
                boolean exists = list.stream().anyMatch(r -> r.getRequestNumber() == number);
                if (exists) {
                    JOptionPane.showMessageDialog(frame, "Заявка з таким номером вже існує.", "Помилка", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                list.add(new Request(number, vehicle, route, date));
                storage.save(list);
                reloadTable.run();
            });

            // ===== Кнопка "Редагувати" =====
            editBtn.addActionListener(ev -> {
                int selectedRow = table.getSelectedRow();
                if (selectedRow < 0) {
                    JOptionPane.showMessageDialog(frame, "Оберіть рядок для редагування.", "Увага", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                int oldNumber = (int) model.getValueAt(selectedRow, 0);
                String oldDate = (String) model.getValueAt(selectedRow, 1);
                String oldVehicle = (String) model.getValueAt(selectedRow, 2);
                String oldRoute = (String) model.getValueAt(selectedRow, 3);

                JTextField numberField = new JTextField(String.valueOf(oldNumber));
                JTextField dateField = new JTextField(oldDate);
                JTextField vehicleField = new JTextField(oldVehicle);
                JTextField routeField = new JTextField(oldRoute);

                JPanel panel = new JPanel(new GridLayout(0, 1, 6, 6));
                panel.add(new JLabel("Номер заявки (ціле число):"));
                panel.add(numberField);
                panel.add(new JLabel("Дата (yyyy-mm-dd):"));
                panel.add(dateField);
                panel.add(new JLabel("Авто:"));
                panel.add(vehicleField);
                panel.add(new JLabel("Маршрут:"));
                panel.add(routeField);

                int result = JOptionPane.showConfirmDialog(
                        frame,
                        panel,
                        "Редагувати заявку",
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.PLAIN_MESSAGE
                );

                if (result != JOptionPane.OK_OPTION) return;

                int newNumber;
                try {
                    newNumber = Integer.parseInt(numberField.getText().trim());
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(frame, "Номер заявки має бути цілим числом.", "Помилка", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                String dateText = dateField.getText().trim();
                String vehicle = vehicleField.getText().trim();
                String route = routeField.getText().trim();

                if (vehicle.isEmpty() || route.isEmpty() || dateText.isEmpty()) {
                    JOptionPane.showMessageDialog(frame, "Дата, авто та маршрут не можуть бути порожніми.", "Помилка", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                LocalDate date;
                try {
                    date = LocalDate.parse(dateText);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(frame, "Дата має бути у форматі yyyy-mm-dd.", "Помилка", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                List<Request> list = requestsHolder[0];

                if (newNumber != oldNumber) {
                    boolean exists = list.stream().anyMatch(r -> r.getRequestNumber() == newNumber);
                    if (exists) {
                        JOptionPane.showMessageDialog(frame, "Заявка з таким номером вже існує.", "Помилка", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                }

                list.removeIf(r -> r.getRequestNumber() == oldNumber);
                list.add(new Request(newNumber, vehicle, route, date));

                storage.save(list);
                reloadTable.run();
            });

            // Перший автозапуск
            reloadTable.run();

            frame.setContentPane(root);
            frame.setVisible(true);
        });
    }
}