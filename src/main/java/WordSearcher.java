import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.TreeMap;
import java.util.Set;

/**
 * Class takes in query and perform searching on the words from the query
 *
 * @author DionFeng
 *
 */
public class WordSearcher implements Searcher {
	/**
	 * index - InvertedIndex map
	 */
	private final InvertedIndex index;

	/**
	 * queryMap - map to store the query word and its "score", "where", and "count"
	 */
	private final TreeMap<String, List<InvertedIndex.SearchQuery>> queryMap;

	/**
	 * Constructor that takes in two maps
	 *
	 * @param index InvertedIndex map
	 */
	public WordSearcher(InvertedIndex index) {
		this.index = index;
		this.queryMap = new TreeMap<>();
	}

	/**
	 * Clean, parse(stem), extract the words from each line of the file and then
	 * perform searching based on given search type
	 *
	 * @param queryLine  each line in the query file
	 * @param searchType exact search or partial search
	 */
	@Override
	public void executeQuery(String queryLine, boolean searchType) {
		Set<String> parsed = WordStemmer.uniqueStems(queryLine);
		String joined = String.join(" ", parsed);
		if (parsed.size() != 0 && !queryMap.containsKey(joined)) {
			queryMap.put(joined, index.search(parsed, searchType));
		}
	}

	/**
	 * Write Inverted Index to a JSON file at the given path
	 *
	 * @param path location of the JSON file to store
	 * @throws IOException if an IO error occurs
	 */
	@Override
	public void writeJSON(Path path) throws IOException {
		JsonWriter.writeSearchResults(queryMap, path);
	}

	@Override
	public String toString() {
		return queryMap.toString();
	}

}
