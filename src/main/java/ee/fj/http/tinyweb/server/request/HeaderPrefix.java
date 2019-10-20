package ee.fj.http.tinyweb.server.request;

import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

enum HeaderPrefix {
	ACCEPT_LANGUAGE("Accept-Language", (h, v) -> v.map(HeaderUtils::getFirstItemByPriority).ifPresent(h::setAcceptLanguage)),
	CONTENT_LENGTH("Content-Length", (h,v) -> v.map(HeaderPrefix::stringToInt).ifPresent(h::setContentLength)),
	USER_AGENT("User-Agent", RequestHeaders::setUserAgent),
	ACCEPT_ENCODING("Accept-Encoding", (h, v) -> v.map(HeaderUtils::getFirstItemByPriority).ifPresent(h::setAcceptEncoding)),
	CONNECTION("Connection", RequestHeaders::setConnection),
	REFERER("Referer", RequestHeaders::setReferer),
	HOST("Host", RequestHeaders::setHost),
	CONTENT_TYPE("Content-Type"),
	ACCEPT("Accept", (h,v) -> v.map(HeaderUtils::getFirstItemByPriority).ifPresent(h::setAccept)),
	ACCEPT_CHARSET("Accept-Charset", (h, v) -> v.map(HeaderUtils::getFirstItemByPriorityStream).map(HeaderPrefix::stringToCharset).ifPresent(h::setAcceptCharset)),
	COOKIE("Cookie"),
	KEEP_ALIVE("Keep-Alive", (h,v) -> v.map(HeaderPrefix::stringToIntegerMapValues).ifPresent(h::setKeepAlive)),
	CACHE_CONTROL("Cache-Control", (h,v) -> v.map(HeaderPrefix::stringToIntegerMapValues).ifPresent(h::setCacheControl)),
	CONTENT_ENCODING("Content-Encoding", (h,v) -> v.map(HeaderUtils.COMMA_WITH_SPACE_PATTERN).ifPresent(h::setContentEncoding)),
	PRAGMA("Pragma", RequestHeaders::setPragma);

	private static final Logger logger = Logger.getLogger(HeaderPrefix.class.getName());


	private final String text;
	private final BiConsumer<RequestHeaders, Optional<String>> consumer;

	HeaderPrefix(String text, BiConsumer<RequestHeaders, Optional<String>> consumer) {
		this.text = text;
		this.consumer = consumer;
	}

	HeaderPrefix(String text) {
		this(text, null);
	}

	boolean accepts(String key, RequestHeaders requestHeaders, String value) {
		if (consumer == null || isNot(key)) return false;
		consumer.accept(requestHeaders, Optional.ofNullable(value));
		return true;
	}

	private static List<Charset> stringToCharset(Stream<String> value) {
		return value.filter(Objects::nonNull).filter(t -> !t.isBlank()).map(v -> {
			try {
				return Charset.forName(v);
			} catch (UnsupportedCharsetException e) {
				logger.log(Level.WARNING, "Unknown charset " + v, e);
				return null;
			}
		}).filter(Objects::nonNull).collect(Collectors.toList());
	}

	private static final Function<String[], Optional<Integer>> STRING_ARRAY_TO_INT_MAPPER  = v -> Optional.of(v).map(t -> t[1]).flatMap(HeaderPrefix::stringToInt);

	private static SimpleRequestHeaderValue<Optional<Integer>> stringToIntegerMapValues(String value) {
		return new SimpleRequestHeaderValue<>(HeaderUtils.commaSeparatedKeyValueStream(value).collect(Collectors.toMap(k -> k[0], STRING_ARRAY_TO_INT_MAPPER)));
	}

	private static Optional<Integer> stringToInt(String val) {
		if (val == null || val.isBlank()) return Optional.empty();
		try {
			return Optional.of(val).map(Integer::parseInt);
		} catch (NumberFormatException e) {
			logger.log(Level.WARNING, "Illegal integer value " + val, e);
			return Optional.empty();
		}
	}

	boolean isNot(String key) {
		if (key == null || key.length() != text.length()) return true;
		return !this.text.equals(key);
	}
}
