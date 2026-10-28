package br.org.proximaetapa.app.data;

import android.content.Context;

import java.util.concurrent.TimeUnit;

import br.org.proximaetapa.app.BuildConfig;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Cria o cliente Retrofit já configurado com o token de autenticação (quando
 * existir) e com tempos de espera tolerantes a redes instáveis (RFM15).
 */
public final class ApiClient {

    private static ApiService instance;

    private ApiClient() {
    }

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
                    // 60 segundos, e não os 15 da Entrega 1, por um motivo
                    // concreto: o servidor roda no plano gratuito do Render,
                    // que hiberna após alguns minutos sem uso e leva de 30 a 50
                    // segundos para voltar. Com 15 segundos, a primeira
                    // requisição do dia falhava sempre — o servidor respondia,
                    // mas o app já tinha desistido.
                    .connectTimeout(60, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .writeTimeout(60, TimeUnit.SECONDS)
                    // Repete uma vez sozinho quando a conexão cai no meio do
                    // caminho, situação comum em rede móvel instável.
                    .retryOnConnectionFailure(true)
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
