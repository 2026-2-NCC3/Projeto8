/**
 * Duas telas que acontecem dentro da sala de aula:
 *
 *  • Projeção do QR Code — vai para o telão no começo do encontro e os alunos
 *    apontam a câmera do app. Por isso ocupa o monitor inteiro, sem navegação,
 *    e tem versão para impressão (nem toda sala tem projetor).
 *  • Chamada — quem veio e quem faltou, com o percentual da turma.
 */

import { api, tituloDoCurso } from '../api.js';
import {
  el, limpar, comEstados, carregando, erro, vazio, barra,
  dataBR, dataHoraBR, plural,
} from '../ui.js';

const META = 75;

// ── Projeção do QR Code ───────────────────────────────────────────────────

export async function projetarQrCode(palco, atividadeId, aoSair) {
  limpar(palco).append(carregando('Gerando o QR Code…'));

  try {
    // A lista de presença entrega os dados do encontro; o SVG vem em seguida.
    // O SVG exige o cabeçalho Authorization, então é buscado por fetch e
    // injetado no DOM — um <img src> não conseguiria mandar o token.
    const dados = await api.presencas(atividadeId);
    const [svg, curso] = await Promise.all([
      api.qrCodeSvg(atividadeId),
      tituloDoCurso(dados.atividade.curso_id),
    ]);

    const a = dados.atividade;
    const placa = el('div', { classe: 'projecao__placa' });
    placa.innerHTML = svg; // SVG gerado pelo próprio backend

    limpar(palco).append(
      el('p', { classe: 'projecao__encontro', texto: curso }),
      el('h1', { classe: 'projecao__curso', texto: a.titulo }),
      placa,
      el('p', { classe: 'projecao__instrucao', texto: 'Abra o app Próxima Etapa, toque em “Registrar presença” e aponte a câmera para o código.' }),
      el('p', { classe: 'projecao__codigo', texto: a.qr_code }),
      el('p', {
        classe: 'projecao__encontro',
        texto: `${dataBR(a.data)}, das ${String(a.hora_inicio).slice(0, 5)} às ${String(a.hora_fim || a.hora_inicio).slice(0, 5)}, em ${a.local || 'local a definir'}`,
      }),
      el('div', { classe: 'projecao__acoes' }, [
        el('button', { classe: 'botao botao--contorno', texto: 'Imprimir', onclick: () => window.print() }),
        el('a', { classe: 'botao botao--contorno', href: `#/encontros/${atividadeId}/chamada`, texto: 'Ver chamada' }),
        el('button', { classe: 'botao botao--contorno', texto: 'Fechar', onclick: aoSair }),
      ]),
    );
  } catch (e) {
    if (e.status === 401) return;
    limpar(palco).append(
      erro(e.message, () => projetarQrCode(palco, atividadeId, aoSair)),
      el('div', { classe: 'projecao__acoes' }, [
        el('button', { classe: 'botao botao--contorno', texto: 'Fechar', onclick: aoSair }),
      ]),
    );
  }
}

// ── Chamada ───────────────────────────────────────────────────────────────

export function chamada(container, atividadeId) {
  const area = el('div');
  container.append(area);

  async function buscar() {
    const dados = await api.presencas(atividadeId);
    const curso = await tituloDoCurso(dados.atividade.curso_id);
    return { ...dados, curso };
  }

  function desenhar(d) {
    const a = d.atividade;
    const pct = d.total > 0 ? Math.round((d.presentes / d.total) * 100) : 0;
    const presentes = d.lista.filter((p) => p.presente);
    const ausentes = d.lista.filter((p) => !p.presente);

    const cabecalho = el('header', { classe: 'topo' }, [
      el('div', {}, [
        el('h1', { texto: a.titulo }),
        el('p', {
          texto: `${d.curso} — ${dataBR(a.data)}, às ${String(a.hora_inicio).slice(0, 5)}, em ${a.local || 'local a definir'}`,
        }),
      ]),
      el('div', { classe: 'topo__acoes' }, [
        el('a', { classe: 'botao botao--contorno', href: `#/cursos/${a.curso_id}`, texto: 'Ver o curso' }),
        el('a', { classe: 'botao botao--principal', href: `#/encontros/${a.id}/qrcode`, texto: 'Projetar QR Code' }),
      ]),
    ]);

    const resumo = el('section', { classe: 'chamada-resumo' }, [
      el('div', {}, [
        el('p', { classe: 'chamada-resumo__figura', html: `${d.presentes}<small> de ${d.total}</small>` }),
        el('p', { classe: 'chamada-resumo__rotulo', texto: 'alunos presentes neste encontro' }),
      ]),
      el('div', { classe: 'chamada-resumo__barra' }, [
        barra(d.presentes, d.total, { cheiaEmAmbar: false }),
        el('p', { classe: 'chamada-resumo__rotulo', texto: `${pct}% da turma compareceu${pct < META ? `, abaixo dos ${META}% exigidos para o certificado` : ''}` }),
      ]),
    ]);

    if (d.total === 0) {
      return el('div', {}, [cabecalho, el('section', { classe: 'painel' }, [
        vazio('Ninguém inscrito neste curso ainda', 'Quando os alunos se inscreverem pelo app, eles aparecem aqui para a chamada.'),
      ])]);
    }

    return el('div', {}, [
      cabecalho,
      resumo,
      el('div', { classe: 'grade-dupla' }, [
        bloco('Presentes', presentes, true),
        bloco('Ausentes', ausentes, false),
      ]),
    ]);
  }

  function bloco(titulo, pessoas, presente) {
    const corpo = pessoas.length === 0
      ? vazio(
        presente ? 'Ninguém registrou presença ainda' : 'Nenhuma falta',
        presente
          ? 'Projete o QR Code para a turma fazer o check-in pelo app.'
          : 'Todo mundo que está inscrito compareceu.',
      )
      : el('ul', { classe: 'lista' }, pessoas.map((p) => el('li', { classe: presente ? '' : 'ausente' }, [
        el('div', { classe: 'presenca' }, [
          el('span', {
            classe: `presenca__marca presenca__marca--${presente ? 'sim' : 'nao'}`,
            'aria-hidden': 'true',
            texto: presente ? '✓' : '–',
          }),
          el('div', { classe: 'lista__principal' }, [
            el('p', { classe: 'lista__titulo', texto: p.nome }),
            el('p', {
              classe: 'lista__meta',
              texto: presente ? `Check-in em ${dataHoraBR(p.registrado_em)}` : p.email,
            }),
          ]),
        ]),
        el('a', { classe: 'botao botao--contorno botao--pequeno', href: `#/alunos/${p.id}`, texto: 'Ficha' }),
      ])));

    return el('section', { classe: 'painel' }, [
      el('div', { classe: 'painel__cabeca' }, [
        el('div', {}, [
          el('h2', { texto: titulo }),
          el('p', { texto: plural(pessoas.length, 'aluno', 'alunos') }),
        ]),
      ]),
      corpo,
    ]);
  }

  comEstados(area, buscar, desenhar);
}
