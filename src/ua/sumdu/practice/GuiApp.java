package ua.sumdu.practice;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

public class GuiApp {

    private static final String CSV_FILE = "requests.csv";
    private static final String BUNDLE_BASE_NAME = "ua.sumdu.practice.messages"; // messages_uk.properties / messages_en.properties

    private static Locale currentLocale = Locale.forLanguageTag("uk");
    private static ResourceBundle bundle = ResourceBundle.getBundle(BUNDLE_BASE_NAME, currentLocale);

    private static String t(String key) {
        try {
            return bundle.getString(key);
        } catch (Exception ex) {
            return key; // якщо ключ не знайдено — покажемо сам ключ
        }
    }

    private static String t(String key, Object... args) {
        String pattern = t(key);
        return MessageFormat.format(pattern, args);
    }

    private static void setLocale(Locale locale) {
        currentLocale = locale;
        bundle = ResourceBundle.getBundle(BUNDLE_BASE_NAME, currentLocale);
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) { }

        CsvStorage storage = new CsvStorage(CSV_FILE);

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame(t("app.title"));
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(980, 560);
            frame.setLocationRelativeTo(null);

            JPanel root = new JPanel(new BorderLayout(10, 10));
            root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            // ===== Верх: заголовок + панель пошуку + кнопка мови =====
            JPanel top = new JPanel(new BorderLayout(8, 8));

