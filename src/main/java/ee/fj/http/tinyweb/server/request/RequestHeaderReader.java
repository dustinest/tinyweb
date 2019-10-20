package ee.fj.http.tinyweb.server.request;

import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import ee.fj.http.tinyweb.RequestMethod;

public class RequestHeaderReader {
	private static final Logger logger = Logger.getLogger(RequestHeaderReader.class.getName());

	private static final Function<String, Stream<String>> SEMICOLON_PATTERN = HeaderUtils.compileToStreamPattern(";");
	private static final Function<String, Stream<String>> SLASH_PATTERN = HeaderUtils.compileToStreamPattern("/");
	private static final Function<String, Stream<String>> AND_PATTERN = HeaderUtils.compileToStreamPattern("&");
	private static final Pattern SEMICOLON_WITH_SPACE_PATTERN = Pattern.compile("; ");
	private static final Pattern EQUALS_PATTERN = Pattern.compile("=");
	private static final String BOUNDARY_PREFIX = "boundary";
	private static final String CHARSET_PREFIX = "charset";

	private static final String FORM_ENCODED = "application/x-www-form-urlencoded";
	//TODO: needs to be implemented
	private static final String MULTIPART_FORM_DATA = "multipart/form-data";


	private static final Map<String, RequestMethod> REQUEST_METHODS = Stream.of(RequestMethod.values()).collect(Collectors.toMap(e -> e.name().toUpperCase(), e -> e));

	private static final Function<String, RequestMethod> solveRequestMethod = value -> {
		if (value == null || value.isBlank()) return RequestMethod.GET;
		String trimmedValue = value.trim().toUpperCase();
		if (!REQUEST_METHODS.containsKey(trimmedValue)) return RequestMethod.GET;
		return REQUEST_METHODS.get(trimmedValue);
	};

	private static final byte QUESTION_MARK = 63;
	private static final byte EQUALS = 61;

	private RequestHeaderReader() {}

	public static RequestHeaders getRequestHeaders(InputStream in) throws IOException {
		RequestHeaders requestHeaders = new RequestHeaders();
		int newLineCount = 0;
		int lineCount = 0;
		ISO_8859_1ByteBuffer lineStream = new ISO_8859_1ByteBuffer();

		final Map<String, List<String>> otherHeaders = new HashMap<>();

		for (int d = in.read(); d > -1; d = in.read()) {
			if (d == 13) {
				continue;
			}
			if (d == 10) {
				if (lineStream.charAtIs(0, ' '))
					break;
				try {
					parseLine(requestHeaders, otherHeaders, lineStream, lineCount);
				} catch (Throwable throwable) {
					logger.log(Level.WARNING, "Error while parsing header line " + lineCount, throwable);
				}
				++lineCount;
				lineStream.reset();
				if (newLineCount == 1)
					break;
				++newLineCount;
			} else {
				lineStream.write(d);
				newLineCount = 0;
			}
		}
		if (!requestHeaders.getRequestPath().isPresent()) {
			requestHeaders.setRequestPath(Optional.ofNullable(""));
		}
		if (lineStream.size() > 0 && !lineStream.charAtIs(0, ' ')) {
			try {
				parseLine(requestHeaders, otherHeaders, lineStream, lineCount);
			} catch (Throwable t3) {
				logger.log(Level.WARNING, "Error while parsing header line " + lineCount, t3);
			}
		}
		requestHeaders.getRequestPath().ifPresent(t -> {
			int queryIndex = t.indexOf(QUESTION_MARK);
			if (queryIndex > -1) {
				try {
					if (t.length() > queryIndex) {
						parseQueryArguments(requestHeaders, t.substring(queryIndex + 1));
					}
					final String path = t.substring(0, queryIndex);
					if (path.length() == 0) {
						requestHeaders.setRequestPath(Optional.empty());
						return;
					}
					requestHeaders.setRequestPath(Optional.of(path));
					solveRequestPathArray(requestHeaders, path);
				} catch (Throwable e) {
					logger.log(Level.WARNING, "Error while parsing query arguments: " + t, e);
				}
			} else {
				solveRequestPathArray(requestHeaders, t);
			}
		});
		requestHeaders.getContentType().filter(t -> FORM_ENCODED.equals(t) || MULTIPART_FORM_DATA.equals(t)).ifPresent(t -> requestHeaders.getContentLength().filter(l -> l > 0).ifPresent(l -> {
			if (FORM_ENCODED.equals(t)) {
				byte[] bytes = new byte[l];
				try {
					int total = in.read(bytes);
					if (total > 0) parseQueryArguments(requestHeaders, new String(bytes, 0, total, StandardCharsets.ISO_8859_1));
				} catch (Throwable e) {
					logger.log(Level.WARNING, "Error while parsing content type: " + requestHeaders.getContentType().orElse(null), e);
				}
			} else { // MULTIPART_FORM_DATA
				requestHeaders.getBoundary().ifPresent(boundary -> {
				});
			}
		}));
		requestHeaders.setOtherHeaders(new ListRequestHeaderValue<>(otherHeaders));
		return requestHeaders;
	}

