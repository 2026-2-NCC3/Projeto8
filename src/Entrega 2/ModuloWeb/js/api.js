/**
 * Conversa com a API do Próxima Etapa.
 *
 * Tudo o que o painel faz passa por aqui, para que exista um único lugar
 * cuidando do token e do que acontece quando ele expira (o token vale 1 dia;
 * quem deixa o painel aberto da manhã para a noite cai num 401 e precisa ser
 * levado de volta ao login em vez de ver uma tela quebrada).
 */

const BASE = '/api';
const CHAVE_TOKEN = 'pe.painel.token';
const CHAVE_ADMIN = 'pe.painel.admin';

/** Erro de API com o status junto, para as telas decidirem o que mostrar. */
export class ErroApi extends Error {
  constructor(mensagem, status) {
    super(mensagem);
    this.status = status;
  }
}

export const sessao = {
  get token() {
    try { return localStorage.getItem(CHAVE_TOKEN); } catch { return null; }
  },
  get admin() {
    try { return JSON.parse(localStorage.getItem(CHAVE_ADMIN) || 'null'); } catch { return null; }
  },
  abrir(token, admin) {
    try {
      localStorage.setItem(CHAVE_TOKEN, token);
      localStorage.setItem(CHAVE_ADMIN, JSON.stringify(admin));
    } catch { /* navegador sem localStorage: a sessão vale só enquanto a aba estiver aberta */ }
  },
  encerrar() {
    try {
      localStorage.removeItem(CHAVE_TOKEN);
      localStorage.removeItem(CHAVE_ADMIN);
    } catch { /* ignora */ }
  },
};

/** Avisado pelo app.js para voltar ao login quando a sessão cai. */
let aoExpirar = () => {};
export function quandoExpirar(callback) { aoExpirar = callback; }

async function requisitar(caminho, opcoes = {}) {
  const cabecalhos = { ...(opcoes.headers || {}) };
  if (opcoes.body !== undefined) cabecalhos['Content-Type'] = 'application/json';
  if (sessao.token) cabecalhos.Authorization = `Bearer ${sessao.token}`;

  let resposta;
  try {
    resposta = await fetch(BASE + caminho, {
      ...opcoes,
      headers: cabecalhos,
      body: opcoes.body !== undefined ? JSON.stringify(opcoes.body) : undefined,
    });
  } catch {
    // Servidor fora do ar, Wi-Fi caiu, cabo solto no laboratório.
    throw new ErroApi('Não foi possível falar com o servidor. Verifique a conexão e tente de novo.', 0);
  }

  // 401 em qualquer rota significa sessão vencida: limpa e devolve ao login.
  if (resposta.status === 401 && !caminho.endsWith('/admin/login')) {
    sessao.encerrar();
    aoExpirar();
    throw new ErroApi('Sua sessão expirou. Entre novamente.', 401);
  }

  if (resposta.status === 204) return null;

  const tipo = resposta.headers.get('content-type') || '';
  if (tipo.includes('image/svg')) {
    const svg = await resposta.text();
    if (!resposta.ok) throw new ErroApi('Não foi possível gerar o QR Code.', resposta.status);
    return svg;
  }

  let dados = null;
  try { dados = await resposta.json(); } catch { dados = null; }

  if (!resposta.ok) {
    throw new ErroApi((dados && dados.erro) || 'Não foi possível concluir a ação.', resposta.status);
  }
  return dados;
}

const get = (caminho) => requisitar(caminho);
const post = (caminho, body) => requisitar(caminho, { method: 'POST', body });
const patch = (caminho, body) => requisitar(caminho, { method: 'PATCH', body });
const remover = (caminho) => requisitar(caminho, { method: 'DELETE' });

export const api = {
  entrar: (email, senha) => post('/admin/login', { email, senha }),

  metricas: () => get('/admin/metricas'),

  cursos: () => get('/admin/cursos'),
  criarCurso: (dados) => post('/admin/cursos', dados),
  atualizarCurso: (id, dados) => patch(`/admin/cursos/${id}`, dados),
  desativarCurso: (id) => remover(`/admin/cursos/${id}`),

  atividades: (cursoId) => get(`/admin/cursos/${cursoId}/atividades`),
  criarAtividade: (cursoId, dados) => post(`/admin/cursos/${cursoId}/atividades`, dados),
  excluirAtividade: (id) => remover(`/admin/atividades/${id}`),
  // O SVG exige o cabeçalho Authorization, então vem por fetch e é injetado no
  // DOM — não dá para usar <img src="...">.
  qrCodeSvg: (atividadeId) => get(`/admin/atividades/${atividadeId}/qrcode.svg`),
  presencas: (atividadeId) => get(`/admin/atividades/${atividadeId}/presencas`),

  alunos: (busca = '') => get(`/admin/alunos${busca ? `?busca=${encodeURIComponent(busca)}` : ''}`),
  aluno: (id) => get(`/admin/alunos/${id}`),

  mensagens: () => get('/admin/mensagens'),
  enviarMensagem: (dados) => post('/admin/mensagens', dados),

  elegiveis: (minimo = 75) => get(`/admin/certificados/elegiveis?frequencia_minima=${minimo}`),
  emitirCertificado: (aluno_id, curso_id) => post('/admin/certificados', { aluno_id, curso_id }),

  conversas: () => get('/admin/chat'),
  conversa: (alunoId) => get(`/admin/chat/${alunoId}`),
  responder: (alunoId, texto) => post(`/admin/chat/${alunoId}`, { texto }),
};

/**
 * Cache simples dos cursos.
 *
 * Várias telas precisam só do título de um curso (a chamada, o QR Code, o
 * seletor de comunicados) e a API não repete esse dado em cada rota. Guardar
 * a lista evita pedir a mesma coisa a cada navegação.
 */
let cursosCache = null;
export async function cursosEmCache({ recarregar = false } = {}) {
  if (!cursosCache || recarregar) cursosCache = await api.cursos();
  return cursosCache;
}
export function invalidarCursos() { cursosCache = null; }

export async function tituloDoCurso(cursoId) {
  const lista = await cursosEmCache();
  const curso = lista.find((c) => Number(c.id) === Number(cursoId));
  return curso ? curso.titulo : 'Curso';
}
