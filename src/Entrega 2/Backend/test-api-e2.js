/**
 * Teste de fumaça da Entrega 2 — cobre o que foi acrescentado nesta etapa:
 * sincronização (download e upload), check-in por QR Code com fila offline,
 * chat persistido e o módulo web administrativo.
 *
 * Uso:
 *   node server.js &        (em um terminal)
 *   node test-api-e2.js     (em outro)
 */
const BASE = process.env.API_URL || 'http://localhost:3000/api';

let falhas = 0;
let total = 0;
function ok(cond, msg) {
  total++;
  if (cond) console.log(`  OK      ${msg}`);
  else { console.log(`  FALHOU  ${msg}`); falhas++; }
}

const json = (r) => r.json();
const post = (url, body, headers = {}) => fetch(url, {
  method: 'POST',
  headers: { 'Content-Type': 'application/json', ...headers },
  body: JSON.stringify(body),
});

async function main() {
  console.log('Testando API (Entrega 2) em', BASE, '\n');

  // --- Saúde e versão ------------------------------------------------------
  const health = await fetch(`${BASE}/health`).then(json);
  ok(health.status === 'ok', 'GET /health responde ok');
  ok(health.versao === '2.0', `GET /health informa versão 2.0 (veio "${health.versao}")`);

  // --- Login do aluno ------------------------------------------------------
  const login = await post(`${BASE}/auth/login`, {
    email: 'ana.souza@aluno.proximaetapa.org.br', senha: '123456',
  }).then(json);
  ok(!!login.token, 'POST /auth/login (aluno) retorna token');
  const auth = { Authorization: `Bearer ${login.token}` };

  // --- Sincronização: download completo ------------------------------------
  const syncCompleto = await fetch(`${BASE}/sync`, { headers: auth }).then(json);
  ok(syncCompleto.completo === true, 'GET /sync sem "desde" devolve pacote completo');
  ok(!!syncCompleto.servidor_em, 'GET /sync devolve servidor_em para a próxima sincronização');
  ok(!!syncCompleto.aluno && syncCompleto.aluno.id > 0, 'GET /sync inclui os dados do aluno');
  ok(Array.isArray(syncCompleto.cursos) && syncCompleto.cursos.length > 0,
    `GET /sync inclui cursos (${syncCompleto.cursos.length})`);
  ok(Array.isArray(syncCompleto.agenda), `GET /sync inclui agenda (${syncCompleto.agenda.length} encontros)`);
  ok(Array.isArray(syncCompleto.notificacoes), 'GET /sync inclui notificações');

  // --- Sincronização incremental -------------------------------------------
  const agoraServidor = syncCompleto.servidor_em;
  const syncDelta = await fetch(`${BASE}/sync?desde=${encodeURIComponent(agoraServidor)}`, { headers: auth }).then(json);
  ok(syncDelta.completo === false, 'GET /sync?desde=... entra em modo incremental');
  ok(syncDelta.cursos.length <= syncCompleto.cursos.length,
    `delta devolve menos ou igual ao completo (${syncDelta.cursos.length} <= ${syncCompleto.cursos.length})`);

  // --- Login do administrador ----------------------------------------------
  const loginAdmin = await post(`${BASE}/admin/login`, {
    email: 'admin@proximaetapa.org.br', senha: '123456',
  }).then(json);
  ok(!!loginAdmin.token, 'POST /admin/login retorna token de administrador');
  const authAdmin = { Authorization: `Bearer ${loginAdmin.token}` };

  const adminComTokenDeAluno = await fetch(`${BASE}/admin/metricas`, { headers: auth });
  ok(adminComTokenDeAluno.status === 403, 'token de aluno NÃO abre rota de administrador (403)');

  const alunoComTokenDeAdmin = await fetch(`${BASE}/cursos`, { headers: authAdmin });
  ok(alunoComTokenDeAdmin.status === 403, 'token de administrador NÃO abre rota de aluno (403)');

  // --- Painel administrativo ------------------------------------------------
  const metricas = await fetch(`${BASE}/admin/metricas`, { headers: authAdmin }).then(json);
  ok(metricas.totalAlunos > 0, `GET /admin/metricas traz total de alunos (${metricas.totalAlunos})`);
  ok(typeof metricas.frequenciaMedia === 'number', `frequência média calculada (${metricas.frequenciaMedia}%)`);

  // Criar curso pelo painel e conferir que ele aparece para o aluno.
  const novoCurso = await post(`${BASE}/admin/cursos`, {
    titulo: 'Curso de Teste Automatizado',
    descricao: 'Criado pelo teste de fumaça da Entrega 2.',
    categoria: 'Tecnologia',
    local: 'Online',
    carga_horaria: 10,
    data_inicio: '2026-12-01',
    data_fim: '2026-12-20',
    vagas_totais: 25,
  }, authAdmin).then(json);
  ok(novoCurso.id > 0, 'POST /admin/cursos cria curso');

  const cursosDepois = await fetch(`${BASE}/cursos`, { headers: auth }).then(json);
  ok(cursosDepois.some((c) => c.id === novoCurso.id),
    'curso criado no painel aparece na lista do aluno');

  // Criar encontro gera o QR Code do check-in.
  const novaAtividade = await post(`${BASE}/admin/cursos/${novoCurso.id}/atividades`, {
    titulo: 'Encontro de Teste',
    data: '2026-12-05',
    hora_inicio: '19:00',
    hora_fim: '21:00',
    local: 'Online',
  }, authAdmin).then(json);
  ok(!!novaAtividade.qr_code && novaAtividade.qr_code.startsWith('PE-'),
    `POST /admin/cursos/:id/atividades gera QR Code (${novaAtividade.qr_code})`);

  // --- Check-in e fila de sincronização offline ----------------------------
  // O aluno ainda não está inscrito nesse curso: o check-in tem que ser negado.
  const checkinSemInscricao = await post(`${BASE}/sync/presencas`, {
    presencas: [{ uuid_local: 'teste-sem-inscricao', qr_code: novaAtividade.qr_code, registrado_em: '2026-12-05 19:05:00' }],
  }, auth).then(json);
  ok(checkinSemInscricao.resultados[0].status === 'nao_inscrito',
    'fila rejeita check-in de curso em que o aluno não está inscrito');

  // Inscreve e repete.
  await post(`${BASE}/cursos/${novoCurso.id}/inscricao`, {}, auth);
  const checkin = await post(`${BASE}/sync/presencas`, {
    presencas: [{ uuid_local: 'teste-checkin-1', qr_code: novaAtividade.qr_code, registrado_em: '2026-12-05 19:05:00' }],
  }, auth).then(json);
  ok(checkin.sincronizadas === 1, 'fila sincroniza check-in válido');

  // Reenvio do mesmo item (o app não recebeu a resposta e tentou de novo):
  // tem que ser reconhecido como duplicado, sem criar presença repetida.
  const reenvio = await post(`${BASE}/sync/presencas`, {
    presencas: [{ uuid_local: 'teste-checkin-1', qr_code: novaAtividade.qr_code, registrado_em: '2026-12-05 19:05:00' }],
  }, auth).then(json);
  ok(reenvio.resultados[0].status === 'duplicado',
    'reenvio do mesmo check-in é idempotente (status duplicado)');

  // O horário salvo tem que ser o do check-in no celular, não o da sincronização.
  const historico = await fetch(`${BASE}/presencas/historico`, { headers: auth }).then(json);
  const registrada = historico.find((h) => h.atividade === 'Encontro de Teste');
  ok(!!registrada, 'presença sincronizada aparece no histórico');
  ok(registrada && String(registrada.registrado_em).startsWith('2026-12-05 19:05'),
    `horário original do check-in foi preservado (${registrada && registrada.registrado_em})`);

  // Lote com um item válido e um inválido: o inválido não derruba o resto.
  const atividade2 = await post(`${BASE}/admin/cursos/${novoCurso.id}/atividades`, {
    titulo: 'Encontro de Teste 2', data: '2026-12-12', hora_inicio: '19:00',
  }, authAdmin).then(json);
  const lote = await post(`${BASE}/sync/presencas`, {
    presencas: [
      { uuid_local: 'lote-ok', qr_code: atividade2.qr_code, registrado_em: '2026-12-12 19:02:00' },
      { uuid_local: 'lote-invalido', qr_code: 'QR-QUE-NAO-EXISTE', registrado_em: '2026-12-12 19:03:00' },
    ],
  }, auth).then(json);
  ok(lote.sincronizadas === 1 && lote.resultados.length === 2,
    'lote processa item a item: 1 sincronizado, 1 recusado');
  ok(lote.resultados.find((r) => r.uuid_local === 'lote-invalido').status === 'qr_invalido',
    'QR inexistente é marcado como qr_invalido sem travar o lote');

  // --- Lista de presença no painel -----------------------------------------
  const listaPresenca = await fetch(`${BASE}/admin/atividades/${novaAtividade.id}/presencas`, { headers: authAdmin }).then(json);
  ok(listaPresenca.presentes >= 1, `GET /admin/atividades/:id/presencas mostra presentes (${listaPresenca.presentes}/${listaPresenca.total})`);

  // --- Comunicado vira notificação no app ----------------------------------
  const antesNotif = await fetch(`${BASE}/notificacoes`, { headers: auth }).then(json);
  const comunicado = await post(`${BASE}/admin/mensagens`, {
    titulo: 'Comunicado de Teste', corpo: 'Mensagem enviada pelo teste automatizado.', destinatario_tipo: 'todos',
  }, authAdmin).then(json);
  ok(comunicado.notificados > 0, `POST /admin/mensagens notifica alunos (${comunicado.notificados})`);
  const depoisNotif = await fetch(`${BASE}/notificacoes`, { headers: auth }).then(json);
  ok(depoisNotif.length > antesNotif.length, 'comunicado do painel chega como notificação do aluno');

  // --- Chat persistido ------------------------------------------------------
  const chat = await post(`${BASE}/chat`, { texto: 'Mensagem de teste do aluno' }, auth).then(json);
  ok(Array.isArray(chat) && chat.some((m) => m.texto === 'Mensagem de teste do aluno'),
    'POST /chat grava a mensagem do aluno');
  const chatAdmin = await fetch(`${BASE}/admin/chat`, { headers: authAdmin }).then(json);
  ok(chatAdmin.length > 0, `conversa aparece no painel da equipe (${chatAdmin.length} conversas)`);

  const respostaEquipe = await post(`${BASE}/admin/chat/${login.aluno.id}`, {
    texto: 'Resposta da equipe pelo painel.',
  }, authAdmin);
  ok(respostaEquipe.status === 201, 'equipe responde pelo painel');
  const chatDepois = await fetch(`${BASE}/chat`, { headers: auth }).then(json);
  ok(chatDepois.some((m) => m.autor === 'equipe' && m.texto === 'Resposta da equipe pelo painel.'),
    'resposta da equipe chega no chat do aluno');

  // --- Certificados por critério de frequência -----------------------------
  const elegiveis = await fetch(`${BASE}/admin/certificados/elegiveis?frequencia_minima=50`, { headers: authAdmin }).then(json);
  ok(Array.isArray(elegiveis.elegiveis), `GET /admin/certificados/elegiveis lista candidatos (${elegiveis.total})`);

  // --- Encerramento ---------------------------------------------------------
  console.log(`\n${total - falhas}/${total} verificações passaram.`);
  if (falhas > 0) {
    console.log(`${falhas} falha(s).`);
    process.exit(1);
  }
}

main().catch((e) => {
  console.error('Erro ao executar os testes:', e);
  process.exit(1);
});
