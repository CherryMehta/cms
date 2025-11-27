

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.util.Scanner;
import java.sql.Timestamp;

public class CanteenManagementSystem {
    private static final String url = "jdbc:mysql://localhost:3306/canteen2_db";
    private static final String username = "root";
    private static final String password = "cherry07";

    public static void main(String[] args) {
        
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.out.println("JDBC Driver not found: " + e.getMessage());
        }

        try (Connection connection = DriverManager.getConnection(url, username, password);
             Scanner scanner = new Scanner(System.in)) {

            while (true) {
                System.out.println();
                System.out.println("CAMPUS CRAVINGS ");
                System.out.println("1. View Menu");
                System.out.println("2. Place An Order");
                System.out.println("3. View Orders");
                System.out.println("4. Get Order ID");
                System.out.println("5. Cancel Order");
                System.out.println("6. Get Order History");
                System.out.println("7. Staff Login");
                System.out.println("0. Exit");
                System.out.print("Choose an option: ");

                int choice;
                if (scanner.hasNextInt()) {
                    choice = scanner.nextInt();
                    scanner.nextLine(); // consume newline
                } else {
                    System.out.println("Please enter a valid number.");
                    scanner.nextLine(); // discard invalid input
                    continue;
                }

                switch (choice) {
                    case 1:
                        viewMenu(connection);
                        break;
                    case 2:
                        placeAnOrder(connection, scanner);
                        break;
                    case 3:
                        viewOrders(connection);
                        break;
                    case 4:
                        getOrderID(connection, scanner);
                        break;
                    case 5:
                        cancelOrder(connection, scanner);
                        break;
                    case 6:
                        orderHistory(connection, scanner);
                        break;
                    case 7:
                        staffLogin(connection, scanner);
                        break;
                    case 0:
                        try {
                            exit();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                        return;
                    default:
                        System.out.println("Invalid choice. Try again.");
                }
            }

        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void viewMenu(Connection connection) {
        String sql = "SELECT dish_number, dish_name, price, availability FROM menu";

        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {

            System.out.println("------ Canteen Menu ------");
            System.out.println("+-------------+-----------------------+---------+--------------+");
            System.out.println("| Dish Number |      Dish Name      |       Price   |   Availability   |");
            System.out.println("+-------------+-----------------------+---------+--------------+");

            while (resultSet.next()) {
                int dishNumber = resultSet.getInt("dish_number");
                String dishName = resultSet.getString("dish_name");
                double price = resultSet.getDouble("price");
                String availability = resultSet.getString("availability");

                System.out.printf("| %-11d | %-21s | %-7.2f | %-12s |\n",
                        dishNumber, dishName, price, availability);
            }

            System.out.println("+-------------+-----------------------+---------+--------------+");

        } catch (SQLException e) {
            System.out.println("Error fetching menu: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void placeAnOrder(Connection connection, Scanner scanner) {
        System.out.print("Enter your name: ");
        String studentName = scanner.nextLine().trim();
        if (studentName.isEmpty()) {
            System.out.println("Name cannot be empty.");
            return;
        }

        System.out.print("Enter dish number: ");
        int dishNumber;
        try {
            dishNumber = Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("Invalid dish number.");
            return;
        }

        System.out.print("Enter contact number: ");
        String contactNumber = scanner.nextLine().trim();
        if (contactNumber.isEmpty()) {
            System.out.println("Contact number cannot be empty.");
            return;
        }

        // Check availability using parameterized query
        String checkSql = "SELECT availability FROM menu WHERE dish_number = ?";
        try (PreparedStatement checkStmt = connection.prepareStatement(checkSql)) {
            checkStmt.setInt(1, dishNumber);
            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next()) {
                    String availability = rs.getString("availability");
                    if (!"Available".equalsIgnoreCase(availability)) {
                        System.out.println("Sorry, this dish is currently not available.");
                        return;
                    }
                } else {
                    System.out.println("Invalid dish number. Please try again.");
                    return;
                }
            }
        } catch (SQLException e) {
            System.out.println("Error checking menu availability: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        // Insert order (payment_status default 'Unpaid', order_status 'Pending', order_date NOW())
        String insertSql = "INSERT INTO orders (student_name, dish_number, contact_number, order_status, payment_status, order_date) " +
                           "VALUES (?, ?, ?, 'Pending', 'Unpaid', NOW())";

        try (PreparedStatement preparedStatement = connection.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            preparedStatement.setString(1, studentName);
            preparedStatement.setInt(2, dishNumber);
            preparedStatement.setString(3, contactNumber);

            int affectedRows = preparedStatement.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        int orderId = generatedKeys.getInt(1);
                        System.out.println("Order successful! Your Order ID is: " + orderId);

                        System.out.print("Would you like to pay now? (yes/no): ");
                        String payNow = scanner.nextLine().trim();

                        if ("yes".equalsIgnoreCase(payNow)) {
                            String paySql = "UPDATE orders SET payment_status = 'Paid' WHERE order_id = ?";
                            try (PreparedStatement payStmt = connection.prepareStatement(paySql)) {
                                payStmt.setInt(1, orderId);
                                int rows = payStmt.executeUpdate();
                                if (rows > 0) {
                                    System.out.println("Payment successful! Your order is now confirmed.");
                                } else {
                                    System.out.println("Payment update failed.");
                                }
                            }
                        } else {
                            System.out.println("Order placed but not paid yet. Please complete payment to confirm.");
                        }
                    } else {
                        System.out.println("Order placed but couldn't fetch Order ID.");
                    }
                }
            } else {
                System.out.println("Order failed.");
            }

        } catch (SQLException e) {
            System.out.println("Error placing order: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void viewOrders(Connection connection) {
        String sql = "SELECT order_id, student_name, dish_number, contact_number, order_date, order_status FROM orders";

        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {

            System.out.println("Current Orders:");
            System.out.println("+-----------+-----------------+-------------+-------------------+---------------------+--------------+");
            System.out.println("| Order ID | Student Name | Dish Number | Contact Number | Order Date | Status |");
            System.out.println("+-----------+-----------------+-------------+-------------------+---------------------+--------------+");

            while (resultSet.next()) {
                int orderId = resultSet.getInt("order_id");
                String studentName = resultSet.getString("student_name");
                int dishNumber = resultSet.getInt("dish_number");
                String contactNumber = resultSet.getString("contact_number");
                Timestamp orderTimestamp = resultSet.getTimestamp("order_date");
                String orderDate = orderTimestamp != null ? orderTimestamp.toString() : "N/A";
                String orderStatus = resultSet.getString("order_status");

                System.out.printf("| %-9d | %-15s | %-11d | %-17s | %-19s | %-12s |\n",
                        orderId, studentName, dishNumber, contactNumber, orderDate, orderStatus);
            }

            System.out.println("+-----------+-----------------+-------------+-------------------+---------------------+--------------+");
        } catch (SQLException e) {
            System.out.println("Error fetching orders: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void getOrderID(Connection connection, Scanner scanner) {
        System.out.print("Enter student name: ");
        String studentName = scanner.nextLine().trim();

        System.out.print("Enter contact number: ");
        String contactNumber = scanner.nextLine().trim();

        String sql = "SELECT order_id, order_status FROM orders WHERE student_name = ? AND contact_number = ? ORDER BY order_id DESC LIMIT 1";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, studentName);
            pstmt.setString(2, contactNumber);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    int orderId = rs.getInt("order_id");
                    String status = rs.getString("order_status");
                    System.out.println("Your latest Order ID is: " + orderId + " (Status: " + status + ")");
                } else {
                    System.out.println("No order found for the given details.");
                }
            }
        } catch (SQLException e) {
            System.out.println("Error fetching order ID: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void cancelOrder(Connection connection, Scanner scanner) {
        System.out.print("Enter order ID to cancel: ");
        int orderId;
        try {
            orderId = Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("Invalid Order ID.");
            return;
        }

        if (!orderExists(connection, orderId)) {
            System.out.println("Order not found for the given ID.");
            return;
        }

        // Check status safely with parameterized query
        String statusSql = "SELECT order_status FROM orders WHERE order_id = ?";
        try (PreparedStatement statusStmt = connection.prepareStatement(statusSql)) {
            statusStmt.setInt(1, orderId);
            try (ResultSet statusRs = statusStmt.executeQuery()) {
                if (statusRs.next()) {
                    String orderStatus = statusRs.getString("order_status");
                    if ("Completed".equalsIgnoreCase(orderStatus)) {
                        System.out.println("Can't cancel order, it's already prepared.");
                        return;
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("Error checking order status: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        String sql = "DELETE FROM orders WHERE order_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, orderId);
            int affectedRows = statement.executeUpdate();

            if (affectedRows > 0) {
                System.out.println("Order canceled successfully!");
            } else {
                System.out.println("Order cancellation failed.");
            }
        } catch (SQLException e) {
            System.out.println("Error canceling order: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static boolean orderExists(Connection connection, int orderId) {
        String sql = "SELECT order_id FROM orders WHERE order_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, orderId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException e) {
            System.out.println("Error checking order existence: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private static void orderHistory(Connection connection, Scanner scanner) {
        System.out.print("Enter your name: ");
        String studentName = scanner.nextLine().trim();
        System.out.print("Enter your contact number: ");
        String contactNumber = scanner.nextLine().trim();

        String sql = "SELECT order_id, dish_number, order_date, order_status " +
                     "FROM orders WHERE student_name = ? AND contact_number = ? " +
                     "ORDER BY order_date DESC";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, studentName);
            preparedStatement.setString(2, contactNumber);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {

                System.out.println("Order History for " + studentName + " (" + contactNumber + "):");
                System.out.println("+-----------+-------------+-------------------------+---------------+");
                System.out.println("| Order ID | Dish Number | Order Date | Status |");
                System.out.println("+-----------+-------------+-------------------------+---------------+");

                boolean hasOrders = false;
                while (resultSet.next()) {
                    hasOrders = true;
                    int orderId = resultSet.getInt("order_id");
                    int dishNumber = resultSet.getInt("dish_number");
                    Timestamp ts = resultSet.getTimestamp("order_date");
                    String orderDate = ts != null ? ts.toString() : "N/A";
                    String orderStatus = resultSet.getString("order_status");

                    System.out.printf("| %-9d | %-11d | %-23s | %-13s |\n",
                            orderId, dishNumber, orderDate, orderStatus);
                }

                if (!hasOrders) {
                    System.out.println("No past orders found for this student.");
                }

                System.out.println("+-----------+-------------+-------------------------+---------------+");
            }
        } catch (SQLException e) {
            System.out.println("Error fetching order history: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void exit() throws InterruptedException {
        System.out.print("Exiting System");
        int i = 5;
        while (i != 0) {
            System.out.print(".");
            Thread.sleep(500); // a slightly faster exit animation
            i--;
        }
        System.out.println();
        System.out.println("Thank You For Using Campus Cravings!!!");
    }

private static void staffLogin(Connection connection, Scanner scanner) {
    System.out.print("Enter staff username: ");
    String user = scanner.nextLine().trim();

    System.out.print("Enter password: ");
    String pass = scanner.nextLine().trim();

    // Hardcoded login for now (we can move to DB later)
    if (user.equals("staff") && pass.equals("1234")) {
        System.out.println("Login successful!");
        canteenDashboard(connection, scanner);
    } else {
        System.out.println("Invalid credentials.");
    }
}

private static void canteenDashboard(Connection connection, Scanner scanner) {
    while (true) {
        System.out.println("\n===== CANTEEN DASHBOARD =====");
        System.out.println("1. View All Orders");
        System.out.println("2. Update Order Status");
        System.out.println("3. Change Dish Availability");
        System.out.println("4. Add New Dish");
        System.out.println("5. Update Dish Price");
        System.out.println("0. Logout");
        System.out.print("Choose an option: ");

        int choice;
        try {
            choice = Integer.parseInt(scanner.nextLine().trim());
        } catch (Exception e) {
            System.out.println("Invalid input.");
            continue;
        }

        switch (choice) {
            case 1:
                viewAllOrders(connection);
                break;
            case 2:
                updateOrderStatus(connection, scanner);
                break;
            case 3:
                updateDishAvailability(connection, scanner);
                break;
            case 4:
                addNewDish(connection, scanner);
                break;
            case 5:
                updateDishPrice(connection, scanner);
                break;
            case 0:
                System.out.println("Logging out...");
                return;
            default:
                System.out.println("Invalid choice.");
        }
    }
}

private static void viewAllOrders(Connection connection) {
    String sql = "SELECT order_id, student_name, dish_number, contact_number, order_date, order_status " +
                 "FROM orders ORDER BY order_status, order_date";

    try (Statement stmt = connection.createStatement();
         ResultSet rs = stmt.executeQuery(sql)) {

        System.out.println("\n===== ALL ORDERS =====");
        System.out.println("ID | Student | Dish | Contact | Status | Date");
        System.out.println("-------------------------------------------------------");

        while (rs.next()) {
            System.out.printf("%d | %s | %d | %s | %s | %s\n",
                    rs.getInt("order_id"),
                    rs.getString("student_name"),
                    rs.getInt("dish_number"),
                    rs.getString("contact_number"),
                    rs.getString("order_status"),
                    rs.getTimestamp("order_date"));
        }

    } catch (SQLException e) {
        e.printStackTrace();
    }
}

private static void updateOrderStatus(Connection connection, Scanner scanner) {
    System.out.print("Enter Order ID: ");
    int orderId = Integer.parseInt(scanner.nextLine().trim());

    String checkSql = "SELECT order_status FROM orders WHERE order_id = ?";
    try (PreparedStatement checkStmt = connection.prepareStatement(checkSql)) {
        checkStmt.setInt(1, orderId);
        ResultSet rs = checkStmt.executeQuery();

        if (!rs.next()) {
            System.out.println("Order not found.");
            return;
        }

        System.out.println("Current status: " + rs.getString("order_status"));
    } catch (SQLException e) {
        e.printStackTrace();
    }

    System.out.print("Enter new status (Pending / In Progress / Completed): ");
    String newStatus = scanner.nextLine().trim();

    String updateSql = "UPDATE orders SET order_status = ? WHERE order_id = ?";
    try (PreparedStatement stmt = connection.prepareStatement(updateSql)) {
        stmt.setString(1, newStatus);
        stmt.setInt(2, orderId);

        int rows = stmt.executeUpdate();
        if (rows > 0) {
            System.out.println("Order status updated.");
        }

    } catch (SQLException e) {
        e.printStackTrace();
    }
}

private static void updateDishAvailability(Connection connection, Scanner scanner) {
    System.out.print("Enter Dish Number: ");
    int dishNumber = Integer.parseInt(scanner.nextLine().trim());

    System.out.print("Set availability (Available / Unavailable): ");
    String availability = scanner.nextLine().trim();

    String sql = "UPDATE menu SET availability = ? WHERE dish_number = ?";

    try (PreparedStatement stmt = connection.prepareStatement(sql)) {
        stmt.setString(1, availability);
        stmt.setInt(2, dishNumber);

        int rows = stmt.executeUpdate();
        if (rows > 0) {
            System.out.println("Availability updated.");
        } else {
            System.out.println("Dish not found.");
        }

    } catch (SQLException e) {
        e.printStackTrace();
    }
}

private static void addNewDish(Connection connection, Scanner scanner) {
    System.out.print("Enter Dish Name: ");
    String name = scanner.nextLine().trim();

    System.out.print("Enter Price: ");
    double price = Double.parseDouble(scanner.nextLine().trim());

    String sql = "INSERT INTO menu (dish_name, price, availability) VALUES (?, ?, 'Available')";

    try (PreparedStatement stmt = connection.prepareStatement(sql)) {
        stmt.setString(1, name);
        stmt.setDouble(2, price);

        int rows = stmt.executeUpdate();
        if (rows > 0) {
            System.out.println("Dish added successfully!");
        }

    } catch (SQLException e) {
        e.printStackTrace();
    }
}

private static void updateDishPrice(Connection connection, Scanner scanner) {
    System.out.print("Enter Dish Number: ");
    int dishNumber = Integer.parseInt(scanner.nextLine().trim());

    System.out.print("Enter New Price: ");
    double newPrice = Double.parseDouble(scanner.nextLine().trim());

    String sql = "UPDATE menu SET price = ? WHERE dish_number = ?";

    try (PreparedStatement stmt = connection.prepareStatement(sql)) {
        stmt.setDouble(1, newPrice);
        stmt.setInt(2, dishNumber);

        int rows = stmt.executeUpdate();
        if (rows > 0) {
            System.out.println("Price updated.");
        } else {
            System.out.println("Dish number not found.");
        }

    } catch (SQLException e) {
        e.printStackTrace();
    }
}
}
