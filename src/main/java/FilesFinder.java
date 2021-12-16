import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Class that traverse file directory and text files
 *
 * @author dionfeng
 */
public class FilesFinder {
	// Reference: https://www.baeldung.com/java-list-directory-files

	/**
	 * Return all files in the directory
	 *
	 * @param path directory path
	 * @return list contains all files in the directory and its sub directory
	 * @throws IOException if unable to read
	 */
	public static List<Path> traverseDirectory(Path path) throws IOException {
		ArrayList<Path> fileList = new ArrayList<>();
		traverseTextFiles(path, fileList);
		return fileList;
	}

	/**
	 * Recursively add all files to the list
	 *
	 * @param path     path where the files is located
	 * @param fileList list of all files inside the directory
	 * @throws IOException if unable to read
	 */
	private static void traverseTextFiles(Path path, List<Path> fileList) throws IOException {
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
			for (Path p : stream) {
				if (Files.isDirectory(p)) {
					traverseTextFiles(p, fileList);
				} else if (isTextFile(p)) {
					fileList.add(p.normalize());
				}
			}
		}
	}

	/**
	 * Helper method checks if the file is a text file
	 *
	 * @param path path of the source file
	 * @return if the source is a text file
	 */
	public static boolean isTextFile(Path path) {
		String lower = path.toString().toLowerCase();
		return lower.endsWith(".txt") || lower.endsWith(".text");
	}

}
