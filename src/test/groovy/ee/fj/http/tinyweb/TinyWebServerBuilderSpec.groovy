package ee.fj.http.tinyweb

import org.springframework.http.HttpStatus
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.RestTemplate
import spock.lang.Shared
import spock.lang.Specification

class TinyWebServerBuilderSpec extends Specification {
    private static final int SERVER_PORT = TestUtils.getServerPort()
    private static final int SERVER_PORT_1 = TestUtils.getServerPort()

    @Shared
    private TinyWebServer tinyWebServer
    @Shared
    private RestTemplate restTemplate = TestUtils.getRestTemplate()

    def setupSpec() {
        tinyWebServer = TinyWebServerBuilder.builder()
                .build({ req, resp ->
                    resp.setResponseType(Response.TYPE.TEXT)
                    resp.getWriter().append("Lorem Ipsum Est")
                    resp.setHeader(true, Response.STATUS.OK);
                })
        def subPath = tinyWebServer.bind("first", {req, resp ->
            resp.setResponseType(Response.TYPE.TEXT)
            resp.getWriter().append("This is subpath")
            resp.setHeader(true, Response.STATUS.OK);
        })
        subPath.bind("not_found", {req, resp ->
            resp.setResponseType(Response.TYPE.TEXT)
            resp.getWriter().append("This is not found")
            resp.setHeader(true, Response.STATUS.NOT_FOUND);
        })

        tinyWebServer.bind("managed_error", {req, resp ->
            resp.setResponseType(Response.TYPE.TEXT)
            resp.getWriter().append("Managed error")
            resp.setHeader(true, Response.STATUS.INTERNAL_SERVER_ERROR);
        })
        tinyWebServer.bind("unmanaged_error", {req, resp ->
            throw new IllegalStateException("This is unmanaged error for test!")
        })

        tinyWebServer.start(SERVER_PORT)
    }

    def cleanupSpec() {
        tinyWebServer.stop()
    }

    def "The states are fired correctly"() {
        given:
            def stateArray = []
        when:
            TinyWebServer tinyWebServer2 = TinyWebServerBuilder.builder().serverStateChange({ state -> stateArray.add(state) })
                    .build({ req, resp ->
                        throw new IllegalStateException("It should not run this!")
            })
        then:
            stateArray == []
            tinyWebServer2.getAbsolutePath().isEmpty()
            tinyWebServer2.getPath().isEmpty()
        when:
            tinyWebServer2.start(SERVER_PORT_1)
        then:
            stateArray == [ServerState.INITIALIZING, ServerState.RUNNING]
            tinyWebServer2.stop()  == true
            stateArray == [ServerState.INITIALIZING, ServerState.RUNNING, ServerState.SHUTTING_DDOWN, ServerState.STOP]
    }

    def "Sub path calls correctly"() {
        when:
            def response = restTemplate.getForEntity("http://localhost:${SERVER_PORT}/first", String.class)
        then:
            response.toString() == "<200,This is subpath,[Content-Type:\"text/plain;charset=UTF-8\", Server:\"FJ tiny server\", Content-Length:\"15\", Connection:\"Closed\"]>"
            response.getBody() == "This is subpath"
            response.getStatusCode() == HttpStatus.OK
            response.getStatusCodeValue() == 200

    }

    def "Test simple request and response"() {
        when:
            def response = restTemplate.getForEntity("http://localhost:${SERVER_PORT}", String.class)
        then:
            response.toString() == "<200,Lorem Ipsum Est,[Content-Type:\"text/plain;charset=UTF-8\", Server:\"FJ tiny server\", Content-Length:\"15\", Connection:\"Closed\"]>"
            response.getBody() == "Lorem Ipsum Est"
            response.getStatusCode() == HttpStatus.OK
            response.getStatusCodeValue() == 200

    }

    def "Sub path of sub path finds closest one calls correctly"() {
        when:
            def response = restTemplate.getForEntity("http://localhost:${SERVER_PORT}/first/second", String.class)
        then:
            response.toString() == "<200,This is subpath,[Content-Type:\"text/plain;charset=UTF-8\", Server:\"FJ tiny server\", Content-Length:\"15\", Connection:\"Closed\"]>"
            response.getBody() == "This is subpath"
            response.getStatusCode() == HttpStatus.OK
            response.getStatusCodeValue() == 200
    }

    def "Unknown path goes to root"() {
        when:
            def response = restTemplate.getForEntity("http://localhost:${SERVER_PORT}/this/is/not/defined", String.class)
        then:
            response.toString() == "<200,Lorem Ipsum Est,[Content-Type:\"text/plain;charset=UTF-8\", Server:\"FJ tiny server\", Content-Length:\"15\", Connection:\"Closed\"]>"
            response.getBody() == "Lorem Ipsum Est"
            response.getStatusCode() == HttpStatus.OK
            response.getStatusCodeValue() == 200
    }


    def "Unmanaged exceptons are being written into response"() {
        when:
            def response = restTemplate.getForEntity("http://localhost:${SERVER_PORT}/unmanaged_error", String.class)
        then:
            response == null
            def error = thrown (HttpServerErrorException)
            error.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR
            error.getResponseBodyAsString().startsWith("java.lang.IllegalStateException: This is unmanaged error for test!")
    }

    def "When managing exceptions all is good"() {
        when:
            def response = restTemplate.getForEntity("http://localhost:${SERVER_PORT}/managed_error", String.class)
        then:
            response == null
            def error = thrown (HttpServerErrorException)
            error.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR
            error.getResponseBodyAsString() == "Managed error"
    }

    def "When the page is not found is being returned"() {
        when:
            def response = restTemplate.getForEntity("http://localhost:${SERVER_PORT}/first/not_found", String.class)
        then:
            def error = thrown (HttpClientErrorException)
            error.getStatusCode() == HttpStatus.NOT_FOUND
            error.getResponseBodyAsString() == "This is not found"
    }

    def "Not found sub path goes back to last known path"() {
        when:
            def response = restTemplate.getForEntity("http://localhost:${SERVER_PORT}/first/not_found/sub_path", String.class)
        then:
            def error = thrown (HttpClientErrorException)
            error.getStatusCode() == HttpStatus.NOT_FOUND
            error.getResponseBodyAsString() == "This is not found"
    }
}
