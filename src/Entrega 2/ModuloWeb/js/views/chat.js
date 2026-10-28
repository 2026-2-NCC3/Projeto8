/**
 * Dúvidas dos alunos.
 *
 * O aluno pergunta pelo app e a equipe responde aqui. A resposta também vira
 * notificação, então quem perguntou fica sabendo mesmo sem abrir o chat.
 */

import { api } from '../api.js';
import { el, limpar, carregando, erro, vazio, avisar, dataHoraBR, plural } from '../ui.js';

export default function chat(container, alunoIdAtual = null) {
  const colunaConversas = el('section', { classe: 'painel chat__conversas' }, [carregando()]);
  const colunaAberta = el('section', { classe: 'painel conversa' });

  container.append(
    el('header', { classe: 'topo' }, [
      el('div', {}, [
        el('h1', { texto: 'Dúvidas dos alunos' }),
        el('p', { texto: 'Perguntas que chegam pelo aplicativo. A resposta vai junto com uma notificação para o aluno.' }),
      ]),
    ]),
    el('div', { classe: 'chat' }, [colunaConversas, colunaAberta]),
  );

  async function carregarConversas() {
    limpar(colunaConversas).append(carregando());
    try {
      const conversas = await api.conversas();

      if (conversas.length === 0) {
        limpar(colunaConversas).append(
          vazio('Nenhuma conversa', 'Quando um aluno mandar uma dúvida pelo app, ela aparece aqui.'),
        );
        limpar(colunaAberta).append(vazio('Nada para responder', 'A caixa está vazia por enquanto.'));
        return;
      }

      limpar(colunaConversas).append(
        el('div', { classe: 'painel__cabeca' }, [
          el('div', {}, [
            el('h2', { texto: 'Conversas' }),
            el('p', { texto: plural(conversas.length, 'aluno escreveu', 'alunos escreveram') }),
          ]),
        ]),
        el('ul', { classe: 'lista lista--clicavel' }, conversas.map((c) => el('li', {
          'aria-current': Number(c.aluno_id) === Number(alunoIdAtual) ? 'true' : null,
          onclick: () => { window.location.hash = `#/chat/${c.aluno_id}`; },
        }, [
          el('div', { classe: 'lista__principal' }, [
            el('p', { classe: 'lista__titulo' }, [
              el('a', {
                href: `#/chat/${c.aluno_id}`,
                texto: c.aluno,
                // o <li> inteiro já navega; o link existe para o teclado
                onclick: (ev) => ev.stopPropagation(),
              }),
            ]),
            el('p', { classe: 'chat__trecho', texto: c.ultima_mensagem || 'Sem mensagens' }),
            el('p', { classe: 'lista__meta', texto: dataHoraBR(c.ultima_em) }),
          ]),
        ]))),
      );

      // Sem aluno na URL, abre a conversa mais recente — a equipe cai direto
      // no que está esperando resposta.
      if (!alunoIdAtual) {
        window.location.hash = `#/chat/${conversas[0].aluno_id}`;
        return;
      }
      abrirConversa(alunoIdAtual, conversas.find((c) => Number(c.aluno_id) === Number(alunoIdAtual)));
    } catch (e) {
      if (e.status === 401) return;
      limpar(colunaConversas).append(erro(e.message, carregarConversas));
    }
  }

  async function abrirConversa(alunoId, resumo) {
    limpar(colunaAberta).append(carregando());
    try {
      const mensagens = await api.conversa(alunoId);
      const nome = resumo ? resumo.aluno : 'Conversa';

      const painelMensagens = el('div', { classe: 'conversa__mensagens' },
        mensagens.length === 0
          ? [vazio('Conversa vazia', 'Escreva a primeira mensagem para este aluno.')]
          : mensagens.map((m) => el('div', {
            classe: `balao balao--${m.autor === 'equipe' ? 'equipe' : 'aluno'}`,
          }, [
            el('p', { texto: m.texto }),
            el('time', { datetime: String(m.criado_em).replace(' ', 'T'), texto: dataHoraBR(m.criado_em) }),
          ])));

      const campo = el('textarea', {
        id: 'resposta', name: 'texto', required: true, maxlength: 600,
        placeholder: 'Escreva a resposta…',
      });

      const botao = el('button', { classe: 'botao botao--principal', type: 'submit', texto: 'Responder' });

      const form = el('form', { classe: 'conversa__responder' }, [
        el('div', { classe: 'campo' }, [
          el('label', { for: 'resposta', texto: `Responder para ${nome}` }),
          campo,
        ]),
        botao,
      ]);

      form.addEventListener('submit', async (ev) => {
        ev.preventDefault();
        const texto = campo.value.trim();
        if (!texto) { campo.focus(); return; }

        botao.disabled = true;
        botao.textContent = 'Enviando…';
        try {
          await api.responder(alunoId, texto);
          campo.value = '';
          avisar('Resposta enviada e notificada no app.');
          await abrirConversa(alunoId, resumo);
          carregarConversas();
        } catch (e) {
          avisar(e.message, { tipo: 'erro' });
        } finally {
          botao.disabled = false;
          botao.textContent = 'Responder';
        }
      });

      limpar(colunaAberta).append(
        el('div', { classe: 'painel__cabeca' }, [
          el('div', {}, [
            el('h2', { texto: nome }),
            el('p', { texto: plural(mensagens.length, 'mensagem na conversa', 'mensagens na conversa') }),
          ]),
          el('a', { classe: 'botao botao--contorno botao--pequeno', href: `#/alunos/${alunoId}`, texto: 'Ver ficha' }),
        ]),
        painelMensagens,
        form,
      );

      // A conversa abre no fim, como em qualquer aplicativo de mensagem.
      painelMensagens.scrollTop = painelMensagens.scrollHeight;
    } catch (e) {
      if (e.status === 401) return;
      limpar(colunaAberta).append(erro(e.message, () => abrirConversa(alunoId, resumo)));
    }
  }

  carregarConversas();
}
