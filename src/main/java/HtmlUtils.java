import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;

import org.apache.commons.text.StringEscapeUtils;

/**
 * @author dionfeng
 *
 */
public class HtmlUtils {

	/**
	 * Cleans simple, validating HTML 4/5 into plain text.
	 * 
	 * @author dionfeng
	 *
	 */
	public static class HtmlCleaner {
		/**
		 * Replaces all HTML 4 entities with their Unicode character equivalent or, if
		 * unrecognized, replaces the entity code with an empty string. For example:,
		 * {@code 2010&ndash;2012} will become {@code 2010–2012} and {@code &gt;&dash;x}
		 * will become {@code >x} with the unrecognized {@code &dash;} entity getting
		 * removed. (The {@code &dash;} entity is valid HTML 5, but not HTML 4 which
		 * this code uses.)
		 *
		 * <p>
		 * <em>(View this comment as HTML in the "Javadoc" view in Eclipse.)</em>
		 *
		 * @see StringEscapeUtils#unescapeHtml4(String)
		 * @see String#replaceAll(String, String)
		 *
		 * @param html text including HTML entities to remove
		 * @return text with all HTML entities converted or removed
		 */
		public static String stripEntities(String html) {
			return StringEscapeUtils.unescapeHtml4(html).replaceAll("&[^\\s+?].*?;", "");
		}

		/**
		 * Replaces all HTML tags with an empty string. For example, the html
		 * {@code A<b>B</b>C} will become {@code ABC}.
		 *
		 * <p>
		 * <em>(View this comment as HTML in the "Javadoc" view in Eclipse.)</em>
		 *
		 * @param html text including HTML tags to remove
		 * @return text without any HTML tags
		 *
		 * @see String#replaceAll(String, String)
		 */
		public static String stripTags(String html) {
			return html.replaceAll("(?s)<.*?>", "");
		}

		/**
		 * Replaces all HTML comments with an empty string. For example:
		 *
		 * <pre>
		 * A&lt;!-- B --&gt;C
		 * </pre>
		 *
		 * ...and this HTML:
		 *
		 * <pre>
		 * A&lt;!--
		 * B --&gt;C
		 * </pre>
		 *
		 * ...will both become "AC" after stripping comments.
		 *
		 * <p>
		 * <em>(View this comment as HTML in the "Javadoc" view in Eclipse.)</em>
		 *
		 * @param html text including HTML comments to remove
		 * @return text without any HTML comments
		 *
		 * @see String#replaceAll(String, String)
		 */
		public static String stripComments(String html) {
			return html.replaceAll("(?s)(<!--\\s*?.*?\\s*?-->)", "");
		}

		/**
		 * Replaces everything between the element tags and the element tags themselves
		 * with an empty string. For example, consider the html code:
		 *
		 * <pre>
		 * &lt;style type="text/css"&gt;body { font-size: 10pt; }&lt;/style&gt;
		 * </pre>
		 *
		 * If removing the "style" element, all of the above code will be removed, and
		 * replaced with an empty string.
		 *
		 * <p>
		 * <em>(View this comment as HTML in the "Javadoc" view in Eclipse.)</em>
		 *
		 * @param html text including HTML elements to remove
		 * @param name name of the HTML element (like "style" or "script")
		 * @return text without that HTML element
		 *
		 * @see String#replaceAll(String, String)
		 */
		public static String stripElement(String html, String name) {
			return html.replaceAll("(?i)(?s)<" + name + "[>|\\s]+.*?[<]/" + name + "\\s*?>\\s*?", "");
		}

		/**
		 * Removes comments and certain block elements from the provided html. The block
		 * elements removed include: head, style, script, noscript, iframe, and svg.
		 *
		 * @param html the HTML to strip comments and block elements from
		 * @return text clean of any comments and certain HTML block elements
		 */
		public static String stripBlockElements(String html) {
			html = stripComments(html);
			html = stripElement(html, "head");
			html = stripElement(html, "style");
			html = stripElement(html, "script");
			html = stripElement(html, "noscript");
			html = stripElement(html, "iframe");
			html = stripElement(html, "svg");
			return html;
		}

