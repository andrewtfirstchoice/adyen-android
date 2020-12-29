package uk.co.firstchoice_cs.core.api.legacyAPI.restful;

import android.os.AsyncTask;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import uk.co.firstchoice_cs.App;
import uk.co.firstchoice_cs.core.api.legacyAPI.restful.api.Api;

/**
 * Created by steveparrish on 4/11/17.
 */
public abstract class BaseAsync<T> {

    final public static int NETWORK_ERROR_CODE = 0;
    final public static String NETWORK_ERROR_MSG = "Network Unavailable, try again later.";
    // This will enable/disable logging for async uk.co.firstchoice_cs.firstchoice.tasks that do not specifically request logging.
    private static final boolean ENABLE_DEFAULT_LOGGING = true;//BuildConfig.DEBUG;
    final protected OkHttpClient okHttpClient;
    final protected Retrofit retrofit;
    private int NETWORK_TIMEOUT_MS = 30000;
    private OnCompletion callback;
    private AsyncTask<Void, Void, Response<T>> task = new AsyncTask<Void, Void, Response<T>>() {
        @Override
        protected Response<T> doInBackground(Void... params) {
            return search();
        }

        @Override
        protected void onPostExecute(Response<T> result) {
            if (callback != null) {
                if (result != null) {
                    T body = result.body();
                    if (result.isSuccessful() && body != null) {
                        callback.onSuccess(BaseAsync.this, body);
                        return;
                    }
                    callback.onFailure(BaseAsync.this, result.code(), result.message());
                    return;
                }
                callback.onFailure(BaseAsync.this, BaseAsync.NETWORK_ERROR_CODE, BaseAsync.NETWORK_ERROR_MSG);
            }
        }
    };

    public BaseAsync() {
        this(Api.TARGET_URL);
    }

    public BaseAsync(final String url) {
        this(url, ENABLE_DEFAULT_LOGGING);
    }

    public BaseAsync(final String url, boolean withDebugging) {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        // set your desired log level
        if (withDebugging) {
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);
        } else {
            logging.setLevel(HttpLoggingInterceptor.Level.NONE);
        }

        Gson gson = new GsonBuilder()
                .serializeNulls()
                .create();

        okHttpClient = App.httpClient.newBuilder()
                .connectTimeout(NETWORK_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .readTimeout(NETWORK_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .writeTimeout(NETWORK_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .addInterceptor(logging)
                .build();

        retrofit = new Retrofit.Builder()
                .baseUrl(url)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .client(okHttpClient)
                .build();
    }

    public BaseAsync(final String userId, final String token) {
        this(userId, token, ENABLE_DEFAULT_LOGGING);
    }

    public BaseAsync(final String userId, final String token, final boolean withDebugging) {
        this(userId, token, Api.TARGET_URL, withDebugging);
    }

    public BaseAsync(final String userId, final String token, final String url) {
        this(userId, token, url, ENABLE_DEFAULT_LOGGING);
    }

    public BaseAsync(final String userId, final String token, final String url, final boolean withDebugging) {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        // set your desired log level
        if (withDebugging) {
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);
        } else {
            logging.setLevel(HttpLoggingInterceptor.Level.NONE);
        }

        Gson gson = new GsonBuilder()
                .serializeNulls()
                .create();

        okHttpClient = App.httpClient.newBuilder()
                .connectTimeout(NETWORK_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .readTimeout(NETWORK_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .writeTimeout(NETWORK_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .addInterceptor(new MyInterceptor(userId, token))
                .addInterceptor(logging)
                .build();

        retrofit = new Retrofit.Builder()
                .baseUrl(url)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .client(okHttpClient)
                .build();
    }

    public void execute(OnCompletion callback) {
        this.callback = callback;
        task.execute();
    }

    public void cancel() {
        task.cancel(true);
    }

    public boolean isExecuting() {
        return (task.getStatus() == AsyncTask.Status.RUNNING);
    }

    abstract public Response<T> search();

    private class MyInterceptor implements Interceptor {

        private String userId;
        private String token;

        public MyInterceptor(final String userId, final String token) {
            this.userId = userId;
            this.token = token;
        }

        @Override
        public okhttp3.Response intercept(Chain chain) throws IOException {
            Request request = chain.request();
            if (!TextUtils.isEmpty(userId) || !TextUtils.isEmpty(token)) {
                okhttp3.Headers.Builder moreHeaders = request.headers().newBuilder();
                if (!TextUtils.isEmpty(userId)) {
                    moreHeaders.add("UserID", userId);
                }
                if (!TextUtils.isEmpty(token)) {
                    moreHeaders.add("admin_token", token);
                }
                request = request.newBuilder().headers(moreHeaders.build()).build();
            }
            return chain.proceed(request);
        }
    }

}
