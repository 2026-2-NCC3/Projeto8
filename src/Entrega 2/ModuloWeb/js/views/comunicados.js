/**
 * Comunicados: o que a equipe escreve aqui vira notificação na caixa de
 * entrada do app, na hora.
 *
 * O histórico mostra alcance e leitura porque "mandei para todo mundo" e
 * "todo mundo leu" são coisas diferentes — e é a taxa de leitura que diz se
 * o recado precisa ser repetido no encontro.
 */

import { api } from '../api.js';
import { el, comEstados, vazio, avisar, barra, dataHoraBR, plural } from '../ui.js';
import { opcoesDeCurso } from './cursos.js';

export default async function comunicados(container) {
  const historico = el('div');

  container.append(
    el('header', { classe: 'topo' }, [
      el('div', {}, [
        el('h1', { texto: 'Comunicados' }),
        el('p', { texto: 'Avisos da equipe que chegam como notificação no aplicativo dos alunos.' }),
      ]),
    ]),
  );

  // Seletor de curso: carregado antes do formulário para já vir preenchido.
  let cursos = [];
  try {
    cursos = await opcoesDeCurso();
  } catch {
    cursos = [];
  }

  const titulo = el('input', { id: 'com-titulo', name: 'titulo', type: 'text', required: true, maxlength: 120 });
  const corpo = el('textarea', { id: 'com-corpo', name: 'corpo', required: true, maxlength: 600 });

  const seletor = el('select', { id: 'com-curso', name: 'curso_id', disabled: true },
    cursos.length
      ? cursos.map((c) => el('option', { value: c.valor, texto: c.rotulo }))
      : [el('option', { value: '', texto: 'Nenhum curso ativo' })]);

  const campoCurso = el('div', { classe: 'campo' }, [
    el('label', { for: 'com-curso', texto: 'Curso de destino' }),
    seletor,
  ]);

  const radioTodos = el('input', { type: 'radio', name: 'destinatario_tipo', value: 'todos', id: 'dest-todos', checked: true });
  const radioCurso = el('input', { type: 'radio', name: 'destinatario_tipo', value: 'curso', id: 'dest-curso' });

  const atualizarDestino = () => { seletor.disabled = !radioCurso.checked; };
  radioTodos.addEventListener('change', atualizarDestino);
  radioCurso.addEventListener('change', atualizarDestino);

  const erroEl = el('p', { classe: 'aviso aviso--erro', hidden: true });
  const enviar = el('button', { classe: 'botao botao--principal', type: 'submit', texto: 'Enviar comunicado' });

  const form = el('form', { novalidate: true }, [
    erroEl,
    el('div', { classe: 'campo' }, [el('label', { for: 'com-titulo', texto: 'Título *' }), titulo]),
    el('div', { classe: 'campo' }, [
      el('label', { for: 'com-corpo', texto: 'Mensagem *' }),
      corpo,
      el('p', { classe: 'campo__dica', texto: 'Escreva como você falaria com o aluno. O texto aparece inteiro na notificação.' }),
    ]),
    el('fieldset', { style: 'border:none;padding:0;margin:0 0 16px' }, [
      el('legend', { classe: 'campo__rotulo', style: 'padding:0', texto: 'Quem recebe' }),
      el('div', { classe: 'opcoes' }, [
        el('label', { for: 'dest-todos' }, [radioTodos, el('span', { texto: 'Todos os alunos' })]),
        el('label', { for: 'dest-curso' }, [radioCurso, el('span', { texto: 'Só quem está em um curso' })]),
      ]),
    ]),
    campoCurso,
    enviar,
  ]);

  form.addEventListener('submit', async (ev) => {
    ev.preventDefault();
    erroEl.hidden = true;

    if (!form.checkValidity()) {
      erroEl.textContent = 'Preencha o título e a mensagem.';
      erroEl.hidden = false;
      form.reportValidity();
      return;
    }

    const paraCurso = radioCurso.checked;
    if (paraCurso && !seletor.value) {
      erroEl.textContent = 'Escolha o curso que vai receber o comunicado.';
      erroEl.hidden = false;
      return;
    }

    enviar.disabled = true;
    enviar.textContent = 'Enviando…';
    try {
      const r = await api.enviarMensagem({
        titulo: titulo.value.trim(),
        corpo: corpo.value.trim(),
        destinatario_tipo: paraCurso ? 'curso' : 'todos',
        curso_id: paraCurso ? Number(seletor.value) : null,
      });
      avisar(`Comunicado enviado para ${plural(r.notificados, 'aluno', 'alunos')}.`);
      form.reset();
      atualizarDestino();
      carregarHistorico();
    } catch (e) {
      erroEl.textContent = e.message;
      erroEl.hidden = false;
    } finally {
      enviar.disabled = false;
      enviar.textContent = 'Enviar comunicado';
    }
  });

  function carregarHistorico() {
    comEstados(historico, () => api.mensagens(), (mensagens) => {
      if (mensagens.length === 0) {
        return vazio('Nenhum comunicado enviado', 'O primeiro aviso que você mandar aparece aqui, com quantos alunos receberam e leram.');
      }
      return el('ul', { classe: 'lista' }, mensagens.map((m) => {
        const taxa = m.enviadas > 0 ? Math.round((m.lidas / m.enviadas) * 100) : 0;
        return el('li', {}, [
          el('div', { classe: 'lista__principal' }, [
            el('p', { classe: 'lista__titulo', texto: m.titulo }),
            el('p', { classe: 'lista__meta', texto: m.corpo }),
            el('p', {
              classe: 'lista__meta',
              texto: `${m.destinatario_tipo === 'curso' ? `Só para ${m.curso || 'um curso'}` : 'Para todos os alunos'}, em ${dataHoraBR(m.criado_em)}`,
            }),
          ]),
          el('div', { classe: 'ocupacao' }, [
            el('p', { classe: 'ocupacao__texto' }, [
              el('span', { texto: 'Leram' }),
              el('span', { texto: `${m.lidas} de ${m.enviadas}` }),
            ]),
            barra(m.lidas, m.enviadas, { cheiaEmAmbar: false }),
          ]),
          el('span', {
            classe: `etiqueta ${taxa === 0 ? 'etiqueta--neutra' : ''}`,
            texto: `${taxa}% lido`,
          }),
        ]);
      }));
    });
  }

  container.append(
    el('div', { classe: 'grade-dupla' }, [
      el('section', { classe: 'painel' }, [
        el('div', { classe: 'painel__cabeca' }, [
          el('div', {}, [
            el('h2', { texto: 'Novo comunicado' }),
            el('p', { texto: 'Chega como notificação, na hora' }),
          ]),
        ]),
        el('div', { classe: 'painel__corpo' }, [form]),
      ]),
      el('section', { classe: 'painel' }, [
        el('div', { classe: 'painel__cabeca' }, [
          el('div', {}, [
            el('h2', { texto: 'Enviados' }),
            el('p', { texto: 'Alcance e leitura de cada aviso' }),
          ]),
        ]),
        historico,
      ]),
    ]),
  );

  atualizarDestino();
  carregarHistorico();
}
