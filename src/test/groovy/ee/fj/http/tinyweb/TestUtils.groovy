package ee.fj.http.tinyweb

import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.web.client.RestTemplate

class TestUtils {
    private static int PORT_START = 9090

    static int getServerPort() {
        return PORT_START ++;
    }

    static RestTemplate getRestTemplate() {
        HttpComponentsClientHttpRequestFactory httpRequestFactory = new HttpComponentsClientHttpRequestFactory()
        httpRequestFactory.setConnectionRequestTimeout(5 * 1000)
        httpRequestFactory.setConnectTimeout(5 * 1000)
        httpRequestFactory.setReadTimeout(5 * 1000)
        return new RestTemplate(httpRequestFactory)
    }
}
