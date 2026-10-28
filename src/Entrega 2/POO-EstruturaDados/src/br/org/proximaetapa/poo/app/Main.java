package br.org.proximaetapa.poo.app;

import br.org.proximaetapa.poo.modelo.Alternativa;
import br.org.proximaetapa.poo.modelo.Aluno;
import br.org.proximaetapa.poo.modelo.Atividade;
import br.org.proximaetapa.poo.modelo.Card;
import br.org.proximaetapa.poo.modelo.Certificado;
import br.org.proximaetapa.poo.modelo.Curso;
import br.org.proximaetapa.poo.modelo.Inscricao;
import br.org.proximaetapa.poo.modelo.ItemFeed;
import br.org.proximaetapa.poo.modelo.Mensagem;
import br.org.proximaetapa.poo.modelo.Notificacao;
import br.org.proximaetapa.poo.modelo.Pergunta;
import br.org.proximaetapa.poo.modelo.Presenca;
import br.org.proximaetapa.poo.modelo.ResultadoPerfil;
import br.org.proximaetapa.poo.modelo.TestePerfil;
import br.org.proximaetapa.poo.repositorio.AgendaDoAluno;
import br.org.proximaetapa.poo.repositorio.Comparadores;
import br.org.proximaetapa.poo.repositorio.ControleDePresenca;
import br.org.proximaetapa.poo.repositorio.FilaDeSincronizacao;
import br.org.proximaetapa.poo.repositorio.RepositorioAtividade;
import br.org.proximaetapa.poo.repositorio.RepositorioEmMemoria;
import br.org.proximaetapa.poo.servico.Resultado;
import br.org.proximaetapa.poo.servico.ServicoCertificado;
import br.org.proximaetapa.poo.servico.ServicoFeed;
import br.org.proximaetapa.poo.servico.ServicoInscricao;
import br.org.proximaetapa.poo.servico.ServicoPresenca;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Demonstração executável da arquitetura de classes e das estruturas de dados
 * da UC "Programação Orientada a Objetos e Estrutura de Dados" — Entrega 2 do
 * projeto "Próxima Etapa — App do Aluno".
 *
 * <p>Dados fictícios. Todas as datas são calculadas a partir de hoje, para que
 * a agenda e a consulta "próximos encontros" façam sentido em qualquer dia em
 * que a demonstração for executada.</p>
 *
 * <p>Compilar e rodar:
 * <pre>
 * javac -encoding UTF-8 -d out $(find src -name "*.java")
 * java -Dfile.encoding=UTF-8 -cp out br.org.proximaetapa.poo.app.Main
 * </pre>
 */
public class Main {

    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final LocalDate HOJE = LocalDate.now();
    private static final LocalDateTime AGORA = LocalDateTime.now();

    // ---------------------------------------------------------------- estado
    private static final RepositorioEmMemoria<Aluno> repoAlunos = new RepositorioEmMemoria<>();
    private static final RepositorioEmMemoria<Curso> repoCursos = new RepositorioEmMemoria<>();
    private static final RepositorioAtividade repoAtividades = new RepositorioAtividade();
    private static final RepositorioEmMemoria<Inscricao> repoInscricoes = new RepositorioEmMemoria<>();
    private static final RepositorioEmMemoria<Certificado> repoCertificados = new RepositorioEmMemoria<>();
    private static final RepositorioEmMemoria<Mensagem> repoMensagens = new RepositorioEmMemoria<>();
    private static final RepositorioEmMemoria<Notificacao> repoNotificacoes = new RepositorioEmMemoria<>();
    private static final RepositorioEmMemoria<ResultadoPerfil> repoResultados = new RepositorioEmMemoria<>();
    private static final ControleDePresenca controlePresenca = new ControleDePresenca();

    private static final ServicoInscricao servicoInscricao =
            new ServicoInscricao(repoAlunos, repoCursos, repoInscricoes, repoAtividades);
    private static final ServicoPresenca servicoPresenca =
            new ServicoPresenca(repoAlunos, repoAtividades, servicoInscricao, controlePresenca);
    private static final ServicoFeed servicoFeed =
            new ServicoFeed(repoAlunos, repoInscricoes, repoMensagens, repoNotificacoes);
    private static final ServicoCertificado servicoCertificado =
            new ServicoCertificado(repoAlunos, repoCursos, repoCertificados, repoAtividades,
                    controlePresenca, servicoInscricao, servicoFeed);

