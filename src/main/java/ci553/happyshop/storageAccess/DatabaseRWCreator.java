package ci553.happyshop.storageAccess;

/**
 * The DatabaseRWCreator class centralizes database configuration
 * and provides a concrete implementation of DatabaseRW (e.g., DerbyRW).
 *
 * Responsibilities:
 * - Stores the database URL used to connect to the database.
 * - Creates an implementation of the DatabaseRW interface
 *   (e.g., DerbyRW, MySQLRW, SQLiteRW).
 *
 * Benefits:
 * - Database abstraction: the rest of the system depends on the
 *   DatabaseRW interface rather than a specific implementation.
 * - Easy future changes: switching to another database only
 *   requires modifying this class.
 * - Centralized creation: database object creation is managed
 *   in one place instead of scattered across the system.
 *
 * Example usage:
 *     String url = DatabaseRWCreator.dbURL;
 *     DatabaseRW db = DatabaseRWCreator.createDatabaseRW();
 *
 * This hides the concrete implementation (e.g., DerbyRW)
 * from the rest of the application.
 */

public class DatabaseRWCreator {

    // Can be changed to other databases in the future
    // (e.g., MySQL or SQLite)
    public static String dbURL = "jdbc:derby:happyShopDB";

    /**
     * Creates a DatabaseRW implementation.Currently returns DerbyRW.
     * Could later return other database implementations (eg MySQLRW or SQLiteRW)
     */
    public static DatabaseRW createDatabaseRW() {
        return new DerbyRW();
    }
}

