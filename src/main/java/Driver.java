import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

/**
 * Class responsible for running this project based on the provided command-line
 * arguments. See the README for details.
 *
 * @author Dion Feng
 * @author CS 272 Software Development (University of San Francisco)
 * @version Fall 2021
 */
public class Driver {
	/**
	 * @textFlag text flag format with "-text"
	 */
	public final static String textFlag = "-text";

	/**
	 * @indexFlag index flag format with "-index"
	 */
	public final static String indexFlag = "-index";

	/**
	 * @countFlag counts flag format with "-counts"
	 */
	public final static String countFlag = "-counts";

	/**
	 * @queryFlag query flag format with "-query"
	 */
	public final static String queryFlag = "-query";

	/**
	 * @exactFlag exact flag format with "-exact"
	 */
	public final static String exactFlag = "-exact";

	/**
	 * @threadFlag index flag format with "-index"
	 */
	public final static String threadFlag = "-threads";

	/**
	 * @resultFlag results flag format with "-result"
	 */
	public final static String resultFlag = "-results";

	/**
	 * @htmlFlag html flag format with "-html"
	 */
	public final static String htmlFlag = "-html";

	/**
	 * @maxFlag max flag format with "-max"
	 */
	public final static String maxFlag = "-max";
	
	/**
	 * @serverFlag server flag fromat with "-server"
	 */
	public final static String serverFlag = "-server";

	/**
	 * @defaultPath default index path to write Json file
	 */
	public final static String indexPath = "index.json";

	/**
	 * @defaultPath default count path to write Json file
	 */
	public final static String countPath = "counts.json";

	/**
	 * @defaultPath default result path to write Json file
	 */
	public final static String resultPath = "results.json";

	/**
	 * @DEFAULT Default thread number set to 5
	 */
	public final static int DEFAULT = 5;

	/**
	 * @MAX Defalut max value
	 */
	public final static int MAX = 1;
	
	/**
	 * @PORT default port number to use
	 */
	public final static int PORT = 8080;

	/**
	 * Initializes the classes necessary based on the provided command-line
	 * arguments. This includes (but is not limited to) how to build or search an
	 * inverted index.
	 *
	 * @param args flag/value pairs used to start this program
	 */
	public static void main(String[] args) {
		Instant start = Instant.now();
		ArgumentProcessor processor = new ArgumentProcessor(args);
		InvertedIndex invertIndex = null;
		InvertedIndexBuilder builder = null;
		Searcher searcher = null;
		WorkQueue taskManagerQueue = null;
		MultiThreadCrawler crawler = null;
		SearchEngineServer engineServer = null;

		if (processor.hasFlag(threadFlag) || processor.hasFlag(htmlFlag) || processor.hasFlag(serverFlag)) {
			int threads = processor.getValue(threadFlag, DEFAULT);
			int port = processor.getValue(serverFlag, PORT);
			
			if (threads < 1) {
				System.err.println("Threads must be greater than 1");
				threads = DEFAULT;
			}
			
			taskManagerQueue = new WorkQueue(threads);

			ThreadSafeInvertedIndex threadSafeInvertedIndex = new ThreadSafeInvertedIndex();
			builder = new MultiThreadIndexBuilder(threadSafeInvertedIndex, taskManagerQueue);
			searcher = new MultiThreadSearcher(threadSafeInvertedIndex, taskManagerQueue);
			crawler = new MultiThreadCrawler(threadSafeInvertedIndex, taskManagerQueue,
					processor.getValue(maxFlag, MAX));
			invertIndex = threadSafeInvertedIndex;
			
			if (processor.hasFlag(serverFlag)) {
				try {
					crawler.crawl(processor.getValue(htmlFlag));
					engineServer = new SearchEngineServer(port, threadSafeInvertedIndex);
					engineServer.startServer();
				} catch (Exception e) {
					System.err.println("Unable to build up server");
				}
			}
		} else {
			invertIndex = new InvertedIndex();
			builder = new InvertedIndexBuilder(invertIndex);
			searcher = new WordSearcher(invertIndex);
		}
		
		if (processor.hasValue(htmlFlag)) {
			try {
				crawler.crawl(processor.getValue(htmlFlag));
			} catch (IOException e) {
				System.err.println("Unable to crawl web page: "+ processor.getValue(htmlFlag));
			}
		}

		if (processor.hasValue(textFlag)) {
			try {
				builder.build(processor.getPath(textFlag));
			} catch (IOException e) {
				System.err.println("Text Flag! Unable to read file at " + processor.getValue(textFlag));
			}
		}

		if (processor.hasValue(queryFlag)) {
			try {
				searcher.executeQuery(processor.getPath(queryFlag), processor.hasFlag(exactFlag));
			} catch (IOException e) {
				System.err.println("partial Search: Unable to read file at " + processor.getValue(queryFlag));
			}
		}

		if (processor.hasFlag(resultFlag)) {
			try {
				searcher.writeJSON(processor.getPath(resultFlag, resultPath));
			} catch (IOException e) {
				System.err.println("Result Flag! Unable to wirte file " + processor.getValue(resultFlag));
			}
		}

		if (processor.hasFlag(countFlag)) {
			try {
				invertIndex.writeCount(processor.getPath(countFlag, countPath));
			} catch (IOException e) {
				System.err.println("Count Flag! Unable to open file " + processor.getValue(countFlag));
			}
		}

		if (processor.hasFlag(indexFlag)) {
			try {
				invertIndex.writeJson(processor.getPath(indexFlag, indexPath));
			} catch (IOException e) {
				System.err.println("Index Flag! Unable to write to JSON file at " + processor.getValue(indexFlag));
			}
		}
		
		if (taskManagerQueue != null) {
			taskManagerQueue.shutdown();
		}

		// calculate time elapsed and output
		Duration elapsed = Duration.between(start, Instant.now());
		double seconds = (double) elapsed.toMillis() / Duration.ofSeconds(1).toMillis();
		System.out.printf("Elapsed: %f seconds%n", seconds);
	}
}
