package ee.fj.http.tinyweb;

import java.util.Optional;
import java.util.function.BiConsumer;

public interface Binder {
    Binder bind(String path, BiConsumer<Request, Response> responseHandler);
    Optional<String> getPath();
    Optional<String> getAbsolutePath();
}