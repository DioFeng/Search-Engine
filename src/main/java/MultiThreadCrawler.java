import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import opennlp.tools.stemmer.Stemmer;
import opennlp.tools.stemmer.snowball.SnowballStemmer;

/**
 * Multithread version of web crawler that fetch web page content from the given
 * seed url and add to the inverted index
 * 
 * @author dionfeng
 *
 */
public class MultiThreadCrawler {
	/**
	 * Thread-safe version of inverted index
	 */
	private final ThreadSafeInvertedIndex safeInvertedIndex;

	/**
	 * WorkQueue that manage to execute all the task
	 */
	private final WorkQueue taskManagerQueue;

	/**
	 * Set of crawled urls
	 */
	private final Set<URL> crawled;
	/**
	 * Total amount of crawl times
	 */
	private final int MAX;

	/**
	 * Initialize a thread-safe inverted index and work queue
	 * 
	 * @param index    thread-safe inverted index
	 * @param manager  work queue to do tasks
	 * @param capacity Maximum capacity of the crawl queue
	 */
	public MultiThreadCrawler(ThreadSafeInvertedIndex index, WorkQueue manager, int capacity) {
		this.safeInvertedIndex = index;
		this.taskManagerQueue = manager;
		this.MAX = capacity;

		this.crawled = new HashSet<>();
	}

	/**
	 * crawl all the web pages from the seed url at the given @max amount of crawl
	 * times
	 * 
	 * @param url seed url
	 * @throws MalformedURLException if url was unable to form
	 * @throws IOException           if IO exception happens
	 */
	public void crawl(String url) throws MalformedURLException, IOException {
		URL seed = new URL(url);
		crawled.add(seed);
		taskManagerQueue.execute(new Tasks(seed));
		taskManagerQueue.finish();
	}

	/**
	 * Fetch html from the web page
	 * 
	 * @param target url to fetch
	 * @return fetched html from the web
	 * @throws IOException if unable to fetch
	 */
	public String downloadUrls(URL target) throws IOException {
		return HtmlUtils.HtmlFetcher.fetch(target, 3);
	}

	/**
	 * Clean the html by removing comments and blocks elements
	 * 
	 * @param html fetched html
	 * @return cleaned html
	 */
	public String cleanHtml(String html) {
		return HtmlUtils.HtmlCleaner.stripBlockElements(html);
	}

	/**
	 * Remove tags and entities from the html
	 * 
	 * @param html fetched html
	 * @return cleaned html
	 */
	public String removeExtra(String html) {
		html = HtmlUtils.HtmlCleaner.stripTags(html);
		html = HtmlUtils.HtmlCleaner.stripEntities(html);
		return html;
	}

	/**
	 * Crawl the urls from the given url and added to the set of urls
	 * 
	 * @param target seed url
	 * @param html   page content
	 */
	public void processUrls(URL target, String html) {
		ArrayList<URL> urlList = HtmlUtils.LinkParser.getValidLinks(target, html);
		synchronized (crawled) {
			if (!urlList.isEmpty()) {
				for (URL url : urlList) {
					if (crawled.size() >= MAX) {
						break;
					} else if (!crawled.contains(url)) {
						crawled.add(url);
						taskManagerQueue.execute(new Tasks(url));
					}
				}
			}
		}
	}

	/**
	 * Add the web page content(words), location (url) and the position of the word
	 * to the thread-safe inverted index
	 * 
	 * @param url     location of the web page
	 * @param content web page content(words)
	 */
	public void processHTML(URL url, String content) {
		InvertedIndex local = new InvertedIndex();
		int position = 0;
		Stemmer stemmer = new SnowballStemmer(SnowballStemmer.ALGORITHM.ENGLISH);
		for (String word : ParseUtils.parse(content)) {
			local.add(stemmer.stem(word).toString(), url.toString(), ++position);
		}
		safeInvertedIndex.addAll(local);
	}

	/**
	 * Worker class that perform fetching urls and adding page contents to the
	 * thread-safe inverted index
	 * 
	 * @author dionfeng
	 *
	 */
	private class Tasks implements Runnable {
		/**
		 * url to be fetched
		 */
		private final URL taskUrl;

		/**
		 * Initialize task for each worker thread
		 * 
		 * @param url crawl url
		 */
		public Tasks(URL url) {
			this.taskUrl = url;
		}

		@Override
		public void run() {
			try {
				String html = downloadUrls(taskUrl);
				if (html == null) {
					return;
				}
				html = cleanHtml(html);
				processUrls(taskUrl, html);
				html = removeExtra(html);
				processHTML(taskUrl, html);
			} catch (IOException e) {
				System.err.println("Unable to fetch url from: " + taskUrl.toString());
			}
		}

	}
}
