package ee.fj.http.tinyweb.server.request;

import java.util.Collections;
import java.util.Map;

public class SimpleRequestHeaderValue<T> extends BaseRequestHeaderValue<T, T> {
    @SuppressWarnings("rawtypes")
    private static final SimpleRequestHeaderValue EMPTY_VALUE = new SimpleRequestHeaderValue<>(Collections.emptyMap());

    SimpleRequestHeaderValue(Map<String, T> values) {
        super(values);
    }

    @Override
    public T getValue(String key) {
        return super.getValueFromMap(key);
    }

    @SuppressWarnings("unchecked")
    static <T> SimpleRequestHeaderValue<T> emptyContainer() {
        return (SimpleRequestHeaderValue<T>) EMPTY_VALUE;
    }
}
