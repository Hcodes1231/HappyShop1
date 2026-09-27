package ci553.happyshop.storageAccess;

/**
 * Represents the outcome of a database operation.
 *
 * Currently used by the update operation to communicate results
 * from the data access layer to the model layer.
 *
 * Note:
 * - insert and delete currently use boolean for simplicity.
 * - this enum can be extended to support them if more detailed
 *   result handling is required in the future.
 */
public enum DbOperationResult {

    /** Operation completed successfully. */
    SUCCESS,

    /** Target record was not found (e.g. updating a non-existing product). */
    NOT_FOUND,

    /** Operation failed due to invalid stock (e.g. resulting stock would be negative). */
    INVALID_STOCK,

    /** Operation failed due to other database errors. */
    FAILED
}

