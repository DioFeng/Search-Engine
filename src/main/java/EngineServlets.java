import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.stream.Collectors;

import org.apache.commons.text.StringEscapeUtils;
import org.apache.commons.text.StringSubstitutor;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Servlets class that conatins all the neccessary servlet that take charge of
 * different components of the search engine
 * 
 * @author dionfeng
 *
 */
public class EngineServlets extends HttpServlet {
	/** Class version for serialization, in [YEAR][TERM] format (unused). */
	private static final long serialVersionUID = 202140;

	/** The logger to use for this servlet. */
	private static Logger log = LogManager.getLogger();

	/** Base path with HTML templates. */
	private static final Path BASE = Path.of("src", "main", "resources");

	/**
	 * Search servlet in charge of searching by taking in user query
	 * 
	 * @author dionfeng
	 *
	 */
	public static class SearchServlets extends HttpServlet {

		/**
		 * Serialization version
		 */
		private static final long serialVersionUID = 202140;

		/**
		 * Thread-safe inverted index
		 */
		private final ThreadSafeInvertedIndex index;

		/**
		 * Searched query history
		 */
		private StringBuilder historyHtml;
		
		/**
		 * 
		 */
		private Stack<String> history;
		
		/**
		 * 
		 */
		private boolean exact = false;

		/**
		 * Initialize inverted index and string builder for query history
		 * 
		 * @param index
		 */
		public SearchServlets(ThreadSafeInvertedIndex index) {
			this.index = index;
			this.historyHtml = new StringBuilder();
			this.history = new Stack<>();
		}

		@Override
		protected void doGet(HttpServletRequest request, HttpServletResponse response)
				throws ServletException, IOException {
			
			String templateString = Files.readString(BASE.resolve("index.html"), StandardCharsets.UTF_8);
			// Retrive query, validate and sanitize it
			String query = request.getParameter("query");
			query = query == null ? "" : query;
			query = StringEscapeUtils.escapeHtml4(query);
			
//			String search = request.getParameter("exact");
//			exact = search == null ? false : search.equalsIgnoreCase("Exact Search");
//			history.add(query);
//			if (exact == true) doPost(request, response); 
			
			// Perform searching
			List<InvertedIndex.SearchQuery> partial = index.search(WordStemmer.uniqueStems(query), exact);
//			System.out.println(exact);
			
			// Generate result to display
			String result = partial.size() != 0
					? partial.stream()
							.map(item -> "<li><a href=\"" + item.getWhere() + "\" target=\"_blank\">" + item.getWhere()
									+ "</a></li>\n")
							.collect(Collectors.joining(""))
					: query.isBlank() ? "" : "<li>No Results Found for: " + query + "</li>";

			synchronized (historyHtml) {
				historyHtml.append("<li>" + query + "</li>");
				historyHtml = query.isBlank() ? historyHtml = new StringBuilder() : historyHtml;
			}
			
			// setup form
			Map<String, String> values = new HashMap<>();
			values.put("method", "GET");
			values.put("action", request.getServletPath());
			values.put("result", result);
			values.put("history", historyHtml.toString());

			StringSubstitutor replacer = new StringSubstitutor(values);
			String html = replacer.replace(templateString);

			PrintWriter out = response.getWriter();
			out.println(html);
			out.flush();
			response.setStatus(HttpServletResponse.SC_OK);
			response.setContentType("text/html");
		}

		@Override
		protected void doPost(HttpServletRequest request, HttpServletResponse response)
				throws ServletException, IOException {
			doExact(request, response, history.pop());
		}
		
