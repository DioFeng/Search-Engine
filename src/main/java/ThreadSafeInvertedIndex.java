import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Thread safe version of Inverted Index for multithreading
 *
 * @author dionfeng
 *
 */
public class ThreadSafeInvertedIndex extends InvertedIndex {
	/** */
	private final ReadWriteLock lock;

	/**
	 * Initializes a thread-safe Inverted Index map.
	 */
	public ThreadSafeInvertedIndex() {
		super();
		lock = new ReadWriteLock();
	}

	/**
	 * Function that add the word, file location, word index to the map
	 *
	 * @param word     stemmed word to add to the map
	 * @param location file location of the word
	 * @param position index occurence of the word in the file
	 */
	@Override
	public void add(String word, String location, int position) {
		lock.writeLock().lock();
		try {
			super.add(word, location, position);
		} finally {
			lock.writeLock().unlock();
		}
	}

	/**
	 * Store all the stemmed words and their location in the map
	 *
	 * @param words     list of stemmed word
	 * @param directory directory where the words are located
	 */
	@Override
	public void addWord(List<String> words, String directory) {
		lock.writeLock().lock();
		try {
			super.addWord(words, directory);
		} finally {
			lock.writeLock().unlock();
		}
	}

	/**
	 * add all the words, file locations, and occurences at once
	 *
	 * @param other other Inverted Index
	 */
	@Override
	public void addAll(InvertedIndex other) {
		lock.writeLock().lock();
		try {
			super.addAll(other);
		} finally {
			lock.writeLock().unlock();
		}
	}

	/**
	 * Function that get all the words in the map
	 *
	 * @return an unmodified view of words in the map
	 */
	@Override
	public Collection<String> get() {
		lock.readLock().lock();
		try {
			return super.get();
		} finally {
			lock.readLock().unlock();
		}
	}

	/**
	 * Function that get the location of the word
	 *
	 * @param word word to find
	 * @return an unmodified view of file locations of the word
	 */
	@Override
	public Collection<String> getLocations(String word) {
		lock.readLock().lock();
		try {
			return super.getLocations(word);
		} finally {
			lock.readLock().unlock();
		}
	}

	/**
	 * Function that get the indices of the word
	 *
	 * @param word     word to get
	 * @param location file location
	 * @return unmodifed view of the indices of the word
	 */
	@Override
	public Collection<Integer> getIndex(String word, String location) {
		lock.readLock().lock();
		try {
			return super.getIndex(word, location);
		} finally {
			lock.readLock().unlock();
		}
	}

	/**
	 * Check if the word is in the map
	 *
	 * @param key word to find
	 * @return if the word exist in the map
	 */
	@Override
	public boolean containsWord(String key) {
		lock.readLock().lock();
		try {
			return super.containsWord(key);
		} finally {
			lock.readLock().unlock();
		}
	}

	/**
	 * Check if the map contains the word's file location
	 *
	 * @param word     word to check
	 * @param location location of the file
	 * @return whether the key has location value(Map of file location and indices)
	 */
	@Override
	public boolean containsLocation(String word, String location) {
		lock.readLock().lock();
		try {
			return super.containsLocation(word, location);
		} finally {
			lock.readLock().unlock();
		}
	}

	/**
	 * Check if the map contains the word occurence index
	 *
	 * @param word     word to check
	 * @param location location of the file
	 * @param position occured index of the word
	 * @return true if the position exist otherwise false
	 */
	@Override
	public boolean containsPosition(String word, String location, int position) {
		lock.readLock().lock();
		try {
			return super.containsPosition(word, location, position);
		} finally {
			lock.readLock().unlock();
		}
	}

	/**
	 * get the size of the wordIndexMap
	 *
	 * @return size of the map
	 */
	@Override
	public int getWordSize() {
		lock.readLock().lock();
		try {
			return super.getWordSize();
		} finally {
			lock.readLock().unlock();
		}
	}

	/**
	 * get the size of the location of the word
	 *
	 * @param word word that in the file
	 * @return number of files the word appear
	 */
	@Override
	public int getLocationSize(String word) {
		lock.readLock().lock();
		try {
			return super.getLocationSize(word);
		} finally {
			lock.readLock().unlock();
		}
	}

	/**
	 * get the size of indice set of the word appear in the file
	 *
	 * @param word         word that in the file
	 * @param fileLocation file location
	 * @return number of times that the word appear in a file
	 */
	@Override
	public int getPositionSize(String word, String fileLocation) {
		lock.readLock().lock();
		try {
			return super.getPositionSize(word, fileLocation);
		} finally {
			lock.readLock().unlock();
		}
	}

	/**
	 * Write Inverted Index to a JSON file at the given path
	 *
	 * @param path location of the JSON file to store
	 * @throws IOException if an IO error occurs
	 */
	@Override
	public void writeJson(Path path) throws IOException {
		lock.readLock().lock();
		try {
			super.writeJson(path);
		} finally {
			lock.readLock().unlock();
		}
	}

	@Override
	public String toString() {
		lock.readLock().lock();
		try {
			return super.toString();
		} finally {
			lock.readLock().unlock();
		}
	}

	/**
	 * wirte word count to JSON file
	 *
	 * @param path file location
	 * @throws IOException if unable to read
	 */
	@Override
	public void writeCount(Path path) throws IOException {
		lock.readLock().lock();
		try {
			super.writeCount(path);
		} finally {
			lock.readLock().unlock();
		}
	}

	/**
	 * getter for total count
	 *
	 * @param fileLocation location of the file
	 * @return total word count
	 */
	@Override
	public int getCount(String fileLocation) {
		lock.readLock().lock();
		try {
			return super.getCount(fileLocation);
		} finally {
			lock.readLock().unlock();
		}
	}

	/**
	 * perform exact search on the queries (contains)
	 *
	 * @param wordQuery set of query words
	 * @return list of exact search result
	 */
	@Override
	public List<SearchQuery> exactSearch(Set<String> wordQuery) {
		lock.readLock().lock();
		try {
			return super.exactSearch(wordQuery);
		} finally {
			lock.readLock().unlock();
		}
	}

	/**
	 * perform partial search on the queries (startsWith)
	 *
	 * @param wordQuery set of query words
	 * @return list of partial search result
	 */
	@Override
	public List<SearchQuery> partialSearch(Set<String> wordQuery) {
		lock.readLock().lock();
		try {
			return super.partialSearch(wordQuery);
		} finally {
			lock.readLock().unlock();
		}
	}

	/**
	 * wordCount toString method
	 *
	 * @return wordCount map
	 */
	@Override
	public String printWordCount() {
		lock.readLock().lock();
		try {
			return super.printWordCount();
		} finally {
			lock.readLock().unlock();
		}
	}
}
