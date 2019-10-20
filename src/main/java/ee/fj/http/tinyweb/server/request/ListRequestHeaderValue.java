package ee.fj.http.tinyweb.server.request;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class ListRequestHeaderValue<T> extends BaseRequestHeaderValue<Stream<T>, List<T>> {
    @SuppressWarnings("rawtypes")
    private static final ListRequestHeaderValue EMPTY_VALUE = new ListRequestHeaderValue<>(Collections.emptyMap());

    ListRequestHeaderValue(Map<String, List<T>> values) {
        super(values);
    }

    @Override
    public Stream<T> getValue(String key) {
        List<T> rv = super.getValueFromMap(key);
        if (rv == null) return Stream.empty();
        return rv.stream();
    }

    @SuppressWarnings("unchecked")
    static <T> ListRequestHeaderValue<T> emptyContainer() {
        return (ListRequestHeaderValue<T>) EMPTY_VALUE;
    }
}
