package ee.fj.http.tinyweb

import groovy.json.JsonSlurper
import org.springframework.http.*
import org.springframework.web.client.RestTemplate
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import java.util.stream.Collectors

class TinyWebServerBuilderGetRequestsSpec extends Specification {
    private static final int SERVER_PORT = TestUtils.getServerPort()

    @Shared
    private TinyWebServer tinyWebServer

    def setupSpec() {
        tinyWebServer = TinyWebServerBuilder.builder()
                .build({ req, resp ->
                    resp.setResponseType(Response.TYPE.TEXT)
                    resp.getWriter().append("Lorem Ipsum Est")
                    resp.setHeader(true, Response.STATUS.INTERNAL_SERVER_ERROR);
                })
        RequestMethod.values().each {
            String path = "request_${it.name().toLowerCase()}"
            tinyWebServer.bind(path, { req, resp ->
                assert req.getMethod() == it
                assert req.getAccept().collect(Collectors.joining(",")) == "application/json"
                if (it != RequestMethod.PUT)
                    resp.getWriter().append("{\"name\": \"${req.getQueryArgument("name").get()}\", \"age\": ${req.getQueryArgument("age").get()}1}")
                resp.setResponseType(Response.TYPE.JSON)
                resp.setHeader(true, Response.STATUS.OK)
            })
        }
        tinyWebServer.start(SERVER_PORT)
    }

    def cleanupSpec() {
        tinyWebServer.stop()
    }

    @Unroll
    def "The response body is set correctly #requestMethod"() {
        setup:
            HttpHeaders headers = new HttpHeaders();
            headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);
            HttpEntity<String> entity = new HttpEntity(headers)

            def jsonSlurper = new JsonSlurper()
            String url = "http://localhost:${SERVER_PORT}/request_${requestMethod.name().toLowerCase()}?name=My name is&age=15"
            HttpMethod method = HttpMethod.resolve(requestMethod.name())
        when:
            def response = TestUtils.getRestTemplate().exchange(url, method, entity, String.class)
        then:
            response.getStatusCodeValue() == 200
            response.getStatusCode() == HttpStatus.OK
            // head request has no body
            response.getBody() != null
            response.getBody() != ""
            def jsonResult = jsonSlurper.parseText(response.getBody())
            jsonResult.age == 151
            jsonResult.name == "My name is"
        where:
            requestMethod << RequestMethod.values().findAll{it != RequestMethod.HEAD && it != RequestMethod.PUT}
    }

    @Unroll
    def "The response body is is empty for #requestMethod"() {
        setup:
            HttpHeaders headers = new HttpHeaders();
            headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);
            HttpEntity<String> entity = new HttpEntity(headers)

            def jsonSlurper = new JsonSlurper()
            String url = "http://localhost:${SERVER_PORT}/request_${requestMethod.name().toLowerCase()}?name=My name is&age=15"
            HttpMethod method = HttpMethod.resolve(requestMethod.name())
        when:
            def response = TestUtils.getRestTemplate().exchange(url, method, entity, String.class)
        then:
            response.getStatusCodeValue() == 200
            response.getStatusCode() == HttpStatus.OK
            // head request has no body
            response.getBody() == null
        where:
            requestMethod << RequestMethod.values().findAll{it == RequestMethod.HEAD || it == RequestMethod.PUT}
    }
}
