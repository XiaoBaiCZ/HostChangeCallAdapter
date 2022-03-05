package cc.xiaobaicz.calladapter.hostchange;

import androidx.annotation.NonNull;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import okhttp3.ResponseBody;
import retrofit2.CallAdapter;
import retrofit2.Converter;
import retrofit2.Retrofit;

public final class HostChangeCallAdapterFactory extends CallAdapter.Factory {

    private final static Map<Class<? extends Annotation>, Method> methodMap = new HashMap<>();

    static {
        registerAnnotation(CustomizeHost.class);
    }

    private HostChangeCallAdapterFactory() {}

    public static HostChangeCallAdapterFactory create() {
        return new HostChangeCallAdapterFactory();
    }

    /**
     * 注册自定义baseUrl注解
     * @param clazz 包含 [String baseUrl()] 函数的自定义注解
     */
    public static void registerAnnotation(Class<? extends Annotation> clazz) {
        if (!methodMap.containsKey(clazz)) {
            Method baseUrl = Utils.checkNull(Utils.findBaseUrlMethod(clazz), "cannot find annotation method <String baseUrl()>");
            methodMap.put(clazz, baseUrl);
        }
    }

    @Override
    public CallAdapter<?, ?> get(@NonNull Type returnType, @NonNull Annotation[] annotations, @NonNull Retrofit retrofit) {
        return _get(returnType, annotations, retrofit);
    }

    /**
     * 创建CallAdapter
     */
    private <X, Y> CallAdapter<X, Y> _get(Type returnType, Annotation[] annotations, Retrofit retrofit) {
        // 检索自定义注解
        final Set<Class<? extends Annotation>> annotationClassSet = methodMap.keySet();
        for (Annotation target : annotations) {
            for (Class<? extends Annotation> annotationClass : annotationClassSet) {
                if (annotationClass.isAssignableFrom(target.getClass())) {
                    final Method hostMethod = Utils.checkNull(methodMap.get(annotationClass), "HostMethod is null");

                    // 实际 CallAdapter
                    CallAdapter<X, Y> realAdapter = findRealCallAdapter(returnType, annotations, retrofit);
                    if (realAdapter == null)
                        // 无效 CallAdapter
                        return null;

                    // 检索 Converter 链
                    final Converter<ResponseBody, X> responseBodyConverter = retrofit.responseBodyConverter(realAdapter.responseType(), annotations);

                    return new HostChangeCallAdapter<>(retrofit, realAdapter, responseBodyConverter, target, hostMethod);
                }
            }
        }
        return null;
    }

    // 查找真实 CallAdapter
    private <X, Y> CallAdapter<X, Y> findRealCallAdapter(Type returnType, Annotation[] annotations, Retrofit retrofit) {
        CallAdapter<X, Y> realAdapter = null;
        // 检索 CallAdapter 链
        for (CallAdapter.Factory callAdapterFactory : retrofit.callAdapterFactories()) {
            // 过滤自身
            if (callAdapterFactory instanceof HostChangeCallAdapterFactory)
                continue;
            CallAdapter<X, Y> callAdapter = (CallAdapter<X, Y>) callAdapterFactory.get(returnType, annotations, retrofit);
            if (callAdapter == null)
                continue;
            // 合适的 CallAdapter
            realAdapter = callAdapter;
            break;
        }
        return realAdapter;
    }
}