    public static void main(String[] args) {
        cabecalho();
        secao1Cadastrar();
        secao2LocalizarPorId();
        secao3BuscarPorFiltro();
        secao4EditarERemover();
        secao5Inscricoes();
        secao6Agenda();
        secao7CheckIn();
        secao8FilaOffline();
        secao9Certificados();
        secao10TestePerfil();
        secao11Feed();
        secao12Card();
        secao13Ordenacao();
        rodape();
    }

    // ============================================================== SEÇÃO 1
    private static void secao1Cadastrar() {
        secao(1, "CADASTRAR E VISUALIZAR (ArrayList + HashMap)");

        novoAluno("Ana Souza", "ana.souza@email.com", "EE Prof. Lauro Gomes", "3o ano EM", "São Paulo");
        novoAluno("Bruno Lima", "bruno.lima@email.com", "EE Jardim Paulista", "2o ano EM", "São Paulo");
        novoAluno("Carla Mendes", "carla.mendes@email.com", "EE Vila Nova", "3o ano EM", "Diadema");
        novoAluno("Diego Rocha", "diego.rocha@email.com", "EE Parque Industrial", "1o ano EM", "Santo André");
        novoAluno("Elaine Castro", "elaine.castro@email.com", "ETEC Centro", "3o ano EM", "São Paulo");
        novoAluno("Felipe Araujo", "felipe.araujo@email.com", "EE Vila Nova", "2o ano EM", "Diadema");

        novoCurso("Introdução à Programação", "Lógica, Python e primeiros projetos.",
                "Tecnologia", "Sala 12 - Sede", 40, HOJE.minusDays(21), HOJE.plusDays(10), 30, true);
        novoCurso("Oficina de Redação para o ENEM", "Estrutura dissertativa e repertório.",
                "Educação", "Sala 3 - Sede", 20, HOJE.plusDays(5), HOJE.plusDays(40), 25, true);
        novoCurso("Finanças Pessoais para Jovens", "Orçamento, poupança e consumo consciente.",
                "Empreendedorismo", "Auditório", 12, HOJE.plusDays(9), HOJE.plusDays(30), 2, true);

        // Encontros do curso 1 (3 já realizados + 1 futuro)
        novaAtividade(1, "Encontro 1 - Lógica de programação", HOJE.minusDays(21), "PE-PROG-01");
        novaAtividade(1, "Encontro 2 - Variáveis e condicionais", HOJE.minusDays(14), "PE-PROG-02");
        novaAtividade(1, "Encontro 3 - Repetição e listas", HOJE.minusDays(7), "PE-PROG-03");
        novaAtividade(1, "Encontro 4 - Projeto final", HOJE.plusDays(3), "PE-PROG-04");
        // Encontros dos demais cursos
        novaAtividade(2, "Oficina 1 - Estrutura do texto", HOJE.plusDays(5), "PE-RED-01");
        novaAtividade(2, "Oficina 2 - Repertório sociocultural", HOJE.plusDays(12), "PE-RED-02");
        novaAtividade(3, "Aula única - Orçamento pessoal", HOJE.plusDays(9), "PE-FIN-01");

        linha("Alunos cadastrados: " + repoAlunos.total());
        repoAlunos.listarTodos().forEach(a -> item(a.toString()));
        linha("");
        linha("Cursos cadastrados: " + repoCursos.total());
        repoCursos.listarTodos().forEach(c -> item(c.toString()));
        linha("");
        linha("Atividades (encontros) cadastradas: " + repoAtividades.total());
        repoAtividades.listarTodos().forEach(a -> item(a.toString()));
    }

