import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

// =================================================================================
// 1. DATA MODEL CLASSES (Unchanged)
// =================================================================================

/**
 * Represents a single book. Implements Serializable for saving to a file.
 */
class Book implements Serializable {
    private static final long serialVersionUID = 1L;
    private final int id;
    private String title;
    private String author;
    private boolean isIssued;

    public Book(int id, String title, String author) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.isIssued = false;
    }

    // Getters
    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public boolean isIssued() { return isIssued; }

    // Setters
    public void setIssued(boolean issued) { isIssued = issued; }

    @Override
    public String toString() {
        return String.format("ID: %-5d | Title: %-30s | Author: %-25s | Status: %s",
                id, title, author, (isIssued ? "Issued" : "Available"));
    }
}

/**
 * Represents a library member.
 */
class Member implements Serializable {
    private static final long serialVersionUID = 1L;
    private final int id;
    private String name;

    public Member(int id, String name) {
        this.id = id;
        this.name = name;
    }

    // Getters
    public int getId() { return id; }
    public String getName() { return name; }

    @Override
    public String toString() {
        return String.format("ID: %-5d | Name: %s", id, name);
    }
}

/**
 * Represents a transaction of a book being issued to a member.
 */
class Transaction implements Serializable {
    private static final long serialVersionUID = 1L;
    private final int bookId;
    private final int memberId;
    private final LocalDate issueDate;
    private final LocalDate dueDate;

    public Transaction(int bookId, int memberId) {
        this.bookId = bookId;
        this.memberId = memberId;
        this.issueDate = LocalDate.now();
        this.dueDate = this.issueDate.plusDays(14); // 14-day borrowing period
    }

    // Getters
    public int getBookId() { return bookId; }
    public int getMemberId() { return memberId; }
    public LocalDate getDueDate() { return dueDate; }

    @Override
    public String toString() {
        return String.format("Book ID: %-5d | Member ID: %-5d | Issue Date: %s | Due Date: %s",
                bookId, memberId, issueDate, dueDate);
    }
}

/**
 * A wrapper class to hold all library data. This single object will be serialized.
 */
class LibraryData implements Serializable {
    private static final long serialVersionUID = 1L;
    List<Book> books = new ArrayList<>();
    List<Member> members = new ArrayList<>();
    List<Transaction> transactions = new ArrayList<>();
}


// =================================================================================
// 2. CORE LOGIC CLASS (The "How" - Modified for GUI)
// =================================================================================

/**
 * Manages all library operations and data. Methods return status strings for GUI feedback.
 */
class Library {
    private LibraryData data;
    private static final String FILE_NAME = "library_data.dat";
    private static final long FINE_PER_DAY = 1; // Fine of ₹1 per day

    public Library() {
        this.data = loadData();
    }

    // --- Book Management ---
    public String addBook(int id, String title, String author) {
        if (findBookById(id) != null) {
            return "Error: Book with this ID already exists.";
        }
        data.books.add(new Book(id, title, author));
        return "Book added successfully!";
    }

    public Book findBookById(int id) {
        return data.books.stream().filter(b -> b.getId() == id).findFirst().orElse(null);
    }

    public List<Book> getAllBooks() {
        return data.books;
    }

    // --- Member Management ---
    public String addMember(int id, String name) {
        if (findMemberById(id) != null) {
            return "Error: Member with this ID already exists.";
        }
        data.members.add(new Member(id, name));
        return "Member added successfully!";
    }

    public Member findMemberById(int id) {
        return data.members.stream().filter(m -> m.getId() == id).findFirst().orElse(null);
    }

    public List<Member> getAllMembers() {
        return data.members;
    }

    // --- Transaction Management ---
    public String issueBook(int bookId, int memberId) {
        Book book = findBookById(bookId);
        Member member = findMemberById(memberId);

        if (book == null) {
            return "Error: Book not found.";
        }
        if (member == null) {
            return "Error: Member not found.";
        }
        if (book.isIssued()) {
            return "Error: Book is already issued.";
        }

        book.setIssued(true);
        data.transactions.add(new Transaction(bookId, memberId));
        return "Book issued successfully.";
    }

    public String returnBook(int bookId) {
        Book book = findBookById(bookId);
        if (book == null) {
            return "Error: Book not found.";
        }
        if (!book.isIssued()) {
            return "Error: Book is not currently issued.";
        }

        Transaction transaction = data.transactions.stream()
                .filter(t -> t.getBookId() == bookId)
                .findFirst().orElse(null);

        String message = "";
        if (transaction != null) {
            long overdueDays = ChronoUnit.DAYS.between(transaction.getDueDate(), LocalDate.now());
            if (overdueDays > 0) {
                long fine = overdueDays * FINE_PER_DAY;
                message += String.format("Book is overdue by %d days. Fine to be paid: ₹%d\n", overdueDays, fine);
            }
            data.transactions.remove(transaction);
        }

        book.setIssued(false);
        message += "Book returned successfully.";
        return message;
    }

