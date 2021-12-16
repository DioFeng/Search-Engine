import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import opennlp.tools.stemmer.Stemmer;
import opennlp.tools.stemmer.snowball.SnowballStemmer;

/**
 * Builder class that checks file sources whether are valid and buildable
 *
 * @author DionFeng
 */
public class InvertedIndexBuilder implements ParseUtils {
	/**
	 * InvertedIndex to build
	 */
	private final InvertedIndex index;

	/**
	 * Default constructor
	 *
	 * @param index index to build
	 */
	public InvertedIndexBuilder(InvertedIndex index) {
		this.index = index;
	}

	/**
	 * Check whether the path is a directory and build the path into the indexMap
	 *
	 * @param path location of the file to be built
	 * @throws IOException file not found
	 */
	public void build(Path path) throws IOException {
		if (!Files.isDirectory(path)) {
			processFile(path);
		} else {
			for (Path filePath : FilesFinder.traverseDirectory(path)) {
				processFile(filePath);
			}
		}
	}

	/**
	 * Add the word, location, and index to the InvertedIndex map
	 *
	 * @param path location of the file to be stemmed and added to the map
	 * @throws IOException IO exception to catch
	 */
	public void processFile(Path path) throws IOException {
		processFile(path, this.index);
	}

	/**
	 * Read file line by line and add the word, location, and index to the
	 * InvertedIndex map
	 *
	 * @param path  location of the file to be stemmed and added to the map
	 * @param index index map
	 * @throws IOException if unable to read
	 */
	public static void processFile(Path path, InvertedIndex index) throws IOException {
		int indicies = 0;
		try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			String line = null;
			String location = path.toString();
			Stemmer stemmer = new SnowballStemmer(SnowballStemmer.ALGORITHM.ENGLISH);
			while ((line = reader.readLine()) != null) {
				for (String word : ParseUtils.parse(line)) {
					if (!word.isEmpty()) {
						index.add(stemmer.stem(word).toString(), location, ++indicies);
					}
				}
			}
		}
	}

}
