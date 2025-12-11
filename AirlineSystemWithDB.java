import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.List;

public class AirlineSystemWithDB {

    // ---------------- DB config ----------------
    public static class DBConfig {
        public static final String URL = "jdbc:mysql://localhost:3306/airline_system?useSSL=false&serverTimezone=UTC";
        public static final String USER = "root";
        public static final String PASSWORD = "2333333";

        static {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
            } catch (ClassNotFoundException e) {
                JOptionPane.showMessageDialog(null,
                        "MySQL Connector/J driver not found. Add the JDBC driver to classpath.",
                        "Driver Error", JOptionPane.ERROR_MESSAGE);
            }
        }

        public static Connection getConnection() throws SQLException {
            return DriverManager.getConnection(URL, USER, PASSWORD);
        }

        public static boolean testConnection() {
            try (Connection c = getConnection()) {
                return true;
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(null, "Cannot connect to DB: " + e.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }
    }
    // ---------------- Database manager ----------------
    public static class DatabaseManager {
        public static void initSchemaAndDefaultAdmin() {
            String createUsers = "CREATE TABLE IF NOT EXISTS users (" +
                    "id VARCHAR(50) PRIMARY KEY, username VARCHAR(50) UNIQUE NOT NULL, password VARCHAR(100) NOT NULL, role VARCHAR(20) NOT NULL)";
            String createFlights = "CREATE TABLE IF NOT EXISTS flights (" +
                    "id INT PRIMARY KEY AUTO_INCREMENT, flight_no VARCHAR(50) NOT NULL, origin VARCHAR(100) NOT NULL, destination VARCHAR(100) NOT NULL, departure VARCHAR(50) NOT NULL, arrival VARCHAR(50) NOT NULL, seats INT NOT NULL)";
            String createPassengers = "CREATE TABLE IF NOT EXISTS passengers (" +
                    "id INT PRIMARY KEY AUTO_INCREMENT, name VARCHAR(100) NOT NULL, email VARCHAR(100), phone VARCHAR(30))";
            String createBookings = "CREATE TABLE IF NOT EXISTS bookings (" +
                    "id INT PRIMARY KEY AUTO_INCREMENT, flight_no VARCHAR(50) NOT NULL, passenger VARCHAR(100) NOT NULL, seat VARCHAR(20) NOT NULL, booked_at VARCHAR(50) NOT NULL, booked_by VARCHAR(50) NOT NULL)";

            try (Connection conn = DBConfig.getConnection();
                 Statement st = conn.createStatement()) {
                st.execute(createUsers);
                st.execute(createFlights);
                st.execute(createPassengers);
                st.execute(createBookings);
                String check = "SELECT COUNT(*) FROM users WHERE username='admin'";
                try (ResultSet rs = st.executeQuery(check)) {
                    rs.next();
                    if (rs.getInt(1) == 0) {
                        String insert = "INSERT INTO users (id, username, password, role) VALUES (?, ?, ?, ?)";
                        try (PreparedStatement ps = conn.prepareStatement(insert)) {
                            ps.setString(1, "U" + System.currentTimeMillis());
                            ps.setString(2, "admin");
                            ps.setString(3, "admin123");
                            ps.setString(4, "admin");
                            ps.executeUpdate();
                            System.out.println("Default admin created: admin / admin123");
                        }
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "DB initialization failed: " + e.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
            }
        }

        // Authentication
        public static User authenticate(String username, String password) {
            String sql = "SELECT * FROM users WHERE username=? AND password=?";
            try (Connection conn = DBConfig.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, username);
                ps.setString(2, password);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return new User(rs.getString("id"), rs.getString("username"), rs.getString("password"), rs.getString("role"));
                }
            } catch (SQLException e) { e.printStackTrace(); }
            return null;
        }

        public static boolean createUser(String username, String password, String role) {
            String sql = "INSERT INTO users (id, username, password, role) VALUES (?, ?, ?, ?)";
            try (Connection conn = DBConfig.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, "U" + System.currentTimeMillis());
                ps.setString(2, username);
                ps.setString(3, password);
                ps.setString(4, role);
                ps.executeUpdate();
                return true;
            } catch (SQLException e) { return false; }
        }

        // Flights
        public static List<Flight> getAllFlights() {
            List<Flight> l = new ArrayList<>();
            String sql = "SELECT * FROM flights ORDER BY id";
            try (Connection conn = DBConfig.getConnection();
                 Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery(sql)) {
                while (rs.next()) {
                    l.add(new Flight(rs.getInt("id"), rs.getString("flight_no"),
                            rs.getString("origin"), rs.getString("destination"),
                            rs.getString("departure"), rs.getString("arrival"),
                            rs.getInt("seats")));
                }
            } catch (SQLException e) { e.printStackTrace(); }
            return l;
        }

        public static void addFlight(String flightNo, String origin, String dest, String dep, String arr, int seats) throws SQLException {
            String sql = "INSERT INTO flights (flight_no, origin, destination, departure, arrival, seats) VALUES (?, ?, ?, ?, ?, ?)";
            try (Connection conn = DBConfig.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, flightNo); ps.setString(2, origin); ps.setString(3, dest);
                ps.setString(4, dep); ps.setString(5, arr); ps.setInt(6, seats);
                ps.executeUpdate();
            }
        }

