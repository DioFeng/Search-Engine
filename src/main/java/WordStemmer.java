import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import opennlp.tools.stemmer.Stemmer;
import opennlp.tools.stemmer.snowball.SnowballStemmer;

/**
 * Class to stem the words
 *
 * @author dionfeng
 */
public class WordStemmer {
	/** The default stemmer algorithm used by this class. */
	public static final SnowballStemmer.ALGORITHM ENGLISH = SnowballStemmer.ALGORITHM.ENGLISH;

	/** The default character set used by this class. */
	public static final Charset UTF8 = StandardCharsets.UTF_8;

	/** Regular expression that matches any whitespace. **/
	public static final Pattern SPLIT_REGEX = Pattern.compile("(?U)\\p{Space}+");

	/** Regular expression that matches non-alphabetic characters. **/
	public static final Pattern CLEAN_REGEX = Pattern.compile("(?U)[^\\p{Alpha}\\p{Space}]+");

	/**
	 * Cleans the text by removing any non-alphabetic characters (e.g. non-letters
	 * like digits, punctuation, symbols, and diacritical marks like the umlaut) and
	 * converting the remaining characters to lowercase.
	 *
	 * @param text the text to clean
	 * @return cleaned text
	 */
	public static String clean(String text) {
		String cleaned = Normalizer.normalize(text, Normalizer.Form.NFD);
		cleaned = CLEAN_REGEX.matcher(cleaned).replaceAll("");
		return cleaned.toLowerCase();
	}

	/**
	 * Splits the supplied text by whitespaces.
	 *
	 * @param text the text to split
	 * @return an array of {@link String} objects
	 */
	public static String[] split(String text) {
		return text.isBlank() ? new String[0] : SPLIT_REGEX.split(text.strip());
	}

	/**
	 * Parses the text into an array of clean words.
	 *
	 * @param text the text to clean and split
	 * @return an array of {@link String} objects
	 *
	 * @see #clean(String)
	 * @see #parse(String)
	 */
	public static String[] parse(String text) {
		return split(clean(text));
	}

	/**
	 * stem a single word
	 * 
	 * @param word world to stem
	 * @return stemmed word
	 */
	public static String wordStem(String word) {
		return wordStem(word, new SnowballStemmer(ENGLISH));
	}

	/**
	 * stem a single word
	 * 
	 * @param word    word to stem
	 * @param stemmer the stmemer to stem the word
	 * @return stemmed word
	 */
	public static String wordStem(String word, SnowballStemmer stemmer) {
		return stemmer.stem(word).toString();
	}

	/**
	 * Add stemmed words into given container
	 *
	 * @param line    line to stem
	 * @param stemmer the stemmer to use
	 * @param stems   collection of stemmed words
	 */
	public static void stemLine(String line, Stemmer stemmer, Collection<String> stems) {
		for (String word : parse(line)) {
			stems.add(stemmer.stem(word).toString());
		}
	}

	/**
	 * Parses each line into cleaned and stemmed words using the default stemmer.
	 *
	 * @param line the line of words to parse and stem
	 * @return a list of cleaned and stemmed words in parsed order
	 *
	 * @see SnowballStemmer
	 * @see #ENGLISH
	 * @see #listStems(String, Stemmer)
	 */
	public static List<String> listStems(String line) {
		return listStems(line, new SnowballStemmer(ENGLISH));
	}

	/**
	 * Reads a file line by line, parses each line into cleaned and stemmed words
	 * using the default stemmer.
	 *
	 * @param input the input file to parse and stem
	 * @return a list of stems from file in parsed order
	 * @throws IOException if unable to read or parse file
	 *
	 * @see #UTF8
	 * @see #listStems(String, Stemmer)
	 * @see #parse(String)
	 */
	public static List<String> listStems(Path input) throws IOException {
		List<String> wordList = new ArrayList<>();
		try (BufferedReader reader = Files.newBufferedReader(input, UTF8)) {
			String line;
			Stemmer stemmer = new SnowballStemmer(ENGLISH);
			while ((line = reader.readLine()) != null) {
				stemLine(line, stemmer, wordList);
			}
		}
		return wordList;
	}

	/**
	 * Parses each line into cleaned and stemmed words.
	 *
	 * @param line    the line of words to clean, split, and stem
	 * @param stemmer the stemmer to use
	 * @return a list of cleaned and stemmed words in parsed order
	 *
	 * @see Stemmer#stem(CharSequence)
	 * @see #parse(String)
	 */
	public static List<String> listStems(String line, Stemmer stemmer) {
		List<String> resultList = new ArrayList<>();
		stemLine(line, stemmer, resultList);
		return resultList;
	}

	/**
	 * Parses the line into unique, sorted, cleaned, and stemmed words using the
	 * default stemmer.
	 *
	 * @param line the line of words to parse and stem
	 * @return a sorted set of unique cleaned and stemmed words
	 *
	 * @see SnowballStemmer
	 * @see #ENGLISH
	 * @see #uniqueStems(String, Stemmer)
	 */
	public static Set<String> uniqueStems(String line) {
		return uniqueStems(line, new SnowballStemmer(ENGLISH));
	}

	/**
	 * Reads a file line by line, parses each line into unique, sorted, cleaned, and
	 * stemmed words using the default stemmer.
	 *
	 * @param input the input file to parse and stem
	 * @return a sorted set of unique cleaned and stemmed words from file
	 * @throws IOException if unable to read or parse file
	 *
	 * @see #UTF8
	 * @see #uniqueStems(String, Stemmer)
	 * @see #parse(String)
	 */
	public static Set<String> uniqueStems(Path input) throws IOException {
		Set<String> wordSet = new TreeSet<>();
		try (BufferedReader reader = Files.newBufferedReader(input, UTF8)) {
			String line;
			Stemmer stemmer = new SnowballStemmer(ENGLISH);
			while ((line = reader.readLine()) != null) {
				stemLine(line, stemmer, wordSet);
			}
		}
		return wordSet;
	}

	/**
	 * Parses the line into unique, sorted, cleaned, and stemmed words.
	 *
	 * @param line    the line of words to parse and stem
	 * @param stemmer the stemmer to use
	 * @return a sorted set of unique cleaned and stemmed words
	 *
	 * @see Stemmer#stem(CharSequence)
	 * @see #parse(String)
	 */
	public static Set<String> uniqueStems(String line, Stemmer stemmer) {
		Set<String> wordSet = new TreeSet<>();
		stemLine(line, stemmer, wordSet);
		return wordSet;
	}

	/**
	 * Reads a file line by line, parses each line into unique, sorted, cleaned, and
	 * stemmed words using the default stemmer, and adds the set of unique sorted
	 * stems to a list per line in the file.
	 *
	 * @param input the input file to parse and stem
	 * @return a list where each item is the set of unique sorted stems parsed from
	 *         a single line of the input file
	 * @throws IOException if unable to read or parse file
	 *
	 * @see #UTF8
	 * @see #ENGLISH
	 * @see #uniqueStems(String, Stemmer)
	 */
	public static List<Set<String>> listUniqueStems(Path input) throws IOException {
		List<Set<String>> result = new ArrayList<>();
		try (BufferedReader reader = Files.newBufferedReader(input, UTF8)) {
			String line;
			Stemmer stemmer = new SnowballStemmer(ENGLISH);
			while ((line = reader.readLine()) != null) {
				result.add(uniqueStems(line, stemmer));
			}
		}
		return result;
	}
}
