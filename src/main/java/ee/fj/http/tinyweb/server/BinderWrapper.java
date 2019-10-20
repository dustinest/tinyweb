package ee.fj.http.tinyweb.server;

import java.io.InputStream;
import java.util.*;
import java.util.function.BiConsumer;

import ee.fj.http.tinyweb.Binder;
import ee.fj.http.tinyweb.Request;
import ee.fj.http.tinyweb.Response;
import ee.fj.http.tinyweb.server.request.RequestHeaders;

public class BinderWrapper implements Binder {
    private final Map<String, BinderWrapper> paths = new HashMap<>();
    private final BiConsumer<Request, Response> responseHandler;
    private final BinderWrapper parentBinder;
    private final Optional<String> path;
    private final Optional<String> absolutePath;

    BinderWrapper(BiConsumer<Request, Response> responseHandler, String path, BinderWrapper parentBinder) {
        Objects.requireNonNull(responseHandler);
        this.responseHandler = responseHandler;
        this.path = Optional.ofNullable(path);
        this.parentBinder = parentBinder;
        BinderWrapper parentBinderWrapper = parentBinder;
        StringJoiner joinedPath = new StringJoiner("/");
        while (parentBinderWrapper != null) {
            parentBinderWrapper.path.ifPresent(joinedPath::add);
            parentBinderWrapper = parentBinderWrapper.parentBinder;
        }
        this.path.ifPresent(joinedPath::add);
        this.absolutePath = Optional.ofNullable(joinedPath.length() > 0 ? joinedPath.toString() : null);
    }

    @Override
    public final Binder bind(String path, BiConsumer<Request, Response> responseHandler) {
        Objects.requireNonNull(path);
        Objects.requireNonNull(responseHandler);
        if (path.indexOf(47) > -1) {
            throw new IllegalArgumentException("You cannot bind to the subPath!");
        }
        BinderWrapper sv = new BinderWrapper(responseHandler, path, this);
        this.paths.put(path, sv);
        return sv;
    }

    final void handle(List<String> path, RequestHeaders requestHeaders, Response response, InputStream in) {
        if (path.size() > 0 && this.paths.containsKey(path.get(0))) {
            String _path = path.remove(0);
            this.paths.get(_path).handle(path, requestHeaders, response, in);
        } else {
            this.responseHandler.accept(new ServerRequest(path, requestHeaders, in), response);
        }
    }

    @Override
    public Optional<String> getPath() {
        return this.path;
    }

    @Override
    public Optional<String> getAbsolutePath() {
        return this.absolutePath;
    }
}