    // ============================================================== SEÇÃO 2
    private static void secao2LocalizarPorId() {
        secao(2, "LOCALIZAR POR ID - O(1) no HashMap");

        linha("repoAlunos.localizarPorId(3)  -> " + repoAlunos.localizarPorId(3).orElseThrow());
        linha("repoCursos.localizarPorId(2)  -> " + repoCursos.localizarPorId(2).orElseThrow());
        linha("repoAlunos.localizarPorId(99) -> " + repoAlunos.localizarPorId(99)
                .map(Object::toString).orElse("Optional.empty (nenhum registro com esse id)"));
        linha("");
        linha("Índice secundário do RepositorioAtividade (QR Code -> Atividade), também O(1):");
        linha("  localizarPorQrCode(\"PE-PROG-03\") -> "
                + repoAtividades.localizarPorQrCode("PE-PROG-03").orElseThrow().getTitulo());
        linha("  localizarPorQrCode(\"PE-XXXX-99\") -> "
                + repoAtividades.localizarPorQrCode("PE-XXXX-99")
                        .map(Atividade::getTitulo).orElse("QR Code inválido"));
    }

    // ============================================================== SEÇÃO 3
    private static void secao3BuscarPorFiltro() {
        secao(3, "BUSCAR POR FILTRO - O(n) com Predicate (equivale a um WHERE)");

        linha("Cursos da categoria \"Tecnologia\":");
        repoCursos.localizar(c -> "Tecnologia".equals(c.getCategoria()))
                  .forEach(c -> item(c.getTitulo()));

        linha("");
        linha("Cursos que ainda têm vaga:");
        repoCursos.localizar(Curso::temVaga)
                  .forEach(c -> item(c.getTitulo() + " - " + c.getVagasDisponiveis() + " vaga(s)"));

        linha("");
        linha("Alunos de Diadema:");
        repoAlunos.localizar(a -> "Diadema".equals(a.getCidade()))
                  .forEach(a -> item(a.getNome() + " - " + a.getEscola()));

        linha("");
        linha("Encontros do curso 1 (Introdução à Programação):");
        repoAtividades.localizar(a -> a.getCursoId() == 1)
                      .forEach(a -> item(a.getData().format(DATA) + " - " + a.getTitulo()));
    }

    // ============================================================== SEÇÃO 4
    private static void secao4EditarERemover() {
        secao(4, "EDITAR E REMOVER");

        Aluno bruno = repoAlunos.localizarPorId(2).orElseThrow();
        linha("Antes : " + bruno);
        bruno.setEscola("ETEC Jardim Paulista");   // edição encapsulada, com validação
        bruno.setSerie("3o ano EM");
        linha("Depois: " + bruno);

        linha("");
        Curso redacao = repoCursos.localizarPorId(2).orElseThrow();
        linha("Antes : " + redacao);
        redacao.setLocal("Sala 7 - Anexo");
        redacao.setCargaHoraria(24);
        linha("Depois: " + redacao + " | local=" + redacao.getLocal());

        linha("");
        linha("Edição com validação (tentar nome em branco):");
        try {
            bruno.setNome("   ");
        } catch (IllegalArgumentException e) {
            linha("  Recusado pelo encapsulamento -> " + e.getMessage());
        }

        linha("");
        Curso piloto = novoCurso("Curso piloto de Robótica", "Turma de teste interna.",
                "Tecnologia", "Laboratório", 8, HOJE.plusDays(60), HOJE.plusDays(70), 10, false);
        linha("Cadastrado para teste de remoção: " + piloto);
        linha("repoCursos.remover(" + piloto.getId() + ")  -> " + repoCursos.remover(piloto.getId()));
        linha("repoCursos.remover(" + piloto.getId() + ")  -> " + repoCursos.remover(piloto.getId())
                + "  (já havia sido removido)");

        Aluno felipe = repoAlunos.localizarPorId(6).orElseThrow();
        linha("repoAlunos.remover(6) (" + felipe.getNome() + ") -> " + repoAlunos.remover(6));
        linha("Total após remoções: " + repoAlunos.total() + " alunos, "
                + repoCursos.total() + " cursos.");
    }

