import java.awt.*;
import java.awt.event.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

class Err extends RuntimeException { Err(String m) { super(m); } }

class Item {
    String code, name, cat; double price; int qty, min;
    Item(String c, String n, String t, double p, int q, int r) { code=c; name=n; cat=t; price=p; qty=q; min=r; }
    Item(String[] p) { this(p[0], p[1], p[2], Double.parseDouble(p[3]), Integer.parseInt(p[4]), Integer.parseInt(p[5])); }
    String status() { return qty == 0 ? "OUT" : qty <= min ? "LOW" : "OK"; }
    public String toString() { return code+"|"+name+"|"+cat+"|"+price+"|"+qty+"|"+min; }
}

class Store {
    static final String LINE = "--------------------------------------------------";
    ArrayList<Item> items = new ArrayList<>();
    ArrayList<String> moves = new ArrayList<>();
    LinkedHashMap<String, String[]> users = new LinkedHashMap<>();   // name -> {password, role}

    Store() {
        boolean fresh = !Files.exists(Paths.get("items.txt"));
        for (String s : read("items.txt")) try { items.add(new Item(s.split("\\|"))); } catch (Exception e) {}
        moves.addAll(read("movements.txt"));
        for (String s : read("users.txt")) { String[] p = s.split("\\|"); if (p.length > 2) users.put(p[0], new String[]{p[1], p[2]}); }
        if (users.isEmpty()) { users.put("admin", new String[]{"admin123", "ADMIN"}); users.put("staff", new String[]{"staff123", "STAFF"}); }
        if (fresh) for (String[] r : new String[][]{
            {"S101","LED Bulb","Electrical","85","120","30"}, {"S102","Notebook","Stationery","45","18","20"},
            {"S103","Ball Pen","Stationery","10","0","25"}, {"S104","Phone Charger","Electronics","299","56","15"},
            {"S105","Steel Bottle","Household","350","14","10"}}) items.add(new Item(r));
        save();
    }

    static ArrayList<String> read(String f) {
        try { return new ArrayList<>(Files.readAllLines(Paths.get(f))); } catch (Exception e) { return new ArrayList<>(); }
    }
    static void write(String f, Collection<?> c) {
        try { Files.write(Paths.get(f), c.stream().map(String::valueOf).collect(Collectors.toList())); }
        catch (Exception e) { throw new Err("Could not write " + f); }
    }
    void save() {
        write("items.txt", items); write("movements.txt", moves);
        ArrayList<String> u = new ArrayList<>();
        users.forEach((k, v) -> u.add(k + "|" + v[0] + "|" + v[1]));
        write("users.txt", u);
    }
    void log(Item x, String type, int q) {
        moves.add(LocalDateTime.now().toString().substring(0, 16).replace('T', ' ') + " | " + x.code + " | " + type + " | " + q + " | stock " + x.qty);
    }

    Item get(String c) { for (Item i : items) if (i.code.equalsIgnoreCase(c.trim())) return i; return null; }
    Item find(String c) {
        Item i = get(c);
        if (i == null) throw new Err(c.trim().isEmpty() ? "Enter or select an item code first" : "Item not found: " + c.trim());
        return i;
    }
    int num(String s) { try { return Integer.parseInt(s.trim()); } catch (Exception e) { throw new Err("Invalid number: '" + s.trim() + "'"); } }
    double price(String s) { try { return Double.parseDouble(s.trim()); } catch (Exception e) { throw new Err("Invalid price: '" + s.trim() + "'"); } }