        public static void updateFlight(int id, String flightNo, String origin, String dest, String dep, String arr, int seats) throws SQLException {
            String sql = "UPDATE flights SET flight_no=?, origin=?, destination=?, departure=?, arrival=?, seats=? WHERE id=?";
            try (Connection conn = DBConfig.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, flightNo); ps.setString(2, origin); ps.setString(3, dest);
                ps.setString(4, dep); ps.setString(5, arr); ps.setInt(6, seats); ps.setInt(7, id);
                ps.executeUpdate();
            }
        }

        public static void deleteFlight(int id) throws SQLException {
            String sql = "DELETE FROM flights WHERE id=?";
            try (Connection conn = DBConfig.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, id); ps.executeUpdate();
            }
        }

        // Bookings
        public static List<Booking> getAllBookings() {
            List<Booking> list = new ArrayList<>();
            String sql = "SELECT * FROM bookings ORDER BY id DESC";
            try (Connection conn = DBConfig.getConnection();
                 Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery(sql)) {
                while (rs.next()) list.add(new Booking(rs.getInt("id"), rs.getString("flight_no"), rs.getString("passenger"), rs.getString("seat"), rs.getString("booked_at"), rs.getString("booked_by")));
            } catch (SQLException e) { e.printStackTrace(); }
            return list;
        }

        public static List<String> getBookedSeats(String flightNo) {
            List<String> l = new ArrayList<>();
            String sql = "SELECT seat FROM bookings WHERE flight_no=?";
            try (Connection conn = DBConfig.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, flightNo);
                try (ResultSet rs = ps.executeQuery()) { while (rs.next()) l.add(rs.getString("seat")); }
            } catch (SQLException e) { e.printStackTrace(); }
            return l;
        }

        public static int addBookingReturnId(String flightNo, String passenger, String seat, String bookedBy) throws SQLException {
            String sql = "INSERT INTO bookings (flight_no, passenger, seat, booked_at, booked_by) VALUES (?, ?, ?, ?, ?)";
            try (Connection conn = DBConfig.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, flightNo); ps.setString(2, passenger); ps.setString(3, seat);
                ps.setString(4, LocalDateTime.now().toString()); ps.setString(5, bookedBy);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) { if (keys.next()) return keys.getInt(1); }
            }
            return -1;
        }

        public static void deleteBooking(int id) throws SQLException {
            String sql = "DELETE FROM bookings WHERE id=?";
            try (Connection conn = DBConfig.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, id); ps.executeUpdate();
            }
        }
    }

    // ---------------- Models ----------------
    public static class User { public final String id, username, password, role; public User(String id, String u, String p, String r){ this.id=id; this.username=u; this.password=p; this.role=r; } }
    public static class Flight { public final int id; public final String flightNo, origin, dest, dep, arr; public final int seats; public Flight(int id,String f,String o,String d,String dep,String arr,int s){ this.id=id; this.flightNo=f; this.origin=o; this.dest=d; this.dep=dep; this.arr=arr; this.seats=s; } }
    public static class Booking { public final int id; public final String flightNo, passenger, seat, bookedAt, bookedBy; public Booking(int i,String f,String p,String s,String b,String bb){ id=i; flightNo=f; passenger=p; seat=s; bookedAt=b; bookedBy=bb; } }

    // ---------------- Login Frame ----------------
    public static class LoginFrame extends JFrame {
        private JTextField usernameField;
        private JPasswordField passwordField;

        public LoginFrame() {
            super("Airline System — Login");
            setSize(420, 360);
            setLocationRelativeTo(null);
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            getContentPane().setBackground(new Color(34,34,34));
            setLayout(new BorderLayout());

            JLabel title = new JLabel("Airline Management System", SwingConstants.CENTER);
            title.setFont(new Font("Segoe UI", Font.BOLD, 20));
            title.setForeground(Color.WHITE);
            title.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));
            add(title, BorderLayout.NORTH);

            JPanel form = new JPanel(new GridLayout(3,2,10,10));
            form.setBackground(new Color(34,34,34));
            form.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));
            JLabel ulabel = new JLabel("Username:"); 
            ulabel.setForeground(Color.WHITE); 
            ulabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));
            usernameField = new JTextField(); styleTextField(usernameField);
            JLabel plabel = new JLabel("Password:"); 
            plabel.setForeground(Color.WHITE); 
            plabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));
            passwordField = new JPasswordField(); 
            styleTextField(passwordField);
            JButton loginBtn = createStyledButton("Login", new Color(70,41,167), Color.WHITE);
            JButton registerBtn = createStyledButton("Register", new Color(70,41,167), Color.WHITE);
            form.add(ulabel); form.add(usernameField);
            form.add(plabel); form.add(passwordField);
            form.add(registerBtn); form.add(loginBtn);
            add(form, BorderLayout.CENTER);
            loginBtn.addActionListener(e -> handleLogin());
            registerBtn.addActionListener(e -> openRegisterDialog());
            passwordField.addActionListener(e -> handleLogin());
            setVisible(true);
        }

        private void handleLogin() {
            String u = usernameField.getText().trim();
            String p = new String(passwordField.getPassword()).trim();
            if (u.isEmpty() || p.isEmpty()) { JOptionPane.showMessageDialog(this, "Enter username and password"); return; }
            User user = DatabaseManager.authenticate(u, p);
            if (user != null) {
                JOptionPane.showMessageDialog(this, "Login successful! Welcome " + u);
                dispose();
                if ("admin".equalsIgnoreCase(user.role)) new AdminPanel(user).setVisible(true);
                else new UserPanel(user).setVisible(true);
            } else {
                JOptionPane.showMessageDialog(this, "Invalid credentials");
            }
        }

        private void openRegisterDialog() {
            JDialog d = new JDialog(this, "Register", true);
            d.setSize(420,300);
            d.setLocationRelativeTo(this);
            d.setLayout(new GridLayout(4,2,10,10));
            d.getContentPane().setBackground(new Color(34,34,34));
            JTextField newUser = new JTextField(); 
            JPasswordField newPass = new JPasswordField(); 
            JPasswordField confirm = new JPasswordField();
            styleTextField(newUser); 
            styleTextField(newPass); 
            styleTextField(confirm);
            d.add(createLabel("Username:")); 
            d.add(newUser);
            d.add(createLabel("Password:")); 
            d.add(newPass);
            d.add(createLabel("Confirm Password:")); 
            d.add(confirm);
            JButton save = createStyledButton("Register", new Color(70,41,167), Color.WHITE);
            d.add(new JLabel()); 
            d.add(save);
            save.addActionListener(ae -> {
                String u = newUser.getText().trim(), p = new String(newPass.getPassword()).trim(), c = new String(confirm.getPassword()).trim();
                if (u.isEmpty() || p.isEmpty()) { JOptionPane.showMessageDialog(d, "All fields required"); return; }
                if (!p.equals(c)) { JOptionPane.showMessageDialog(d, "Passwords do not match"); return; }
                if (DatabaseManager.createUser(u, p, "user")) {
                    JOptionPane.showMessageDialog(d, "Registration successful — logged in!");
                    d.dispose();
                    dispose();
                    User user = DatabaseManager.authenticate(u, p);
                    new UserPanel(user).setVisible(true);
                } else JOptionPane.showMessageDialog(d, "Error: username may already exist");
            });

            d.setVisible(true);
        }
    }
    // ---------------- Admin Panel ----------------
    public static class AdminPanel extends JFrame {
        private final User adminUser;
        private DefaultTableModel flightsModel, bookingsModel;
        private JTable flightsTable, bookingsTable;

        public AdminPanel(User admin) {
            super("Airline System — Admin Panel");
            this.adminUser = admin;
            setSize(1000,650);
            setLocationRelativeTo(null);
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setLayout(new BorderLayout());
            getContentPane().setBackground(new Color(30,30,30));

            JLabel title = new JLabel("Admin Dashboard", SwingConstants.CENTER); 
            title.setFont(new Font("Segoe UI", Font.BOLD, 22)); 
            title.setForeground(Color.WHITE); 
            title.setBorder(BorderFactory.createEmptyBorder(15,0,15,0));
            add(title, BorderLayout.NORTH);
            flightsModel = new DefaultTableModel(new Object[]{"ID","Flight No","Origin","Destination","Departure","Arrival","Seats"},0) { public boolean isCellEditable(int r,int c){return false;}};
            bookingsModel = new DefaultTableModel(new Object[]{"ID","Flight No","Passenger","Seat","Booked At","Booked By"},0) { public boolean isCellEditable(int r,int c){return false;}};
            JTabbedPane tabs = new JTabbedPane(); styleTabbedPane(tabs);
            tabs.addTab("Flights", createFlightsPanel());
            tabs.addTab("Bookings", createBookingsPanel());
            add(tabs, BorderLayout.CENTER);
            JButton logout = createStyledButton("Logout", new Color(200,0,0), Color.WHITE);
            logout.addActionListener(e -> { dispose(); new LoginFrame().setVisible(true); });
            JPanel south = new JPanel(); south.setBackground(new Color(30,30,30)); south.add(logout); add(south, BorderLayout.SOUTH);
            loadAll();
        }
        private JPanel createFlightsPanel() {
            JPanel p = new JPanel(new BorderLayout()); 
            p.setBackground(new Color(34,34,34));
            flightsTable = new JTable(flightsModel); 
            styleTable(flightsTable);
            JScrollPane sp = new JScrollPane(flightsTable); 
            sp.setBorder(BorderFactory.createEmptyBorder(10,10,10,10)); 
            p.add(sp, BorderLayout.CENTER);
            JPanel btns = new JPanel(); 
            btns.setBackground(new Color(40,40,40));
            JButton add = createStyledButton("Add Flight", new Color(70,41,167), Color.WHITE);
            JButton edit = createStyledButton("Edit Flight", new Color(70,41,167), Color.WHITE);
            JButton del = createStyledButton("Delete Flight", new Color(200,0,0), Color.WHITE);
            JButton ref = createStyledButton("Refresh", new Color(90,90,90), Color.WHITE);
            btns.add(add); btns.add(edit); btns.add(del); btns.add(ref);
            p.add(btns, BorderLayout.SOUTH);
            add.addActionListener(e -> openFlightDialog(null));
            edit.addActionListener(e -> {
                int r = flightsTable.getSelectedRow(); if (r<0) { JOptionPane.showMessageDialog(this,"Select flight"); return; }
                openFlightDialog((Integer)flightsModel.getValueAt(r,0));
            });
            del.addActionListener(e -> {
                int r = flightsTable.getSelectedRow(); if (r<0) { JOptionPane.showMessageDialog(this,"Select flight"); return; }
                if (JOptionPane.showConfirmDialog(this,"Delete flight?","Confirm",JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION) {
                    try { DatabaseManager.deleteFlight((Integer)flightsModel.getValueAt(r,0)); loadFlights(); } catch (SQLException ex) { JOptionPane.showMessageDialog(this,"Error: "+ex.getMessage()); }
                }
            });
            ref.addActionListener(e -> loadFlights());

            return p;
        }
            private JPanel createBookingsPanel() {
        JPanel p = new JPanel(new BorderLayout()); 
        p.setBackground(new Color(34,34,34));
        bookingsTable = new JTable(bookingsModel); 
        styleTable(bookingsTable);
        JScrollPane sp = new JScrollPane(bookingsTable); 
        sp.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        p.add(sp, BorderLayout.CENTER);
        JPanel btns = new JPanel(); 
        btns.setBackground(new Color(40,40,40));
        JButton del = createStyledButton("Delete Booking", new Color(200,0,0), Color.WHITE);
        JButton ref = createStyledButton("Refresh", new Color(90,90,90), Color.WHITE);
        btns.add(del);
        btns.add(ref);
        p.add(btns, BorderLayout.SOUTH);
    // Delete booking action
    del.addActionListener(e -> deleteSelectedBooking());
    ref.addActionListener(e -> loadBookings());

    return p;
}
private void deleteSelectedBooking() {
    int selectedRow = bookingsTable.getSelectedRow();
    if (selectedRow == -1) {
        JOptionPane.showMessageDialog(this, 
            "Please select a booking to delete.", 
            "No Selection", 
            JOptionPane.WARNING_MESSAGE);
        return;
    }
    int bookingId = (Integer) bookingsModel.getValueAt(selectedRow, 0);
    String flightNo = (String) bookingsModel.getValueAt(selectedRow, 1);
    String passenger = (String) bookingsModel.getValueAt(selectedRow, 2);
    String seat = (String) bookingsModel.getValueAt(selectedRow, 3);

    // Confirmation dialog
    int confirm = JOptionPane.showConfirmDialog(this,
            "Delete booking?\n\n" +
            "Flight: " + flightNo + "\n" +
            "Passenger: " + passenger + "\n" +
            "Seat: " + seat,
            "Confirm Deletion", 
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);

    if (confirm != JOptionPane.YES_OPTION) {
        return;
    }
    try {
        DatabaseManager.deleteBooking(bookingId);
        JOptionPane.showMessageDialog(this, 
            "Booking deleted successfully.", 
            "Success", 
            JOptionPane.INFORMATION_MESSAGE);
        loadBookings(); // Refresh the table
    } catch (SQLException ex) {
        JOptionPane.showMessageDialog(this, 
            "Error deleting booking: " + ex.getMessage(), 
            "Database Error", 
            JOptionPane.ERROR_MESSAGE);
        ex.printStackTrace();
    }
}
        private void loadAll() { loadFlights(); loadBookings(); }
        private void loadFlights() {
            flightsModel.setRowCount(0);
            for (Flight f : DatabaseManager.getAllFlights()) flightsModel.addRow(new Object[]{f.id, f.flightNo, f.origin, f.dest, f.dep, f.arr, f.seats});
        }
        private void loadBookings() {
            bookingsModel.setRowCount(0);
            for (Booking b : DatabaseManager.getAllBookings()) bookingsModel.addRow(new Object[]{b.id, b.flightNo, b.passenger, b.seat, b.bookedAt, b.bookedBy});
        }

        private void openFlightDialog(Integer editId) {
            JDialog d = new JDialog(this, editId==null?"Add Flight":"Edit Flight", true);
            d.setSize(420,380);
            d.setLocationRelativeTo(this); 
            d.setLayout(new GridLayout(7,2,8,8)); 
            d.getContentPane().setBackground(new Color(34,34,34));
            JTextField fno=new JTextField(), orig=new JTextField(), dest=new JTextField(), dep=new JTextField(), arr=new JTextField(), seats=new JTextField();
            styleTextField(fno); 
            styleTextField(orig); 
            styleTextField(dest); 
            styleTextField(dep); 
            styleTextField(arr); 
            styleTextField(seats);
            if (editId!=null) {
                for (int i=0;i<flightsModel.getRowCount();i++) if (((Integer)flightsModel.getValueAt(i,0)).equals(editId)) {
                    fno.setText((String)flightsModel.getValueAt(i,1)); orig.setText((String)flightsModel.getValueAt(i,2)); dest.setText((String)flightsModel.getValueAt(i,3));
                    dep.setText((String)flightsModel.getValueAt(i,4)); arr.setText((String)flightsModel.getValueAt(i,5)); seats.setText(flightsModel.getValueAt(i,6).toString());
                    break;
                }
            }
            d.add(createLabel("Flight No:")); d.add(fno);
            d.add(createLabel("Origin:")); d.add(orig);
            d.add(createLabel("Destination:")); d.add(dest);
            d.add(createLabel("Departure:")); d.add(dep);
            d.add(createLabel("Arrival:")); d.add(arr);
            d.add(createLabel("Seats (6x6 UI fixed):")); d.add(seats);
            JButton save = createStyledButton("Save", new Color(70,41,167), Color.WHITE);
            d.add(new JLabel()); d.add(save);
            save.addActionListener(ae -> {
                try {
                    int s = Integer.parseInt(seats.getText().trim());
                    if (editId==null) DatabaseManager.addFlight(fno.getText(), orig.getText(), dest.getText(), dep.getText(), arr.getText(), s);
                    else DatabaseManager.updateFlight(editId, fno.getText(), orig.getText(), dest.getText(), dep.getText(), arr.getText(), s);
                    loadFlights();
                    d.dispose();
                } catch (NumberFormatException nfe) { JOptionPane.showMessageDialog(d,"Seats must be a number"); }
                catch (SQLException sqe) { JOptionPane.showMessageDialog(d,"DB error: "+sqe.getMessage()); }
            });

            d.setVisible(true);
        }
    }
    // ---------------- User Panel ----------------
    public static class UserPanel extends JFrame {
        private final User currentUser;
        private DefaultTableModel flightsModel, myBookingsModel;
        private JTable flightsTable, bookingsTable;

        public UserPanel(User user) {
            super("Airline System — User Panel");
            this.currentUser = user;
            setSize(950,650);
            setLocationRelativeTo(null);
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setLayout(new BorderLayout());
            getContentPane().setBackground(new Color(30,30,30));
            JLabel title = new JLabel("Welcome, " + currentUser.username, SwingConstants.CENTER); 
            title.setFont(new Font("Segoe UI", Font.BOLD, 20)); 
            title.setForeground(Color.WHITE); 
            title.setBorder(BorderFactory.createEmptyBorder(15,0,15,0));
            add(title, BorderLayout.NORTH);
            flightsModel = new DefaultTableModel(new Object[]{"ID","Flight No","Origin","Destination","Departure","Arrival","Seats"},0) { public boolean isCellEditable(int r,int c){return false;}};
            flightsTable = new JTable(flightsModel); 
            styleTable(flightsTable);
            JScrollPane sp = new JScrollPane(flightsTable); 
            sp.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
            add(sp, BorderLayout.CENTER);
            JPanel bottom = new JPanel(); bottom.setBackground(new Color(30,30,30));
            JButton book = createStyledButton("Book Selected Flight", new Color(70,41,167), Color.WHITE);
            JButton view = createStyledButton("My Bookings", new Color(90,90,90), Color.WHITE);
            JButton logout = createStyledButton("Logout", new Color(200,0,0), Color.WHITE);
            bottom.add(book); bottom.add(view); bottom.add(logout);
            add(bottom, BorderLayout.SOUTH);
            book.addActionListener(e -> {
                int r = flightsTable.getSelectedRow(); if (r<0) { JOptionPane.showMessageDialog(this,"Select a flight"); return; }
                int id = (Integer)flightsModel.getValueAt(r,0);
                String fno = (String)flightsModel.getValueAt(r,1);
                String orig = (String)flightsModel.getValueAt(r,2);
                String dest = (String)flightsModel.getValueAt(r,3);
                SeatSelectionDialog sd = new SeatSelectionDialog(this, fno, orig, dest, currentUser.username);
                String seat = sd.showDialog();
                if (seat == null) return;
                PaymentDialog pd = new PaymentDialog(this, currentUser.username, fno, seat, orig, dest);
                boolean ok = pd.showDialog();
                if (ok) loadUserBookings();
            });
            view.addActionListener(e -> showMyBookings());
            logout.addActionListener(e -> { dispose(); new LoginFrame().setVisible(true); });

            loadFlights();
            loadUserBookings();
        }
        private void loadFlights() {
            flightsModel.setRowCount(0);
            for (Flight f : DatabaseManager.getAllFlights()) flightsModel.addRow(new Object[]{f.id, f.flightNo, f.origin, f.dest, f.dep, f.arr, f.seats});
        }
        private void loadUserBookings() {
            if (myBookingsModel == null) {
                myBookingsModel = new DefaultTableModel(new Object[]{"ID","Flight No","Passenger","Seat","Booked At"},0) { public boolean isCellEditable(int r,int c){return false;}};
                bookingsTable = new JTable(myBookingsModel); styleTable(bookingsTable);
            }
            myBookingsModel.setRowCount(0);
            String sql = "SELECT id, flight_no, passenger, seat, booked_at FROM bookings WHERE booked_by=? ORDER BY id DESC";
            try (Connection conn = DBConfig.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, currentUser.username);
                try (ResultSet rs = ps.executeQuery()) { while (rs.next()) myBookingsModel.addRow(new Object[]{rs.getInt("id"), rs.getString("flight_no"), rs.getString("passenger"), rs.getString("seat"), rs.getString("booked_at")}); }
            } catch (SQLException e) { e.printStackTrace(); }
        }
        private void showMyBookings() {
            JDialog d = new JDialog(this, "My Bookings", true);
            d.setSize(700,400); d.setLocationRelativeTo(this);
            d.setLayout(new BorderLayout());
            d.add(new JScrollPane(bookingsTable), BorderLayout.CENTER);
            JButton refresh = createStyledButton("Refresh", new Color(90,90,90), Color.WHITE);
            JPanel p = new JPanel(); p.add(refresh); d.add(p, BorderLayout.SOUTH);
            refresh.addActionListener(e -> loadUserBookings());
            d.setVisible(true);
        }
    }
    // ---------------- Seat Selection Dialog (6x6 grid) ----------------
    public static class SeatSelectionDialog extends JDialog {
        private final String flightNo, origin, destination, username;
        private String selectedSeat = null;
        private final Map<String, JToggleButton> btns = new HashMap<>();
        private final Color available = new Color(200,200,200); // grey
        private final Color booked = new Color(200,0,0); // red
        private final Color chosen = new Color(0,120,215); // blue

        public SeatSelectionDialog(Frame parent, String flightNo, String origin, String destination, String username) {
            super(parent, "Select Seat — " + flightNo, true);
            this.flightNo = flightNo; this.origin = origin; this.destination = destination; this.username = username;
            initUI(parent);
        }

        private void initUI(Frame parent) {
            setSize(680,520); setLocationRelativeTo(parent); getContentPane().setBackground(new Color(34,34,34));
            setLayout(new BorderLayout(8,8));
            JLabel lbl = new JLabel("Select seat for: " + flightNo, SwingConstants.CENTER); lbl.setForeground(Color.WHITE); lbl.setFont(new Font("Segoe UI", Font.BOLD, 16)); add(lbl, BorderLayout.NORTH);

            JPanel grid = new JPanel(new GridLayout(6,6,6,6));
            grid.setBackground(new Color(34,34,34));
            List<String> bookedSeats = DatabaseManager.getBookedSeats(flightNo);

            for (int r = 1; r <= 6; r++) {
                for (char c = 'A'; c <= 'F'; c++) {
                    String id = r + String.valueOf(c);
                    JToggleButton t = new JToggleButton(id);
                    t.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                    t.setOpaque(true);
                    t.setBorderPainted(false);
                    t.setFocusPainted(false);
                    if (bookedSeats.contains(id)) {
                        t.setBackground(booked);
                        t.setEnabled(false);
                        t.setForeground(Color.WHITE);
                    } else {
                        t.setBackground(available);
                        t.setForeground(Color.BLACK);
                        t.addActionListener(e -> {
                            // deselect others
                            if (selectedSeat != null && btns.containsKey(selectedSeat)) {
                                btns.get(selectedSeat).setBackground(available); btns.get(selectedSeat).setSelected(false); btns.get(selectedSeat).setForeground(Color.BLACK);
                            }
                            selectedSeat = id;
                            t.setBackground(chosen); t.setForeground(Color.WHITE);
                        });
                    }
                    btns.put(id, t);
                    grid.add(t);
                }
            }
            add(new JScrollPane(grid), BorderLayout.CENTER);

            JPanel bottom = new JPanel(); bottom.setBackground(new Color(34,34,34));
            JButton cancel = createStyledButton("Cancel", new Color(90,90,90), Color.WHITE);
            JButton proceed = createStyledButton("Proceed", new Color(70,41,167), Color.WHITE);
            bottom.add(cancel); bottom.add(proceed);
            add(bottom, BorderLayout.SOUTH);

            cancel.addActionListener(e -> { selectedSeat = null; dispose(); });
            proceed.addActionListener(e -> {
                if (selectedSeat == null) { JOptionPane.showMessageDialog(this, "Please select a seat"); return; }
                // double-check seat still available
                List<String> recheck = DatabaseManager.getBookedSeats(flightNo);
                if (recheck.contains(selectedSeat)) {
                    JOptionPane.showMessageDialog(this, "Seat was just booked by someone else. Choose another.");
                    btns.get(selectedSeat).setBackground(booked); btns.get(selectedSeat).setEnabled(false); selectedSeat = null; return;
                }
                dispose();
            });
        }

        public String showDialog() { setVisible(true); return selectedSeat; }
    }

    // ---------------- Payment Dialog ----------------
    public static class PaymentDialog extends JDialog {
        private final String username, flightNo, seat, origin, destination;
        private boolean success = false;

        public PaymentDialog(Frame parent, String username, String flightNo, String seat, String origin, String destination) {
            super(parent, "Payment", true);
            this.username = username; this.flightNo = flightNo; this.seat = seat; this.origin = origin; this.destination = destination;
            initUI(parent);
        }

        private void initUI(Frame parent) {
            setSize(420,300); setLocationRelativeTo(parent); setLayout(new GridLayout(6,1,8,8));
            getContentPane().setBackground(new Color(34,34,34));
            add(createLabel("Passenger: " + username));
            add(createLabel("Flight: " + flightNo + "   Seat: " + seat));
            add(createLabel("Select Payment Method:"));

            JComboBox<String> methods = new JComboBox<>(new String[]{"UPI", "Debit Card", "Credit Card", "NetBanking"});
            methods.setBackground(new Color(60,60,60)); methods.setForeground(Color.WHITE);
            add(methods);

            JPanel bottom = new JPanel(); bottom.setBackground(new Color(34,34,34));
            JButton cancel = createStyledButton("Cancel", new Color(90,90,90), Color.WHITE);
            JButton pay = createStyledButton("Pay Now (Mock)", new Color(70,41,167), Color.WHITE);
            bottom.add(cancel); bottom.add(pay); add(bottom);

            cancel.addActionListener(e -> { success = false; dispose(); });
            pay.addActionListener(e -> {
                String method = (String) methods.getSelectedItem();
                // final insert booking
                try {
                    int bookingId = DatabaseManager.addBookingReturnId(flightNo, username, seat, username);
                    if (bookingId > 0) {
                        String file = TicketGenerator.generateTextTicket(flightNo, username, seat, origin, destination, username, LocalDateTime.now().toString(), method, bookingId);
                        JOptionPane.showMessageDialog(this, "Payment successful. Ticket saved: " + file);
                        success = true;
                    } else {
                        JOptionPane.showMessageDialog(this, "Booking failed");
                        success = false;
                    }
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(this, "DB error: " + ex.getMessage());
                    success = false;
                    ex.printStackTrace();
                }
                dispose();
            });
        }

        public boolean showDialog() { setVisible(true); return success; }
    }