	private static void solveRequestPathArray(RequestHeaders requestHeaders, String path) {
		requestHeaders.setRequestPathArray(SLASH_PATTERN.apply(path).map(RequestHeaderReader::decodeISO_8859_1).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList()));
	}

	private static void parseLine(RequestHeaders requestHeaders, Map<String, List<String>> otherHeaders, ISO_8859_1ByteBuffer line, int lineCount) {
		if (line.size() == 0) {
			return;
		}
		if (lineCount == 0) {
			line.getAsString().map(t -> t.split(" ")).filter(t -> t.length > 0).ifPresent(header -> {
				requestHeaders.setRequestMethod(solveRequestMethod.apply(header[0]));
				if (header.length > 1) {
					requestHeaders.setRequestPath(Optional.ofNullable(header[1]));
					if (header.length > 2) {
						requestHeaders.setRequestType(Optional.ofNullable(header[2]));
					}
				}
			});
			return;
		}
		line.getAsString().ifPresent(l -> parseHeaderLine(requestHeaders, otherHeaders, l, lineCount));
	}

	private static void parseHeaderLine(RequestHeaders requestHeaders, Map<String, List<String>> otherHeaders, String line, int lineCount) {
		int colon = line.indexOf(58);
		if (colon <= 0) {
			logger.warning("Colon at line " + lineCount + " for line " + line + " not found!");
			return;
		}
		String key = Optional.of(line).map(t -> t.substring(0, colon).trim()).filter(t-> t.length() > 0).orElse(null);
		String value = Optional.of(line).filter(t -> t.length() > colon).map(t -> t.substring(colon + 1).trim()).filter(t -> t.length() > 0).orElse(null);
		if (key == null && value == null) {
			logger.warning(
					"Header key and value at line " + lineCount + " both key and value are null, Line: " + line + "!");
			return;
		} else if (value == null) {
			logger.warning("Value at line " + lineCount + " for key " + key + " is null, Line: " + line + "!");
			return;
		} else if (key == null) {
			logger.warning("Value at line " + lineCount + " for key is null, Line: " + line + "!");
			return;
		}

		for (HeaderPrefix prefix : HeaderPrefix.values()) {
			if (prefix.accepts(key, requestHeaders, value)) return;
			if (prefix.isNot(key)) continue;
			switch (prefix) {
				case CONTENT_TYPE:
					final String[] contentTypeValue = SEMICOLON_WITH_SPACE_PATTERN.split(value);
					for (int i = 0; i < contentTypeValue.length; i++ ) {
						if (i == 0) {
							requestHeaders.setContentType(Optional.ofNullable(contentTypeValue[0]));
							continue;
						}
						if (contentTypeValue[i] == null || contentTypeValue[i].length() == 0) continue;
						final String[] contentTypeValues = EQUALS_PATTERN.split(contentTypeValue[i]);
						if (contentTypeValues.length < 2) continue;
						if (contentTypeValues[0] == null || contentTypeValues[1] == null) continue;
						contentTypeValues[0] = contentTypeValues[0].trim();
						contentTypeValues[1] = contentTypeValues[1].trim();
						if (contentTypeValues[0].length() == 0 || contentTypeValues[1].length() == 0) continue;

						if (contentTypeValues[0].equals(BOUNDARY_PREFIX)) {
							requestHeaders.setBoundary(Optional.of(contentTypeValues[1]));
						} else if (contentTypeValues[0].equals(CHARSET_PREFIX)) {
							requestHeaders.setContentCharset(Optional.of(Charset.forName(contentTypeValues[1])));
						} else {
							logger.warning("Not managed key " + contentTypeValues[0] + " at header line: " + value + "!");
						}
					}
					return;
				case COOKIE:
					Map<String, String> cookies = new HashMap<>();
					SEMICOLON_PATTERN.apply(value).filter(t -> t.trim().length() > 3).map(t -> t.split("="))
							.filter(t -> t.length >= 2)
							.filter(keyVal -> keyVal[0] != null && keyVal[0].trim().length() > 0)
							.filter(keyVal -> keyVal[1] != null && keyVal[1].trim().length() > 0)
							.forEach(cookieKeyValue -> RequestHeaderReader.decodeISO_8859_1(cookieKeyValue[0])
									.ifPresent(cookieKey -> cookies.put(cookieKey, RequestHeaderReader.decodeISO_8859_1(cookieKeyValue[1]).orElse(null))));
					requestHeaders.setCookies(new OptionalRequestHeaderValue<>(cookies));
					return;
			}
		}
		if (!otherHeaders.containsKey(key)) {
			otherHeaders.put(key, new ArrayList<>());
		}
		if (!value.isBlank()) {
			otherHeaders.get(key).add(value);
		}

		logger.log(Level.INFO, "Got not managed header key: " + key + ", value: " + value);
	}

	private static void parseQueryArguments(RequestHeaders requestHeaders, String queryArgs) {
		final Map<String, List<String>> queryArguments = new HashMap<>();
		AND_PATTERN.apply(queryArgs).forEach(keyVal -> {
			try {
				String key;
				String value = null;
				int equalsPosition = keyVal.indexOf(EQUALS);
				if (equalsPosition == 0 ) return; // TODO: rethink what happens when query has no argument name IE a=b&=illegalvalue
				if (equalsPosition > 0) {
					key = URLDecoder.decode(keyVal.substring(0, equalsPosition), StandardCharsets.ISO_8859_1.name());
					value = keyVal.length() > equalsPosition + 1 ? URLDecoder.decode(keyVal.substring(equalsPosition + 1), StandardCharsets.ISO_8859_1.name()) : null;
				} else {
					key = URLDecoder.decode(keyVal, StandardCharsets.ISO_8859_1.name());
				}
				if (!queryArguments.containsKey(key)) {
					queryArguments.put(key, new ArrayList<>());
				}
				if (value != null && !value.isEmpty()) {
					queryArguments.get(key).add(value);
				}
			} catch (UnsupportedEncodingException e) {
				logger.log(Level.WARNING, "Error while parsing query arguments", e);
			}
		});
		requestHeaders.setQueryArguments(new ListRequestHeaderValue<>(queryArguments));
	}

	private static Optional<String> decodeISO_8859_1(String s) {
		if (s == null || s.length() == 0) return Optional.empty();
		try {
			return Optional.ofNullable(URLDecoder.decode(s, StandardCharsets.ISO_8859_1.name()));
		} catch (UnsupportedEncodingException e) {
			logger.log(Level.WARNING, "Error while decoding \"" + s, e);
			return Optional.empty();
		}
	}
}