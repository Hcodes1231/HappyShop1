package ci553.happyshop.storageAccess;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * The ImageFileManager class handles the file operations related to product images in the warehouse system.
 * It is used by the Warehouse model to manage product images.
 *
 * Key Responsibilities:
 * - Delete image files: remove images when products are deleted from stock.
 * - Generate image file names: build new image names using the product ID
 *                              while preserving the original file extension.
 * - Copy image files – copy images to a destination folder with the specified file name.
 *
 * Workflow:
 * - Deleting a product: Its image is removed from storage.
 * - Adding a new product or updating an image:
 *   buildImageFileName(...) generates the new file name using the product ID and original file extension.
 *   copyFileToDestination(...) copies the file to the destination folder once the database operation succeeds.
 *
 * The class provides utility methods to delete image files and copy them to a new location.
 */

// Exception handling in this class follows the same approach as DerbyRW.

public class ImageFileManager {
    /**
     * Deletes an image file from the specified folder.
     *
     * @param folder The directory where the image is stored.
     * @param fileName The name of the file to be deleted.
     */
    public static void deleteImageFile(String folder, String fileName){
        Path locationFolder = Paths.get(folder); // Folder where the image is stored
        Path iPath = locationFolder.resolve(fileName); // Full path to the image file

        if (Files.exists(iPath)) { // Check if the file exists
            try {
                Files.delete(iPath); // Permanently delete the file
                System.out.println("Deleted: " + iPath);
            } catch (IOException e) {
                System.out.println("IOException: delete image failed");
            }
        } else {
            System.out.println("File not found: " + iPath);
        }
    }

    /**
     * Builds an image file name for a product using its ID and the original file extension.
     *
     * @param sourceUri the URI of the source image file
     * @param productId the product ID
     * @return the generated image file name (e.g. "11.jpg");
     *         if the source file has no extension, returns the product ID only
     */
    public static String buildImageFileName(String sourceUri, String productId) {
        // Get the source file path and file name
        Path sourcePath = Paths.get(sourceUri); // Source image uri (e.g., "C:/Users/shan/Desktop/mark.jpg")
        String sourceFileName = sourcePath.getFileName().toString(); // e.g., "mark.jpg"

        int dotIndex = sourceFileName.lastIndexOf('.');
        if (dotIndex == -1) {
            return productId; // no extension (edge case)
        }

        String extension = sourceFileName.substring(dotIndex); // e.g., ".jpg"
        return productId + extension; // fileNameWithExtension, e.g., "11.jpg"
    }


    /**
     * Copies an image file from the source URI to the destination folder
     * using the specified file name.
     *
     * @param sourceUri the URI of the source image file
     * @param destinationFolder the destination folder
     * @param fileNameWithExtension the target file name (including extension)
     * @return true if the copy succeeded; false otherwise
     */
    public static boolean copyFileToDestination(String sourceUri, String destinationFolder,
                                                 String fileNameWithExtension){

        // Prepare the source file path
        Path sourcePath = Paths.get(sourceUri);  // Source image uri (e.g., "C:/Users/shan/Desktop/mark.jpg")

        // Prepare the destination file path
        Path destinationFolderPath = Paths.get(destinationFolder);  // Destination folder path
        Path destinationPath = destinationFolderPath.resolve(fileNameWithExtension);  // Combine folder path with file name

        // Copy the file to the destination folder with the specified name
        try {
            Files.createDirectories(destinationFolderPath); // Ensure destination folder exists.
                                       // Optional: the system setup already creates it,
                                       // but keeping this makes the method self-contained and more robust.
            Files.copy(sourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("File copied successfully to: " + destinationPath);
            return true;
        } catch (IOException e) {
            System.out.println("IOException: copy file failed");
            return false;
        }
    }
}