public static class TicketGenerator {
    public static String generateTextTicket(String flightNo, String passenger, String seat,
                                            String origin, String destination, String bookedBy,
                                            String bookedAt, String paymentMethod, int bookingId) {
        String fileName = "ticket_" + bookingId + ".txt";
        try (FileWriter fw = new FileWriter(new File(fileName))) {
            fw.write("===== AIRLINE E-TICKET =====\n");
            fw.write("Booking ID : " + bookingId + "\n");
            fw.write("Passenger   : " + passenger + "\n");
            fw.write("Flight No   : " + flightNo + "\n");
            fw.write("Seat        : " + seat + "\n");
            fw.write("From        : " + origin + "\n");
            fw.write("To          : " + destination + "\n");
            fw.write("Booked By   : " + bookedBy + "\n");
            fw.write("Booked At   : " + bookedAt + "\n");
            fw.write("Payment     : " + paymentMethod + "\n");
            fw.write("============================\n");
            fw.flush();

            // After saving ticket, trigger print option
            printTicket(fileName);

            return new File(fileName).getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static void printTicket(String filePath) {
        try {
            java.nio.file.Path path = java.nio.file.Paths.get(filePath);
            String ticketText = java.nio.file.Files.readString(path);

            java.awt.print.PrinterJob job = java.awt.print.PrinterJob.getPrinterJob();
            job.setPrintable((graphics, pageFormat, pageIndex) -> {
                if (pageIndex > 0) return java.awt.print.Printable.NO_SUCH_PAGE;

                java.awt.Graphics2D g2d = (java.awt.Graphics2D) graphics;
                g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY());

                java.awt.Font font = new java.awt.Font("Monospaced", java.awt.Font.PLAIN, 12);
                g2d.setFont(font);

                int y = 50;
                for (String line : ticketText.split("\n")) {
                    g2d.drawString(line, 50, y);
                    y += 15;
                }
                return java.awt.print.Printable.PAGE_EXISTS;
            });

            if (job.printDialog()) {
                job.print();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
    // ---------------- UI helpers ----------------
    static JLabel createLabel(String text) { JLabel l = new JLabel(text); l.setForeground(Color.WHITE); l.setFont(new Font("Segoe UI", Font.PLAIN, 13)); return l; }
    static void styleTextField(JTextField f) { f.setBackground(new Color(60,60,60)); f.setForeground(Color.WHITE); f.setCaretColor(Color.WHITE); f.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(80,80,80)), BorderFactory.createEmptyBorder(5,8,5,8))); f.setFont(new Font("Segoe UI", Font.PLAIN, 13)); }
    static JButton createStyledButton(String text, Color bg, Color fg) { JButton b = new JButton(text); b.setBackground(bg); b.setForeground(fg); b.setFont(new Font("Segoe UI", Font.BOLD, 13)); b.setFocusPainted(false); b.setBorderPainted(false); b.setOpaque(true); b.setCursor(new Cursor(Cursor.HAND_CURSOR)); b.setBorder(BorderFactory.createEmptyBorder(8,12,8,12)); return b; }
    static void styleTable(JTable t) { t.setBackground(new Color(50,50,50)); t.setForeground(Color.WHITE); t.setGridColor(new Color(70,70,70)); t.setRowHeight(26); t.setFont(new Font("Segoe UI", Font.PLAIN, 12)); t.setSelectionBackground(new Color(0,120,215)); t.setSelectionForeground(Color.WHITE); t.getTableHeader().setBackground(new Color(70,41,167)); t.getTableHeader().setForeground(Color.WHITE); t.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13)); t.getTableHeader().setBorder(BorderFactory.createLineBorder(new Color(70,41,167))); }
    static void styleTabbedPane(JTabbedPane t) { t.setBackground(new Color(40,40,40)); t.setForeground(Color.WHITE); t.setFont(new Font("Segoe UI", Font.BOLD, 13)); }
    static JButton createStyledButton(String text, Color bg, Color fg, int width, int height) {
    JButton button = new JButton(text);
    button.setBackground(bg);
    button.setForeground(fg);
    button.setFocusPainted(false);
    button.setFont(new Font("Segoe UI", Font.BOLD, 13));
    button.setPreferredSize(new Dimension(width, height));
    button.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
    return button;
}
    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); } catch (Exception ignored) {}
        if (!DBConfig.testConnection()) return;
        DatabaseManager.initSchemaAndDefaultAdmin();

        SwingUtilities.invokeLater(() -> new LoginFrame());
    }
}