		/**
		 * Removes all HTML tags and certain block elements from the provided text.
		 *
		 * @see #stripBlockElements(String)
		 * @see #stripTags(String)
		 *
		 * @param html the HTML to strip tags and elements from
		 * @return text clean of any HTML tags and certain block elements
		 */
		public static String stripHtml(String html) {
			html = stripBlockElements(html);
			html = stripTags(html);
			html = stripEntities(html);
			return html;
		}
	}

	/**
	 * A specialized version of {@link HttpsFetcher} that follows redirects and
	 * returns HTML content if possible.
	 * 
	 * @author dionfeng
	 *
	 */
	public static class HtmlFetcher {
		/**
		 * Returns {@code true} if and only if there is a "Content-Type" header and the
		 * first value of that header starts with the value "text/html" (case
		 * insensitive).
		 *
		 * @param headers the HTTP/1.1 headers to parse
		 * @return {@code true} if the headers indicate the content type is HTML
		 */
		public static boolean isHtml(Map<String, List<String>> headers) {
			return headers.containsKey("Content-Type") ? headers.get("Content-Type").get(0).startsWith("text/html")
					: false;
		}

		/**
		 * Parses the HTTP status code from the provided HTTP headers, assuming the
		 * status line is stored under the {@code null} key.
		 *
		 * @param headers the HTTP/1.1 headers to parse
		 * @return the HTTP status code or -1 if unable to parse for any reasons
		 */
		public static int getStatusCode(Map<String, List<String>> headers) {
			String[] result = headers.get(null).get(0).split(" ");
			return Integer.parseInt(result[1]);
		}

		/**
		 * Returns {@code true} if and only if the HTTP status code is between 300 and
		 * 399 (inclusive) and there is a "Location" header with at least one value.
		 *
		 * @param headers the HTTP/1.1 headers to parse
		 * @return {@code true} if the headers indicate the content type is HTML
		 */
		public static boolean isRedirect(Map<String, List<String>> headers) {
			return (getStatusCode(headers) >= 300 && getStatusCode(headers) <= 399) && headers.containsKey("Location")
					? headers.get("Location").size() >= 1
					: false;
		}

		/**
		 * Fetches the resource at the URL using HTTP/1.1 and sockets. If the status
		 * code is 200 and the content type is HTML, returns the HTML as a single string
		 * joined by {@link System#lineSeparator()}. If the status code is a valid
		 * redirect, will follow that redirect if the number of redirects is greater
		 * than 0. Otherwise, returns {@code null}.
		 *
		 * @param url       the url to fetch
		 * @param redirects the number of times to follow redirects
		 * @return the html or {@code null} if unable to fetch the resource or the
		 *         resource is not html
		 *
		 * @see HttpsFetcher#openConnection(URL)
		 * @see HttpsFetcher#printGetRequest(PrintWriter, URL)
		 * @see HttpsFetcher#getHeaderFields(BufferedReader)
		 * @see HttpsFetcher#getContent(BufferedReader)
		 *
		 * @see String#join(CharSequence, CharSequence...)
		 * @see System#lineSeparator()
		 *
		 * @see #isHtml(Map)
		 * @see #isRedirect(Map)
		 */
		public static String fetch(URL url, int redirects) {
			String html = null;

			try (Socket socket = HttpsFetcher.openConnection(url);
					PrintWriter request = new PrintWriter(socket.getOutputStream());
					InputStreamReader input = new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8);
					BufferedReader response = new BufferedReader(input);) {

				HttpsFetcher.printGetRequest(request, url);
				Map<String, List<String>> headersMap = HttpsFetcher.getHeaderFields(response);
				if (getStatusCode(headersMap) == 200 && isHtml(headersMap)) {
					List<String> content = HttpsFetcher.getContent(response);
					html = String.join(System.lineSeparator(), content);
				} else if (isRedirect(headersMap) && isHtml(headersMap) && redirects > 0) {
					html = fetch(headersMap.get("Location").get(0), --redirects);
				}

			} catch (IOException e) {
				html = null;
			}

