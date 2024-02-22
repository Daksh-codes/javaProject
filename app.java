import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.*;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableModel;

class Logic {

  private Connection conn = null;

  public Logic() {
    try {
      Class.forName("org.sqlite.JDBC");
      conn = DriverManager.getConnection("jdbc:sqlite:marks.db");
      System.out.println("Opened database successfully");
    } catch (Exception e) {
      System.err.println(e.getClass().getName() + ": " + e.getMessage());
      e.printStackTrace();
    }
  }

  public void closeConnection() {
    try {
      if (conn != null) {
        conn.close();
        System.out.println("Closed database connection");
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  public boolean createUser(String user, String pass, String role) {
    String createUserTableSQL =
      "CREATE TABLE IF NOT EXISTS users (\n" +
      "id INTEGER PRIMARY KEY AUTOINCREMENT,\n" +
      "name TEXT NOT NULL,\n" +
      "pass TEXT NOT NULL,\n" +
      "role TEXT NOT NULL\n" +
      ");";

    String addUser =
      "INSERT INTO users (name, pass , role) VALUES ('" +
      user +
      "','" +
      pass +
      "','" +
      role +
      "');";

    try (Statement stmt = conn.createStatement()) {
      stmt.executeUpdate(createUserTableSQL);
      stmt.executeUpdate(addUser);
      return true;
    } catch (SQLException e) {
      e.printStackTrace();
      return false;
    }
  }

  public String authenticate(String user, String pass) {
    String q = "Select pass , id from users where name = " + "'" + user + "'";

    try (Statement stmt = conn.createStatement()) {
      ResultSet rs = stmt.executeQuery(q);
      if (rs.next()) {
        String ogPass = rs.getString("pass");
        String id = rs.getString("id");
        if (ogPass.equals(pass)) {
          //System.out.println("\n"+id+"\n");
          return id;
        }
        //System.out.println("\n"+pass+" "+ogPass+"\n");
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return "error";
  }

  public String getRole(String id) {
    String q = "Select role from users where id = " + "'" + id + "'";

    try (Statement stmt = conn.createStatement()) {
      ResultSet rs = stmt.executeQuery(q);
      if (rs.next()) {
        String role = rs.getString("role");
        return role;
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return "error";
  }

  public void createClassTable(
    String className,
    int numOfStudents,
    String[] subjects
  ) {
    StringBuilder createTableSQL = new StringBuilder();
    createTableSQL
      .append("CREATE TABLE IF NOT EXISTS ")
      .append(className)
      .append(" (\n");
    createTableSQL.append("student_id INTEGER PRIMARY KEY AUTOINCREMENT,\n");

    // Add columns for each subject
    for (String subject : subjects) {
      createTableSQL.append(subject).append(" INTEGER,\n");
    }

    // Remove the trailing comma and newline
    createTableSQL.delete(createTableSQL.length() - 2, createTableSQL.length());
    createTableSQL.append("\n);");

    try (Statement stmt = conn.createStatement()) {
      stmt.executeUpdate(createTableSQL.toString());
      System.out.println("Table created successfully: " + className);

      // Insert empty marks for each student
      for (int i = 1; i <= numOfStudents; i++) {
        StringBuilder insertSQL = new StringBuilder();
        insertSQL
          .append("INSERT INTO ")
          .append(className)
          .append(" DEFAULT VALUES;");
        stmt.executeUpdate(insertSQL.toString());
      }
      System.out.println(
        "Empty marks inserted for " + numOfStudents + " students."
      );
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  public Vector<String> fetchAllTableNames() {
    Vector<String> tableNames = new Vector<>();

    // SQL query to retrieve all table names
    String sql = "SELECT name FROM sqlite_master WHERE type='table'";

    try (
      Statement stmt = conn.createStatement();
      ResultSet rs = stmt.executeQuery(sql)
    ) {
      // Iterate through the result set and add table names to the vector
      while (rs.next()) {
        String tableName = rs.getString("name");
        tableNames.add(tableName);
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }

    return tableNames;
  }

  public Object[][] fetchMarks(String className) {
    try (Statement stmt = conn.createStatement()) {
      int rowCount = 0;
      ResultSet rs1 = stmt.executeQuery(
        "SELECT COUNT(*) AS row_count FROM " + "sem12"
      );
      if (rs1.next()) {
        rowCount = rs1.getInt("row_count");
      }
      String sql = "Select * from " + className + ";";
      ResultSet rs = stmt.executeQuery(sql);
      ResultSetMetaData metaData = rs.getMetaData();
      int columnCount = metaData.getColumnCount();

      Object[][] data = new Object[rowCount][columnCount];

      int row = 0;
      while (rs.next()) {
        System.out.println("loop:");
        for (int j = 0; j < columnCount; j++) {
          data[row][j] = rs.getObject(j + 1); // Since ResultSet columns are 1-indexed
          System.out.println(j + 1 + " " + rs.getObject(j + 1));
        }
        row++;
      }
      return data;
    } catch (SQLException e) {
      e.printStackTrace();
      // Return an empty 2D array in case of exception
      return new Object[0][0];
    }
  }

  public void addMarks(
    String className,
    int student_id,
    String subject,
    int marks
  ) {
    String updateMarksSQL =
      "UPDATE " + className + " SET " + subject + " = ? WHERE student_id = ?";
    try (PreparedStatement pstmt = conn.prepareStatement(updateMarksSQL)) {
      pstmt.setInt(1, marks);
      pstmt.setInt(2, student_id);
      pstmt.executeUpdate();
      System.out.println(
        "Marks added successfully for Student ID: " +
        student_id +
        ", Subject: " +
        subject
      );
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  public String[] fetchStudentIDs(String className) {
    String fetchStudentIDsSQL = "SELECT student_id FROM " + className;
    Vector<String> studentIDs = new Vector<>();
    try (Statement stmt = conn.createStatement()) {
      ResultSet rs = stmt.executeQuery(fetchStudentIDsSQL);
      while (rs.next()) {
        String studentID = rs.getString("student_id");
        studentIDs.add(studentID);
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return studentIDs.toArray(new String[0]);
  }

  public String[] fetchSubjects(String className) {
    String fetchSubjectsSQL = "PRAGMA table_info(" + className + ")";
    Vector<String> subjectsList = new Vector<>();
    try (Statement stmt = conn.createStatement()) {
      ResultSet rs = stmt.executeQuery(fetchSubjectsSQL);
      while (rs.next()) {
        String columnName = rs.getString("name");
        // Exclude the student_id column
        // if (!columnName.equals("student_id")) {
        //     subjectsList.add(columnName);
        // }
        subjectsList.add(columnName);
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return subjectsList.toArray(new String[0]);
  }

  public static void main(String[] args) {
    Logic logic = new Logic();
    //logic.createUser("hahaha123", "password", "superUser");
    logic.authenticate("hahaha123", "password");
    logic.closeConnection();
  }
}

public class App extends JFrame {

  JFrame f;
  JPanel loginPanel;
  JPanel registerPanel;
  JPanel dashPanel;
  String userId;
  final int panelW = 400;
  final int panelH = 400;
  Font boldFont = new Font("Verdana", Font.BOLD, 24);
  Font normalFont = new Font("Verdana", Font.PLAIN, 20);
  Font smallFont = new Font("Verdana", Font.PLAIN, 14);
  Logic l = new Logic();

  App() {
    f = new JFrame("Marks");
    f.setSize(600, 600);
    f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    f.getContentPane().setBackground(new Color(34, 87, 122));

    // Using CardLayout
    CardLayout cardLayout = new CardLayout();
    f.setLayout(cardLayout);

    //cardLayout.show(f.getContentPane(), "Dashboard");
    loginPanel = login();
    registerPanel = register();
    
    // Adding panels to the frame with corresponding names
    f.add(loginPanel, "Login");
    f.add(registerPanel, "Register");

    f.setVisible(true);

    // here changing the position of panel based on the size of the window
    f.addComponentListener(
      new ComponentAdapter() {
        @Override
        public void componentResized(ComponentEvent e) {
          // Revalidate and repaint to update the layout
          f.revalidate();
          f.repaint();
        }
      }
    );
  }

  private JPanel login() {
    loginPanel = new JPanel(new BorderLayout()); // Using BorderLayout for loginPanel

    JPanel contentPanel = new JPanel(new GridBagLayout()); // Content panel to center components
    contentPanel.setBackground(Color.WHITE);
    Border blackline = BorderFactory.createLineBorder(Color.black);
    contentPanel.setBorder(blackline);
    GridBagConstraints c = new GridBagConstraints();

    // Heading
    JLabel h1 = new JLabel("Welcome Back");
    h1.setFont(boldFont);
    c.gridx = 0;
    c.gridy = 0;
    c.gridwidth = 2; // Span across two columns
    c.insets = new Insets(10, 0, 10, 0); // Add some vertical space
    contentPanel.add(h1, c);

    // Reset gridwidth and insets
    c.gridwidth = 1;
    c.insets = new Insets(0, 0, 0, 0);

    // UserName Input
    JLabel ulabel = new JLabel("User ");
    ulabel.setFont(normalFont);
    JTextField user = new JTextField(20);
    c.gridx = 0;
    c.gridy = 1;
    contentPanel.add(ulabel, c);
    c.gridx = 1;
    c.gridy = 1;
    contentPanel.add(user, c);

    // Pass Input
    JLabel plabel = new JLabel("Password ");
    plabel.setFont(normalFont);
    JPasswordField pass = new JPasswordField(20);
    c.gridx = 0;
    c.gridy = 2;
    contentPanel.add(plabel, c);
    c.gridx = 1;
    c.gridy = 2;
    contentPanel.add(pass, c);

    // Login Btn
    JButton loginBtn = new JButton("Login");
    loginBtn.setBackground(new Color(0, 168, 232));
    loginBtn.setFont(normalFont);
    c.gridx = 0;
    c.gridy = 3;
    c.gridwidth = 2; // Span across two columns
    c.insets = new Insets(10, 0, 10, 0); // Add some vertical space
    contentPanel.add(loginBtn, c);
    loginBtn.addActionListener(
      new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          userId =
            l.authenticate(user.getText(), String.valueOf(pass.getPassword()));
          if (userId.equals("error")) {
            // Display error message in a dialog box
            JDialog d = new JDialog(f, "Error", true);
            JLabel mes = new JLabel("Error in Login!");
            d.add(mes);
            d.pack(); // Adjusts the size of the dialog to fit its contents
            d.setLocationRelativeTo(f); // Centers the dialog relative to the frame
            d.setVisible(true);
          } else {
            System.out.println("Login successful. User ID: " + userId);
            dashPanel = dashboard(user.getText(), userId);
            f.add(dashPanel, "Dashboard");
            CardLayout cardLayout = (CardLayout) f.getContentPane().getLayout();
            cardLayout.show(f.getContentPane(), "Dashboard");
          }
        }
      }
    );

    // Reset gridwidth and insets
    c.gridwidth = 1;
    c.insets = new Insets(0, 0, 0, 0);

    // Link to register page
    JLabel link = new JLabel("Don't have an account?");
    link.setFont(smallFont);
    JButton registerLink = new JButton("Register");
    registerLink.setForeground(new Color(0, 168, 232));
    registerLink.setFont(smallFont);
    registerLink.setBorderPainted(false);
    registerLink.setFocusPainted(false);
    registerLink.setContentAreaFilled(false);
    registerLink.addActionListener(
      new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            
          CardLayout cardLayout = (CardLayout) f.getContentPane().getLayout();
          cardLayout.show(f.getContentPane(), "Register");
        }
      }
    );

    JPanel linkPanel = new JPanel(new FlowLayout(FlowLayout.CENTER)); // Panel for the link
    linkPanel.setOpaque(false);
    linkPanel.add(link);
    linkPanel.add(registerLink);

    // Adding components to the loginPanel
    loginPanel.add(contentPanel, BorderLayout.CENTER);
    loginPanel.add(linkPanel, BorderLayout.SOUTH);

    return loginPanel;
  }

  private JPanel register() {
    registerPanel = new JPanel(new BorderLayout()); // Using BorderLayout for registerPanel

    JPanel contentPanel = new JPanel(new GridBagLayout()); // Content panel to center components
    contentPanel.setBackground(Color.WHITE);
    Border blackline = BorderFactory.createLineBorder(Color.black);
    contentPanel.setBorder(blackline);
    GridBagConstraints c = new GridBagConstraints();

    // Heading
    JLabel h1 = new JLabel("Create Account");
    h1.setFont(boldFont);
    c.gridx = 0;
    c.gridy = 0;
    c.gridwidth = 2; // Span across two columns
    c.insets = new Insets(10, 0, 10, 0); // Add some vertical space
    contentPanel.add(h1, c);

    // Reset gridwidth and insets
    c.gridwidth = 1;
    c.insets = new Insets(0, 0, 0, 0);

    // UserName Input
    JLabel ulabel = new JLabel("User ");
    ulabel.setFont(normalFont);
    JTextField user = new JTextField(20);
    c.gridx = 0;
    c.gridy = 1;
    contentPanel.add(ulabel, c);
    c.gridx = 1;
    c.gridy = 1;
    contentPanel.add(user, c);

    // Pass Input
    JLabel plabel = new JLabel("Password ");
    plabel.setFont(normalFont);
    JPasswordField pass = new JPasswordField(20);
    c.gridx = 0;
    c.gridy = 2;
    contentPanel.add(plabel, c);
    c.gridx = 1;
    c.gridy = 2;
    contentPanel.add(pass, c);

    // User or SuperUser
    JRadioButton r1 = new JRadioButton("SuperUser");
    JRadioButton r2 = new JRadioButton("User");
    r1.setBackground(Color.WHITE);
    r2.setBackground(Color.WHITE);
    ButtonGroup bg = new ButtonGroup();
    bg.add(r1);
    bg.add(r2);
    JPanel radioPanel = new JPanel(new FlowLayout(FlowLayout.CENTER)); // Panel for radio buttons
    radioPanel.setOpaque(false);
    radioPanel.add(r1);
    radioPanel.add(r2);
    c.gridx = 0;
    c.gridy = 3;
    c.gridwidth = 2; // Span across two columns
    c.insets = new Insets(10, 0, 10, 0); // Add some vertical space
    contentPanel.add(radioPanel, c);

    // Reset gridwidth and insets
    c.gridwidth = 1;
    c.insets = new Insets(0, 0, 0, 0);

    // Register Btn
    JButton registerBtn = new JButton("Register");
    registerBtn.setBackground(new Color(0, 168, 232));
    registerBtn.setFont(normalFont);
    c.gridx = 0;
    c.gridy = 4;
    c.gridwidth = 2; // Span across two columns
    c.insets = new Insets(10, 0, 10, 0); // Add some vertical space
    contentPanel.add(registerBtn, c);
    registerBtn.addActionListener(
      new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          // Check which radio button is selected
          String userType;
          if (r1.isSelected()) {
            userType = r1.getText(); // SuperUser
          } else if (r2.isSelected()) {
            userType = r2.getText(); // User
          } else {
            // No radio button selected
            // Handle this case according to your requirements
            return;
          }

          // Register the user with the selected user type
          Boolean res = l.createUser(
            user.getText(),
            String.valueOf(pass.getPassword()),
            userType
          );

          if (!res) {
            // Display error message in a dialog box
            JDialog d = new JDialog(f, "Error", true);
            JLabel mes = new JLabel("Error in Registration!");
            d.add(mes);
            d.pack(); // Adjusts the size of the dialog to fit its contents
            d.setLocationRelativeTo(f); // Centers the dialog relative to the frame
            d.setVisible(true);
          } else {
            System.out.println("Registration successful.");
            // Proceed with further actions if registration is successful
            CardLayout cardLayout = (CardLayout) f.getContentPane().getLayout();
          cardLayout.show(f.getContentPane(), "Login");
          }
        }
      }
    );

    // Reset gridwidth and insets
    c.gridwidth = 1;
    c.insets = new Insets(0, 0, 0, 0);

    // Link to login page
    JLabel link = new JLabel("Already have an account?");
    link.setFont(smallFont);
    JButton loginLink = new JButton("Login");
    loginLink.setForeground(new Color(0, 168, 232));
    loginLink.setFont(smallFont);
    loginLink.setBorderPainted(false);
    loginLink.setFocusPainted(false);
    loginLink.setContentAreaFilled(false);
    loginLink.addActionListener(
      new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          CardLayout cardLayout = (CardLayout) f.getContentPane().getLayout();
          cardLayout.show(f.getContentPane(), "Login");
        }
      }
    );

    JPanel linkPanel = new JPanel(new FlowLayout(FlowLayout.CENTER)); // Panel for the link
    linkPanel.setOpaque(false);
    linkPanel.add(link);
    linkPanel.add(loginLink);

    // Adding components to the registerPanel
    registerPanel.add(contentPanel, BorderLayout.CENTER);
    registerPanel.add(linkPanel, BorderLayout.SOUTH);

    return registerPanel;
  }

  private JPanel dashboard(String user, String id) {
    dashPanel = new JPanel(new BorderLayout());
    CardLayout cardLayout = new CardLayout();
    dashPanel.setLayout(cardLayout);

    // Create combo boxes
    JComboBox<String> showTableComboBox = new JComboBox<>();
    JComboBox<String> addTableComboBox = new JComboBox<>();

    JPanel contentPanel = new JPanel(new GridBagLayout());
    contentPanel.setBackground(Color.WHITE);
    Border blackline = BorderFactory.createLineBorder(Color.black);
    contentPanel.setBorder(blackline);
    GridBagConstraints c = new GridBagConstraints();

    JPanel panelWithBorder = new JPanel(new BorderLayout()); // Panel to hold contentPanel with border

    String userRole = l.getRole(id);
    System.out.println(userRole);
    JLabel ulabel = new JLabel("Welcome " + user);
    ulabel.setFont(boldFont);
    c.gridx = 0;
    c.gridy = 0;
    c.anchor = GridBagConstraints.LINE_START; // Align text to the left
    c.insets = new Insets(10, 10, 10, 10); // Add padding
    contentPanel.add(ulabel, c);

    if (userRole.equals("SuperUser")) {
      JButton createClass = new JButton("Create class");
      createClass.setFont(boldFont);
      c.gridx = 0;
      c.gridy = 1;
      contentPanel.add(createClass, c);

      createClass.addActionListener(
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            cardLayout.show(dashPanel, "create class");
          }
        }
      );
    }
      JButton showMarks = new JButton("Show Marks");
      showMarks.setFont(boldFont);
      c.gridy = 2;
      contentPanel.add(showMarks, c);

      showMarks.addActionListener(
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            // Pass the selected table name to the showMarksJPanel method
            String selectedTableName = (String) showTableComboBox.getSelectedItem();
            JPanel showMarksPanel = showMarksJPanel(
              dashPanel,
              selectedTableName
            );
            dashPanel.add(showMarksPanel, "show marks");
            cardLayout.show(dashPanel, "show marks");
          }
        }
      );

      // Add the combo box for showing marks
      c.gridy = 3;
      contentPanel.add(showTableComboBox, c);

      JButton addMarks = new JButton("Add Marks");
      addMarks.setFont(boldFont);
      c.gridy = 4;
      contentPanel.add(addMarks, c);

      addMarks.addActionListener(
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            // Pass the selected table name to the addMarksJPanel method
            String selectedTableName = (String) addTableComboBox.getSelectedItem();
            JPanel addMarksPanel = addMarksJPanel(dashPanel, selectedTableName);
            dashPanel.add(addMarksPanel, "add marks");
            cardLayout.show(dashPanel, "add marks");
          }
        }
      );

      // Add the combo box for adding marks
      c.gridy = 5;
      contentPanel.add(addTableComboBox, c);
    

    panelWithBorder.add(contentPanel, BorderLayout.CENTER);
    panelWithBorder.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    dashPanel.add(panelWithBorder, "main page"); // Add panelWithBorder to dashPanel

    // Create the other panels
    JPanel createClassPanel = createClassJPanel(dashPanel);
    //JPanel addMarksPanel = addMarksJPanel(dashPanel);

    // Add panels to the dashPanel
    dashPanel.add(createClassPanel, "create class");
    //dashPanel.add(addMarksPanel, "update class");

    // Populate combo boxes with initial table names
    populateTableComboBoxes(showTableComboBox, addTableComboBox);

    return dashPanel;
  }

  // Method to populate combo boxes with initial table names
  private void populateTableComboBoxes(
    JComboBox<String> showTableComboBox,
    JComboBox<String> addTableComboBox
  ) {
    Vector<String> tableNames = l.fetchAllTableNames();
    showTableComboBox.setModel(new DefaultComboBoxModel<>(tableNames));
    addTableComboBox.setModel(new DefaultComboBoxModel<>(tableNames));
  }

  private JPanel createClassJPanel(Container parentContainer) {
    JPanel createClassPanel = new JPanel(new BorderLayout());
    JPanel formPanel = new JPanel(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    formPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20)); // Add padding

    // Class Name
    JLabel classNameLabel = new JLabel("Class Name:");
    JTextField classNameField = new JTextField(20);
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.anchor = GridBagConstraints.WEST;
    formPanel.add(classNameLabel, gbc);
    gbc.gridx = 1;
    formPanel.add(classNameField, gbc);

    // Number of Students
    JLabel numStudentsLabel = new JLabel("Number of Students:");
    JTextField numStudentsField = new JTextField(20);
    gbc.gridx = 0;
    gbc.gridy = 1;
    formPanel.add(numStudentsLabel, gbc);
    gbc.gridx = 1;
    formPanel.add(numStudentsField, gbc);

    // Number of Subjects
    JLabel numSubjectsLabel = new JLabel("Number of Subjects:");
    JTextField numSubjectsField = new JTextField(20);
    gbc.gridx = 0;
    gbc.gridy = 2;
    formPanel.add(numSubjectsLabel, gbc);
    gbc.gridx = 1;
    formPanel.add(numSubjectsField, gbc);

    // Submit Button
    JButton submitButton = new JButton("Submit");
    gbc.gridx = 0;
    gbc.gridy = 3;
    gbc.gridwidth = 2;
    gbc.insets = new Insets(10, 0, 0, 0);
    gbc.anchor = GridBagConstraints.CENTER;
    formPanel.add(submitButton, gbc);

    submitButton.addActionListener(
      new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          if (
            isValidInput(numStudentsField.getText()) &&
            isValidInput(numSubjectsField.getText())
          ) {
            int numStudents = Integer.parseInt(numStudentsField.getText());
            int numSubjects = Integer.parseInt(numSubjectsField.getText());
            String[] subjects = new String[numSubjects]; // Create the subjects array

            // Get subject names
            for (int i = 0; i < numSubjects; i++) {
              subjects[i] =
                JOptionPane.showInputDialog(
                  createClassPanel,
                  "Enter subject " + (i + 1) + " name:"
                );
            }

            // Create the class table
            l.createClassTable(classNameField.getText(), numStudents, subjects);

            // Navigate back to the main page
            CardLayout cardLayout = (CardLayout) parentContainer.getLayout();
            cardLayout.show(parentContainer, "main page");
          } else {
            JOptionPane.showMessageDialog(
              createClassPanel,
              "Please enter valid numerical values.",
              "Invalid Input",
              JOptionPane.ERROR_MESSAGE
            );
          }
        }
      }
    );

