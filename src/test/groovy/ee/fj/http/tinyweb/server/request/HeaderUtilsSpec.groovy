import ee.fj.http.tinyweb.server.request.HeaderUtils
import ee.fj.http.tinyweb.server.request.RequestHeaderReader
import spock.lang.Specification
import spock.lang.Unroll

import java.util.stream.Collectors

class HeaderUtilsSpec extends Specification {
    @Unroll
    def "Simple SplitStream with #value"() {
        given:
            def res = HeaderUtils.compileToStreamPattern("/").apply(value).collect(Collectors.toList())
        expect:
            res == result
        where:
            value        | result
            "a/b/c"      | ["a", "b", "c"]
            "/a//b//c//" | ["a", "b", "c"]
    }

    @Unroll
    def "test getFirstItemByPriority on #headerValue"() {
        when:
            def result = HeaderUtils.getFirstItemByPriority(headerValue)
        then:
            result == expected
        where:
            headerValue                                    | expected
            "fr-CH, fr;q=0.9, en;q=0.8, de;q=0.7, *;q=0.5" | ["fr-CH", "fr", "en", "de", "*"]
            "fr-CH, fr;q=0.9, en;q=0.7, de;q=0.8, *;q=0.5" | ["fr-CH", "fr", "de", "en", "*"]
    }
}