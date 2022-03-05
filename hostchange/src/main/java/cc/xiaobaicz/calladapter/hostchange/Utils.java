package cc.xiaobaicz.calladapter.hostchange;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.lang.reflect.Method;

import okhttp3.ResponseBody;
import okio.Buffer;

final class Utils {

    static Method findBaseUrlMethod(Class<?> clazz) {
        try {
            Method baseUrl = clazz.getDeclaredMethod("baseUrl");
            if (!String.class.isAssignableFrom(baseUrl.getReturnType()))
                return null;
            return baseUrl;
        } catch (Throwable t) {
            return null;
        }
    }

    @NonNull
    static <T> T checkNull(T obj, String msg) {
        if (obj != null)
            return obj;
        throw new NullPointerException(msg);
    }

    static void throwIfFatal(Throwable t) {
        if (t instanceof VirtualMachineError) {
            throw (VirtualMachineError) t;
        } else if (t instanceof ThreadDeath) {
            throw (ThreadDeath) t;
        } else if (t instanceof LinkageError) {
            throw (LinkageError) t;
        }
    }

    static ResponseBody buffer(final ResponseBody body) throws IOException {
        Buffer buffer = new Buffer();
        body.source().readAll(buffer);
        return ResponseBody.create(body.contentType(), body.contentLength(), buffer);
    }
}
