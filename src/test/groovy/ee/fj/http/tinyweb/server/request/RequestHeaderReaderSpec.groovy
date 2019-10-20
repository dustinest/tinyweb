package ee.fj.http.tinyweb.server.request

import spock.lang.Unroll

import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.stream.Collectors

import ee.fj.http.tinyweb.RequestMethod
import spock.lang.Specification

class RequestHeaderReaderSpec extends Specification {
	@Unroll
	def "Encoding fails on #value"() {
		expect:
		!RequestHeaderReader.decodeISO_8859_1(value).isPresent()
		where:
		value << [null, ""]
	}

	@Unroll
	def "Encoding succeeds #value"() {
		given:
		def res = RequestHeaderReader.decodeISO_8859_1(value)
		expect:
		res.isPresent()
		res.get() == result
		where:
		value                                             | result
		"abc"                                             | "abc"
		"http%3A%2F%2Fwww.delfi.%2Cee%2F%3Fa%3Db%3Bc%3Dd" | "http://www.delfi.,ee/?a=b;c=d"
	}

	void "Request Body 1 test"() {
		when:
			def result = RequestHeaderReader.getRequestHeaders(readFile("request_body1"))
		then:
			result != null
		and:
			result.requestMethod == RequestMethod.POST
		and:
			result.getRequestPath().get() == "/docs/index.html"
			result.getHost().get() == "www.nowhere123.com"
			result.getAccept().collect(Collectors.toList()) == ["image/gif", "image/jpeg", "*/*"]
			result.getAcceptLanguage().collect(Collectors.toList()) == ["fr-CH", "fr", "en", "de", "*"]
			result.getAcceptEncoding().collect(Collectors.toList()) == ["gzip", "deflate"]
			result.getUserAgent().get().equals("Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1)")
			result.getContentType().get().equals("text/html")
			result.getBoundary().isEmpty()
			result.getContentLength().get() == 12
			result.getConnection().get() == "Keep-Alive"
			result.getRequestPathArray() == ["docs", "index.html"]
			result.getQueryArguments().getKeys().collect(Collectors.toList()) == ["third", "first", "second"]
			result.getQueryArguments().getValue("third").collect(Collectors.toList()) == ["whatever"]
			result.getQueryArguments().getValue("first").collect(Collectors.toList()) == ["value1"]
			result.getQueryArguments().getValue("second").collect(Collectors.toList()) == []
			result.getContentEncoding().collect(Collectors.toList()) == ["gzip", "identity"]
			result.getContentCharset().get() == StandardCharsets.UTF_8
			result.getCookies().getKeys().sorted().collect(Collectors.toList()) == ["name", "name2", "name3"]
			result.getCookies().getValue("name")
			result.getCookies().getValue("name2")
			result.getCookies().getValue("name3")
			result.getCookies().getValue("name").get().equals("value")
			result.getCookies().getValue("name2").get().equals("value2")
			result.getCookies().getValue("name3").get().equals("value3")
			result.getCacheControl().getKeys().collect(Collectors.toList()) == ["no-store"]
			result.getCacheControl().hasKey("no-store")
			result.getCacheControl().getValue("no-store").isEmpty()
			result.getPragma().get() == "no-cache"
	}

	void "Request Body 2 test"() {
		when:
		def result = RequestHeaderReader.getRequestHeaders(readFile("request_body2"))
		then:
		result != null
		and:
		result.requestMethod == RequestMethod.POST
		and:
		result.getRequestPath().get() == "/docs/index.html"
		result.getHost().get() == "www.nowhere123.com"
		result.getAccept().collect(Collectors.toList()) == ["text/html", "application/xhtml+xml", "application/xml", "image/webp", "*/*"]
		result.getAcceptLanguage().collect(Collectors.toList()) == ["fr-CH", "fr", "en", "de", "*"]
		result.getAcceptEncoding().collect(Collectors.toList()) == ["deflate", "gzip", "*"]
		result.getUserAgent().get().equals("Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1)")
		result.getContentType().get().equals("multipart/form-data")
		result.getBoundary().get().equals("another cool boundary")
		result.getContentLength().get() == 12
		result.getConnection().get() == "Keep-Alive"
		result.getRequestPathArray() == ["docs", "index.html"]
		result.getQueryArguments().getKeys().collect(Collectors.toList()) == ["third", "first", "second"]
		result.getQueryArguments().getValue("third").collect(Collectors.toList()) == ["whatever"]
		result.getQueryArguments().getValue("first").collect(Collectors.toList()) == ["value1"]
		result.getQueryArguments().getValue("second").collect(Collectors.toList()) == []
		result.getContentEncoding().collect(Collectors.toList()) == ["gzip", "identity"]
		result.getContentCharset().get() == StandardCharsets.UTF_8
		result.getCookies().getKeys().sorted().collect(Collectors.toList()) == ["name", "name2", "name3"]
		result.getCookies().hasKey("name")
		result.getCookies().hasKey("name2")
		result.getCookies().hasKey("name3")
		result.getCookies().getValue("name").get().equals("value")
		result.getCookies().getValue("name2").get().equals("value2")
		result.getCookies().getValue("name3").get().equals("value3")
		result.getReferer().get().equals("https://developer.mozilla.org/en-US/docs/Web/JavaScript")
		result.getAcceptCharset().collect(Collectors.toList()) == [StandardCharsets.UTF_8, StandardCharsets.ISO_8859_1]
		result.getKeepAlive().getKeys().sorted().collect(Collectors.toList()) == ["max", "timeout"]
		result.getKeepAlive().getValue("timeout").get() == 5
		result.getKeepAlive().getValue("max").get() == 1000

		result.getCacheControl().getKeys().sorted().collect(Collectors.toList()) == ["max-age", "public"]
		result.getCacheControl().hasKey("public")
		result.getCacheControl().getValue("public").isEmpty()
		result.getCacheControl().hasKey("max-age")
		result.getCacheControl().getValue("max-age").get() == 31536000

		result.getPragma().isEmpty()
	}

	private InputStream readFile(String name) {
		def file = new File("src/test/resources/${name}.txt").getText()
		return new ByteArrayInputStream(file.getBytes())
	}
}
