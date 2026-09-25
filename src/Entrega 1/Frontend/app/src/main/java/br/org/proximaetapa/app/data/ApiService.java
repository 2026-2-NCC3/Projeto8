package br.org.proximaetapa.app.data;

import java.util.List;

import br.org.proximaetapa.app.model.AgendaItem;
import br.org.proximaetapa.app.model.Curso;
import br.org.proximaetapa.app.model.LoginRequest;
import br.org.proximaetapa.app.model.LoginResponse;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

/** Contrato da API consumido pelo app (ver backend/API.md para a documentação completa). */
public interface ApiService {

    @POST("auth/login")
    Call<LoginResponse> login(@Body LoginRequest request);

    @GET("cursos")
    Call<List<Curso>> listarCursos();

    @GET("cursos/inscritos/meus")
    Call<List<Curso>> listarMeusCursos();

    @GET("agenda")
    Call<List<AgendaItem>> listarAgenda();
}
