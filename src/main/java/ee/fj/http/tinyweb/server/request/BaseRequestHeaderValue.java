package ee.fj.http.tinyweb.server.request;

import ee.fj.http.tinyweb.RequestHeaderValue;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

abstract class BaseRequestHeaderValue<G, T> implements RequestHeaderValue<G> {
    private final Map<String, T> values;

    BaseRequestHeaderValue(Map<String, T> values) {
        this.values = values;
    }

    @Override
    public Stream<String> getKeys() {
        return values.keySet().stream().filter(Objects::nonNull);
    }

    T getValueFromMap(String key) {
        if (hasKey(key)) {
            return values.get(key);
        }
        return null;
    }

    @Override
    public boolean hasKey(String key) {
        return values.containsKey(key);
    }
}
