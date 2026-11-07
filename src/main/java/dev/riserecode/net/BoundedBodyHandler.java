package dev.riserecode.net;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

/** Enforces a byte limit during acquisition, even for chunked bodies with no Content-Length. */
public final class BoundedBodyHandler implements HttpResponse.BodyHandler<byte[]> {
    private final int limit;
    public BoundedBodyHandler(int limit) {
        if (limit < 0) { throw new IllegalArgumentException("Negative body limit"); }
        this.limit = limit;
    }
    @Override public HttpResponse.BodySubscriber<byte[]> apply(HttpResponse.ResponseInfo info) {
        return new Subscriber(limit);
    }
    private static final class Subscriber implements HttpResponse.BodySubscriber<byte[]> {
        private final int limit;
        private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        private final CompletableFuture<byte[]> result = new CompletableFuture<>();
        private Flow.Subscription subscription;
        Subscriber(int limit) { this.limit = limit; }
        @Override public CompletionStage<byte[]> getBody() { return result; }
        @Override public void onSubscribe(Flow.Subscription value) {
            if (subscription != null) { value.cancel(); return; }
            subscription = value;
            result.whenComplete((body, error) -> { if (result.isCancelled()) { value.cancel(); } });
            value.request(1);
        }
        @Override public void onNext(List<ByteBuffer> buffers) {
            if (result.isDone()) { return; }
            for (ByteBuffer buffer : buffers) {
                if (buffer.remaining() > limit - bytes.size()) {
                    subscription.cancel();
                    result.completeExceptionally(new IOException("Response exceeds " + limit + " bytes"));
                    return;
                }
                byte[] part = new byte[buffer.remaining()];
                buffer.get(part); bytes.writeBytes(part);
            }
            subscription.request(1);
        }
        @Override public void onError(Throwable error) { result.completeExceptionally(error); }
        @Override public void onComplete() { result.complete(bytes.toByteArray()); }
    }
}
