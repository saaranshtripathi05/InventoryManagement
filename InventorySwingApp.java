import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

/* ========= SIMPLE AUTH ========= */
class AuthService {
    // In-memory user store; replace with DB later
    private final Map<String, String> users = new HashMap<>();

    public AuthService() {
        users.put("admin", "admin123");
        users.put("manager", "manager@123");
        users.put("clerk", "clerk@123");
    }

    public boolean authenticate(String username, String password) {
        if (username == null || password == null) return false;
        String stored = users.get(username.trim());
        return stored != null && stored.equals(password);
    }
}

/* ========= LOGIN DIALOG ========= */
class LoginDialog extends JDialog {
    private boolean authenticated = false;
    private final JTextField userField = new JTextField(16);
    private final JPasswordField passField = new JPasswordField(16);

    public LoginDialog(Frame owner, AuthService auth) {
        super(owner, "Login - Inventory Management", true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel content = new JPanel(new BorderLayout(10, 10));
        content.setBorder(new EmptyBorder(16, 16, 16, 16));

        JLabel title = new JLabel("Please sign in");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(8, 8, 8, 8);
        g.fill = GridBagConstraints.HORIZONTAL;

        int r = 0;
        g.gridx = 0; g.gridy = r; form.add(new JLabel("Username"), g);
        g.gridx = 1; form.add(userField, g); r++;
        g.gridx = 0; g.gridy = r; form.add(new JLabel("Password"), g);
        g.gridx = 1; form.add(passField, g);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton loginBtn = new JButton("Login");
        JButton cancelBtn = new JButton("Cancel");
        buttons.add(cancelBtn);
        buttons.add(loginBtn);

        loginBtn.addActionListener((ActionEvent e) -> {
            String u = userField.getText().trim();
            String p = new String(passField.getPassword());
            if (auth.authenticate(u, p)) {
                authenticated = true;
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Invalid username or password.", "Login Failed", JOptionPane.ERROR_MESSAGE);
                passField.setText("");
                passField.requestFocusInWindow();
            }
        });

        cancelBtn.addActionListener(e -> {
            authenticated = false;
            dispose();
        });

        content.add(title, BorderLayout.NORTH);
        content.add(form, BorderLayout.CENTER);
        content.add(buttons, BorderLayout.SOUTH);

        setContentPane(content);
        pack();
        setResizable(false);
        setLocationRelativeTo(owner);
        // Enter to submit
        getRootPane().setDefaultButton(loginBtn);
    }

    public boolean isAuthenticated() { return authenticated; }
}

/* ========= DOMAIN MODEL ========= */
class Product {
    private String productId;
    private String name;
    private String category;
    private double price;
    private int quantity;
    private int minStockLevel;
    private LocalDateTime lastUpdated;

    public Product(String productId, String name, String category, double price, int quantity, int minStockLevel) {
        this.productId = productId;
        this.name = name;
        this.category = category;
        this.price = price;
        this.quantity = quantity;
        this.minStockLevel = minStockLevel;
        this.lastUpdated = LocalDateTime.now();
    }
    public String getProductId() { return productId; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public double getPrice() { return price; }
    public int getQuantity() { return quantity; }
    public int getMinStockLevel() { return minStockLevel; }
    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public void setName(String name) { this.name = name; touch(); }
    public void setCategory(String category) { this.category = category; touch(); }
    public void setPrice(double price) { this.price = price; touch(); }
    public void setQuantity(int quantity) { this.quantity = quantity; touch(); }
    public void setMinStockLevel(int minStockLevel) { this.minStockLevel = minStockLevel; touch(); }
    public boolean isLowStock() { return quantity <= minStockLevel; }
    public double getTotalValue() { return price * quantity; }
    private void touch() { this.lastUpdated = LocalDateTime.now(); }
}

class Transaction {
    private final String transactionId;
    private final String productId;
    private final String type; // IN / OUT
    private final int quantity;
    private final LocalDateTime timestamp;
    private final String reason;
    public Transaction(String transactionId, String productId, String type, int quantity, String reason) {
        this.transactionId = transactionId;
        this.productId = productId;
        this.type = type;
        this.quantity = quantity;
        this.reason = reason;
        this.timestamp = LocalDateTime.now();
    }
    public String getTransactionId() { return transactionId; }
    public String getProductId() { return productId; }
    public String getType() { return type; }
    public int getQuantity() { return quantity; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getReason() { return reason; }
}

/* ========= SERVICE ========= */
class InventoryService {
    private final Map<String, Product> inventory = new HashMap<>();
    private final List<Transaction> transactions = new ArrayList<>();
    private int txnCounter = 1;

