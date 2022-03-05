package cc.xiaobaicz.calladapter.hostchange;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.util.Objects;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSource;
import okio.ForwardingSource;
import okio.Okio;
import okio.Timeout;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Converter;
import retrofit2.Response;

/**
 * 仿OkHttpCall, 真实的Call
 * @see retrofit2.OkHttpCall<T>
 * @param <T>
 */
final class RealCall<T> implements Call<T> {

    private final okhttp3.Call call;

    private final Converter<ResponseBody, T> responseConverter;

    private boolean executed = false;

    private boolean canceled = false;

    RealCall(okhttp3.Call call, Converter<ResponseBody, T> responseConverter) {
        this.call = call;
        this.responseConverter = responseConverter;
    }

    @NonNull
    @Override
    public Response<T> execute() throws IOException {
        if (executed) throw new IllegalStateException("Already executed.");
        executed = true;

        if (canceled) {
            call.cancel();
        }
        return parseResponse(call.execute());
    }

    @Override
    public void enqueue(@NonNull Callback<T> callback) {
        Objects.requireNonNull(callback, "callback == null");

        if (executed) throw new IllegalStateException("Already executed.");
        executed = true;

        if (canceled) {
            call.cancel();
        }

        call.enqueue(new okhttp3.Callback() {
            @Override
            public void onResponse(@NonNull okhttp3.Call call, @NonNull okhttp3.Response rawResponse) {
                Response<T> response;
                try {
                    response = parseResponse(rawResponse);
                } catch (Throwable e) {
                    Utils.throwIfFatal(e);
                    callFailure(e);
                    return;
                }

                try {
                    callback.onResponse(RealCall.this, response);
                } catch (Throwable t) {
                    Utils.throwIfFatal(t);
                    t.printStackTrace();
                }
            }

            @Override
            public void onFailure(@NonNull okhttp3.Call call, @NonNull IOException e) {
                callFailure(e);
            }

            private void callFailure(Throwable e) {
                try {
                    callback.onFailure(RealCall.this, e);
                } catch (Throwable t) {
                    Utils.throwIfFatal(t);
                    t.printStackTrace();
                }
            }
        });
    }

    @Override
    public boolean isExecuted() {
        return executed;
    }

    @Override
    public void cancel() {
        canceled = true;
        call.cancel();
    }

    @Override
    public boolean isCanceled() {
        if (canceled)
            return true;
        return call.isCanceled();
    }

    @NonNull
    @Override
    public Call<T> clone() {
        return new RealCall<>(call.clone(), responseConverter);
    }

    @NonNull
    @Override
    public Request request() {
        return call.request();
    }

    @NonNull
    @Override
    public Timeout timeout() {
        return call.timeout();
    }

    Response<T> parseResponse(okhttp3.Response rawResponse) throws IOException {
        ResponseBody rawBody = rawResponse.body();
        assert rawBody != null;

        rawResponse = rawResponse.newBuilder()
                .body(new NoContentResponseBody(rawBody.contentType(), rawBody.contentLength()))
                .build();

        int code = rawResponse.code();
        if (code < 200 || code >= 300) {
            try {
                ResponseBody bufferedBody = Utils.buffer(rawBody);
                return Response.error(bufferedBody, rawResponse);
            } finally {
                rawBody.close();
            }
        }

        if (code == 204 || code == 205) {
            rawBody.close();
            return Response.success(null, rawResponse);
        }

        ExceptionCatchingResponseBody catchingBody = new ExceptionCatchingResponseBody(rawBody);
        try {
            T body = responseConverter.convert(catchingBody);
            return Response.success(body, rawResponse);
        } catch (RuntimeException e) {
            catchingBody.throwIfCaught();
            throw e;
        }
    }

    static final class NoContentResponseBody extends ResponseBody {
        private final MediaType contentType;
        private final long contentLength;

        NoContentResponseBody(MediaType contentType, long contentLength) {
            this.contentType = contentType;
            this.contentLength = contentLength;
        }

        @Override
        public MediaType contentType() {
            return contentType;
        }

        @Override
        public long contentLength() {
            return contentLength;
        }

        @NonNull
        @Override
        public BufferedSource source() {
            throw new IllegalStateException("Cannot read raw response body of a converted body.");
        }
    }

    static final class ExceptionCatchingResponseBody extends ResponseBody {
        private final ResponseBody delegate;
        private final BufferedSource delegateSource;
        IOException thrownException;

        ExceptionCatchingResponseBody(ResponseBody delegate) {
            this.delegate = delegate;
            this.delegateSource = Okio.buffer(new ForwardingSource(delegate.source()) {
                @Override
                public long read(@NonNull Buffer sink, long byteCount) throws IOException {
                    try {
                        return super.read(sink, byteCount);
                    } catch (IOException e) {
                        thrownException = e;
                        throw e;
                    }
                }
            });
        }

        @Override
        public MediaType contentType() {
            return delegate.contentType();
        }

        @Override
        public long contentLength() {
            return delegate.contentLength();
        }

        @NonNull
        @Override
        public BufferedSource source() {
            return delegateSource;
        }

        @Override
        public void close() {
            delegate.close();
        }

        void throwIfCaught() throws IOException {
            if (thrownException != null) {
                throw thrownException;
            }
        }
    }

}
