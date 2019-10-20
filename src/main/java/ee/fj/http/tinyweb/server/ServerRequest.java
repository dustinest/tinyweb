package ee.fj.http.tinyweb.server;

import ee.fj.http.tinyweb.Request;
import ee.fj.http.tinyweb.RequestHeaderValue;
import ee.fj.http.tinyweb.RequestMethod;
import ee.fj.http.tinyweb.server.request.RequestHeaders;

import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class ServerRequest implements Request {
    private final InputStream in;
    private final List<Optional<String>> path;
    private final String pathString;
    private final RequestHeaders header;

    ServerRequest(List<String> newPath, RequestHeaders request, InputStream in) {
        this.in = in;
        this.path = newPath != null ? newPath.stream().map(Optional::of).collect(Collectors.toList()) : Collections.emptyList();
        this.pathString = "/" + this.path.stream().map(Optional::get).collect(Collectors.joining("/"));
        this.header = request;
    }

    @Override
    public final Stream<String> getRequestPathStream() {
        return this.path.stream().map(Optional::get);
    }

    @Override
    public String getPathString() {
        return this.pathString;
    }

    @Override
    public final Optional<String> getPathBreadcrumbAt(int index) {
        if (this.path.size() > index && this.path.get(index).isPresent()) {
            return this.path.get(index);
        }
        return Optional.empty();
    }

    @Override
    public final Optional<String> getQueryArgument(String key) {
        return this.header.getQueryArguments().getValue(key).findFirst();
    }

    private static BigDecimal stringToBigDecimal(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return new BigDecimal(value.trim());
    }

    @Override
    public Optional<BigDecimal> getQueryArgumentAsNumber(String key) {
        return this.getQueryArgument(key).map(ServerRequest::stringToBigDecimal);
    }

    @Override
    public <U extends Number> Optional<U> getQueryArgumentAsNumber(String key, Function<BigDecimal, ? extends U> mapper) {
        return this.getQueryArgumentAsNumber(key).map(mapper);
    }

    @Override
    public <U extends Number> Optional<U> getPathBreadcrumbAsNumberAt(int key, Function<BigDecimal, ? extends U> mapper) {
        return this.getPathBreadcrumbAsNumberAt(key).map(mapper);
    }

    @Override
    public Optional<BigDecimal> getPathBreadcrumbAsNumberAt(int i) {
        return this.getPathBreadcrumbAt(i).map(ServerRequest::stringToBigDecimal);
    }

    @Override
    public InputStream getInputStream() {
        return this.in;
    }

    @Override
    public RequestMethod getMethod() {
        return this.header.getRequestMethod();
    }

    @Override
    public Optional<String> getRequestPath() {
        return this.header.getRequestPath();
    }

    @Override
    public int getPathBreadcrumbsSize() { return this.header.getPathBreadcrumbsSize(); }

    @Override
    public Optional<String> getRequestType() {
        return this.header.getRequestType();
    }

    @Override
    public RequestHeaderValue<Stream<String>> getQueryArguments() {
        return this.header.getQueryArguments();
    }

    @Override
    public Stream<String> getAcceptLanguage() {
        return this.header.getAcceptLanguage();
    }

    @Override
    public Optional<Integer> getContentLength() {
        return this.header.getContentLength();
    }

    @Override
    public Optional<String> getUserAgent() {
        return this.header.getUserAgent();
    }

    @Override
    public Optional<String> getContentType() {
        return this.header.getContentType();
    }

    @Override
    public Stream<String> getContentEncoding() {
        return this.header.getContentEncoding();
    }

    @Override
    public Stream<String> getAcceptEncoding() {
        return this.header.getAcceptEncoding();
    }

    @Override
    public Optional<String> getConnection() {
        return this.header.getConnection();
    }

    @Override
    public Optional<String> getReferer() {
        return this.header.getReferer();
    }

    @Override
    public Optional<String> getHost() {
        return this.header.getHost();
    }

    @Override
    public Optional<String> getBoundary() {
        return this.header.getBoundary();
    }

    @Override
    public Stream<String> getAccept() {
        return this.header.getAccept();
    }

    @Override
    public Stream<Charset> getAcceptCharset() {
        return this.header.getAcceptCharset();
    }

    @Override
    public RequestHeaderValue<Optional<String>> getCookies() {
        return this.header.getCookies();
    }

    @Override
    public RequestHeaderValue<Optional<Integer>> getKeepAlive() { return this.header.getKeepAlive(); }

    @Override
    public RequestHeaderValue<Stream<String>> getOtherHeaders() {
        return this.header.getOtherHeaders();
    }

    @Override
    public Optional<Charset> getContentCharset() { return this.header.getContentCharset(); }
}