    String add(String c, String n, String t, String p, String q, String r) {
        c = c.trim().toUpperCase(); n = n.trim();
        if (c.isEmpty() || n.isEmpty()) throw new Err("Code and name are required");
        if (get(c) != null) throw new Err("Item " + c + " already exists");
        Item x = new Item(c, n, t, price(p), num(q), num(r));
        if (x.price < 0 || x.qty < 0 || x.min < 0) throw new Err("Negative values are not allowed");
        items.add(x); if (x.qty > 0) log(x, "OPENING", x.qty); save();
        return "Added " + c + " - " + n + " (Qty " + x.qty + ")";
    }
    String update(String c, String n, String t, String p, String r) {
        Item x = find(c); double pr = price(p); int re = num(r);
        if (n.trim().isEmpty()) throw new Err("Name is required");
        if (pr < 0 || re < 0) throw new Err("Negative values are not allowed");
        x.name = n.trim(); x.cat = t; x.price = pr; x.min = re; save();
        return "Updated " + x.code + " (quantity is changed only via Stock In/Out or Adjust)";
    }
    String delete(String c) { Item x = find(c); items.remove(x); save(); return "Deleted " + x.code + " - " + x.name; }
    String stock(String c, int q, boolean in) {
        Item x = find(c); int old = x.qty;
        if (q <= 0) throw new Err("Quantity must be a positive number");
        if (!in && q > x.qty) throw new Err("Insufficient stock: only " + x.qty + " of " + x.code + " available");
        x.qty += in ? q : -q; log(x, in ? "STOCK-IN" : "STOCK-OUT", q); save();
        return x.code + " " + x.name + ": " + old + (in ? " + " : " - ") + q + " = " + x.qty + " (" + x.status() + ")";
    }
    String adjust(String c, String reason, int q) {
        Item x = find(c); int old = x.qty;
        if (reason.equals("Count Correction")) { if (q < 0) throw new Err("Counted quantity cannot be negative"); x.qty = q; }
        else {
            if (q <= 0) throw new Err("Quantity must be a positive number");
            if (q > x.qty) throw new Err("Cannot remove " + q + ": only " + x.qty + " in stock");
            x.qty -= q;
        }
        log(x, reason, q); save();
        return x.code + " " + reason + ": " + old + " -> " + x.qty + " (" + x.status() + ")";
    }
    String addUser(String n, String p, String r) {
        n = n.trim();
        if (n.isEmpty() || p.isEmpty()) throw new Err("Enter a username and password");
        if (users.containsKey(n)) throw new Err("User '" + n + "' already exists");
        users.put(n, new String[]{p, r}); save();
        return "User '" + n + "' added as " + r;
    }

    String report(boolean low) {
        StringBuilder s = new StringBuilder((low ? "LOW STOCK ALERTS" : "STOCK SUMMARY") + "\n" + LINE + "\n"
            + String.format("%-6s %-22s %5s %8s  %s\n", "CODE", "NAME", "QTY", "REORDER", "STATUS") + LINE + "\n");
        int n = 0;
        for (Item x : items) if (!low || x.qty <= x.min) {
            n++; s.append(String.format("%-6s %-22s %5d %8d  %s\n", x.code, x.name, x.qty, x.min, x.status()));
        }
        return s + LINE + "\n" + n + " item(s)";
    }
    String value() {
        StringBuilder s = new StringBuilder("STOCK VALUATION\n" + LINE + "\n"); double t = 0;
        for (Item x : items) {
            double v = x.price * x.qty; t += v;
            s.append(String.format("%-6s %-22s %5d x %8.2f = Rs.%10.2f\n", x.code, x.name, x.qty, x.price, v));
        }
        return s + LINE + "\nTOTAL VALUE: Rs." + String.format("%.2f", t);
    }
    String history() {
        return "MOVEMENTS (time | code | type | qty | stock after)\n" + LINE + "\n"
            + (moves.isEmpty() ? "No movements recorded yet." : String.join("\n", moves));
    }
    void saveReport(String t) {
        if (t.trim().isEmpty()) throw new Err("Open a report first, then save it");
        write("stock_report.txt", Arrays.asList(t.split("\n")));
    }
}

class Win extends Frame {
    Win(String title, int w, int h) {
        super(title); setFont(new Font("SansSerif", Font.PLAIN, 14)); setSize(w, h); setLocationRelativeTo(null);
        addWindowListener(new WindowAdapter() { public void windowClosing(WindowEvent e) { close(); } });
    }
    void close() { System.exit(0); }
    public Insets getInsets() { Insets i = super.getInsets(); return new Insets(i.top+10, i.left+10, i.bottom+10, i.right+10); }
}

class LoginFrame extends Win implements ActionListener {
    Store m; TextField user = new TextField(), pass = new TextField(); Label msg = new Label();

    LoginFrame(Store s) {
        super("Stock Management - Login", 420, 230); m = s;
        setLayout(new GridLayout(4, 2, 10, 12)); pass.setEchoChar('*'); msg.setForeground(Color.RED);
        Button in = new Button("Login"), ex = new Button("Exit");
        in.addActionListener(this); user.addActionListener(this); pass.addActionListener(this); ex.addActionListener(e -> close());
        add(new Label("Username")); add(user); add(new Label("Password")); add(pass); add(in); add(ex);
        add(new Label("Demo: admin / admin123")); add(msg);
        setVisible(true); user.requestFocus();
    }
    public void actionPerformed(ActionEvent e) {
        String n = user.getText().trim(); String[] u = m.users.get(n);
        if (u != null && u[0].equals(pass.getText())) { dispose(); new MainFrame(m, n); }
        else { msg.setText("Invalid username or password"); pass.setText(""); }
    }
}

