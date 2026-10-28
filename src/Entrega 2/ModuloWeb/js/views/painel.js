/**
 * Painel inicial.
 *
 * A frequência média ganha a tela inteira em cima porque é o indicador que a
 * ONG leva para os financiadores — e porque um número sozinho não diz nada:
 * ao lado dele vai a meta de 75% e uma frase em português dizendo se o mês
 * está dentro ou fora dela.
 */

import { api } from '../api.js';
import { el, comEstados, barra, diaMes, plural, vazio } from '../ui.js';

const META = 75; // mesmo critério usado para liberar certificado

function prestacaoDeContas(m) {
  const abaixo = m.frequenciaMedia < META;
  const diferenca = Math.abs(m.frequenciaMedia - META);

  const leitura = abaixo
    ? `De cada 100 presenças possíveis, ${m.frequenciaMedia} foram registradas. Está ${plural(diferenca, 'ponto', 'pontos')} abaixo da meta de ${META}% — os alunos nessa faixa ainda não garantem o certificado.`
    : `De cada 100 presenças possíveis, ${m.frequenciaMedia} foram registradas. A meta de ${META}% está cumprida, com ${plural(diferenca, 'ponto', 'pontos')} de folga.`;

  return el('section', { classe: `prestacao${abaixo ? ' prestacao--abaixo' : ''}` }, [
    el('div', {}, [
      el('p', { classe: 'prestacao__figura', html: `${m.frequenciaMedia}<span>%</span>` }),
      el('p', { classe: 'prestacao__rotulo', texto: 'Frequência média do programa' }),
    ]),
    el('div', {}, [
      el('p', { classe: 'prestacao__leitura', texto: leitura }),
      el('div', {
        classe: 'trilha',
        role: 'img',
        'aria-label': `Frequência média de ${m.frequenciaMedia}%, meta de ${META}%`,
      }, [
        el('span', { classe: 'trilha__preenchido', style: `width:${Math.min(100, m.frequenciaMedia)}%` }),
        el('span', { classe: 'trilha__meta', style: `left:${META}%` }),
      ]),
      el('p', { classe: 'trilha__legenda' }, [
        el('span', { texto: '0%' }),
        el('strong', { texto: `meta ${META}%` }),
        el('span', { texto: '100%' }),
      ]),
    ]),
  ]);
}

function contadores(m) {
  const itens = [
    ['Alunos cadastrados', m.totalAlunos],
    ['Cursos ativos', m.totalCursos],
    ['Inscrições', m.totalInscricoes],
    ['Presenças registradas', m.totalPresencas],
    ['Certificados emitidos', m.totalCertificados],
  ];
  return el('dl', { classe: 'contadores' },
    itens.map(([rotulo, valor]) => el('div', {}, [
      el('dt', { texto: rotulo }),
      el('dd', { texto: String(valor) }),
    ])));
}

function ocupacao(cursos) {
  const corpo = cursos.length === 0
    ? vazio('Nenhum curso ativo', 'Crie um curso para que ele apareça no app dos alunos.')
    : el('ul', { classe: 'lista' }, cursos.map((c) => el('li', {}, [
      el('div', { classe: 'lista__principal' }, [
        el('p', { classe: 'lista__titulo' }, [
          el('a', { href: `#/cursos/${c.id}`, texto: c.titulo }),
        ]),
        el('p', { classe: 'lista__meta', texto: `${c.ocupadas} de ${c.vagas_totais} vagas preenchidas` }),
      ]),
      el('div', { classe: 'ocupacao' }, [
        barra(c.ocupadas, c.vagas_totais),
      ]),
      // A etiqueta só aparece quando há o que dizer: quantas vagas sobraram já
      // está escrito na linha de apoio, e repetir isso em cada item só
      // empurrava o conteúdo para uma segunda linha.
      c.vagas_disponiveis === 0
        ? el('span', { classe: 'etiqueta etiqueta--alerta', texto: 'Lotado' })
        : null,
    ])));

  return el('section', { classe: 'painel' }, [
    el('div', { classe: 'painel__cabeca' }, [
      el('div', {}, [
        el('h2', { texto: 'Ocupação dos cursos' }),
        el('p', { texto: 'Onde ainda cabe gente e o que já lotou' }),
      ]),
    ]),
    corpo,
  ]);
}

function proximos(encontros) {
  const corpo = encontros.length === 0
    ? vazio('Nenhum encontro marcado', 'Abra um curso e marque o próximo encontro para gerar o QR Code de presença.')
    : el('ul', { classe: 'lista' }, encontros.map((e) => {
      const d = diaMes(e.data);
      return el('li', {}, [
        el('div', { classe: 'data-bloco' }, [
          el('strong', { texto: d.dia }),
          el('span', { texto: d.mes }),
        ]),
        el('div', { classe: 'lista__principal' }, [
          el('p', { classe: 'lista__titulo', texto: e.titulo }),
          el('p', { classe: 'lista__meta', texto: `${e.curso}, às ${String(e.hora_inicio).slice(0, 5)}` }),
        ]),
        el('div', { classe: 'lista__acoes' }, [
          el('a', { classe: 'botao botao--contorno botao--pequeno', href: `#/encontros/${e.id}/qrcode`, texto: 'QR Code' }),
          el('a', { classe: 'botao botao--contorno botao--pequeno', href: `#/encontros/${e.id}/chamada`, texto: 'Chamada' }),
        ]),
      ]);
    }));

  return el('section', { classe: 'painel' }, [
    el('div', { classe: 'painel__cabeca' }, [
      el('div', {}, [
        el('h2', { texto: 'Próximos encontros' }),
        el('p', { texto: 'Projete o QR Code na hora da aula' }),
      ]),
    ]),
    corpo,
  ]);
}

export default function painel(container) {
  container.append(
    el('header', { classe: 'topo' }, [
      el('div', {}, [
        el('h1', { texto: 'Painel' }),
        el('p', { texto: 'O retrato do programa hoje: quanta gente está sendo alcançada e se a frequência sustenta a entrega dos certificados.' }),
      ]),
    ]),
  );

  const area = el('div');
  container.append(area);

  comEstados(area, () => api.metricas(), (m) => el('div', {}, [
    prestacaoDeContas(m),
    contadores(m),
    el('div', { classe: 'grade-dupla' }, [
      ocupacao(m.cursosPorOcupacao),
      proximos(m.proximosEncontros),
    ]),
  ]));
}