    // ============================================================== SEÇÃO 5
    private static void secao5Inscricoes() {
        secao(5, "INSCRIÇÕES - regra de vaga e HashSet de unicidade");

        linha(servicoInscricao.inscrever(1, 1).linha());  // Ana  -> Programação
        linha(servicoInscricao.inscrever(2, 1).linha());  // Bruno -> Programação
        linha(servicoInscricao.inscrever(3, 1).linha());  // Carla -> Programação
        linha(servicoInscricao.inscrever(1, 2).linha());  // Ana  -> Redação
        linha(servicoInscricao.inscrever(4, 2).linha());  // Diego -> Redação

        linha("");
        linha("Inscrição duplicada (Ana no curso 1 de novo) - bloqueada em O(1) pelo HashSet:");
        linha("  " + servicoInscricao.inscrever(1, 1).linha());

        linha("");
        linha("Curso \"Finanças Pessoais para Jovens\" tem apenas 2 vagas:");
        linha("  " + servicoInscricao.inscrever(4, 3).linha());   // Diego
        linha("  " + servicoInscricao.inscrever(5, 3).linha());   // Elaine
        linha("  " + servicoInscricao.inscrever(3, 3).linha());   // Carla -> sem vaga
        linha("  Estado do curso: " + repoCursos.localizarPorId(3).orElseThrow());

        linha("");
        linha("Inscrições registradas: " + repoInscricoes.total());
        repoInscricoes.listarTodos().forEach(i -> item(i.toString()));
    }

    // ============================================================== SEÇÃO 6
    private static void secao6Agenda() {
        secao(6, "AGENDA DO ALUNO - TreeMap ordenado por data/hora");

        AgendaDoAluno agenda = servicoInscricao.agendaDo(1); // Ana
        linha("Agenda de Ana Souza (" + agenda.total() + " encontros), em ordem cronológica.");
        linha("Os encontros foram inseridos fora de ordem pelo cadastro; o TreeMap os ordena na inserção:");
        agenda.emOrdemCronologica().forEach(Main::itemAgenda);

        linha("");
        linha("Próximos encontros a partir de agora (" + AGORA.format(DATA_HORA)
                + ") - tailMap, O(log n):");
        List<Atividade> proximos = agenda.proximosEncontros(AGORA);
        if (proximos.isEmpty()) linha("  (nenhum encontro futuro)");
        proximos.forEach(Main::itemAgenda);

        linha("");
        linha("Histórico - encontros já realizados - headMap, O(log n):");
        agenda.encontrosAnteriores(AGORA).forEach(Main::itemAgenda);

        linha("");
        linha("Consulta de intervalo - encontros entre " + HOJE.format(DATA) + " e "
                + HOJE.plusDays(7).format(DATA) + " - subMap:");
        List<Atividade> semana = agenda.encontrosEntre(HOJE, HOJE.plusDays(7));
        if (semana.isEmpty()) linha("  (nada nos próximos 7 dias)");
        semana.forEach(Main::itemAgenda);

        linha("");
        Optional<Atividade> proximo = agenda.proximoEncontro(AGORA);
        linha("Cartão \"seu próximo encontro\" (ceilingEntry, O(log n)): "
                + proximo.map(a -> a.getTitulo() + " em " + a.getDataHoraInicio().format(DATA_HORA))
                         .orElse("sem encontros futuros"));
    }

    // ============================================================== SEÇÃO 7
    private static void secao7CheckIn() {
        secao(7, "CHECK-IN POR QR CODE - HashSet impede presença duplicada");

        linha("Ana Souza confirma presença nos encontros 1, 2 e 3 (3 de 4):");
        linha("  " + servicoPresenca.registrarCheckIn(1, "PE-PROG-01", horaDoEncontro("PE-PROG-01")).linha());
        linha("  " + servicoPresenca.registrarCheckIn(1, "PE-PROG-02", horaDoEncontro("PE-PROG-02")).linha());
        linha("  " + servicoPresenca.registrarCheckIn(1, "PE-PROG-03", horaDoEncontro("PE-PROG-03")).linha());

        linha("");
        linha("Ana passa o QR Code do encontro 1 uma segunda vez (UNIQUE aluno+atividade):");
        linha("  " + servicoPresenca.registrarCheckIn(1, "PE-PROG-01", AGORA).linha());

        linha("");
        linha("Bruno Lima confirma presença no encontro 1:");
        linha("  " + servicoPresenca.registrarCheckIn(2, "PE-PROG-01", horaDoEncontro("PE-PROG-01")).linha());

        linha("");
        linha("Diego Rocha tenta usar o QR do curso de Programação, no qual não está inscrito:");
        linha("  " + servicoPresenca.registrarCheckIn(4, "PE-PROG-02", AGORA).linha());

        linha("");
        linha("QR Code inexistente:");
        linha("  " + servicoPresenca.registrarCheckIn(1, "PE-FALSO-00", AGORA).linha());

        linha("");
        linha("Presenças registradas até aqui: " + controlePresenca.total());
        controlePresenca.listarTodas().forEach(p -> item(p.toString()));
    }