class MainFrame extends Win implements ActionListener {
    Store m; boolean admin;
    CardLayout cards = new CardLayout(); Panel pages = new Panel(cards); Label status = new Label();
    TextField code = new TextField(), name = new TextField(), price = new TextField(), qty = new TextField(),
        reorder = new TextField(), search = new TextField(), ioQty = new TextField(), adjQty = new TextField(),
        newUser = new TextField(10), newPass = new TextField(10);
    Choice cat = new Choice(), ioItem = new Choice(), ioType = new Choice(), adjItem = new Choice(), reason = new Choice(), role = new Choice();
    java.awt.List list = new java.awt.List();
    TextArea mon = new TextArea(), out = new TextArea();
    ArrayList<Item> shown = new ArrayList<>();

    MainFrame(Store s, String u) {
        super("Stock Management System", 900, 620); m = s; admin = m.users.get(u)[1].equals("ADMIN");
        newPass.setEchoChar('*');search.addActionListener(e -> {filter(search.getText());say(shown.size() + " item(s) match '" + search.getText().trim() + "'", true);});
        fill(cat, "Electrical,Electronics,Stationery,Household,Grocery,Other"); fill(ioType, "Stock-In,Stock-Out");
        fill(reason, "Damaged,Expired,Lost,Count Correction"); fill(role, "STAFF,ADMIN");
        for (TextArea t : new TextArea[]{mon, out}) { t.setEditable(false); t.setFont(new Font("Monospaced", Font.PLAIN, 13)); }
        list.setFont(new Font("Monospaced", Font.PLAIN, 13)); list.addItemListener(e -> pick());

        Panel nav = new Panel(new FlowLayout(FlowLayout.LEFT, 6, 4)), who = new Panel(new FlowLayout(FlowLayout.RIGHT, 6, 4)), top = new Panel(new BorderLayout());
        for (String t : new String[]{"Items", "Stock In/Out", "Monitor", "Adjust", "Reports"}) nav.add(btn(t));
        who.add(new Label(u + " (" + m.users.get(u)[1] + ")")); who.add(btn("Logout"));
        top.add(nav); top.add(who, BorderLayout.EAST);

        Panel srch = new Panel(new BorderLayout(6, 0)), lp = new Panel(new BorderLayout(0, 6));
        srch.add(new Label("Search"), BorderLayout.WEST); srch.add(search); srch.add(bar("Search", "Show All"), BorderLayout.EAST);
        lp.add(srch, BorderLayout.NORTH); lp.add(list);
        pages.add(page(form("Code", code, "Name", name, "Category", cat, "Price (Rs.)", price, "Quantity (new items)", qty, "Reorder level", reorder), lp, "Add", "Update", "Delete", "Clear"), "Items");
        pages.add(page(form("Item", ioItem, "Type", ioType, "Quantity", ioQty), null, "Record"), "Stock In/Out");
        pages.add(page(null, mon, "Refresh", "Alerts"), "Monitor");
        pages.add(page(form("Item", adjItem, "Reason", reason, "Quantity", adjQty), null, "Apply Adjustment"), "Adjust");
        Panel rp = page(null, out, "Summary", "Low Stock", "Value", "Movements", "Save Report"), ur = new Panel();
        ur.add(new Label("New user")); ur.add(newUser); ur.add(new Label("Password")); ur.add(newPass); ur.add(role); ur.add(btn("Add User"));
        rp.add(ur, BorderLayout.SOUTH); pages.add(rp, "Reports");

        status.setFont(new Font("SansSerif", Font.BOLD, 14));
        add(top, BorderLayout.NORTH); add(pages); add(status, BorderLayout.SOUTH);
        refresh(); mon.setText(m.report(false)); out.setText(m.report(false));
        say("Welcome, " + u + "! Logged in as " + m.users.get(u)[1], true); setVisible(true);
    }

