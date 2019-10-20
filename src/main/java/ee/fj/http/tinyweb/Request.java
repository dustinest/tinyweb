package ee.fj.http.tinyweb;

import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

public interface Request {
    RequestMethod getMethod();

    Stream<String> getRequestPathStream();

    Optional<String> getRequestPath();

    Optional<String> getPathBreadcrumbAt(int var1);

    int getPathBreadcrumbsSize();

    Optional<String> getRequestType();

    RequestHeaderValue<Stream<String>> getQueryArguments();

    Stream<String> getAcceptLanguage();

    Optional<Integer> getContentLength();

    Optional<String> getUserAgent();

    Optional<String> getContentType();

    Stream<String> getContentEncoding();

    Stream<String> getAcceptEncoding();

    Optional<String> getConnection();

    Optional<String> getReferer();

    Optional<String> getHost();

    Optional<String> getBoundary();

    Stream<String> getAccept();

    Stream<Charset> getAcceptCharset();

    RequestHeaderValue<Optional<String>> getCookies();


    RequestHeaderValue<Stream<String>> getOtherHeaders();

    Optional<String> getQueryArgument(String var1);

    Optional<BigDecimal> getQueryArgumentAsNumber(String var1);

    <U extends Number> Optional<U> getQueryArgumentAsNumber(String var1, Function<BigDecimal, ? extends U> var2);

    String getPathString();

    Optional<BigDecimal> getPathBreadcrumbAsNumberAt(int var1);

    <U extends Number> Optional<U> getPathBreadcrumbAsNumberAt(int var1, Function<BigDecimal, ? extends U> var2);

    InputStream getInputStream();

    Optional<Charset> getContentCharset();

    RequestHeaderValue<Optional<Integer>> getKeepAlive();
}