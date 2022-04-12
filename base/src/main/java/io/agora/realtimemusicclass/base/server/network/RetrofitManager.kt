package io.agora.realtimemusicclass.base.server.network

import android.util.Log
import okhttp3.OkHttpClient
import retrofit2.converter.gson.GsonConverterFactory
import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import io.agora.realtimemusicclass.base.server.callback.NetworkCallback
import io.agora.realtimemusicclass.base.server.callback.ThrowableCallback
import okhttp3.Interceptor
import io.agora.realtimemusicclass.base.server.network.interceptor.HttpLoggingInterceptor
import okhttp3.internal.platform.Platform
import retrofit2.*
import java.lang.reflect.Type
import java.util.HashMap
import java.util.concurrent.TimeUnit

object RetrofitManager {
    private val tag = "RetrofitManager"
    private val client: OkHttpClient
    private val headers: MutableMap<String, String> = HashMap()
    private var logger: HttpLoggingInterceptor.Logger? = null

    fun addHeader(key: String, value: String) {
        headers[key] = value
    }

    fun setLogger(logger: HttpLoggingInterceptor.Logger) {
        this.logger = logger
    }

    fun <T> getService(baseUrl: String, tClass: Class<T>): T {
        val retrofit = Retrofit.Builder()
            .client(client)
            .baseUrl(baseUrl)
            .addConverterFactory(NullOnEmptyConverterFactory())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        return retrofit.create(tClass)
    }

    class NullOnEmptyConverterFactory : Converter.Factory() {
        override fun responseBodyConverter(
            type: Type, annotations: Array<Annotation>, retrofit: Retrofit
        ): Converter<okhttp3.ResponseBody, *> {
            val delegate = retrofit.nextResponseBodyConverter<Any>(this, type, annotations)
            return Converter { body: okhttp3.ResponseBody ->
                if (body.contentLength() == 0L) {
                    "[]"
                } else {
                    delegate.convert(body)
                }
            }
        }
    }

    class RetrofitCallback<T : ResponseBody<*>?>(private val successCode: Int,
                                                 private val callback: ThrowableCallback<T>) : retrofit2.Callback<T> {
        override fun onResponse(call: Call<T>, response: Response<T>) {
            response.errorBody()?.let { body ->
                try {
                    // When the server returns error messages via error body segments
                    // By default, the error body contains a error code and a string message
                    val errBody = Gson().fromJson<ResponseBody<String>>(String(body.bytes()),
                        object : TypeToken<ResponseBody<String>>(){}.type)
                    val httpCode = response.code()
                    val errCode: Int
                    val errMsg: String

                    if (errBody.msg.isNullOrEmpty()) {
                        errCode = errBody.code
                        errMsg = errBody.msg
                    } else {
                        errCode = httpCode
                        errMsg = errBody.msg
                    }

                    throwableCallback(BusinessException(errCode, errMsg, httpCode))
                } catch (e: JsonParseException) {
                    throwableCallback(e)
                } catch (e: JsonSyntaxException) {
                    throwableCallback(e)
                }
                return
            }

            response.body()?.let { body ->
                if (body.code == successCode) {
                    callback.onSuccess(body)
                } else {
                    val httpCode = response.code()
                    val errCode = body.code
                    val msg = body.msg as? String ?: ""
                    throwableCallback(BusinessException(errCode, msg, httpCode))
                }
            } ?: Runnable {
                throwableCallback(Throwable("response body is null"))
            }
        }

        override fun onFailure(call: Call<T>, t: Throwable) {
            throwableCallback(t)
        }

        private fun throwableCallback(throwable: Throwable) {
            (callback as? ThrowableCallback<T>)?.onFailure(throwable)
        }
    }

    init {
        val clientBuilder = OkHttpClient.Builder()
        clientBuilder.connectTimeout(30, TimeUnit.SECONDS)
        clientBuilder.readTimeout(30, TimeUnit.SECONDS)
        clientBuilder.addInterceptor(Interceptor { chain: Interceptor.Chain ->
            val request = chain.request()
            val requestBuilder = request.newBuilder()
                .method(request.method, request.body)

            for ((key, value) in headers) {
                requestBuilder.addHeader(key, value)
            }

            chain.proceed(requestBuilder.build())
        })

        clientBuilder.addInterceptor(HttpLoggingInterceptor(
            object : HttpLoggingInterceptor.Logger {
                override fun log(message: String) {
                    logger?.log(message) ?: Runnable {
                        Platform.get().log(message, Platform.INFO, null)
                    }
                }
            }
        ).setLevel(HttpLoggingInterceptor.Level.BODY))

        // use custom RetryInterceptor instead.
        // clientBuilder.retryOnConnectionFailure(false);
        clientBuilder.addInterceptor(RetryInterceptor(3))
        client = clientBuilder.build()

        // Set default logger to logcat console
        setLogger(object : HttpLoggingInterceptor.Logger {
            override fun log(message: String) {
                Log.d(tag, message)
            }
        })
    }
}