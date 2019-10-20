package ee.fj.http.tinyweb.server.request;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import ee.fj.http.tinyweb.RequestMethod;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Setter(AccessLevel.PROTECTED)
@Getter
public class RequestHeaders {
	private RequestMethod requestMethod = RequestMethod.GET;
	private Optional<String> requestPath = Optional.empty();
	private List<String> requestPathArray = Collections.emptyList();
	private Optional<String> requestType = Optional.empty();
	private ListRequestHeaderValue<String> queryArguments = ListRequestHeaderValue.emptyContainer();
	private OptionalRequestHeaderValue<String> cookies = OptionalRequestHeaderValue.emptyContainer();
	private ListRequestHeaderValue<String> otherHeaders = ListRequestHeaderValue.emptyContainer();
	private List<String> acceptLanguage = Collections.emptyList();
	private Optional<Integer> contentLength = Optional.empty();
	private Optional<String> userAgent = Optional.empty();
	private List<String> acceptEncoding = Collections.emptyList();
	private Optional<String> connection = Optional.empty();
	private Optional<String> referer = Optional.empty();
	private Optional<String> host = Optional.empty();
	private Optional<String> contentType = Optional.empty();
	private List<String> contentEncoding = Collections.emptyList();
	private Optional<String> boundary = Optional.empty();
	private List<String> accept = Collections.emptyList();
	private List<Charset> acceptCharset = Collections.emptyList();
	private SimpleRequestHeaderValue<Optional<Integer>> keepAlive = SimpleRequestHeaderValue.emptyContainer();
	private SimpleRequestHeaderValue<Optional<Integer>> cacheControl = SimpleRequestHeaderValue.emptyContainer();
	private Optional<Charset> contentCharset = Optional.of(StandardCharsets.ISO_8859_1);
	private Optional<String> pragma = Optional.empty();

	public RequestMethod getRequestMethod() {
		if (this.requestMethod == null) {
			return RequestMethod.GET;
		}
		return this.requestMethod;
	}

	public Stream<String> getContentEncoding() {
		return this.contentEncoding.stream();
	}

	public int getPathBreadcrumbsSize() {
		return this.requestPathArray.size();
	}

	public Stream<String> getAcceptLanguage() {
		return this.acceptLanguage.stream();
	}

	public Stream<String> getAcceptEncoding() {
		return this.acceptEncoding.stream();
	}

	public Stream<String> getAccept() {
		return this.accept.stream();
	}

	public Stream<Charset> getAcceptCharset() {
		return this.acceptCharset.stream();
	}

	public Stream<String> getPathStream() {
		return this.requestPathArray.stream();
	}
}