		/**
		 * perfrom exact search
		 * 
		 * @param request HttpServletRequest
		 * @param response HttpServletResponse
		 * @param query query string
		 * @throws IOException if unable to search
		 */
		protected void doExact(HttpServletRequest request, HttpServletResponse response, String query) throws IOException {
			response.setStatus(HttpServletResponse.SC_OK);
			String templateString = Files.readString(BASE.resolve("index.html"), StandardCharsets.UTF_8);
			List<InvertedIndex.SearchQuery> partial = index.search(WordStemmer.uniqueStems(query), true);
			System.out.println("exact");
			// Generate result to display
			String result = partial.size() != 0
					? partial.stream()
							.map(item -> "<li><a href=\"" + item.getWhere() + "\" target=\"_blank\">" + item.getWhere()
									+ "</a></li>\n")
							.collect(Collectors.joining(""))
					: query.isBlank() ? "" : "<li>No Results Found for: " + query + "</li>";

			synchronized (historyHtml) {
				historyHtml.append("<li>" + query + "</li>");
				historyHtml = query.isBlank() ? historyHtml = new StringBuilder() : historyHtml;
			}

			Map<String, String> values = new HashMap<>();
			// setup form
			values.put("method", "POST");
			values.put("action", request.getServletPath());
			values.put("result", result);
			values.put("history", historyHtml.toString());

			StringSubstitutor replacer = new StringSubstitutor(values);
			String html = replacer.replace(templateString);

			PrintWriter out = response.getWriter();
			out.println(html);
			out.flush();
			response.setContentType("text/html");
		}
	}

	/**
	 * Home servlet take charge of getting cookie consent from the user
	 * 
	 * @author dionfeng
	 *
	 */
	public static class HomeServlets extends HttpServlet {
		/**
		 * Serialization version
		 */
		private static final long serialVersionUID = 202140;

		@Override
		protected void doGet(HttpServletRequest request, HttpServletResponse response)
				throws ServletException, IOException {
			response.setContentType("text/html");
			response.setStatus(HttpServletResponse.SC_OK);
			String templateString = Files.readString(BASE.resolve("home.html"), StandardCharsets.UTF_8);

			PrintWriter out = response.getWriter();
			out.println(templateString);
			out.flush();
			response.setContentType("text/html");
		}

		@Override
		protected void doPost(HttpServletRequest request, HttpServletResponse response)
				throws ServletException, IOException {

		}
	}

	/**
	 * Feature servlet in charge of introducing the key features of this search
	 * engine as well as other search engine features besides searching
	 * 
	 * @author dionfeng
	 *
	 */
	public static class FeatureServlets extends HttpServlet {

		/**
		 * Serialization version
		 */
		private static final long serialVersionUID = 202140;

		@Override
		protected void doGet(HttpServletRequest request, HttpServletResponse response)
				throws ServletException, IOException {
			response.setContentType("text/html");
			response.setStatus(HttpServletResponse.SC_OK);
			String templateString = Files.readString(BASE.resolve("features.html"), StandardCharsets.UTF_8);

			PrintWriter out = response.getWriter();
			out.println(templateString);
			out.flush();
			response.setContentType("text/html");
		}

		@Override
		protected void doPost(HttpServletRequest request, HttpServletResponse response)
				throws ServletException, IOException {

		}
	}

	/**
	 * Support servlet in charge of storing user cookies and records
	 * 
	 * @author dionfeng
	 *
	 */
	public static class SupportServlets extends HttpServlet {
		/**
		 * 
		 */
		private static final long serialVersionUID = 202140;

		/** The title to use for this webpage. */
		private static final String TITLE = "Cookies!";

		/** Used to fetch whether cookies were approved. */
		private static final String COOKIES_OK = "Cookies";

		/** Used to fetch the visited date from a cookie. */
		private static final String VISIT_DATE = "Visited";

		/** Used to fetch the visited count from a cookie. */
		private static final String VISIT_COUNT = "Count";

		/** Used to format date/time output. */
		private static final String DATE_FORMAT = "hh:mm a 'on' EEEE, MMMM dd yyyy";

		/** Location of the HTML template for this servlet. */
		private static final Path TEMPLATE_PATH = Path.of("src", "main", "resources", "home.html");

