package ee.fj.http.tinyweb.server.request;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

public class OptionalRequestHeaderValue<T> extends BaseRequestHeaderValue<Optional<T>, T> {
    @SuppressWarnings("rawtypes")
    private static final OptionalRequestHeaderValue EMPTY_VALUE = new OptionalRequestHeaderValue<>(Collections.emptyMap());

    OptionalRequestHeaderValue(Map<String, T> values) {
        super(values);
    }

    @Override
    public Optional<T> getValue(String key) {
        return Optional.ofNullable(super.getValueFromMap(key));
    }

    @SuppressWarnings("unchecked")
    static <T> OptionalRequestHeaderValue<T> emptyContainer() {
        return (OptionalRequestHeaderValue<T>) EMPTY_VALUE;
    }
}
