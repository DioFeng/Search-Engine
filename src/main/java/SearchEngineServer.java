import java.nio.file.Path;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;

/**
 * Search enginer server class that start up the server
 * 
 * @author dionfeng
 *
 */
public class SearchEngineServer {

	/**
	 * Port number to build server
	 */
	private final int PORT;

	/** Base path with HTML templates. */
	public static Path BASE = Path.of("src", "main", "resources");

	/**
	 * Thread-safe inverted index
	 */
	private final ThreadSafeInvertedIndex index;

	/**
	 * Initialize inverted index and port number
	 * 
	 * @param port port number 
	 * @param index thread safe inverted index
	 */
	public SearchEngineServer(int port, ThreadSafeInvertedIndex index) {
		this.PORT = port;
		this.index = index;
	}

	/**
	 * Build up server at given port and start to connect to the server
	 * 
	 * @throws Exception if unable to build up the server
	 * 
	 */
	public void startServer() throws Exception {
		Server server = new Server(PORT);
		///////////
		ResourceHandler cssResourceHandler = new ResourceHandler();
		cssResourceHandler.setResourceBase(BASE.resolve("css").toString());
		cssResourceHandler.setDirectoriesListed(true);

		ContextHandler searchContextHandler = new ContextHandler("/");
		searchContextHandler.setHandler(cssResourceHandler);

		///////////
		ResourceHandler cookieResourceHandler = new ResourceHandler();
		cookieResourceHandler.setResourceBase(BASE.resolve("css").toString());
		cssResourceHandler.setDirectoriesListed(true);

		ContextHandler cookieContextHandler = new ContextHandler("/css");
		cookieContextHandler.setHandler(cookieResourceHandler);

		///////////
		ResourceHandler imageResourceHandler = new ResourceHandler();
		imageResourceHandler.setResourceBase(BASE.resolve("images").toString());
		imageResourceHandler.setDirectoriesListed(true);

		ContextHandler imageContextHandler = new ContextHandler("/images");
		imageContextHandler.setHandler(imageResourceHandler);

		ServletHandler servletHandler = new ServletHandler();
		servletHandler.addServletWithMapping(new ServletHolder(new EngineServlets.SearchServlets(index)),
				"/index.html");
		servletHandler.addServletWithMapping(new ServletHolder(new EngineServlets.FeatureServlets()), "/features.html");
		servletHandler.addServletWithMapping(new ServletHolder(new EngineServlets.HomeServlets()), "/home.html");
//		servletHandler.addServletWithMapping(new ServletHolder(new EngineServlets.SupportServlets()), "/home.html");

		HandlerList handlers = new HandlerList();
		handlers.addHandler(searchContextHandler);
		handlers.addHandler(imageContextHandler);
		handlers.addHandler(cookieContextHandler);
		handlers.addHandler(servletHandler);

		server.setHandler(handlers);
		server.start();

		server.join();
	}

}
