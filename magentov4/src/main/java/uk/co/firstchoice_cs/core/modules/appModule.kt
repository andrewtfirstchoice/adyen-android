package uk.co.firstchoice_cs.core.modules


import android.os.Build
import com.adyen.checkout.core.api.SSLSocketUtil
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.core.qualifier.named
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import uk.co.firstchoice_cs.Settings
import uk.co.firstchoice_cs.Settings.DEFAULT_SERVICE
import uk.co.firstchoice_cs.Settings.UNSAFE_SERVICE
import uk.co.firstchoice_cs.adyen.data.api.CheckoutApiService
import uk.co.firstchoice_cs.core.ActivityRetriever
import uk.co.firstchoice_cs.core.api.BasicAuthInterceptor
import uk.co.firstchoice_cs.core.helpers.OkHttpHelper
import uk.co.firstchoice_cs.core.listeners.DefaultCurrentActivityListener
import uk.co.firstchoice_cs.firstchoice.BuildConfig
import java.security.KeyStore
import java.util.*
import java.util.concurrent.TimeUnit
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

val appModule = module {

    fun provideHttpClient(): OkHttpClient {
        val builder = OkHttpClient.Builder().let {
            val interceptor = HttpLoggingInterceptor()
            interceptor.level = HttpLoggingInterceptor.Level.BODY
            it.addNetworkInterceptor(interceptor)
        }

        if (Build.VERSION_CODES.JELLY_BEAN <= Build.VERSION.SDK_INT && Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1) {

            val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
            trustManagerFactory.init(null as KeyStore?)
            val trustManagers = trustManagerFactory.trustManagers
            check(!(trustManagers.size != 1 || trustManagers[0] !is X509TrustManager)) {
                "Unexpected default trust managers:" + Arrays.toString(trustManagers)
            }

            val trustManager = trustManagers[0] as X509TrustManager
            builder.sslSocketFactory(SSLSocketUtil.TLS_SOCKET_FACTORY, trustManager)
        }

        return builder.build()
    }

    fun provideApi(httpClient: OkHttpClient): CheckoutApiService {
        val baseUrl =
            if (CheckoutApiService.isRealUrlAvailable())
                BuildConfig.MERCHANT_SERVER_URL
            else
                "http://myserver.com/my/endpoint/"
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(httpClient)
            .addConverterFactory(
                MoshiConverterFactory.create(
                    Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
                )
            )
            .addCallAdapterFactory(CoroutineCallAdapterFactory())
            .build()
            .create(CheckoutApiService::class.java)
    }

    single() { provideHttpClient() }

    single(named("adyenClient")) {
        provideHttpClient()
    }
    single { provideApi(get()) }
    single { DefaultCurrentActivityListener() }
    single { ActivityRetriever(get()) }

    single {
       val authInterceptor = BasicAuthInterceptor("","")
        authInterceptor
    }

    single {
        val interceptor = HttpLoggingInterceptor()
        interceptor.level = HttpLoggingInterceptor.Level.BODY
        interceptor
    }

    single {
        Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()
    }

    single(named(DEFAULT_SERVICE)) {
        val client = OkHttpClient().newBuilder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)

        client.build()
    }


   single(named(UNSAFE_SERVICE)) {
        val client = OkHttpHelper.unsafeOkHttpClient.newBuilder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)


        if (BuildConfig.DEBUG) {
            client.addInterceptor(get<HttpLoggingInterceptor>())

        }
        client.addInterceptor(get<BasicAuthInterceptor>())
        client.build()
    }
}