    // ============================================================== SEÇÃO 8
    private static void secao8FilaOffline() {
        secao(8, "FILA DE SINCRONIZAÇÃO OFFLINE - Queue/LinkedList (FIFO)");

        linha("A oficina ocorreu em uma escola sem sinal. O app guardou as leituras na fila local,");
        linha("na ordem exata em que os alunos passaram o QR Code:");

        servicoPresenca.enfileirarOffline(3, "PE-PROG-02", horaDoEncontro("PE-PROG-02").plusMinutes(1));
        servicoPresenca.enfileirarOffline(2, "PE-PROG-03", horaDoEncontro("PE-PROG-03").plusMinutes(2));
        servicoPresenca.enfileirarOffline(3, "PE-PROG-03", horaDoEncontro("PE-PROG-03").plusMinutes(3));
        servicoPresenca.enfileirarOffline(3, "PE-PROG-03", horaDoEncontro("PE-PROG-03").plusMinutes(9));
        servicoPresenca.enfileirarOffline(4, "PE-QR-RASGADO", AGORA);

        FilaDeSincronizacao fila = servicoPresenca.getFila();
        linha("");
        linha("Fila pendente (" + fila.tamanho() + " itens), do primeiro ao último:");
        int posicao = 1;
        for (FilaDeSincronizacao.CheckInPendente pendente : fila.listarPendentes()) {
            item(posicao++ + "o. " + pendente);
        }
        linha("Próximo a ser enviado (peek): " + fila.espiarProximo());

        linha("");
        linha("Conexão restabelecida -> drenando a fila (poll, O(1) por item):");
        List<FilaDeSincronizacao.ResultadoEnvio> relatorio = servicoPresenca.sincronizar();
        int ordem = 1;
        for (FilaDeSincronizacao.ResultadoEnvio envio : relatorio) {
            item(ordem++ + "o. " + (envio.aceito() ? "ENVIADO " : "REJEITADO ") + envio.motivo());
        }
        linha("");
        linha("Fila após a sincronização: " + fila.tamanho() + " item(ns) pendente(s) - vazia="
                + fila.estaVazia());
        linha("Observação: o 3o e o 4o item eram o mesmo aluno no mesmo encontro; a ordem FIFO");
        linha("garantiu que valeu a PRIMEIRA leitura, e a duplicata foi rejeitada na sequência.");

        linha("");
        linha("Histórico de presenças de Carla Mendes:");
        for (Presenca p : servicoPresenca.historicoDoAluno(3)) {
            item(repoAtividades.localizarPorId(p.getAtividadeId()).orElseThrow().getTitulo()
                    + " | " + p.getRegistradoEm().format(DATA_HORA)
                    + (p.isSincronizadaOffline() ? " | sincronizada da fila" : ""));
        }
    }

    // ============================================================== SEÇÃO 9
    private static void secao9Certificados() {
        secao(9, "CERTIFICADO POR CRITÉRIO DE FREQUÊNCIA (mínimo "
                + (int) (Certificado.FREQUENCIA_MINIMA * 100) + "%)");

        int cursoId = 1;
        linha("Frequência no curso \"" + repoCursos.localizarPorId(cursoId).orElseThrow().getTitulo()
                + "\" (" + servicoCertificado.totalDeEncontros(cursoId) + " encontros):");
        for (int alunoId : new int[]{1, 2, 3}) {
            Aluno aluno = repoAlunos.localizarPorId(alunoId).orElseThrow();
            item(String.format("%-14s %d/%d presenças = %.0f%%",
                    aluno.getNome(),
                    servicoCertificado.presencasNoCurso(alunoId, cursoId),
                    servicoCertificado.totalDeEncontros(cursoId),
                    servicoCertificado.frequencia(alunoId, cursoId) * 100));
        }

        linha("");
        linha("Emissão:");
        linha("  " + servicoCertificado.emitir(1, cursoId).linha());   // Ana, 75% -> emite
        linha("  " + servicoCertificado.emitir(2, cursoId).linha());   // Bruno, 25% -> recusa
        linha("  " + servicoCertificado.emitir(3, cursoId).linha());   // Carla, 50% -> recusa
        linha("");
        linha("Segunda tentativa para Ana (UNIQUE aluno+curso):");
        linha("  " + servicoCertificado.emitir(1, cursoId).linha());

        linha("");
        linha("Certificados emitidos: " + repoCertificados.total());
        repoCertificados.listarTodos().forEach(c -> item(c.toString()));
        linha("Situação da inscrição de Ana no curso 1: "
                + servicoInscricao.localizar(1, 1).orElseThrow().getStatus());
    }

