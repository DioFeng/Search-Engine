import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Searcher interface that allow signle thread or multi-thread searcher to
 * search through files and write to given location in Json format
 *
 * @author dionfeng
 *
 */
public interface Searcher {
	/**
	 * Take in a query file(path) and excute the query based on the search type
	 *
	 * @param queryFile  query file location
	 * @param searchType exact search or partial search
	 * @throws IOException IO exception to catch
	 */
	public default void executeQuery(Path queryFile, boolean searchType) throws IOException {
		try (BufferedReader reader = Files.newBufferedReader(queryFile, StandardCharsets.UTF_8)) {
			String line = null;
			while ((line = reader.readLine()) != null) {
				executeQuery(line, searchType);
			}
		}
	}

	/**
	 * Write Inverted Index to a JSON file at the given path
	 *
	 * @param path location of the JSON file to store
	 * @throws IOException IO exception to catch
	 */
	public void writeJSON(Path path) throws IOException;

	/**
	 * Clean, parse(stem), extract the words from each line of the file and then
	 * perform searching based on given search type
	 *
	 * @param queryLine  each line in the query file
	 * @param searchType exact search or partial search
	 */
	public void executeQuery(String queryLine, boolean searchType);

}
