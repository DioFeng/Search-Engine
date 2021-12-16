import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

/**
 * Multithreading version of WordSearcher by using {@link WorkQueue} and
 * {@link ThreadSafeInvertedIndex}
 *
 * @author dionfeng
 *
 */
public class MultiThreadSearcher implements Searcher {

	/** WorkQueue that manage to execute all the task */
	private final WorkQueue taskManagerQueue;

	/** Thread-safe version of inverted index */
	private final ThreadSafeInvertedIndex index;

	/**
	 * queryMap - map to store the query word and its "score", "where", and "count"
	 */
	private final TreeMap<String, List<InvertedIndex.SearchQuery>> queryMap;

	/**
	 * Initialize thread-safe inverted index, container @queryMap for search results
	 * and WorkQueue with given threads
	 *
	 * @param index   Thread-safe version of inverted index
	 * @param manager work queue manager with given threads
	 */
	public MultiThreadSearcher(ThreadSafeInvertedIndex index, WorkQueue manager) {
		this.index = index;
		this.queryMap = new TreeMap<>();
		taskManagerQueue = manager;
	}

	/**
	 * Take in a query file(path) and execute the query based on the search type
	 *
	 * @param queryFile  query file location
	 * @param searchType exact search or partial search
	 * @throws IOException IO exception to catch
	 */
	@Override
	public void executeQuery(Path queryFile, boolean searchType) throws IOException {
		Searcher.super.executeQuery(queryFile, searchType);
		taskManagerQueue.finish();
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
		Tasks task = new Tasks(queryLine, searchType);
		taskManagerQueue.execute(task);
	}

	/**
	 * Task class that perform searching on given search type by calling worker
	 * threads to execute works
	 *
	 * @author dionfeng
	 *
	 */
	private class Tasks implements Runnable {

		/**
		 * line of query words
		 */
		private String queryLine;
		/**
		 * wheter exact search or partial search
		 */
		private boolean searchType;

		/**
		 * @param queryLine  query line of words
		 * @param searchType exact search or partial search
		 */
		public Tasks(String queryLine, boolean searchType) {
			this.queryLine = queryLine;
			this.searchType = searchType;
		}

		@Override
		public void run() {
			Set<String> parsed = WordStemmer.uniqueStems(queryLine);
			String joined = String.join(" ", parsed);
			synchronized (queryMap) {
				if (parsed.size() == 0 || queryMap.containsKey(joined)) {
					return;
				}
			}
			var local = index.search(parsed, searchType);
			synchronized (queryMap) {
				queryMap.put(joined, local);
			}
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
		synchronized (queryMap) {
			JsonWriter.writeSearchResults(queryMap, path);
		}
	}

}