            // Заголовок + кнопка прапора
            JPanel headerRow = new JPanel(new BorderLayout());
            JLabel titleLabel = new JLabel(t("title.main"));
            titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 18f));

            JButton langBtn = new JButton("UA"); // стартуємо з укр

            langBtn.setFocusable(false);
            langBtn.setMargin(new Insets(4, 8, 4, 8));
            langBtn.setToolTipText("Українська / English");

            headerRow.add(titleLabel, BorderLayout.WEST);
            headerRow.add(langBtn, BorderLayout.EAST);

            top.add(headerRow, BorderLayout.NORTH);

            // Панель пошуку
            JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            JLabel searchLabel = new JLabel(t("search.label"));
            JTextField queryField = new JTextField(25);
            JLabel byLabel = new JLabel(t("search.by"));

            JComboBox<String> filterType = new JComboBox<>();
            JButton applyFilterBtn = new JButton(t("btn.apply"));
            JButton clearFilterBtn = new JButton(t("btn.clear"));

            searchPanel.add(searchLabel);
            searchPanel.add(queryField);
            searchPanel.add(byLabel);
            searchPanel.add(filterType);
            searchPanel.add(applyFilterBtn);
            searchPanel.add(clearFilterBtn);

            top.add(searchPanel, BorderLayout.SOUTH);
            root.add(top, BorderLayout.NORTH);

            // ===== Центр: таблиця =====
            DefaultTableModel model = new DefaultTableModel(new Object[]{}, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };

            JTable table = new JTable(model);
            table.setRowHeight(24);
            table.setAutoCreateRowSorter(true);
            root.add(new JScrollPane(table), BorderLayout.CENTER);

            // ===== Низ: кнопки =====
            JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton addBtn = new JButton(t("btn.add"));
            JButton editBtn = new JButton(t("btn.edit"));
            JButton deleteBtn = new JButton(t("btn.delete"));
            JButton refreshBtn = new JButton(t("btn.refresh"));

            bottom.add(addBtn);
            bottom.add(editBtn);
            bottom.add(deleteBtn);
            bottom.add(refreshBtn);
            root.add(bottom, BorderLayout.SOUTH);

            // ===== Дані (стан) =====
            final List<Request>[] requestsHolder = new List[]{storage.load()};

            // ===== Функція: оновити тексти інтерфейсу (мова) =====
            Runnable applyLanguage = () -> {

                langBtn.setText(currentLocale.getLanguage().equals("uk") ? "UA" : "EN");
                // Заголовки/кнопки
                frame.setTitle(t("app.title"));
                titleLabel.setText(t("title.main"));

                searchLabel.setText(t("search.label"));
                byLabel.setText(t("search.by"));
                applyFilterBtn.setText(t("btn.apply"));
                clearFilterBtn.setText(t("btn.clear"));

                addBtn.setText(t("btn.add"));
                editBtn.setText(t("btn.edit"));
                deleteBtn.setText(t("btn.delete"));
                refreshBtn.setText(t("btn.refresh"));

                // Оновлюємо варіанти фільтра (і зберігаємо попередній вибір)
                int prevIndex = filterType.getSelectedIndex();
                filterType.removeAllItems();
                filterType.addItem(t("col.number"));
                filterType.addItem(t("col.date"));
                filterType.addItem(t("col.vehicle"));
                filterType.addItem(t("col.route"));
                if (prevIndex >= 0 && prevIndex < filterType.getItemCount()) {
                    filterType.setSelectedIndex(prevIndex);
                } else {
                    filterType.setSelectedIndex(0);
                }

                // Оновлюємо назви колонок таблиці, якщо колонки вже створені
                if (table.getColumnModel().getColumnCount() >= 4) {
                    table.getColumnModel().getColumn(0).setHeaderValue(t("col.number"));
                    table.getColumnModel().getColumn(1).setHeaderValue(t("col.date"));
                    table.getColumnModel().getColumn(2).setHeaderValue(t("col.vehicle"));
                    table.getColumnModel().getColumn(3).setHeaderValue(t("col.route"));
                    table.getTableHeader().repaint();
                }

            };

            // ===== Перезавантаження таблиці з урахуванням фільтра =====
            Runnable reloadTable = () -> {
                requestsHolder[0] = storage.load();
                List<Request> requests = requestsHolder[0];

                // Якщо колонки ще не ініціалізовані — зробимо це один раз
                if (model.getColumnCount() == 0) {
                    model.setColumnIdentifiers(new Object[]{
                            t("col.number"), t("col.date"), t("col.vehicle"), t("col.route")
                    });
                }

                model.setRowCount(0);

                String q = queryField.getText().trim().toLowerCase();
                int filterIndex = filterType.getSelectedIndex(); // 0..3

                for (Request r : requests) {
                    if (!q.isEmpty()) {
                        boolean matches = switch (filterIndex) {
                            case 0 -> String.valueOf(r.getRequestNumber()).contains(q);          // номер
                            case 1 -> String.valueOf(r.getDate()).contains(q);                   // дата
                            case 2 -> r.getVehicle().toLowerCase().contains(q);                  // авто
                            case 3 -> r.getRoute().toLowerCase().contains(q);                    // маршрут
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

                // Тайтл із лічильником
                frame.setTitle(t("app.title") + " (" + t("loaded") + ": " + requests.size() + ")");
            };

            // ===== Обробники пошуку =====
            applyFilterBtn.addActionListener(ev -> reloadTable.run());
            clearFilterBtn.addActionListener(ev -> {
                queryField.setText("");
                reloadTable.run();
            });
            queryField.addActionListener(ev -> reloadTable.run()); // Enter
            refreshBtn.addActionListener(ev -> reloadTable.run());

            // ===== Кнопка мови =====
            langBtn.addActionListener(ev -> {
                if (currentLocale.getLanguage().equals("uk")) {
                    setLocale(Locale.ENGLISH);
                } else {
                    setLocale(Locale.forLanguageTag("uk"));
                }
                applyLanguage.run();
                reloadTable.run();
            });

            // ===== Видалити =====
            deleteBtn.addActionListener(ev -> {
                int selectedRow = table.getSelectedRow();
                if (selectedRow < 0) {
                    JOptionPane.showMessageDialog(frame, t("msg.selectRowDelete"), t("warn.title"), JOptionPane.WARNING_MESSAGE);
                    return;
                }

                int modelRow = table.convertRowIndexToModel(selectedRow);
                int requestNumber = (int) model.getValueAt(modelRow, 0);

                Object[] options = {t("confirm.yes"), t("confirm.no")};
                int confirm = JOptionPane.showOptionDialog(
                        frame,
                        t("confirm.delete", requestNumber),
                        t("confirm.title"),
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

            // ===== Додати =====
            addBtn.addActionListener(ev -> {
                JTextField numberField = new JTextField();
                JTextField dateField = new JTextField("2026-04-13");
                JTextField vehicleField = new JTextField();
                JTextField routeField = new JTextField();

                JPanel panel = new JPanel(new GridLayout(0, 1, 6, 6));
                panel.add(new JLabel(t("field.number")));
                panel.add(numberField);
                panel.add(new JLabel(t("field.date")));
                panel.add(dateField);
                panel.add(new JLabel(t("field.vehicle")));
                panel.add(vehicleField);
                panel.add(new JLabel(t("field.route")));
                panel.add(routeField);

                int result = JOptionPane.showConfirmDialog(
                        frame,
                        panel,
                        t("dlg.add.title"),
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.PLAIN_MESSAGE
                );

                if (result != JOptionPane.OK_OPTION) return;

                int number;
                try {
                    number = Integer.parseInt(numberField.getText().trim());
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(frame, t("err.numberInt"), t("err.title"), JOptionPane.ERROR_MESSAGE);
                    return;
                }

                String dateText = dateField.getText().trim();
                String vehicle = vehicleField.getText().trim();
                String route = routeField.getText().trim();

                if (vehicle.isEmpty() || route.isEmpty() || dateText.isEmpty()) {
                    JOptionPane.showMessageDialog(frame, t("err.empty"), t("err.title"), JOptionPane.ERROR_MESSAGE);
                    return;
                }

                LocalDate date;
                try {
                    date = LocalDate.parse(dateText);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(frame, t("err.dateFormat"), t("err.title"), JOptionPane.ERROR_MESSAGE);
                    return;
                }

                List<Request> list = requestsHolder[0];
                boolean exists = list.stream().anyMatch(r -> r.getRequestNumber() == number);
                if (exists) {
                    JOptionPane.showMessageDialog(frame, t("err.duplicate"), t("err.title"), JOptionPane.ERROR_MESSAGE);
                    return;
                }

                list.add(new Request(number, vehicle, route, date));
                storage.save(list);
                reloadTable.run();
            });

            // ===== Редагувати =====
            editBtn.addActionListener(ev -> {
                int selectedRow = table.getSelectedRow();
                if (selectedRow < 0) {
                    JOptionPane.showMessageDialog(frame, t("msg.selectRowEdit"), t("warn.title"), JOptionPane.WARNING_MESSAGE);
                    return;
                }

                int modelRow = table.convertRowIndexToModel(selectedRow);

                int oldNumber = (int) model.getValueAt(modelRow, 0);
                String oldDate = (String) model.getValueAt(modelRow, 1);
                String oldVehicle = (String) model.getValueAt(modelRow, 2);
                String oldRoute = (String) model.getValueAt(modelRow, 3);

                JTextField numberField = new JTextField(String.valueOf(oldNumber));
                JTextField dateField = new JTextField(oldDate);
                JTextField vehicleField = new JTextField(oldVehicle);
                JTextField routeField = new JTextField(oldRoute);

                JPanel panel = new JPanel(new GridLayout(0, 1, 6, 6));
                panel.add(new JLabel(t("field.number")));
                panel.add(numberField);
                panel.add(new JLabel(t("field.date")));
                panel.add(dateField);
                panel.add(new JLabel(t("field.vehicle")));
                panel.add(vehicleField);
                panel.add(new JLabel(t("field.route")));
                panel.add(routeField);

                int result = JOptionPane.showConfirmDialog(
                        frame,
                        panel,
                        t("dlg.edit.title"),
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.PLAIN_MESSAGE
                );

                if (result != JOptionPane.OK_OPTION) return;

                int newNumber;
                try {
                    newNumber = Integer.parseInt(numberField.getText().trim());
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(frame, t("err.numberInt"), t("err.title"), JOptionPane.ERROR_MESSAGE);
                    return;
                }

                String dateText = dateField.getText().trim();
                String vehicle = vehicleField.getText().trim();
                String route = routeField.getText().trim();

                if (vehicle.isEmpty() || route.isEmpty() || dateText.isEmpty()) {
                    JOptionPane.showMessageDialog(frame, t("err.empty"), t("err.title"), JOptionPane.ERROR_MESSAGE);
                    return;
                }

                LocalDate date;
                try {
                    date = LocalDate.parse(dateText);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(frame, t("err.dateFormat"), t("err.title"), JOptionPane.ERROR_MESSAGE);
                    return;
                }

                List<Request> list = requestsHolder[0];

                if (newNumber != oldNumber) {
                    boolean exists = list.stream().anyMatch(r -> r.getRequestNumber() == newNumber);
                    if (exists) {
                        JOptionPane.showMessageDialog(frame, t("err.duplicate"), t("err.title"), JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                }

                list.removeIf(r -> r.getRequestNumber() == oldNumber);
                list.add(new Request(newNumber, vehicle, route, date));

                storage.save(list);
                reloadTable.run();
            });

            // Перший запуск
            applyLanguage.run(); // ініціалізувати тексти + combo box
            reloadTable.run();

            frame.setContentPane(root);
            frame.setVisible(true);
        });
    }
}