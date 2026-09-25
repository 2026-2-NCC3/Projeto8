package br.org.proximaetapa.app.data;

import android.content.Context;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import br.org.proximaetapa.app.BuildConfig;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Cria o cliente Retrofit já configurado com o token de autenticação
 * (quando existir) e um timeout tolerante a redes instáveis (RFM15).
 */
public final class ApiClient {

    private static ApiService instance;

    private ApiClient() {}

    public static ApiService get(Context context) {
        if (instance == null) {
            SessionManager session = new SessionManager(context);

            Interceptor authInterceptor = chain -> {
                Request original = chain.request();
                String token = session.getToken();
                if (token == null) return chain.proceed(original);
                Request autenticada = original.newBuilder()
                    .header("Authorization", "Bearer " + token)
                    .build();
                return chain.proceed(autenticada);
            };

            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(BuildConfig.DEBUG
                ? HttpLoggingInterceptor.Level.BODY
                : HttpLoggingInterceptor.Level.NONE);

            OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(authInterceptor)
                .addInterceptor(logging)
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build();

            Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BuildConfig.API_BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

            instance = retrofit.create(ApiService.class);
        }
        return instance;
    }
}
