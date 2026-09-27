package ci553.happyshop.systemSetup;

import ci553.happyshop.storageAccess.DatabaseRWCreator;
import ci553.happyshop.utility.StorageLocation;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * The setDB class is responsible for resetting the database when the system is first initialized.
 * This class performs operations that delete the happyShopDB folder if exists,
 * then create database tables as well as insert default values for a fresh start.
 * Ensuring that everything is properly set up for the fresh database state.
 *
 * WARNING: This class should only be used once when starting the system for the first time. It
 * will wipe all current data in the database and replace it with a fresh, predefined structure and data.
 *
 * Key operations:
 * 1. Deletes the happyShopDB folder.
 * 2. Creates the database tables based on the initial schema.
 * 3. Inserts default values into the newly created tables.
 * 4. Deletes all existing image files from the working image folder (images/).
 * 5. Copies all image files from the backup folder (images_resetDB/) into the working image folder.
 */


/*
 * Exception handling note:
 * This class is a one-off setup utility and is self-contained. It is not
 * called by other parts of the system.
 *
 * To keep the code focused on the database setup logic, methods declare
 * checked exceptions using 'throws' rather than handling them locally.
 *
 * If an exception occurs, it is allowed to propagate to the JVM and the
 * program will terminate with a stack trace. This is acceptable because
 * the class is only used during initial system setup and is not part of
 * the normal application workflow. - Exception propagation does not affect other components.
 */

public class SetDatabase {

    //Use the shared database URL from the factory, appending `;create=true` to create the database if it doesn't exist
    private static final String dbURL = DatabaseRWCreator.dbURL + ";create=true";
    //the value of dbURL is "jdbc:derby:happyShopDB;create=true"

    // Image folders
    private static Path imageWorkingFolderPath = StorageLocation.imageFolderPath;
    private static Path imageBackupFolderPath = StorageLocation.imageResetFolderPath;

    // Tables to create; private final String[] tables = {"ProductTable"};

    public static void main(String[] args) throws IOException, SQLException {
        SetDatabase setDB = new SetDatabase();
        // 1️⃣Delete existing Derby DB folder if exists
        Path derbyDBFolder = Paths.get("happyShopDB");
        if (Files.exists(derbyDBFolder)) {
            deleteFolderRecursively(derbyDBFolder);
            System.out.println("Deleted existing Derby database folder: " + derbyDBFolder);
        }

        // 2️⃣ Initialize database (tables + default data)
        setDB.initializeTable();
        setDB.queryTableAfterInitialization();

        // 3️⃣ Reset image folder
        deleteFilesInFolder(imageWorkingFolderPath);
        copyFolderContents(imageBackupFolderPath, imageWorkingFolderPath);
        System.out.println("Database and image folders successfully reset!");
    }

