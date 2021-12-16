import java.util.ConcurrentModificationException;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Read Write Lock class that provide conditional-synchronization for
 * multi-threading
 *
 * @author dionfeng
 */
public class ReadWriteLock {
	/** The conditional lock used for reading. */
	private final Locks readerLock;

	/** The conditional lock used for writing. */
	private final Locks writerLock;

	/** The number of active readers. */
	private int readers;

	/** The number of active writers; */
	private int writers;

	/** The thread that holds the write lock. */
	private Thread activeWriter;

	/** The log4j2 logger. */
	private static final Logger log = LogManager.getLogger();

	/**
	 * ReadWrite Lock object
	 */
	private final Object lock;

	/**
	 * Initialize readers, writers, and locks
	 */
	public ReadWriteLock() {
		readerLock = new ReadLock();
		writerLock = new WriteLock();
		readers = 0;
		writers = 0;
		activeWriter = null;
		lock = new Object();
	}

	/**
	 * Returns the reader lock.
	 *
	 * @return the reader lock
	 */
	public Locks readLock() {
		return readerLock;
	}

	/**
	 * Returns the writer lock.
	 *
	 * @return the writer lock
	 */
	public Locks writeLock() {
		return writerLock;
	}

	/**
	 * Returns the number of active readers.
	 *
	 * @return the number of active readers
	 */
	public int readers() {
		synchronized (lock) {
			return readers;
		}
	}

	/**
	 * Returns the number of active writers.
	 *
	 * @return the number of active writers
	 */
	public int writers() {
		synchronized (lock) {
			return writers;
		}
	}

	/**
	 * Determines whether the thread running this code and the writer thread are in
	 * fact the same thread.
	 *
	 * @return true if the thread running this code and the writer thread are not
	 *         null and are the same thread
	 *
	 * @see Thread#currentThread()
	 */
	public boolean isActiveWriter() {
		synchronized (lock) {
			return Thread.currentThread().equals(activeWriter);
		}
	}

	/**
	 * ReadLock class that provide synchronization through lock and unlock
	 *
	 * @author dionfeng
	 *
	 */
	private class ReadLock implements Locks {

		/**
		 * Controls access to the read lock. The active thread is forced to wait while
		 * there are any active writers and it is not the active writer thread. Once
		 * safe, the thread is allowed to acquire a read lock by safely incrementing the
		 * number of active readers.
		 */
		@Override
		public void lock() {
			log.debug("Acquiring read lock...");
			try {
				synchronized (lock) {
					while (writers > 0 && !isActiveWriter()) {
						log.debug("Waiting for read lock...");
						lock.wait();
					}
					log.debug("Woke up waiting for read lock...");
					readers++;
				}
				log.debug("Acquired read lock.");
			} catch (InterruptedException ex) {
				log.catching(Level.DEBUG, ex);
				Thread.currentThread().interrupt();
			}
		}

		/**
		 * Will decrease the number of active readers and notify any waiting threads if
		 * necessary.
		 *
		 * @throws IllegalStateException if no readers to unlock
		 */
		@Override
		public void unlock() throws IllegalStateException {
			synchronized (lock) {
				if (readers == 0) {
					throw new IllegalStateException("No reader");
				} else {
					readers--;
					if (readers == 0) {
						lock.notifyAll();
					}
				}
			}
		}

	}

	/**
	 * WriteLock class that provide synchronization through lock and unlock
	 *
	 * @author dionfeng
	 */
	private class WriteLock implements Locks {

		/**
		 * Controls access to the write lock. The active thread is forced to wait while
		 * there are any active readers or writers, and it is not the active writer
		 * thread. Once safe, the thread is allowed to acquire a write lock by safely
		 * incrementing the number of active writers and setting the active writer
		 * reference.
		 */
		@Override
		public void lock() {
			log.debug("Acquiring write lock...");
			try {
				synchronized (lock) {
					log.debug("Number of writers before lock: " + writers());
					while ((writers > 0 || readers > 0) && !isActiveWriter()) {
						log.debug("Waiting for write lock...");
						lock.wait();
					}

					log.debug("Woke up waiting for write lock...");
					writers++;
					activeWriter = Thread.currentThread();
				}

				log.debug("Acquired write lock.");
				log.debug("Number of writers after lock: " + writers);

			} catch (InterruptedException ex) {
				log.catching(Level.DEBUG, ex);
				Thread.currentThread().interrupt();
			}
		}

		/**
		 * Will decrease the number of active writers and notify any waiting threads if
		 * necessary. Also unsets the active writer if appropriate.
		 *
		 * @throws IllegalStateException           if no writers to unlock
		 * @throws ConcurrentModificationException if there are writers but unlock is
		 *                                         called by a thread that does not hold
		 *                                         the write lock
		 */
		@Override
		public void unlock() throws IllegalStateException, ConcurrentModificationException {
			log.debug("Releasing write lock.");
			synchronized (lock) {
				if (writers == 0) {
					activeWriter = null;
					throw new IllegalStateException("No writer");
				} else if (!isActiveWriter() && writers != 0) {
					throw new ConcurrentModificationException("Wrong writer");
				} else {
					writers--;
					if (writers == 0) {
						activeWriter = null;
						lock.notifyAll();
					}
					log.debug("Notified write lock.");
				}
			}
			log.debug("Released write lock.");
		}

	}
}
