/**
 * Ponto de entrada do painel: login, roteamento por hash e montagem das telas.
 *
 * É uma SPA sem framework. O endereço guarda o estado (#/cursos/3), então
 * recarregar a página ou mandar o link para outra pessoa da equipe leva ao
 * mesmo lugar — inclusive para a tela de projeção do QR Code, que costuma ser
 * aberta direto no computador ligado ao projetor.
 */

import { api, sessao, quandoExpirar, invalidarCursos } from './api.js';
import { el, limpar, avisar } from './ui.js';

import painel from './views/painel.js';
import { listaCursos, detalheCurso } from './views/cursos.js';
import { projetarQrCode, chamada } from './views/encontros.js';
import { listaAlunos, fichaAluno } from './views/alunos.js';
import comunicados from './views/comunicados.js';
import certificados from './views/certificados.js';
import chat from './views/chat.js';

const telaLogin = document.getElementById('tela-login');
const app = document.getElementById('app');
const conteudo = document.getElementById('conteudo');
const projecao = document.getElementById('tela-projecao');

// ── Login ─────────────────────────────────────────────────────────────────

const formLogin = document.getElementById('form-login');
const erroLogin = document.getElementById('login-erro');
const botaoEntrar = document.getElementById('login-enviar');

document.getElementById('login-preencher').addEventListener('click', () => {
  document.getElementById('login-email').value = 'admin@proximaetapa.org.br';
  document.getElementById('login-senha').value = '123456';
  botaoEntrar.focus();
});

formLogin.addEventListener('submit', async (ev) => {
  ev.preventDefault();
  erroLogin.hidden = true;

  const email = document.getElementById('login-email').value.trim();
  const senha = document.getElementById('login-senha').value;

  if (!email || !senha) {
    erroLogin.textContent = 'Informe o e-mail e a senha para entrar.';
    erroLogin.hidden = false;
    return;
  }

  botaoEntrar.disabled = true;
  botaoEntrar.textContent = 'Entrando…';
  try {
    const r = await api.entrar(email, senha);
    sessao.abrir(r.token, r.admin);
    invalidarCursos();
    mostrarPainel();
    if (!window.location.hash.startsWith('#/')) window.location.hash = '#/painel';
    else rotear();
  } catch (e) {
    erroLogin.textContent = e.message;
    erroLogin.hidden = false;
  } finally {
    botaoEntrar.disabled = false;
    botaoEntrar.textContent = 'Entrar no painel';
  }
});

document.getElementById('botao-sair').addEventListener('click', () => {
  sessao.encerrar();
  invalidarCursos();
  mostrarLogin();
});

// A sessão vence depois de um dia; o aviso explica por que a tela voltou.
quandoExpirar(() => {
  mostrarLogin();
  avisar('Sua sessão expirou. Entre novamente para continuar.', { tipo: 'erro' });
});

function mostrarLogin() {
  app.hidden = true;
  projecao.hidden = true;
  telaLogin.hidden = false;
  document.getElementById('login-email').focus();
}

function mostrarPainel() {
  telaLogin.hidden = true;
  app.hidden = false;
  const admin = sessao.admin;
  const rodape = document.getElementById('rail-admin');
  limpar(rodape).append(
    el('strong', { texto: admin ? admin.nome : 'Equipe' }),
    el('span', { texto: admin ? admin.email : '' }),
  );
}

// ── Roteamento ────────────────────────────────────────────────────────────

/**
 * Cada rota é um padrão com partes fixas e partes variáveis (":id").
 * `secao` diz qual item do menu fica marcado; `telaCheia` tira a navegação
 * do caminho, usado só pela projeção do QR Code.
 */
const ROTAS = [
  { padrao: ['painel'], secao: 'painel', montar: (c) => painel(c) },
  { padrao: ['cursos'], secao: 'cursos', montar: (c) => listaCursos(c) },
  { padrao: ['cursos', ':id'], secao: 'cursos', montar: (c, p) => detalheCurso(c, p.id) },
  { padrao: ['encontros', ':id', 'chamada'], secao: 'cursos', montar: (c, p) => chamada(c, p.id) },
  { padrao: ['encontros', ':id', 'qrcode'], secao: 'cursos', telaCheia: true },
  { padrao: ['alunos'], secao: 'alunos', montar: (c) => listaAlunos(c) },
  { padrao: ['alunos', ':id'], secao: 'alunos', montar: (c, p) => fichaAluno(c, p.id) },
  { padrao: ['comunicados'], secao: 'comunicados', montar: (c) => comunicados(c) },
  { padrao: ['certificados'], secao: 'certificados', montar: (c) => certificados(c) },
  { padrao: ['chat'], secao: 'chat', montar: (c) => chat(c, null) },
  { padrao: ['chat', ':id'], secao: 'chat', montar: (c, p) => chat(c, p.id) },
];

function casar(partes) {
  for (const rota of ROTAS) {
    if (rota.padrao.length !== partes.length) continue;
    const parametros = {};
    const bate = rota.padrao.every((esperado, i) => {
      if (esperado.startsWith(':')) { parametros[esperado.slice(1)] = decodeURIComponent(partes[i]); return true; }
      return esperado === partes[i];
    });
    if (bate) return { rota, parametros };
  }
  return null;
}

function marcarMenu(secao) {
  document.querySelectorAll('.rail__lista a').forEach((a) => {
    if (a.dataset.secao === secao) a.setAttribute('aria-current', 'page');
    else a.removeAttribute('aria-current');
  });
}

let telaAnterior = null;

function rotear() {
  if (!sessao.token) { mostrarLogin(); return; }
  mostrarPainel();

  const partes = window.location.hash.replace(/^#\/?/, '').split('/').filter(Boolean);
  if (partes.length === 0) { window.location.hash = '#/painel'; return; }

  const achado = casar(partes);

  if (!achado) {
    projecao.hidden = true;
    limpar(conteudo).append(
      el('div', { classe: 'estado' }, [
        el('h3', { texto: 'Tela não encontrada' }),
        el('p', { texto: 'O endereço que você abriu não existe no painel.' }),
        el('a', { classe: 'botao botao--principal', href: '#/painel', texto: 'Ir para o painel' }),
      ]),
    );
    marcarMenu(null);
    return;
  }

  const { rota, parametros } = achado;
  marcarMenu(rota.secao);

  // Projeção do QR Code: ocupa a tela inteira, sem o trilho de navegação.
  if (rota.telaCheia) {
    app.hidden = true;
    projecao.hidden = false;
    projetarQrCode(projecao, parametros.id, () => {
      // Volta para o curso do encontro, que é de onde a equipe veio.
      window.history.back();
    });
    telaAnterior = 'qrcode';
    return;
  }

  projecao.hidden = true;
  app.hidden = false;
  limpar(conteudo);
  rota.montar(conteudo, parametros);

  // Rola para o topo e devolve o foco ao conteúdo a cada troca de tela, para
  // quem navega por teclado ou leitor de tela não continuar no menu.
  window.scrollTo(0, 0);
  if (telaAnterior !== null) conteudo.focus({ preventScroll: true });
  telaAnterior = rota.secao;
}

window.addEventListener('hashchange', rotear);

// ── Início ────────────────────────────────────────────────────────────────

if (sessao.token) {
  if (!window.location.hash.startsWith('#/')) window.location.hash = '#/painel';
  rotear();
} else {
  mostrarLogin();
}