    public InventoryService() {
        addProduct(new Product("P001", "Laptop", "Electronics", 999.99, 15, 5));
        addProduct(new Product("P002", "Mouse", "Electronics", 29.99, 50, 10));
        addProduct(new Product("P003", "Keyboard", "Electronics", 79.99, 30, 8));
        addProduct(new Product("P004", "Monitor", "Electronics", 299.99, 12, 5));
        addProduct(new Product("P005", "Office Chair", "Furniture", 199.99, 8, 3));
    }
    public Collection<Product> getAllProducts() { return inventory.values(); }
    public List<Product> getLowStockProducts() {
        List<Product> res = new ArrayList<>();
        for (Product p : inventory.values()) if (p.isLowStock()) res.add(p);
        return res;
    }
    public Product getProduct(String id) { return inventory.get(id); }
    public boolean addProduct(Product p) {
        if (inventory.containsKey(p.getProductId())) return false;
        inventory.put(p.getProductId(), p);
        record(p.getProductId(), "IN", p.getQuantity(), "Initial stock");
        return true;
    }
    public boolean updateProduct(String id, String name, String category, double price, int minStock) {
        Product p = inventory.get(id);
        if (p == null) return false;
        p.setName(name); p.setCategory(category); p.setPrice(price); p.setMinStockLevel(minStock);
        return true;
    }
    public boolean deleteProduct(String id) { return inventory.remove(id) != null; }
    public String addStock(String id, int qty, String reason) {
        Product p = inventory.get(id);
        if (p == null) return "Product not found";
        if (qty <= 0) return "Quantity must be positive";
        p.setQuantity(p.getQuantity() + qty);
        record(id, "IN", qty, reason == null ? "Stock In" : reason);
        return "Stock added";
    }
    public String removeStock(String id, int qty, String reason) {
        Product p = inventory.get(id);
        if (p == null) return "Product not found";
        if (qty <= 0) return "Quantity must be positive";
        if (p.getQuantity() < qty) return "Insufficient stock";
        p.setQuantity(p.getQuantity() - qty);
        record(id, "OUT", qty, reason == null ? "Stock Out" : reason);
        return "Stock removed";
    }
    public List<Transaction> getRecentTransactions(int limit) {
        int from = Math.max(0, transactions.size() - limit);
        return transactions.subList(from, transactions.size());
    }
    public int getTotalItems() {
        int total = 0;
        for (Product p : inventory.values()) total += p.getQuantity();
        return total;
    }
    public double getTotalValue() {
        double sum = 0;
        for (Product p : inventory.values()) sum += p.getTotalValue();
        return sum;
    }
    private void record(String productId, String type, int qty, String reason) {
        String id = "TXN" + String.format("%06d", txnCounter++);
        transactions.add(new Transaction(id, productId, type, qty, reason));
    }
}

/* ========= MAIN UI ========= */
public class InventorySwingApp extends JFrame {
    private final InventoryService service = new InventoryService();

