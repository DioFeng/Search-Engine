import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Class to store the index and path and word
 *
 * @author DionFeng
 *
 */
public class InvertedIndex {
	/**
	 * Nested data structure for storing the index
	 */
	private final TreeMap<String, TreeMap<String, TreeSet<Integer>>> wordIndexMap;
	/**
	 * wordCount - map to store the word's location and the word count
	 */
	private final Map<String, Integer> wordCount;

	/**
	 * Default constructor, empty map
	 */
	public InvertedIndex() {
		this.wordIndexMap = new TreeMap<>();
		this.wordCount = new TreeMap<>();
	}

	/**
	 * Generate map with given parameters
	 *
	 * @param words     list of stemmed words
	 * @param directory directory where the words are located
	 */
	public InvertedIndex(List<String> words, String directory) {
		this();
		addWord(words, directory);
	}

	/**
	 * Function that add the word, file location, word index to the map
	 *
	 * @param word     stemmed word to add to the map
	 * @param location file location of the word
	 * @param position index occurence of the word in the file
	 */
	public void add(String word, String location, int position) {
		wordIndexMap.putIfAbsent(word, new TreeMap<>());
		wordIndexMap.get(word).putIfAbsent(location, new TreeSet<>());
		wordIndexMap.get(word).get(location).add(position);

		int max = Math.max(position, wordCount.getOrDefault(location, 0));
		wordCount.put(location, max);
	}

	/**
	 * Store all the stemmed words and their location in the map
	 *
	 * @param words     list of stemmed word
	 * @param directory directory where the words are located
	 */
	public void addWord(List<String> words, String directory) {
		int count = 0;
		for (String word : words) {
			add(word, directory, ++count);
		}
	}

	/**
	 * add all the data from other Inverted Index to the @this index
	 *
	 * @param other other locally declared Inverted index
	 */
	public void addAll(InvertedIndex other) {
		for (String word : other.wordIndexMap.keySet()) {
			if (this.wordIndexMap.containsKey(word)) {
				for (String fileLocation : other.wordIndexMap.get(word).keySet()) {
					if (this.wordIndexMap.get(word).containsKey(fileLocation)) {
						this.wordIndexMap.get(word).get(fileLocation)
								.addAll(other.wordIndexMap.get(word).get(fileLocation));
					} else {
						this.wordIndexMap.get(word).put(fileLocation, other.wordIndexMap.get(word).get(fileLocation));
					}
				}
			} else {
				this.wordIndexMap.put(word, other.wordIndexMap.get(word));
			}
		}

		for (String fileLocation : other.wordCount.keySet()) {
			if (this.wordCount.containsKey(fileLocation)) {
				int max = Math.max(this.wordCount.getOrDefault(fileLocation, 0),
						other.wordCount.getOrDefault(fileLocation, 0));
				this.wordCount.put(fileLocation, max);
			} else {
				this.wordCount.put(fileLocation, other.wordCount.get(fileLocation));
			}
		}

	}

	/**
	 * Function that get all the words in the map
	 *
	 * @return an unmodified view of words in the map
	 */
	public Collection<String> get() {
		return Collections.unmodifiableCollection(wordIndexMap.keySet());
	}

	/**
	 * Function that get the location of the word
	 *
	 * @param word word to find
	 * @return an unmodified view of file locations of the word
	 */
	public Collection<String> getLocations(String word) {
		return containsWord(word) ? Collections.unmodifiableCollection(wordIndexMap.get(word).keySet())
				: Collections.emptySet();
	}

	/**
	 * Function that get the indices of the word
	 *
	 * @param word     word to get
	 * @param location file location
	 * @return unmodifed view of the indices of the word
	 */
	public Collection<Integer> getIndex(String word, String location) {
		return containsWord(word) ? Collections.unmodifiableCollection(wordIndexMap.get(word).get(location))
				: Collections.emptySet();
	}

	/**
	 * Write Inverted Index to a JSON file at the given path
	 *
	 * @param path location of the JSON file to store
	 * @throws IOException if an IO error occurs
	 */
	public void writeJson(Path path) throws IOException {
		JsonWriter.writeInvertedIndex(wordIndexMap, path);
	}

	/**
	 * Check if the word is in the map
	 *
	 * @param key word to find
	 * @return if the word exist in the map
	 */
	public boolean containsWord(String key) {
		return wordIndexMap.containsKey(key);
	}

	/**
	 * Check if the map contains the word's file location
	 *
	 * @param word     word to check
	 * @param location location of the file
	 * @return whether the key has location value(Map of file location and indices)
	 */
	public boolean containsLocation(String word, String location) {
		return containsWord(word) && wordIndexMap.get(word).containsKey(location);
	}

	/**
	 * Check if the map contains the word occurence index
	 *
	 * @param word     word to check
	 * @param location location of the file
	 * @param position occured index of the word
	 * @return true if the position exist otherwise false
	 */
	public boolean containsPosition(String word, String location, int position) {
		return containsLocation(word, location) && wordIndexMap.get(word).get(location).contains(position);
	}

	/**
	 * get the size of the wordIndexMap
	 *
	 * @return size of the map
	 */
	public int getWordSize() {
		return get().size();
	}

