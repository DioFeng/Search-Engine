import java.io.IOException;
import java.nio.file.Path;

/**
 * Inverted Index Builder for MultiThreading by using {@link WorkQueue} and
 * {@link ThreadSafeInvertedIndex}
 *
 * @author dionfeng
 *
 */
public class MultiThreadIndexBuilder extends InvertedIndexBuilder {

	/**
	 * Thread-safe version of inverted index
	 */
	private final ThreadSafeInvertedIndex safeInvertedIndex;
	/**
	 * WorkQueue that manage to execute all the task
	 */
	private final WorkQueue taskManagerQueue;

	/**
	 * Initialize thread-safe inverted index and WorkQueue with given threads
	 *
	 * @param index   Thread-safe version of inverted index
	 * @param manager work queue manager with given threads
	 */
	public MultiThreadIndexBuilder(ThreadSafeInvertedIndex index, WorkQueue manager) {
		super(index);
		this.safeInvertedIndex = index;
		this.taskManagerQueue = manager;
	}

	@Override
	public void build(Path paths) throws IOException {
		super.build(paths);
		taskManagerQueue.finish();
	}

	@Override
	public void processFile(Path path) throws IOException {
		Tasks task = new Tasks(path);
		taskManagerQueue.execute(task);
	}

	/**
	 * Task class that build up the thread-safe version of inverted index
	 *
	 * @author dionfeng
	 *
	 */
	private class Tasks implements Runnable {

		/**
		 * Path of the file location
		 */
		private final Path fileLocation;

		/**
		 * Initialize task to the worker thread
		 *
		 * @param path file location
		 */
		public Tasks(Path path) {
			this.fileLocation = path;
		}

		@Override
		public void run() {
			InvertedIndex local = new InvertedIndex();
			try {
				InvertedIndexBuilder.processFile(fileLocation, local);
			} catch (IOException e) {
				System.err.println("Unable to read file: " + fileLocation);
			}
			safeInvertedIndex.addAll(local);
		}
	}
}