    // ============================================================= SEÇÃO 10
    private static void secao10TestePerfil() {
        secao(10, "TESTE DE PERFIL - TreeMap de perguntas + EnumMap de apuração");

        TestePerfil teste = montarTestePerfil();
        linha(teste.toString());
        linha("Perguntas na ordem pedagógica (o TreeMap já as devolve ordenadas):");
        for (Pergunta p : teste.getPerguntasEmOrdem()) {
            item(p.getOrdem() + ") " + p.getTexto());
            for (Alternativa a : p.getAlternativas()) {
                linha("        [" + a.getId() + "] " + a.getTexto()
                        + "  (" + a.getPerfil().getCodigo() + ")");
            }
        }

        // Respostas de Ana: pergunta -> alternativa (mesmo formato do backend)
        Map<Integer, Integer> respostas = new LinkedHashMap<>();
        respostas.put(1, 1);   // exatas
        respostas.put(2, 5);   // exatas
        respostas.put(3, 10);  // humanas

        linha("");
        linha("Respostas enviadas por Ana Souza: " + respostas);
        ResultadoPerfil resultado = teste.apurar(repoResultados.proximoId(), 1, respostas);
        repoResultados.cadastrar(resultado);
        linha("Apuração: " + resultado.getContagemFormatada());
        linha("Perfil apurado: " + resultado.getPerfilResultado().getCodigo().toUpperCase());
        linha("Descrição: " + resultado.getDescricao());
        linha("Aviso: " + resultado.getAviso());
        linha("Registro salvo: " + resultado);

        servicoFeed.notificar(1, Notificacao.Tipo.MENSAGEM, "Resultado do teste de perfil",
                "Seu perfil é " + resultado.getPerfilResultado().getCodigo()
                        + ". Veja os cursos sugeridos para você.");
    }

    // ============================================================= SEÇÃO 11
    private static void secao11Feed() {
        secao(11, "FEED UNIFICADO - herança (ItemFeed) e polimorfismo");

        servicoFeed.publicarParaTodos("Inscrições abertas para o 2o semestre",
                "Novas turmas de tecnologia, redação e finanças. Vagas limitadas!",
                AGORA.minusDays(2));
        servicoFeed.publicarParaCurso(1, "Encontro 4 confirmado",
                "O projeto final será apresentado na Sala 12, às 14h. Leve seu notebook.",
                AGORA.minusHours(5));
        servicoFeed.notificar(1, Notificacao.Tipo.AGENDA, "Lembrete de agenda",
                "Você tem um encontro marcado nos próximos dias.");

        linha("Mensagens publicadas: " + repoMensagens.total()
                + " | Notificações geradas: " + repoNotificacoes.total());
        repoMensagens.listarTodos().forEach(m -> item(m.toString()));

        linha("");
        linha("Feed de Ana Souza - uma única List<ItemFeed> com Mensagem e Notificacao juntas,");
        linha("ordenada pelo mesmo Comparator e impressa com a mesma chamada linhaDoFeed():");
        for (ItemFeed itemFeed : servicoFeed.feedDoAluno(1)) {
            item(itemFeed.linhaDoFeed());
        }

        linha("");
        linha("Cada objeto responde com o próprio rótulo (despacho polimórfico):");
        for (ItemFeed itemFeed : servicoFeed.feedDoAluno(1)) {
            item(String.format("%-18s -> rotuloTipo() = %s",
                    itemFeed.getClass().getSimpleName(), itemFeed.rotuloTipo()));
        }

        linha("");
        linha("Notificações não lidas de Ana: " + servicoFeed.naoLidas(1).size());
        linha("Marcadas como lidas: " + servicoFeed.marcarTodasComoLidas(1));
        linha("Não lidas após abrir a tela: " + servicoFeed.naoLidas(1).size());
    }