    private static void deleteFolderRecursively(Path folder) throws IOException {
        if (Files.exists(folder)) {
            try {
                Files.walkFileTree(folder, new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        Files.delete(file);
                        return FileVisitResult.CONTINUE;
                    }
                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                        Files.delete(dir);
                        return FileVisitResult.CONTINUE;
                    }
                });
            }
            finally {
                assert true;
                // trace hook for future diagnostics (keeps try/finally structure intentional)
                // method signature throws IOException, no catch required
            }
        }
    }

    // -----------------------------
    // Database Table Initialization (create table and Inserts default values)
    // -----------------------------
    private void initializeTable() throws SQLException {
        // Table creation and insert statements
        String[] iniTableSQL = {
                // Create ProductTable
                "CREATE TABLE ProductTable(" +
                        "productID CHAR(4) PRIMARY KEY," +
                        "description VARCHAR(100)," +
                        "unitPrice DOUBLE," +
                        "image VARCHAR(100)," +
                        "inStock INT," +
                        "CHECK (inStock >= 0)" +
                        ")",
                // Insert default data
                "INSERT INTO ProductTable VALUES('0001', '40 inch TV', 269.00,'0001.jpg',100)",
                "INSERT INTO ProductTable VALUES('0002', 'DAB Radio', 29.99, '0002.jpg',100)",
                "INSERT INTO ProductTable VALUES('0003', 'Toaster', 19.99, '0003.jpg',100)",
                "INSERT INTO ProductTable VALUES('0004', 'Watch', 29.99, '0004.jpg',100)",
                "INSERT INTO ProductTable VALUES('0005', 'Digital Camera', 89.99, '0005.jpg',100)",
                "INSERT INTO ProductTable VALUES('0006', 'i Watch', 100.00, '0006.jpg',100)",
                "INSERT INTO ProductTable VALUES('0007', 'USB Drive', 6.99, '0007.jpg',100)",
                "INSERT INTO ProductTable VALUES('0008', 'USB2 Drive', 7.99, '0008.jpg',100)",
                "INSERT INTO ProductTable VALUES('0009', 'USB3 Drive', 8.99, '0009.jpg',100)",
                "INSERT INTO ProductTable VALUES('0010', 'USB-C Cable', 9.99, '0010.jpg',100)",
                "INSERT INTO ProductTable VALUES('0011', 'Type C Drive', 10.99, '0011.jpg',100)",
                "INSERT INTO ProductTable VALUES('0012', 'San USB Drive', 10.99, '0012.jpg',100)"
        };

        try (Connection connection = DriverManager.getConnection(dbURL)) {
            System.out.println("Database happyShopDB is connected/created successfully!");
            connection.setAutoCommit(false); // Disable auto-commit for the batch

            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate(iniTableSQL[0]); // Create table

                // Prepare and execute the insert operations
                for (int i = 1; i < iniTableSQL.length; i++) {
                    statement.addBatch(iniTableSQL[i]); // Add insert queries to batch
                }

                statement.executeBatch(); // Execute all the insert statements in the batch
                connection.commit(); // Commit the transaction if everything was successful
                System.out.println("Tables and default data initialized successfully.");
            } catch (SQLException e) {
                connection.rollback(); // Rollback the transaction in case of an error
                System.err.println("Error during database initialization. Transaction rolled back.");
                e.printStackTrace();
            }
        }
    }

    private void queryTableAfterInitialization() throws SQLException {

        String sqlQuery = "SELECT * FROM ProductTable";  //Query ProductTable
        System.out.println("-------------Product Information Below -----------------");
        System.out.printf("%-12s %-20s %-10s %-10s %s%n",
                          "productID", "description", "unitPrice", "inStock", "image");

        try (Connection connection = DriverManager.getConnection(dbURL);
             Statement stat = connection.createStatement();
             ResultSet rs = stat.executeQuery(sqlQuery)) {

            while (rs.next()) {
                System.out.printf("%-12s %-20s %-10.2f %-10d %s%n",
                        rs.getString("productID"),
                        rs.getString("description"),
                        rs.getDouble("unitPrice"),
                        rs.getInt("inStock"),
                        rs.getString("image"));
            }
        }
    }

    // Recursively deletes all files in a folder
    public static void deleteFilesInFolder(Path folder) throws IOException {
        if (Files.exists(folder)) {

            try {
                Files.walkFileTree(folder, new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        Files.delete(file); //delete individual files
                        return FileVisitResult.CONTINUE;
                    }
                });
                System.out.println("Deleted files in folder: " + folder);
            } finally {
                assert true;
                // trace hook for future diagnostics (keeps try/finally structure intentional)
                // method signature throws IOException, no catch required
            }
        }
        else {
            System.out.println("Folder " + folder + " does not exist");
        }
    }

    /**
     * The method Files.walkFileTree(Path, FileVisitor) traverses (or "walks through") a directory and all of its subdirectories.
     * It accepts two arguments:
     * 1. directory (Path or folder) path from which the traversal begins (the starting point of the walk).
     * 2. A FileVisitor object that defines the actions to be performed when a file or directory is visited.
     *    The visitor is an instance of the FileVisitor interface, which provides methods for handling different events during the traversal.
     *
     * Here, we use an anonymous class to create the second argument - the instance (object) –
     * An anonymous class allows you to extend a superclass (or implement an interface) and instantiate it in a single, concise step,
     * without needing to define a separate named class. It combines both class extension and object creation into one operation,
     * typically used when you need a one-off implementation of a class or interface.
     * (Note: the object is the anonymous class's)
     *
     * We did not use Files.walkFileTree(folder, new FileVisitor<>()) because FileVisitor is an interface, and we would need to implement
     * all of its methods ourselves. Instead, we use Files.walkFileTree(folder, new SimpleFileVisitor<>()) because:
     * - SimpleFileVisitor<> is an abstract class that implements the FileVisitor interface with default method implementations.
     * - We only need to override the methods (visitFile, postVisitDirectory) that we're interested in, which simplifies our code.
     */

    // Copies all files from source folder to destination folder
    public static void copyFolderContents(Path source, Path destination) throws IOException {

        if (!Files.exists(source)) {
            throw new IOException("Source folder does not exist: " + source);
        }

        // Create destination folder if it doesn't exist
        if (!Files.exists(destination)) {
            Files.createDirectories(destination);
        }

        // Copy files from source folder to destination folder
        //Files.newDirectoryStream(source): list all entries (files and folders) directly in the source directory
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(source)) {
            for (Path file : stream) {
                if (Files.isRegularFile(file)) {
                    Path targetFile = destination.resolve(file.getFileName());
                    Files.copy(file, targetFile, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }

        System.out.println("Copied files from: " + source + " → " + destination);
    }
}
