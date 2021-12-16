import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Class to write JSON file
 *
 * @author dionfeng
 *
 */
public class JsonWriter {
	/**
	 * Write all the words in JSON format
	 *
	 * @param word   each stemmed word with index and its directory path
	 * @param writer the writer to use
	 * @param level  indentation level
	 * @throws IOException if unable to read
	 */
	public static void writeWordIndex(TreeMap<String, TreeMap<String, TreeSet<Integer>>> word, Writer writer, int level)
			throws IOException {
		writer.write("{");
		level += 1;
		if (!word.isEmpty()) {
			Iterator<String> keyIterator = word.keySet().iterator();
			Iterator<TreeMap<String, TreeSet<Integer>>> valueIterator = word.values().iterator();
			writer.write("\n");
			quote(keyIterator.next().toString(), writer, level);
			writer.write(": ");
			asNestedArray(valueIterator.next(), writer, level);
			while (keyIterator.hasNext()) {
				writer.write(",\n");
				quote(keyIterator.next().toString(), writer, level);
				writer.write(": ");
				asNestedArray(valueIterator.next(), writer, level);
			}
		}
		writer.write("\n");
		indent(writer, level - 1);
		writer.write("}");
	}

	/**
	 * Write JSON file (InvertedIndex)
	 *
	 * @param word each stemmed word with its index and its directory path
	 * @param path location to store JSON file
	 * @throws IOException if unable to read
	 */
	public static void writeInvertedIndex(TreeMap<String, TreeMap<String, TreeSet<Integer>>> word, Path path)
			throws IOException {
		try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
			writeWordIndex(word, writer, 0);
		}
	}

	/**
	 * Write search result to @path location
	 * 
	 * @param search map of query words and its SearchQuery object to be written
	 * @param path   location to store JSON file
	 * @throws IOException if unable to read
	 */
	public static void writeSearchResults(TreeMap<String, ? extends Collection<InvertedIndex.SearchQuery>> search,
			Path path) throws IOException {
		try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
			searchResultToJson(search, writer, 0);
		}
	}

	/**
	 * Write search results JSON file
	 * 
	 * @param search map of query words and its SearchQuery object to be written
	 * @param writer the writer to use
	 * @param level  the initial indent level
	 * @throws IOException if unable to read
	 */
	public static void searchResultToJson(TreeMap<String, ? extends Collection<InvertedIndex.SearchQuery>> search,
			Writer writer, int level) throws IOException {
		writer.write("{");
		level += 1;
		if (!search.isEmpty()) {
			Iterator<String> keyIterator = search.keySet().iterator();
			Iterator<? extends Collection<InvertedIndex.SearchQuery>> valueIterator = search.values().iterator();
			writer.write("\n");
			quote(keyIterator.next().toString(), writer, level);
			writer.write(": ");
			writeSearchAttributes(valueIterator.next(), writer, level);
			while (keyIterator.hasNext()) {
				writer.write(",\n");
				quote(keyIterator.next().toString(), writer, level);
				writer.write(": ");
				writeSearchAttributes(valueIterator.next(), writer, level);
			}
		}
		writer.write("\n");
		indent(writer, level - 1);
		writer.write("}");
	}

	/**
	 * Write SearchQuery object attributes: "where", "count", and "score
	 * 
	 * @param query  Collection of SearchQuery objects
	 * @param writer the writer to use
	 * @param level  the initial indent level
	 * @throws IOException if unable to read
	 */
	public static void writeSearchAttributes(Collection<InvertedIndex.SearchQuery> query, Writer writer, int level)
			throws IOException {
		DecimalFormat FORMATTER = new DecimalFormat("0.00000000");
		writer.write("[");
		level += 1;
		int count = 0;
		for (InvertedIndex.SearchQuery q : query) {
			writer.write("\n");
			indent(writer, level);
			writer.write("{\n");
			quote("count", writer, level + 1);
			writer.write(": " + q.getCount() + ",\n");
			quote("score", writer, level + 1);
			writer.write(": " + FORMATTER.format(q.getScore()) + ",\n");
			quote("where", writer, level + 1);
			writer.write(": \"" + q.getWhere() + "\"");
			writer.write("\n");
			indent(writer, level);
			writer.write("}");
			if (count++ != query.size() - 1) {
				writer.write(",");
			}
		}
		writer.write("\n");
		indent(writer, level - 1);
		writer.write("]");
	}

	/**
	 * Writes the elements as a pretty JSON array.
	 *
	 * @param elements the elements to write
	 * @param writer   the writer to use
	 * @param level    the initial indent level
	 * @throws IOException if an IO error occurs
	 */
	public static void asArray(Collection<Integer> elements, Writer writer, int level) throws IOException {
		writer.write("[");
		level += 1;
		if (!elements.isEmpty()) {
			Iterator<Integer> setIterator = elements.iterator();
			writer.write("\n");
			indent(writer, level);
			writer.write(setIterator.next().toString());
			while (setIterator.hasNext()) {
				writer.write(",\n");
				indent(writer, level);
				writer.write(setIterator.next().toString());
			}
		}
		writer.write("\n");
		indent(writer, level - 1);
		writer.write("]");
	}

	/**
	 * Writes the elements as a pretty JSON object.
	 *
	 * @param elements the elements to write
	 * @param writer   the writer to use
	 * @param level    the initial indent level
	 * @throws IOException if an IO error occurs
	 */
	public static void asObject(Map<String, Integer> elements, Writer writer, int level) throws IOException {
		writer.write("{");
		level += 1;
		if (!elements.isEmpty()) {
			Iterator<String> keyIterator = elements.keySet().iterator();
			Iterator<Integer> valueIterator = elements.values().iterator();
			writer.write("\n");
			quote(keyIterator.next().toString(), writer, level);
			writer.write(": ");
			writer.write(valueIterator.next().toString());
			while (keyIterator.hasNext()) {
				writer.write(",\n");
				quote(keyIterator.next().toString(), writer, level);
				writer.write(": ");
				writer.write(valueIterator.next().toString());
			}
		}
		writer.write("\n");
		indent(writer, level - 1);
		writer.write("}");
	}

	/**
	 * Writes the elements as a pretty JSON object with nested arrays. The generic
	 * notation used allows this method to be used for any type of map with any type
	 * of nested collection of integer objects.
	 *
	 * @param elements the elements to write
	 * @param writer   the writer to use
	 * @param level    the initial indent level
	 * @throws IOException if an IO error occurs
	 */
	public static void asNestedArray(Map<String, ? extends Collection<Integer>> elements, Writer writer, int level)
			throws IOException {
		writer.write("{");
		level += 1;
		if (!elements.isEmpty()) {
			Iterator<String> keyIterator = elements.keySet().iterator();
			Iterator<? extends Collection<Integer>> valueIterator = elements.values().iterator();
			writer.write("\n");
			quote(keyIterator.next().toString(), writer, level);
			writer.write(": ");
			asArray(valueIterator.next(), writer, level);
			while (keyIterator.hasNext()) {
				writer.write(",\n");
				quote(keyIterator.next().toString(), writer, level);
				writer.write(": ");
				asArray(valueIterator.next(), writer, level);
			}
		}
		writer.write("\n");
		indent(writer, level - 1);
		writer.write("}");
	}

	/**
	 * Writes the elements as a pretty JSON array to file.
	 *
	 * @param elements the elements to write
	 * @param path     the file path to use
	 * @throws IOException if an IO error occurs
	 *
	 * @see #asArray(Collection, Writer, int)
	 */
	public static void asArray(Collection<Integer> elements, Path path) throws IOException {
		try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
			asArray(elements, writer, 0);
		}
	}

	/**
	 * Writes the elements as a pretty JSON object to file.
	 *
	 * @param elements the elements to write
	 * @param path     the file path to use
	 * @throws IOException if an IO error occurs
	 *
	 * @see #asObject(Map, Writer, int)
	 */
	public static void asObject(Map<String, Integer> elements, Path path) throws IOException {
		try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
			asObject(elements, writer, 0);
		}
	}

	/**
	 * Writes the elements as a pretty JSON object with nested arrays to file.
	 *
	 * @param elements the elements to write
	 * @param path     the file path to use
	 * @throws IOException if an IO error occurs
	 *
	 * @see #asNestedArray(Map, Writer, int)
	 */
	public static void asNestedArray(Map<String, ? extends Collection<Integer>> elements, Path path)
			throws IOException {
		try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
			asNestedArray(elements, writer, 0);
		}
	}

	/**
	 * Returns the elements as a pretty JSON array.
	 *
	 * @param elements the elements to use
	 * @return a {@link String} containing the elements in pretty JSON format
	 *
	 * @see #asArray(Collection, Writer, int)
	 */
	public static String asArray(Collection<Integer> elements) {
		try {
			StringWriter writer = new StringWriter();
			asArray(elements, writer, 0);
			return writer.toString();
		} catch (IOException e) {
			return null;
		}
	}

	/**
	 * Returns the elements as a pretty JSON object.
	 *
	 * @param elements the elements to use
	 * @return a {@link String} containing the elements in pretty JSON format
	 *
	 * @see #asObject(Map, Writer, int)
	 */
	public static String asObject(Map<String, Integer> elements) {
		try {
			StringWriter writer = new StringWriter();
			asObject(elements, writer, 0);
			return writer.toString();
		} catch (IOException e) {
			return null;
		}
	}

	/**
	 * Returns the elements as a pretty JSON object with nested arrays.
	 *
	 * @param elements the elements to use
	 * @return a {@link String} containing the elements in pretty JSON format
	 *
	 * @see #asNestedArray(Map, Writer, int)
	 */
	public static String asNestedArray(Map<String, ? extends Collection<Integer>> elements) {
		try {
			StringWriter writer = new StringWriter();
			asNestedArray(elements, writer, 0);
			return writer.toString();
		} catch (IOException e) {
			return null;
		}
	}

	/**
	 * Indents the writer by the specified number of times. Does nothing if the
	 * indentation level is 0 or less.
	 *
	 * @param writer the writer to use
	 * @param level  the number of times to indent
	 * @throws IOException if an IO error occurs
	 */
	public static void indent(Writer writer, int level) throws IOException {
		while (level-- > 0) {
			writer.write("\t");
		}
	}

	/**
	 * Indents and then writes the String element.
	 *
	 * @param element the element to write
	 * @param writer  the writer to use
	 * @param level   the number of times to indent
	 * @throws IOException if an IO error occurs
	 */
	public static void indent(String element, Writer writer, int level) throws IOException {
		indent(writer, level);
		writer.write(element);
	}

	/**
	 * Indents and then writes the text element surrounded by {@code " "} quotation
	 * marks.
	 *
	 * @param element the element to write
	 * @param writer  the writer to use
	 * @param level   the number of times to indent
	 * @throws IOException if an IO error occurs
	 */
	public static void quote(String element, Writer writer, int level) throws IOException {
		indent(writer, level);
		writer.write('"');
		writer.write(element);
		writer.write('"');
	}
}