    // ============================================================= SEÇÃO 12
    private static void secao12Card() {
        secao(12, "CARD DIGITAL DO ALUNO (projeção imutável de Aluno)");

        for (int alunoId : new int[]{1, 2, 4}) {
            Aluno aluno = repoAlunos.localizarPorId(alunoId).orElseThrow();
            Card card = Card.paraAluno(aluno, servicoInscricao.cursosAtivos(alunoId));
            item(card.toString());
        }
        linha("");
        linha("O Card expõe apenas o que a carteirinha mostra: não carrega e-mail nem senha.");
    }

    // ============================================================= SEÇÃO 13
    private static void secao13Ordenacao() {
        secao(13, "ORDENAR COM Comparator - O(n log n)");

        linha("Ranking de alunos por pontos (gamificação):");
        int posicao = 1;
        for (Aluno aluno : repoAlunos.listarOrdenado(Comparadores.ALUNO_POR_PONTOS_DESC)) {
            item(String.format("%do. %-15s %3d pts  (%s)",
                    posicao++, aluno.getNome(), aluno.getPontos(), aluno.getNivel()));
        }

        linha("");
        linha("Top 3 do ranking (listarPrimeiros):");
        repoAlunos.listarPrimeiros(Comparadores.ALUNO_POR_PONTOS_DESC, 3)
                  .forEach(a -> item(a.getNome() + " - " + a.getPontos() + " pts"));

        linha("");
        linha("Alunos em ordem alfabética (mesmo repositório, outro Comparator):");
        repoAlunos.listarOrdenado(Comparadores.ALUNO_POR_NOME)
                  .forEach(a -> item(a.getNome()));

        linha("");
        linha("Cursos por data de início:");
        repoCursos.listarOrdenado(Comparadores.CURSO_POR_DATA_INICIO)
                  .forEach(c -> item(c.getDataInicio().format(DATA) + " - " + c.getTitulo()));

        linha("");
        linha("Cursos com mais vagas livres primeiro:");
        repoCursos.listarOrdenado(Comparadores.CURSO_POR_VAGAS_DESC)
                  .forEach(c -> item(c.getVagasDisponiveis() + " vaga(s) - " + c.getTitulo()));

        linha("");
        linha("Ordem de inserção preservada no ArrayList (ordenar não altera o repositório):");
        repoCursos.listarTodos().forEach(c -> item(c.getTitulo()));
    }

    // ========================================================== auxiliares
    private static Aluno novoAluno(String nome, String email, String escola,
                                   String serie, String cidade) {
        return repoAlunos.cadastrar(new Aluno(repoAlunos.proximoId(), nome, email,
                "hash$" + email.hashCode(), escola, serie, cidade));
    }

    private static Curso novoCurso(String titulo, String descricao, String categoria, String local,
                                   int cargaHoraria, LocalDate inicio, LocalDate fim,
                                   int vagas, boolean certificado) {
        return repoCursos.cadastrar(new Curso(repoCursos.proximoId(), titulo, descricao, categoria,
                local, cargaHoraria, inicio, fim, vagas, certificado));
    }

    private static Atividade novaAtividade(int cursoId, String titulo, LocalDate data, String qrCode) {
        Curso curso = repoCursos.localizarPorId(cursoId).orElseThrow();
        return repoAtividades.cadastrar(new Atividade(repoAtividades.proximoId(), cursoId, titulo,
                data, LocalTime.of(14, 0), LocalTime.of(16, 0), curso.getLocal(), qrCode));
    }

    /** Momento verossímil do check-in: 5 minutos após o início do encontro. */
    private static LocalDateTime horaDoEncontro(String qrCode) {
        Atividade atividade = repoAtividades.localizarPorQrCode(qrCode).orElseThrow();
        return atividade.getDataHoraInicio().plusMinutes(5);
    }