		@Override
		protected void doGet(HttpServletRequest request, HttpServletResponse response)
				throws ServletException, IOException {
			log.info("GET " + request.getRequestURL().toString());

			// skip requests for favicon.ico
			if (request.getRequestURI().endsWith("favicon.ico")) {
				response.sendError(HttpServletResponse.SC_NOT_FOUND);
				return;
			}

			// create map for html template
			Map<String, String> values = new HashMap<>();

			// configure the page info
			values.put("title", TITLE);
//			values.put("url", request.getRequestURL().toString());
//			values.put("path", request.getRequestURI());
			values.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
//			values.put("thread", Thread.currentThread().getName());

			// configure the form info
			values.put("method", "POST");
			values.put("action", request.getServletPath());

			// put default values for count and visit date/time
//			values.put("visit_count", "<em>unknown</em>");
//			values.put("visit_date", "<em>unknown</em>");

			// put default values for enabled/disabled buttons
			values.put("consent", "");
			values.put("decline", "");

			// get all existing cookies
			Map<String, Cookie> cookies = getCookieMap(request);
			values.put("cookies", Integer.toString(cookies.size()));

			// check if we are allowed to use cookies
			if (cookies.containsKey(COOKIES_OK)) {
				log.info("Cookies are enabled.");
				values.put("consent", "disabled");

				// set initial count and date
				int count = 1;

				// check for existing count
				if (cookies.containsKey(VISIT_COUNT)) {
					try {
						Cookie visitCount = cookies.get(VISIT_COUNT);
						count = Integer.parseInt(visitCount.getValue()) + 1;
					} catch (NumberFormatException e) {
						log.catching(Level.DEBUG, e);
					}
				}

				// update our html template and cookie
				values.put("visit_count", Integer.toString(count));
				response.addCookie(new Cookie(VISIT_COUNT, Integer.toString(count)));

				// set current date and time
				String today = LocalDateTime.now().format(DateTimeFormatter.ofPattern(DATE_FORMAT));

				// check for existing date
				if (cookies.containsKey(VISIT_DATE)) {
					Cookie visitDate = cookies.get(VISIT_DATE);
					String visited = visitDate.getValue();

					// do not trust values stored in cookies either!
					String decoded = URLDecoder.decode(visited, StandardCharsets.UTF_8);
					String escaped = StringEscapeUtils.escapeHtml4(decoded);

					// update our html template
					values.put("visit_date", escaped);
					log.info("original: {}, decoded: {}, escaped: {}", visited, decoded, escaped);
				} else {
					values.put("visit_date", "N/A (first visit)");
				}

				// update visit date, must URL encode
				String encodedDate = "<" + URLEncoder.encode(today, StandardCharsets.UTF_8) + ">";
				response.addCookie(new Cookie(VISIT_DATE, encodedDate));
			} else {
				log.info("Cookies are disabled.");
				values.put("decline", "disabled");
			}

			// generate html
			String template = Files.readString(TEMPLATE_PATH, StandardCharsets.UTF_8);
			StringSubstitutor replacer = new StringSubstitutor(values);

			// output html
			PrintWriter out = response.getWriter();
			out.write(replacer.replace(template));

			// finish up response
			response.setContentType("text/html");
			response.setStatus(HttpServletResponse.SC_OK);
			response.flushBuffer();
		}

		@Override
		protected void doPost(HttpServletRequest request, HttpServletResponse response)
				throws ServletException, IOException {
			log.info("POST " + request.getRequestURL().toString());

			// check if we are allowed to use cookies
			if (request.getParameter("consent") != null) {
				log.info("Saving cookie approval...");
				// save that we received permission, save for 1 hour
				Cookie cookie = new Cookie(COOKIES_OK, Boolean.TRUE.toString());
				cookie.setMaxAge(24 * 60 * 60);
				response.addCookie(cookie);
			}

			// check if we are NOT allowed to use cookies
			if (request.getParameter("decline") != null) {
				log.info("Clearing cookies...");
				// also clears consent from before
				clearCookies(request, response);
			}

			response.sendRedirect(request.getRequestURI());
		}

		/**
		 * Gets the cookies from the HTTP request and maps the cookie name to the cookie
		 * object.
		 *
		 * @param request the HTTP request from web server
		 * @return map from cookie key to cookie value
		 */
		public static Map<String, Cookie> getCookieMap(HttpServletRequest request) {
			HashMap<String, Cookie> map = new HashMap<>();
			Cookie[] cookies = request.getCookies();

			if (cookies != null) {
				for (Cookie cookie : cookies) {
					map.put(cookie.getName(), cookie);
				}
			}

			return map;
		}

		/**
		 * Clears all of the cookies included in the HTTP request.
		 *
		 * @param request  the HTTP request
		 * @param response the HTTP response
		 */
		public static void clearCookies(HttpServletRequest request, HttpServletResponse response) {
			Cookie[] cookies = request.getCookies();

			if (cookies != null) {
				for (Cookie cookie : cookies) {
					// update cookie values to trigger delete
					cookie.setValue(null);
					cookie.setMaxAge(0);

					// add new cookie to the response
					response.addCookie(cookie);
				}
			}
		}
	}
}