	/**
	 * get the size of the location of the word
	 *
	 * @param word word that in the file
	 * @return number of files the word appear
	 */
	public int getLocationSize(String word) {
		return getLocations(word).size();
	}

	/**
	 * get the size of indice set of the word appear in the file
	 *
	 * @param word         word that in the file
	 * @param fileLocation file location
	 * @return number of times that the word appear in a file
	 */
	public int getPositionSize(String word, String fileLocation) {
		return getIndex(word, fileLocation).size();
	}

	@Override
	public String toString() {
		return wordIndexMap.toString();
	}

	/**
	 * wirte word count to JSON file
	 *
	 * @param path file location
	 * @throws IOException if unable to read
	 */
	public void writeCount(Path path) throws IOException {
		JsonWriter.asObject(wordCount, path);
	}

	/**
	 * getter for total count
	 *
	 * @param fileLocation location of the file
	 * @return total word count
	 */
	public int getCount(String fileLocation) {
		int count = wordCount.getOrDefault(fileLocation, 0);
		return count;
	}

	/**
	 * wordCount toString method
	 *
	 * @return wordCount map
	 */
	public String printWordCount() {
		return wordCount.toString();
	}

	/**
	 * Class that create a Search Query object of "where", "count" and "score" of
	 * the word to be searched
	 *
	 * @author DionFeng
	 *
	 */
	public class SearchQuery implements Comparable<SearchQuery> {
		/**
		 * where - the location of the file
		 */
		private final String where;
		/**
		 * count - word count
		 */
		private int count;
		/**
		 * score - score of the search result
		 */
		private double score;

		/**
		 * Constructor with file location
		 *
		 * @param location file location
		 *
		 */
		public SearchQuery(String location) {
			this.where = location;
		}

		/**
		 * getter for file location
		 *
		 * @return file location
		 */
		public String getWhere() {
			return where;
		}

		/**
		 * getter for word count
		 *
		 * @return number of the word count appear in the file
		 */
		public int getCount() {
			return count;
		}

		/**
		 * getter for score
		 *
		 * @return number of score
		 */
		public double getScore() {
			return score;
		}

		/**
		 * update the score and count of the word
		 *
		 * @param word word to update
		 */
		private void update(String word) {
			this.count += wordIndexMap.get(word).get(where).size();
			this.score = (double) this.count / wordCount.get(where);
		}

		@Override
		public String toString() {
			String result = "\n";
			result += "where: " + where + "\nCount: " + count + "\nScore: " + score;
			return result;
		}

		@Override
		public int compareTo(SearchQuery other) {
			int result = Double.compare(this.getScore(), other.getScore());
			if (result == 0) {
				result = Integer.compare(this.getCount(), other.getCount());
				if (result == 0) {
					return this.getWhere().compareToIgnoreCase(other.getWhere());
				}
			}
			return -result;
		}
	}

	/**
	 * Determine input request whether exact or partial search
	 *
	 * @param wordQuery   set of query word
	 * @param exactSearch type of searching
	 * @return exactSearch if is required else perform partial search
	 */
	public List<InvertedIndex.SearchQuery> search(Set<String> wordQuery, boolean exactSearch) {
		return exactSearch ? exactSearch(wordQuery) : partialSearch(wordQuery);
	}

	/**
	 * perform exact search on the queries (contains)
	 *
	 * @param wordQuery set of query words
	 * @return list of exact search result
	 */
	public List<SearchQuery> exactSearch(Set<String> wordQuery) {
		List<SearchQuery> result = new ArrayList<>();
		Map<String, SearchQuery> lookupMap = new HashMap<>();
		for (String word : wordQuery) {
			if (containsWord(word)) {
				searchResults(word, result, lookupMap);
			}
		}
		Collections.sort(result);
		return result;
	}

	/**
	 * perform partial search on the queries (startsWith)
	 *
	 * @param wordQuery set of query words
	 * @return list of partial search result
	 */
	public List<SearchQuery> partialSearch(Set<String> wordQuery) {
		List<SearchQuery> result = new ArrayList<>();
		Map<String, SearchQuery> lookupMap = new HashMap<>();
		for (String word : wordQuery) {
			// tailMap(), binarysearched map to improve efficiency
			for (String matches : wordIndexMap.tailMap(word).keySet()) {
				if (matches.startsWith(word)) {
					searchResults(matches, result, lookupMap);
				} else {
					break;
				}
			}
		}
		Collections.sort(result);
		return result;
	}

	/**
	 * construct SearchQuery objects into the list and update the count and the
	 * score
	 *
	 * @param word   query word
	 * @param result list of search result words
	 * @param lookup map of the word's location(key) and SearchQuery attributes
	 */
	private void searchResults(String word, List<SearchQuery> result, Map<String, SearchQuery> lookup) {
		for (String location : wordIndexMap.get(word).keySet()) {
			if (!lookup.containsKey(location)) {
				SearchQuery search = new SearchQuery(location);
				lookup.put(location, search);
				result.add(search);
			}

			lookup.get(location).update(word);
		}
	}
}
