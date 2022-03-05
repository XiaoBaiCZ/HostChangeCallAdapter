package cc.xiaobaicz.calladapter.hostchange;

import androidx.annotation.NonNull;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.CallAdapter;
import retrofit2.Converter;
import retrofit2.Retrofit;

final class HostChangeCallAdapter<R, T> implements CallAdapter<R, T> {

    private final Retrofit retrofit;
    private final CallAdapter<R, T> adapter;
    private final Converter<ResponseBody, R> responseBodyConverter;
    private final Annotation annotation;
    private final Method baseUrlMethod;

    public HostChangeCallAdapter(Retrofit retrofit, CallAdapter<R, T> adapter, Converter<ResponseBody, R> responseBodyConverter, Annotation annotation, Method hostMethod) {
        this.retrofit = retrofit;
        this.adapter = adapter;
        this.responseBodyConverter = responseBodyConverter;
        this.annotation = annotation;
        this.baseUrlMethod = hostMethod;
    }

    @NonNull
    @Override
    public Type responseType() {
        return adapter.responseType();
    }

    @NonNull
    @Override
    public T adapt(Call<R> call) {
        try {
            final Request oldReq = call.request();
            final HttpUrl oldUrl = oldReq.url();
            final HttpUrl.Builder urlBuilder = oldUrl.newBuilder();
            final String baseUrlStr = Utils.checkNull((String) baseUrlMethod.invoke(annotation), "baseUrl is null");
            final HttpUrl baseUrl = Utils.checkNull(HttpUrl.parse(baseUrlStr), "baseUrl is null");
            urlBuilder.scheme(baseUrl.scheme());
            urlBuilder.host(baseUrl.host());
            if (baseUrl.port() > 0 && baseUrl.port() < 65536)
                urlBuilder.port(baseUrl.port());
            final HttpUrl newUrl = urlBuilder.build();
            final Request newReq = oldReq.newBuilder().url(newUrl).build();
            final okhttp3.Call newCall = retrofit.callFactory().newCall(newReq);
            return adapter.adapt(new RealCall<>(newCall, responseBodyConverter));
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

}
