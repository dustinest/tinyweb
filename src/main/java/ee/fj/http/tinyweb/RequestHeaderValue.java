package ee.fj.http.tinyweb;

import java.util.stream.Stream;

public interface RequestHeaderValue<T> {
    Stream<String> getKeys();
    T getValue(String key);
    boolean hasKey(String key);
}
