package br.org.proximaetapa.app.model;

import java.util.List;

/**
 * Pacote devolvido por GET /sync — tudo o que o app precisa guardar
 * localmente, em uma requisição só (RFM15).
 */
public class SyncResposta {
    public String servidor_em;   // carimbo do servidor; vira o "desde" da próxima sincronização
    public boolean completo;     // true = pacote inteiro; false = só o que mudou
    public Aluno aluno;
    public List<Curso> cursos;
    public List<Inscricao> inscricoes;
    public List<AgendaItem> agenda;
    public List<Presenca> presencas;
    public List<Certificado> certificados;
    public List<Mensagem> mensagens;
    public List<Notificacao> notificacoes;
}