    private static TestePerfil montarTestePerfil() {
        TestePerfil teste = new TestePerfil(1, "Teste de Perfil Vocacional",
                "Questionário de orientação educacional e profissional (RFM10).");

        Pergunta p1 = new Pergunta(1, "O que mais te interessa em um projeto?", 1);
        p1.adicionarAlternativa(new Alternativa(1, 1, "Resolver o problema lógico por trás dele",
                   Alternativa.Perfil.EXATAS))
          .adicionarAlternativa(new Alternativa(2, 1, "Entender as pessoas envolvidas",
                   Alternativa.Perfil.HUMANAS))
          .adicionarAlternativa(new Alternativa(3, 1, "Cuidar de quem precisa de ajuda",
                   Alternativa.Perfil.SAUDE))
          .adicionarAlternativa(new Alternativa(4, 1, "Criar algo visualmente bonito",
                   Alternativa.Perfil.ARTES));

        Pergunta p2 = new Pergunta(2, "Qual matéria da escola você prefere?", 2);
        p2.adicionarAlternativa(new Alternativa(5, 2, "Matemática", Alternativa.Perfil.EXATAS))
          .adicionarAlternativa(new Alternativa(6, 2, "História", Alternativa.Perfil.HUMANAS))
          .adicionarAlternativa(new Alternativa(7, 2, "Biologia", Alternativa.Perfil.SAUDE))
          .adicionarAlternativa(new Alternativa(8, 2, "Artes", Alternativa.Perfil.ARTES));

        Pergunta p3 = new Pergunta(3, "Como você prefere trabalhar em grupo?", 3);
        p3.adicionarAlternativa(new Alternativa(9, 3, "Organizando os dados e os cálculos",
                   Alternativa.Perfil.EXATAS))
          .adicionarAlternativa(new Alternativa(10, 3, "Apresentando e defendendo as ideias",
                   Alternativa.Perfil.HUMANAS))
          .adicionarAlternativa(new Alternativa(11, 3, "Cuidando do bem-estar da equipe",
                   Alternativa.Perfil.SAUDE))
          .adicionarAlternativa(new Alternativa(12, 3, "Montando a identidade visual",
                   Alternativa.Perfil.ARTES));

        // Inseridas fora de ordem de propósito: o TreeMap as reordena pela "ordem".
        return teste.adicionarPergunta(p3).adicionarPergunta(p1).adicionarPergunta(p2);
    }

    private static void itemAgenda(Atividade atividade) {
        String marcador = atividade.jaOcorreu(AGORA) ? "[realizado]" : "[  futuro ]";
        item(String.format("%s %s  %s  (curso %d, %s)", marcador,
                atividade.getDataHoraInicio().format(DATA_HORA), atividade.getTitulo(),
                atividade.getCursoId(), atividade.getLocal()));
    }

    private static void cabecalho() {
        linha("===============================================================================");
        linha(" PRÓXIMA ETAPA - APP DO ALUNO");
        linha(" UC Programação Orientada a Objetos e Estrutura de Dados - Entrega 2");
        linha(" Demonstração executável em Java puro | data de referência: " + HOJE.format(DATA));
        linha("===============================================================================");
    }

    private static void rodape() {
        linha("");
        linha("===============================================================================");
        linha(" FIM DA DEMONSTRAÇÃO");
        linha(" Estruturas exercitadas: ArrayList + HashMap (repositório genérico),");
        linha(" HashMap secundário por QR Code, TreeMap (agenda cronológica e perguntas),");
        linha(" HashSet (unicidade de presença, inscrição e certificado),");
        linha(" Queue/LinkedList (fila de sincronização offline), EnumMap (apuração do");
        linha(" teste de perfil) e Comparator (ranking e ordenações).");
        linha("===============================================================================");
    }

    private static void secao(int numero, String titulo) {
        linha("");
        linha("-------------------------------------------------------------------------------");
        linha(String.format("%02d) %s", numero, titulo));
        linha("-------------------------------------------------------------------------------");
    }

    private static void linha(String texto) {
        System.out.println(texto);
    }

    private static void item(String texto) {
        System.out.println("   - " + texto);
    }
}
