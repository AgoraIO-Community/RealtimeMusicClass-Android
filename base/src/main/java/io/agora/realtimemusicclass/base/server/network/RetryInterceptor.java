package io.agora.realtimemusicclass.base.server.network;

import android.util.Log;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class RetryInterceptor implements Interceptor {
    private static final String tag = "RetryInterceptor";

    private final AtomicInteger maxNum = new AtomicInteger();
    private final Map<Integer, Integer> retryNumRecords = Collections.synchronizedMap(new MaxSizeHashMap<>(100));


    public RetryInterceptor(int max) {
        maxNum.set(max);
    }

    @Override
    public @NotNull
    Response intercept(@NotNull Chain chain) throws IOException {
        Request request = chain.request();
        Response response = chain.proceed(request);
        Integer retryNum = retryNumRecords.get(request.hashCode());
        retryNum = retryNum == null ? 0 : retryNum;
        while (!response.isSuccessful() && retryNum < maxNum.get()) {
            Log.i(tag, "RetryNum:" + retryNum);
            retryNum++;
            response.close();
            retryNumRecords.put(request.hashCode(), retryNum);
            response = chain.proceed(request);
        }
        return response;
    }

    private static class MaxSizeHashMap<K, V> extends LinkedHashMap<K, V> {
        private final int maxSize;

        private MaxSizeHashMap(int maxSize) {
            this.maxSize = maxSize;
        }

        @Override
        protected boolean removeEldestEntry(Entry eldest) {
            return size() > maxSize;
        }
    }
}