    // --- Reporting ---
    public List<Transaction> getOverdueTransactions() {
        return data.transactions.stream()
            .filter(t -> LocalDate.now().isAfter(t.getDueDate()))
            .collect(Collectors.toList());
    }

    public List<Transaction> getAllTransactions() {
        return data.transactions;
    }

    // --- Data Persistence ---
    public void saveData() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(FILE_NAME))) {
            oos.writeObject(data);
            System.out.println("Data saved successfully.");
        } catch (IOException e) {
            System.err.println("Error saving data: " + e.getMessage());
        }
    }

    private LibraryData loadData() {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(FILE_NAME))) {
            return (LibraryData) ois.readObject();
        } catch (FileNotFoundException e) {
            System.out.println("No existing data file found. Starting with a new library.");
            return new LibraryData();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error loading data: " + e.getMessage());
            return new LibraryData(); // Start fresh if file is corrupt
        }
    }
}

// =================================================================================
// 3. MAIN GUI AND APPLICATION CLASS
// =================================================================================
public class LibrarySystemGUI extends JFrame {
    private final Library library;
    private final DefaultTableModel bookTableModel;
    private final DefaultTableModel memberTableModel;
    private final JTextArea reportTextArea;

    public LibrarySystemGUI() {
        this.library = new Library();

        // --- Frame Setup ---
        setTitle("Library Management System");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE); // Handle save on close
        setLocationRelativeTo(null); // Center the window

