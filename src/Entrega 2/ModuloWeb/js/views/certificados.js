/**
 * Certificados.
 *
 * A API só devolve quem cumpriu o critério de frequência e ainda não tem o
 * documento, então esta tela é uma fila de trabalho: confere e emite. O
 * filtro de frequência mínima existe porque a régua muda conforme o edital
 * do financiador — às vezes 75%, às vezes 80%.
 */

import { api } from '../api.js';
import { el, comEstados, vazio, avisar, confirmar, plural } from '../ui.js';

export default function certificados(container) {
  let minimo = 75;
  const area = el('div');

  const entrada = el('input', {
    id: 'freq-minima', name: 'frequencia_minima', type: 'number',
    min: '0', max: '100', step: '5', value: String(minimo),
  });

  const form = el('form', { classe: 'busca' }, [
    el('div', { classe: 'campo', style: 'flex:0 1 220px' }, [
      el('label', { for: 'freq-minima', texto: 'Frequência mínima (%)' }),
      entrada,
    ]),
    el('button', { classe: 'botao botao--principal', type: 'submit', texto: 'Aplicar critério' }),
  ]);

  form.addEventListener('submit', (ev) => {
    ev.preventDefault();
    const valor = Number(entrada.value);
    minimo = Number.isFinite(valor) ? Math.min(100, Math.max(0, valor)) : 75;
    entrada.value = String(minimo);
    desenhar();
  });

  container.append(
    el('header', { classe: 'topo' }, [
      el('div', {}, [
        el('h1', { texto: 'Certificados' }),
        el('p', { texto: 'Alunos que já cumpriram a frequência exigida e ainda não receberam o documento. Ao emitir, o certificado aparece no app na mesma hora.' }),
      ]),
    ]),
    form,
    area,
  );

  async function emitir(item, linha) {
    const ok = await confirmar({
      titulo: 'Emitir certificado?',
      texto: `${item.aluno} concluiu "${item.curso}" com ${item.frequencia}% de frequência e ${item.carga_horaria} horas. O certificado fica disponível no app imediatamente.`,
      confirmar: 'Emitir certificado',
      perigo: false,
    });
    if (!ok) return;
    try {
      const r = await api.emitirCertificado(item.aluno_id, item.curso_id);
      avisar(`Certificado de ${item.aluno} emitido.`, { detalhe: r.certificado_url, segundos: 8 });
      linha.remove(); // sai da fila: já não é mais elegível
    } catch (e) {
      avisar(e.message, { tipo: 'erro' });
    }
  }

  function desenhar() {
    comEstados(area, () => api.elegiveis(minimo), (dados) => {
      if (dados.total === 0) {
        return el('section', { classe: 'painel' }, [
          vazio(
            'Ninguém elegível com esse critério',
            `Nenhum aluno chegou a ${dados.frequencia_minima}% de frequência em um curso que emite certificado — ou todos os que chegaram já receberam o deles. Experimente baixar a exigência.`,
          ),
        ]);
      }

      const lista = el('ul', { classe: 'lista' });
      dados.elegiveis.forEach((item) => {
        const linha = el('li', {}, [
          el('div', { classe: 'lista__principal' }, [
            el('p', { classe: 'lista__titulo' }, [
              el('a', { href: `#/alunos/${item.aluno_id}`, texto: item.aluno }),
            ]),
            el('p', {
              classe: 'lista__meta',
              texto: `${item.curso}, ${item.carga_horaria} h, presente em ${item.presencas} de ${plural(item.encontros, 'encontro', 'encontros')}`,
            }),
          ]),
          el('span', { classe: 'etiqueta', texto: `${item.frequencia}% de frequência` }),
          el('button', {
            classe: 'botao botao--principal botao--pequeno',
            texto: 'Emitir certificado',
            onclick: () => emitir(item, linha),
          }),
        ]);
        lista.append(linha);
      });

      return el('section', { classe: 'painel' }, [
        el('div', { classe: 'painel__cabeca' }, [
          el('div', {}, [
            el('h2', { texto: 'Prontos para receber' }),
            el('p', { texto: `${plural(dados.total, 'aluno alcançou', 'alunos alcançaram')} ${dados.frequencia_minima}% ou mais` }),
          ]),
        ]),
        lista,
      ]);
    });
  }

  desenhar();
}