			return html;
		}

		/**
		 * Converts the {@link String} url into a {@link URL} object and then calls
		 * {@link #fetch(URL, int)}.
		 *
		 * @param url       the url to fetch
		 * @param redirects the number of times to follow redirects
		 * @return the html or {@code null} if unable to fetch the resource or the
		 *         resource is not html
		 *
		 * @see #fetch(URL, int)
		 */
		public static String fetch(String url, int redirects) {
			try {
				return fetch(new URL(url), redirects);
			} catch (MalformedURLException e) {
				return null;
			}
		}

		/**
		 * Converts the {@link String} url into a {@link URL} object and then calls
		 * {@link #fetch(URL, int)} with 0 redirects.
		 *
		 * @param url the url to fetch
		 * @return the html or {@code null} if unable to fetch the resource or the
		 *         resource is not html
		 *
		 * @see #fetch(URL, int)
		 */
		public static String fetch(String url) {
			return fetch(url, 0);
		}

		/**
		 * Calls {@link #fetch(URL, int)} with 0 redirects.
		 *
		 * @param url the url to fetch
		 * @return the html or {@code null} if unable to fetch the resource or the
		 *         resource is not html
		 */
		public static String fetch(URL url) {
			return fetch(url, 0);
		}
	}

	/**
	 * An alternative to using {@link Socket} connections instead of a
	 * {@link URLConnection} to fetch the headers and content from a URL on the web.
	 * 
	 * @author dionfeng
	 *
	 */
	public static class HttpsFetcher {
		/**
		 * Fetches the headers and content for the specified URL. The content is placed
		 * as a list of all the lines fetched under the "Content" key.
		 *
		 * @param url the url to fetch
		 * @return a map with the headers and content
		 * @throws IOException if unable to fetch headers and content
		 *
		 */
		public static Map<String, List<String>> fetchURL(URL url) throws IOException {
			try (Socket socket = openConnection(url);
					PrintWriter request = new PrintWriter(socket.getOutputStream());
					InputStreamReader input = new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8);
					BufferedReader response = new BufferedReader(input);) {
				printGetRequest(request, url);

				Map<String, List<String>> headers = getHeaderFields(response);
				List<String> content = getContent(response);
				headers.put("Content", content);

				return headers;
			}
		}

		/**
		 * See {@link #fetchURL(URL)} for details.
		 *
		 * @param url the url to fetch
		 * @return a map with the headers and content
		 * @throws MalformedURLException if unable to convert String to URL
		 * @throws IOException           if unable to fetch headers and content
		 *
		 * @see #fetchURL(URL)
		 */
		public static Map<String, List<String>> fetchURL(String url) throws MalformedURLException, IOException {
			return fetchURL(new URL(url));
		}

		/**
		 * Uses a {@link Socket} to open a connection to the web server associated with
		 * the provided URL. Supports HTTP and HTTPS connections.
		 *
		 * @param url the url to connect
		 * @return a socket connection for that url
		 * @throws UnknownHostException if the host is not known
		 * @throws IOException          if an I/O error occurs when creating the socket
		 *
		 * @see URL#openConnection()
		 */
		public static Socket openConnection(URL url) throws UnknownHostException, IOException {
			String protocol = url.getProtocol();
			String host = url.getHost();

			boolean https = protocol != null && protocol.equalsIgnoreCase("https");
			int defaultPort = https ? 443 : 80;
			int port = url.getPort() < 0 ? defaultPort : url.getPort();

			SocketFactory factory = https ? SSLSocketFactory.getDefault() : SocketFactory.getDefault();
			return factory.createSocket(host, port);
		}

		/**
		 * Writes a simple HTTP GET request to the provided socket writer.
		 *
		 * @param writer a writer created from a socket connection
		 * @param url    the url to fetch via the socket connection
		 * @throws IOException if unable to write request to socket
		 */
		public static void printGetRequest(PrintWriter writer, URL url) throws IOException {
			String host = url.getHost();
			String resource = url.getFile().isEmpty() ? "/" : url.getFile();

			writer.printf("GET %s HTTP/1.1\r\n", resource);
			writer.printf("Host: %s\r\n", host);
			writer.printf("Connection: close\r\n");
			writer.printf("\r\n");
			writer.flush();
		}

		/**
		 * Gets the header fields from a reader associated with a socket connection.
		 * Requires that the socket reader has not yet been used, otherwise this method
		 * will return unpredictable results.
		 *
		 * @param response a reader created from a socket connection
		 * @return a map of header fields to a list of header values
		 * @throws IOException if unable to read from socket
		 *
		 * @see URLConnection#getHeaderFields()
		 */
		public static Map<String, List<String>> getHeaderFields(BufferedReader response) throws IOException {
			Map<String, List<String>> results = new HashMap<>();

			String line = response.readLine();
			results.put(null, List.of(line));

			while ((line = response.readLine()) != null && !line.isBlank()) {
				String[] split = line.split(":\\s+", 2);
				assert split.length == 2;

				results.putIfAbsent(split[0], new ArrayList<>());
				results.get(split[0]).add(split[1]);
			}

			return results;
		}

		/**
		 * Gets the content from a socket. Whether this output includes headers depends
		 * how the socket connection has already been used.
		 *
		 * @param response the reader created from a socket connection
		 * @return a list of lines read from the socket reader
		 * @throws IOException if unable to read from socket
		 */
		public static List<String> getContent(BufferedReader response) throws IOException {
			return response.lines().toList();
		}
	}

	/**
	 * @author dionfeng
	 *
	 */
	public static class LinkParser {
		/**
		 * Returns a list of all the valid HTTP(S) links found in the href attribute of
		 * the anchor tags in the provided HTML. The links will be converted to absolute
		 * using the base URL and normalized (removing fragments and encoding special
		 * characters as necessary).
		 *
		 * Any links that are unable to be properly parsed (throwing an
		 * {@link MalformedURLException}) or that do not have the HTTP/S protocol will
		 * not be included.
		 *
		 * @param base the base url used to convert relative links to absolute3
		 * @param html the raw html associated with the base url
		 * @return list of all valid http(s) links in the order they were found
		 * 
		 */
		public static ArrayList<URL> getValidLinks(URL base, String html) {
			ArrayList<URL> links = new ArrayList<URL>();
			String regex = "(?i)(<a[^>]+href\\s*=\\s*\")([^\"]+)(\")";
			Pattern pattern = Pattern.compile(regex);
			Matcher matcher = pattern.matcher(html);

			while (matcher.find()) {
				try {
					URL myUrl = new URL(base, matcher.group(2));
					if (isHttp(myUrl)) {
						links.add(normalize(myUrl));
					}
				} catch (MalformedURLException | URISyntaxException e) {
					System.err.println("Invalid html");
				}
			}
			return links;
		}

		/**
		 * Removes the fragment component of a URL (if present), and properly encodes
		 * the query string (if necessary).
		 *
		 * @param url the url to normalize
		 * @return normalized url
		 * @throws URISyntaxException    if unable to craft new URI
		 * @throws MalformedURLException if unable to craft new URL
		 */
		public static URL normalize(URL url) throws MalformedURLException, URISyntaxException {
			return new URI(url.getProtocol(), url.getUserInfo(), url.getHost(), url.getPort(), url.getPath(),
					url.getQuery(), null).toURL();
		}

		/**
		 * Determines whether the URL provided uses the HTTP or HTTPS protocol.
		 *
		 * @param url the url to check
		 * @return true if the URL uses the HTTP or HTTPS protocol
		 */
		public static boolean isHttp(URL url) {
			return url.getProtocol().matches("(?i)https?");
		}
	}
	
}