        // --- Window Listener for Saving Data ---
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                library.saveData();
                e.getWindow().dispose();
                System.exit(0);
            }
        });

        // --- Tabbed Pane ---
        JTabbedPane tabbedPane = new JTabbedPane();

        // --- Models for Tables ---
        bookTableModel = new DefaultTableModel(new String[]{"ID", "Title", "Author", "Status"}, 0);
        memberTableModel = new DefaultTableModel(new String[]{"ID", "Name"}, 0);
        reportTextArea = new JTextArea(15, 60);
        reportTextArea.setEditable(false);

        // --- Create and Add Tabs ---
        tabbedPane.addTab("Book Management", createBookPanel());
        tabbedPane.addTab("Member Management", createMemberPanel());
        tabbedPane.addTab("Library Operations", createOperationsPanel());
        tabbedPane.addTab("Reports", createReportPanel());

        // Add tabbed pane to frame
        getContentPane().add(tabbedPane);

        // Initial data load
        refreshBookTable();
        refreshMemberTable();
    }

    private JPanel createBookPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // --- Input Form ---
        JPanel formPanel = new JPanel(new GridLayout(4, 2, 5, 5));
        JTextField idField = new JTextField();
        JTextField titleField = new JTextField();
        JTextField authorField = new JTextField();
        JButton addButton = new JButton("Add Book");

        formPanel.add(new JLabel("Book ID:"));
        formPanel.add(idField);
        formPanel.add(new JLabel("Title:"));
        formPanel.add(titleField);
        formPanel.add(new JLabel("Author:"));
        formPanel.add(authorField);
        formPanel.add(new JLabel("")); // Spacer
        formPanel.add(addButton);

        // --- Table View ---
        JTable bookTable = new JTable(bookTableModel);
        JScrollPane scrollPane = new JScrollPane(bookTable);

        // --- Add Components to Panel ---
        panel.add(formPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        // --- Action Listener ---
        addButton.addActionListener(e -> {
            try {
                int id = Integer.parseInt(idField.getText());
                String title = titleField.getText();
                String author = authorField.getText();
                if (title.isEmpty() || author.isEmpty()) {
                     JOptionPane.showMessageDialog(this, "Title and Author cannot be empty.", "Input Error", JOptionPane.ERROR_MESSAGE);
                     return;
                }
                String result = library.addBook(id, title, author);
                JOptionPane.showMessageDialog(this, result);
                refreshBookTable();
                idField.setText("");
                titleField.setText("");
                authorField.setText("");
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Please enter a valid numeric ID.", "Input Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        return panel;
    }

    private JPanel createMemberPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // --- Input Form ---
        JPanel formPanel = new JPanel(new GridLayout(3, 2, 5, 5));
        JTextField idField = new JTextField();
        JTextField nameField = new JTextField();
        JButton addButton = new JButton("Add Member");

        formPanel.add(new JLabel("Member ID:"));
        formPanel.add(idField);
        formPanel.add(new JLabel("Name:"));
        formPanel.add(nameField);
        formPanel.add(new JLabel("")); // Spacer
        formPanel.add(addButton);
        
        // --- Table View ---
        JTable memberTable = new JTable(memberTableModel);
        JScrollPane scrollPane = new JScrollPane(memberTable);

        // --- Add Components to Panel ---
        panel.add(formPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        // --- Action Listener ---
        addButton.addActionListener(e -> {
            try {
                int id = Integer.parseInt(idField.getText());
                String name = nameField.getText();
                 if (name.isEmpty()) {
                     JOptionPane.showMessageDialog(this, "Name cannot be empty.", "Input Error", JOptionPane.ERROR_MESSAGE);
                     return;
                }
                String result = library.addMember(id, name);
                JOptionPane.showMessageDialog(this, result);
                refreshMemberTable();
                idField.setText("");
                nameField.setText("");
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Please enter a valid numeric ID.", "Input Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        return panel;
    }

    private JPanel createOperationsPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // --- Issue Panel ---
        JPanel issuePanel = new JPanel(new GridLayout(3, 2, 10, 10));
        issuePanel.setBorder(BorderFactory.createTitledBorder("Issue a Book"));
        JTextField issueBookIdField = new JTextField(10);
        JTextField issueMemberIdField = new JTextField(10);
        JButton issueButton = new JButton("Issue Book");
        
        issuePanel.add(new JLabel("Book ID:"));
        issuePanel.add(issueBookIdField);
        issuePanel.add(new JLabel("Member ID:"));
        issuePanel.add(issueMemberIdField);
        issuePanel.add(new JLabel("")); // Spacer
        issuePanel.add(issueButton);

        // --- Return Panel ---
        JPanel returnPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        returnPanel.setBorder(BorderFactory.createTitledBorder("Return a Book"));
        JTextField returnBookIdField = new JTextField(10);
        JButton returnButton = new JButton("Return Book");

        returnPanel.add(new JLabel("Book ID:"));
        returnPanel.add(returnBookIdField);
        returnPanel.add(new JLabel("")); // Spacer
        returnPanel.add(returnButton);

        // --- Action Listeners ---
        issueButton.addActionListener(e -> {
            try {
                int bookId = Integer.parseInt(issueBookIdField.getText());
                int memberId = Integer.parseInt(issueMemberIdField.getText());
                String result = library.issueBook(bookId, memberId);
                JOptionPane.showMessageDialog(this, result);
                refreshBookTable(); // Update status in book list
                issueBookIdField.setText("");
                issueMemberIdField.setText("");
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Please enter valid numeric IDs.", "Input Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        returnButton.addActionListener(e -> {
            try {
                int bookId = Integer.parseInt(returnBookIdField.getText());
                String result = library.returnBook(bookId);
                JOptionPane.showMessageDialog(this, result);
                refreshBookTable(); // Update status in book list
                returnBookIdField.setText("");
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Please enter a valid numeric ID.", "Input Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        panel.add(issuePanel);
        panel.add(Box.createRigidArea(new Dimension(0, 20))); // Spacer
        panel.add(returnPanel);

        return panel;
    }

    private JPanel createReportPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // --- Button Panel ---
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        JButton issuedBooksButton = new JButton("View All Issued Books");
        JButton overdueBooksButton = new JButton("View All Overdue Books");
        buttonPanel.add(issuedBooksButton);
        buttonPanel.add(overdueBooksButton);

        // --- Report Display Area ---
        JScrollPane scrollPane = new JScrollPane(reportTextArea);
        
        panel.add(buttonPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // --- Action Listeners ---
        issuedBooksButton.addActionListener(e -> {
            StringBuilder report = new StringBuilder("--- All Issued Books ---\n");
            List<Transaction> transactions = library.getAllTransactions();
            if (transactions.isEmpty()){
                report.append("No books are currently issued.");
            } else {
                transactions.forEach(t -> report.append(t.toString()).append("\n"));
            }
            reportTextArea.setText(report.toString());
        });
        
        overdueBooksButton.addActionListener(e -> {
            StringBuilder report = new StringBuilder("--- Overdue Books ---\n");
            List<Transaction> overdue = library.getOverdueTransactions();
            if (overdue.isEmpty()){
                report.append("No books are currently overdue.");
            } else {
                overdue.forEach(t -> report.append(t.toString()).append("\n"));
            }
            reportTextArea.setText(report.toString());
        });

        return panel;
    }

    // --- Utility Methods to Refresh UI Data ---
    private void refreshBookTable() {
        bookTableModel.setRowCount(0); // Clear existing data
        for (Book book : library.getAllBooks()) {
            bookTableModel.addRow(new Object[]{
                book.getId(),
                book.getTitle(),
                book.getAuthor(),
                book.isIssued() ? "Issued" : "Available"
            });
        }
    }

    private void refreshMemberTable() {
        memberTableModel.setRowCount(0); // Clear existing data
        for (Member member : library.getAllMembers()) {
            memberTableModel.addRow(new Object[]{
                member.getId(),
                member.getName()
            });
        }
    }
    
    // =================================================================================
    // 4. MAIN APPLICATION METHOD (The "Starter" - Launches the GUI)
    // =================================================================================
    public static void main(String[] args) {
        // Run the GUI on the Event Dispatch Thread (EDT) for thread safety
        SwingUtilities.invokeLater(() -> {
            new LibrarySystemGUI().setVisible(true);
        });
    }
}