    void close() { m.save(); System.exit(0); }
    void fill(Choice c, String csv) { for (String s : csv.split(",")) c.add(s); }
    Button btn(String s) { Button b = new Button(s); b.addActionListener(this); return b; }
    Panel bar(String... s) { Panel p = new Panel(new FlowLayout(FlowLayout.LEFT, 8, 6)); for (String t : s) p.add(btn(t)); return p; }
    Panel form(Object... a) {
        Panel p = new Panel(new GridLayout(0, 2, 8, 8));
        for (Object o : a) p.add(o instanceof String ? new Label((String) o) : (Component) o);
        return p;
    }
    Panel page(Panel f, Component center, String... buttons) {
        Panel n = new Panel(new BorderLayout(0, 8)), p = new Panel(new BorderLayout(0, 8));
        if (f != null) n.add(f);
        n.add(bar(buttons), BorderLayout.SOUTH); p.add(n, BorderLayout.NORTH);
        if (center != null) p.add(center);
        return p;
    }
    void say(String s, boolean ok) { status.setForeground(ok ? new Color(0, 120, 0) : Color.RED); status.setText(" " + (ok ? "OK: " : "ERROR: ") + s); }
    String sel(Choice c) { return c.getItemCount() == 0 ? "" : c.getSelectedItem().split(" - ")[0]; }
    void needAdmin() { if (!admin) throw new Err("ADMIN access required for this action"); }
    void clear() { code.setText(""); name.setText(""); price.setText(""); qty.setText(""); reorder.setText(""); }

    void pick() {   // clicking a row in the item list fills the form
        int i = list.getSelectedIndex(); if (i < 0) return;
        Item x = shown.get(i);
        code.setText(x.code); name.setText(x.name); cat.select(x.cat); price.setText("" + x.price); qty.setText("" + x.qty); reorder.setText("" + x.min);
        say("Selected " + x.code + " - edit and click Update, or click Delete", true);
    }
    void filter(String k) {
        list.removeAll(); shown.clear(); k = k.trim().toLowerCase();
        for (Item x : m.items) if ((x.code + " " + x.name + " " + x.cat).toLowerCase().contains(k)) {
            shown.add(x);
            list.add(String.format("%-6s %-20s %-12s Rs.%-9.2f Qty=%-5d %s", x.code, x.name, x.cat, x.price, x.qty, x.status()));
        }
    }
    void refresh() {
        int i = ioItem.getSelectedIndex(), j = adjItem.getSelectedIndex();
        ioItem.removeAll(); adjItem.removeAll();
        for (Item x : m.items) { String s = x.code + " - " + x.name; ioItem.add(s); adjItem.add(s); }
        if (i > 0 && i < ioItem.getItemCount()) ioItem.select(i);
        if (j > 0 && j < adjItem.getItemCount()) adjItem.select(j);
        filter(search.getText());
    }

    public void actionPerformed(ActionEvent e) {
        String b = e.getActionCommand(), msg = "";
        try {
            switch (b) {
                case "Logout": dispose(); new LoginFrame(m); return;
                case "Items": case "Stock In/Out": case "Monitor": case "Adjust": case "Reports": cards.show(pages, b); msg = b + " page"; break;
                case "Add": msg = m.add(code.getText(), name.getText(), cat.getSelectedItem(), price.getText(), qty.getText(), reorder.getText()); clear(); break;
                case "Update": msg = m.update(code.getText(), name.getText(), cat.getSelectedItem(), price.getText(), reorder.getText()); break;
                case "Delete": needAdmin(); msg = m.delete(code.getText()); clear(); break;
                case "Clear": clear(); msg = "Fields cleared"; break;
                case "Search": filter(search.getText()); msg = shown.size() + " item(s) match '" + search.getText().trim() + "'"; break;
                case "Show All": search.setText(""); filter(""); msg = "Showing all " + shown.size() + " items"; break;
                case "Record": msg = m.stock(sel(ioItem), m.num(ioQty.getText()), ioType.getSelectedIndex() == 0); ioQty.setText(""); break;
                case "Apply Adjustment": needAdmin(); msg = m.adjust(sel(adjItem), reason.getSelectedItem(), m.num(adjQty.getText())); adjQty.setText(""); break;
                case "Refresh": mon.setText(m.report(false)); msg = "Stock list refreshed"; break;
                case "Alerts": mon.setText(m.report(true)); msg = "Showing low-stock alerts"; break;
                case "Summary": out.setText(m.report(false)); msg = "Summary report ready"; break;
                case "Low Stock": out.setText(m.report(true)); msg = "Low-stock report ready"; break;
                case "Value": out.setText(m.value()); msg = "Stock valuation ready"; break;
                case "Movements": out.setText(m.history()); msg = "Movement history ready"; break;
                case "Save Report": m.saveReport(out.getText()); msg = "Report saved to stock_report.txt"; break;
                case "Add User": needAdmin(); msg = m.addUser(newUser.getText(), newPass.getText(), role.getSelectedItem()); newUser.setText(""); newPass.setText(""); break;
            }
            refresh(); say(msg, true);
        } catch (RuntimeException ex) { say(ex.getMessage(), false); }
    }
}

public class StockManagementSystem {
    public static void main(String[] args) { new LoginFrame(new Store()); }
}