    private final DefaultTableModel productModel = new DefaultTableModel(
            new String[]{"ID","Name","Category","Price","Quantity","Min Stock","Status"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable productTable = new JTable(productModel);

    private final DefaultTableModel txnModel = new DefaultTableModel(
            new String[]{"Txn ID","Product ID","Type","Qty","Reason","Timestamp"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable txnTable = new JTable(txnModel);

    private final DefaultTableModel lowModel = new DefaultTableModel(
            new String[]{"ID","Name","Qty","Min","Category"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable lowTable = new JTable(lowModel);

    public InventorySwingApp() {
        super("Inventory Management System - Swing");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 720);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        setJMenuBar(buildMenuBar());

        JTabbedPane tabs = new JTabbedPane();
        tabs.add("Products", buildProductsPanel());
        tabs.add("Stock Ops", buildStockPanel());
        tabs.add("Low Stock", buildLowPanel());
        tabs.add("Reports", buildReportsPanel());
        add(tabs, BorderLayout.CENTER);

        refreshAll();
    }

    private JMenuBar buildMenuBar() {
        JMenuBar bar = new JMenuBar();
        JMenu file = new JMenu("File");
        JMenuItem logout = new JMenuItem("Logout");
        JMenuItem exit = new JMenuItem("Exit");
        logout.addActionListener(e -> {
            dispose();
            Bootstrap.launchWithLogin();
        });
        exit.addActionListener(e -> System.exit(0));
        file.add(logout); file.add(exit);

        JMenu view = new JMenu("View");
        JMenuItem refresh = new JMenuItem("Refresh");
        refresh.addActionListener(e -> refreshAll());
        view.add(refresh);

        bar.add(file); bar.add(view);
        return bar;
    }

    private JPanel buildProductsPanel() {
        JPanel root = new JPanel(new BorderLayout(10,10));
        root.setBorder(new EmptyBorder(10,10,10,10));

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder("Product Details"));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(5,5,5,5);
        g.fill = GridBagConstraints.HORIZONTAL;

        JTextField id = new JTextField(12);
        JTextField name = new JTextField(18);
        JTextField category = new JTextField(14);
        JTextField price = new JTextField(10);
        JTextField qty = new JTextField(10);
        JTextField min = new JTextField(10);

        int r=0;
        g.gridx=0; g.gridy=r; form.add(new JLabel("ID"), g);
        g.gridx=1; form.add(id, g);
        g.gridx=2; form.add(new JLabel("Name"), g);
        g.gridx=3; form.add(name, g); r++;
        g.gridx=0; g.gridy=r; form.add(new JLabel("Category"), g);
        g.gridx=1; form.add(category, g);
        g.gridx=2; form.add(new JLabel("Price"), g);
        g.gridx=3; form.add(price, g); r++;
        g.gridx=0; g.gridy=r; form.add(new JLabel("Quantity"), g);
        g.gridx=1; form.add(qty, g);
        g.gridx=2; form.add(new JLabel("Min Stock"), g);
        g.gridx=3; form.add(min, g);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT,10,0));
        JButton add = new JButton("Add");
        JButton update = new JButton("Update");
        JButton remove = new JButton("Delete");
        JButton clear = new JButton("Clear");
        btns.add(add); btns.add(update); btns.add(remove); btns.add(clear);

        add.addActionListener(e -> {
            try {
                Product p = new Product(
                        id.getText().trim(),
                        name.getText().trim(),
                        category.getText().trim(),
                        Double.parseDouble(price.getText().trim()),
                        Integer.parseInt(qty.getText().trim()),
                        Integer.parseInt(min.getText().trim())
                );
                boolean ok = service.addProduct(p);
                if (!ok) JOptionPane.showMessageDialog(this, "Product ID already exists.", "Error", JOptionPane.ERROR_MESSAGE);
                refreshAll();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Invalid input: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        update.addActionListener(e -> {
            try {
                boolean ok = service.updateProduct(
                        id.getText().trim(),
                        name.getText().trim(),
                        category.getText().trim(),
                        Double.parseDouble(price.getText().trim()),
                        Integer.parseInt(min.getText().trim())
                );
                if (!ok) JOptionPane.showMessageDialog(this, "Product not found.", "Error", JOptionPane.ERROR_MESSAGE);
                Product p = service.getProduct(id.getText().trim());
                if (p != null && !qty.getText().isBlank()) {
                    p.setQuantity(Integer.parseInt(qty.getText().trim()));
                }
                refreshAll();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Invalid input: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        remove.addActionListener(e -> {
            String pid = id.getText().trim();
            if (pid.isEmpty()) { JOptionPane.showMessageDialog(this, "Enter Product ID to delete."); return; }
            int c = JOptionPane.showConfirmDialog(this, "Delete product " + pid + "?", "Confirm", JOptionPane.YES_NO_OPTION);
            if (c == JOptionPane.YES_OPTION) {
                boolean ok = service.deleteProduct(pid);
                if (!ok) JOptionPane.showMessageDialog(this, "Product not found.", "Error", JOptionPane.ERROR_MESSAGE);
                refreshAll();
            }
        });
        clear.addActionListener(e -> { id.setText(""); name.setText(""); category.setText(""); price.setText(""); qty.setText(""); min.setText(""); });

        productTable.setRowHeight(24);
        JScrollPane scroll = new JScrollPane(productTable);
        productTable.getSelectionModel().addListSelectionListener(ev -> {
            int i = productTable.getSelectedRow();
            if (i >= 0) {
                id.setText(productModel.getValueAt(i,0).toString());
                name.setText(productModel.getValueAt(i,1).toString());
                category.setText(productModel.getValueAt(i,2).toString());
                price.setText(productModel.getValueAt(i,3).toString());
                qty.setText(productModel.getValueAt(i,4).toString());
                min.setText(productModel.getValueAt(i,5).toString());
            }
        });

        JPanel top = new JPanel(new BorderLayout(0,8));
        top.add(form, BorderLayout.CENTER);
        top.add(btns, BorderLayout.SOUTH);

        root.add(top, BorderLayout.NORTH);
        root.add(scroll, BorderLayout.CENTER);
        return root;
    }

    private JPanel buildStockPanel() {
        JPanel root = new JPanel(new BorderLayout(10,10));
        root.setBorder(new EmptyBorder(10,10,10,10));

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder("Stock Operation"));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(5,5,5,5);
        g.fill = GridBagConstraints.HORIZONTAL;

        JTextField pid = new JTextField(12);
        JTextField qty = new JTextField(10);
        JTextField reason = new JTextField(20);

        int r=0;
        g.gridx=0; g.gridy=r; form.add(new JLabel("Product ID"), g);
        g.gridx=1; form.add(pid, g);
        g.gridx=2; form.add(new JLabel("Quantity"), g);
        g.gridx=3; form.add(qty, g); r++;
        g.gridx=0; g.gridy=r; form.add(new JLabel("Reason"), g);
        g.gridx=1; g.gridwidth=3; form.add(reason, g); g.gridwidth=1;

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT,10,0));
        JButton add = new JButton("Add Stock");
        JButton remove = new JButton("Remove Stock");
        JButton recent = new JButton("Load Recent (50)");
        btns.add(add); btns.add(remove); btns.add(recent);

        add.addActionListener(e -> {
            try {
                String msg = service.addStock(pid.getText().trim(), Integer.parseInt(qty.getText().trim()), reason.getText().trim());
                JOptionPane.showMessageDialog(this, msg);
                refreshAll();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Invalid input: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        remove.addActionListener(e -> {
            try {
                String msg = service.removeStock(pid.getText().trim(), Integer.parseInt(qty.getText().trim()), reason.getText().trim());
                JOptionPane.showMessageDialog(this, msg);
                refreshAll();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Invalid input: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        recent.addActionListener(e -> loadRecentTransactions(50));

        txnTable.setRowHeight(24);
        JScrollPane scroll = new JScrollPane(txnTable);

        JPanel top = new JPanel(new BorderLayout(0,8));
        top.add(form, BorderLayout.CENTER);
        top.add(btns, BorderLayout.SOUTH);

        root.add(top, BorderLayout.NORTH);
        root.add(scroll, BorderLayout.CENTER);
        return root;
    }

    private JPanel buildLowPanel() {
        JPanel root = new JPanel(new BorderLayout(10,10));
        root.setBorder(new EmptyBorder(10,10,10,10));
        JLabel info = new JLabel("Products at or below minimum stock level");
        info.setFont(info.getFont().deriveFont(Font.BOLD));
        lowTable.setRowHeight(24);
        JScrollPane scroll = new JScrollPane(lowTable);
        JButton refresh = new JButton("Refresh");
        refresh.addActionListener(e -> refreshLowStock());
        JPanel top = new JPanel(new BorderLayout());
        top.add(info, BorderLayout.WEST);
        top.add(refresh, BorderLayout.EAST);
        root.add(top, BorderLayout.NORTH);
        root.add(scroll, BorderLayout.CENTER);
        return root;
    }

    private JPanel buildReportsPanel() {
        JPanel root = new JPanel(new BorderLayout(10,10));
        root.setBorder(new EmptyBorder(10,10,10,10));
        JTextArea report = new JTextArea(20, 60);
        report.setEditable(false);
        report.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        JButton inventorySummary = new JButton("Inventory Summary");
        JButton productList = new JButton("Product Listing");
        JButton exportCSV = new JButton("Export Products as CSV (console)");
        inventorySummary.addActionListener(e -> {
            StringBuilder sb = new StringBuilder();
            sb.append("=== INVENTORY SUMMARY ===\n");
            sb.append("Total Products: ").append(service.getAllProducts().size()).append("\n");
            sb.append("Total Items: ").append(service.getTotalItems()).append("\n");
            sb.append("Total Inventory Value: $").append(String.format("%.2f", service.getTotalValue())).append("\n");
            sb.append("Low Stock Items: ").append(service.getLowStockProducts().size()).append("\n\n");
            if (!service.getLowStockProducts().isEmpty()) {
                sb.append("Low Stock Products:\n");
                for (Product p : service.getLowStockProducts()) {
                    sb.append("- %s (%s): %d remaining (min %d)\n"
                            .formatted(p.getName(), p.getProductId(), p.getQuantity(), p.getMinStockLevel()));
                }
            }
            report.setText(sb.toString());
        });
        productList.addActionListener(e -> {
            StringBuilder sb = new StringBuilder();
            sb.append("=== PRODUCT LIST ===\n");
            for (Product p : service.getAllProducts()) {
                sb.append("%s | %s | %s | $%.2f | Q=%d | Min=%d | %s\n"
                        .formatted(p.getProductId(), p.getName(), p.getCategory(), p.getPrice(),
                                p.getQuantity(), p.getMinStockLevel(), p.isLowStock() ? "LOW" : "OK"));
            }
            report.setText(sb.toString());
        });
        exportCSV.addActionListener(e -> {
            System.out.println("id,name,category,price,quantity,minStock");
            for (Product p : service.getAllProducts()) {
                System.out.printf("%s,%s,%s,%.2f,%d,%d%n",
                        p.getProductId(), p.getName(), p.getCategory(), p.getPrice(), p.getQuantity(), p.getMinStockLevel());
            }
            JOptionPane.showMessageDialog(this, "Exported to console output.");
        });
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT,10,0));
        buttons.add(inventorySummary); buttons.add(productList); buttons.add(exportCSV);
        root.add(buttons, BorderLayout.NORTH);
        root.add(new JScrollPane(report), BorderLayout.CENTER);
        return root;
    }

    /* ======= Helpers ======= */
    private void refreshAll() {
        refreshProductsTable();
        refreshLowStock();
        loadRecentTransactions(20);
    }
    private void refreshProductsTable() {
        productModel.setRowCount(0);
        for (Product p : service.getAllProducts()) {
            productModel.addRow(new Object[]{
                    p.getProductId(), p.getName(), p.getCategory(),
                    String.format("%.2f", p.getPrice()),
                    p.getQuantity(), p.getMinStockLevel(),
                    p.isLowStock() ? "LOW" : "OK"
            });
        }
    }
    private void refreshLowStock() {
        lowModel.setRowCount(0);
        for (Product p : service.getLowStockProducts()) {
            lowModel.addRow(new Object[]{
                    p.getProductId(), p.getName(), p.getQuantity(), p.getMinStockLevel(), p.getCategory()
            });
        }
    }
    private void loadRecentTransactions(int limit) {
        txnModel.setRowCount(0);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        List<Transaction> recent = service.getRecentTransactions(limit);
        for (int i = recent.size() - 1; i >= 0; i--) {
            Transaction t = recent.get(i);
            txnModel.addRow(new Object[]{
                    t.getTransactionId(), t.getProductId(), t.getType(), t.getQuantity(),
                    t.getReason(), t.getTimestamp().format(fmt)
            });
        }
    }

    /* ======= Bootstrap with login ======= */
    static class Bootstrap {
        static void launchWithLogin() {
            SwingUtilities.invokeLater(() -> {
                try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}
                AuthService auth = new AuthService();
                LoginDialog dialog = new LoginDialog(null, auth);
                dialog.setVisible(true);
                if (dialog.isAuthenticated()) {
                    new InventorySwingApp().setVisible(true);
                } else {
                    System.exit(0);
                }
            });
        }
    }

    public static void main(String[] args) {
        Bootstrap.launchWithLogin();
    }
}