    createClassPanel.add(formPanel, BorderLayout.CENTER);

    return createClassPanel;
  }

  private boolean isValidInput(String input) {
    if (input == null || input.isEmpty()) {
      return false;
    }
    try {
      int value = Integer.parseInt(input);
      return value >= 0; // Ensure the value is non-negative
    } catch (NumberFormatException e) {
      return false;
    }
  }

  private JPanel showMarksJPanel(Container parentContainer, String className) {
    JPanel panel = new JPanel(new BorderLayout());

    // Fetch marks from the database
    Object[][] data = l.fetchMarks(className);

    // Check if data is not null and has at least one row
    if (data != null && data.length > 0) {
      // Extract column names from the first row of data
      String[] columnNames = new String[data[0].length];
      for (int i = 0; i < columnNames.length; i++) {
        columnNames[i] = "Subject " + (i + 1);
      }
      String[] subjects = l.fetchSubjects(className);

      // Create table model with data and column names
      DefaultTableModel model = new DefaultTableModel(data, subjects);

      // Create table
      JTable table = new JTable(model);
      table.setFont(new Font("Verdana", Font.PLAIN, 16));

      // Set row height
      table.setRowHeight(30);

      // Create scroll pane
      JScrollPane scrollPane = new JScrollPane(table);
      scrollPane.setPreferredSize(new Dimension(500, 400));

      // Add scroll pane to panel
      panel.add(scrollPane, BorderLayout.CENTER);

      JButton btn = new JButton("Back");
      btn.addActionListener(
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            CardLayout cardLayout = (CardLayout) parentContainer.getLayout();
            cardLayout.show(parentContainer, "main page");
          }
        }
      );
      panel.add(btn, BorderLayout.SOUTH);
    } else {
      // If data is null or empty, display a message
      JLabel messageLabel = new JLabel("No marks available for this class.");
      messageLabel.setFont(new Font("Verdana", Font.PLAIN, 16));
      messageLabel.setHorizontalAlignment(JLabel.CENTER);
      panel.add(messageLabel, BorderLayout.CENTER);
    }

    return panel;
  }

  private JPanel addMarksJPanel(Container parentContainer, String className) {
    JPanel contentPanel = new JPanel();

    // Fetch marks data and subjects
    Object[][] data = l.fetchMarks(className);
    String[] subjects = l.fetchSubjects(className);

    // Create a new array to hold data for the table with student IDs
    Object[][] marksData = new Object[data.length][subjects.length + 1];

    // Iterate over the fetched data and populate marksData array
    for (int i = 0; i < data.length; i++) {
      marksData[i][0] = data[i][0]; // Copy student IDs

      // Iterate over each subject to copy marks data
      for (int j = 1; j <= subjects.length; j++) {
        // Check if marks data is available for the subject
        if (j < data[i].length) {
          marksData[i][j] = data[i][j]; // Copy marks if available
        } else {
          marksData[i][j] = ""; // Otherwise, leave the cell empty
        }
      }
    }

    // Create a DefaultTableModel with marksData array and subjects
    DefaultTableModel model = new DefaultTableModel(marksData, subjects);

    // Create a JTable with the model
    JTable table = new JTable(model);

    // Create a JScrollPane to hold the table
    JScrollPane scrollPane = new JScrollPane(table);

    // Add the scroll pane to the content panel
    contentPanel.add(scrollPane);

    JButton btn = new JButton("Back");
    btn.addActionListener(
      new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          CardLayout cardLayout = (CardLayout) parentContainer.getLayout();
          cardLayout.show(parentContainer, "main page");
        }
      }
    );
    contentPanel.add(btn);

    // Create and add a submit button
    JButton submitButton = new JButton("Submit");
    submitButton.addActionListener(
      new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          // Iterate over the table rows
          for (int i = 0; i < table.getRowCount(); i++) {
            // Extract student ID
            int studentID = Integer.parseInt(table.getValueAt(i, 0).toString());

            // Iterate over subjects
            for (int j = 1; j < table.getColumnCount(); j++) {
              // Extract subject name
              String subject = table.getColumnName(j);

              // Extract marks and handle null values
              Object marksObj = table.getValueAt(i, j);
              int marks = 0; // Default value if marks are null
              if (marksObj != null) {
                String marksStr = marksObj.toString();
                if (!marksStr.isEmpty()) {
                  marks = Integer.parseInt(marksStr);
                }
              }

              // Add marks to the database
              l.addMarks(className, studentID, subject, marks);
            }
          }

          // Show success message or perform any other necessary actions
          JOptionPane.showMessageDialog(
            contentPanel,
            "Marks added successfully.",
            "Success",
            JOptionPane.INFORMATION_MESSAGE
          );
        }
      }
    );

    contentPanel.add(submitButton);

    return contentPanel;
  }

  private JPanel updateClassJPanel() {
    JPanel updateClassPanel = new JPanel(new BorderLayout());

    // Your code for creating the "Update Class" panel goes here
    // For now, I'll just create a simple panel with a label
    JLabel label = new JLabel("Update Class Panel");
    label.setFont(boldFont);
    label.setHorizontalAlignment(JLabel.CENTER);
    updateClassPanel.add(label, BorderLayout.CENTER);

    return updateClassPanel;
  }

  public static void main(String args[]) {
    App a = new App();
  }
}
