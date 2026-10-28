/**
 * Alunos: busca, lista e ficha.
 *
 * Na ficha, a frequência por curso é o dado que importa — é ela que decide o
 * certificado. Quem está abaixo de 75% aparece marcado em brasa, para a
 * equipe conseguir ligar para a família antes do curso acabar.
 */

import { api } from '../api.js';
import { el, comEstados, vazio, plural, dataBR } from '../ui.js';

const META = 75;

function faixaDeFrequencia(freq) {
  if (freq === null || freq === undefined) {
    return { classe: 'etiqueta etiqueta--neutra', texto: 'sem encontros até hoje' };
  }
  if (freq < META) return { classe: 'etiqueta etiqueta--risco', texto: `${freq}% de frequência` };
  return { classe: 'etiqueta', texto: `${freq}% de frequência` };
}

// ── Lista com busca ───────────────────────────────────────────────────────

export function listaAlunos(container) {
  const resultado = el('div');

  const entrada = el('input', {
    id: 'busca-aluno',
    type: 'search',
    name: 'busca',
    placeholder: 'Nome ou e-mail',
    autocomplete: 'off',
  });

  const form = el('form', { classe: 'busca', role: 'search' }, [
    el('div', { classe: 'campo' }, [
      el('label', { for: 'busca-aluno', texto: 'Buscar aluno' }),
      entrada,
    ]),
    el('button', { classe: 'botao botao--principal', type: 'submit', texto: 'Buscar' }),
  ]);

  container.append(
    el('header', { classe: 'topo' }, [
      el('div', {}, [
        el('h1', { texto: 'Alunos' }),
        el('p', { texto: 'Quem está no programa, em quantos cursos e com que frequência.' }),
      ]),
    ]),
    form,
    resultado,
  );

  function buscar() {
    const termo = entrada.value.trim();
    comEstados(resultado, () => api.alunos(termo), (alunos) => {
      if (alunos.length === 0) {
        return el('section', { classe: 'painel' }, [
          vazio('Nenhum aluno encontrado', termo
            ? `Nada corresponde a "${termo}". Tente parte do nome ou do e-mail.`
            : 'Os alunos aparecem aqui assim que se cadastram pelo aplicativo.'),
        ]);
      }
      return el('section', { classe: 'painel' }, [
        el('div', { classe: 'painel__cabeca' }, [
          el('div', {}, [
            el('h2', { texto: termo ? `Resultado para "${termo}"` : 'Todos os alunos' }),
            el('p', { texto: plural(alunos.length, 'aluno', 'alunos') }),
          ]),
        ]),
        el('ul', { classe: 'lista' }, alunos.map((a) => el('li', {}, [
          el('div', { classe: 'lista__principal' }, [
            el('p', { classe: 'lista__titulo' }, [el('a', { href: `#/alunos/${a.id}`, texto: a.nome })]),
            el('p', {
              classe: 'lista__meta',
              texto: [a.escola, a.serie, a.cidade].filter(Boolean).join(', ') || a.email,
            }),
          ]),
          el('p', { classe: 'lista__numero', texto: String(a.cursos) }, [
            el('small', { texto: a.cursos === 1 ? 'curso' : 'cursos' }),
          ]),
          el('p', { classe: 'lista__numero', texto: String(a.presencas) }, [
            el('small', { texto: a.presencas === 1 ? 'presença' : 'presenças' }),
          ]),
          el('span', { classe: 'etiqueta etiqueta--neutra', texto: a.nivel || '—' }),
          el('a', { classe: 'botao botao--contorno botao--pequeno', href: `#/alunos/${a.id}`, texto: 'Abrir ficha' }),
        ]))),
      ]);
    });
  }

  form.addEventListener('submit', (ev) => { ev.preventDefault(); buscar(); });

  // Busca enquanto digita, com pausa — evita uma requisição por tecla.
  let timer;
  entrada.addEventListener('input', () => {
    clearTimeout(timer);
    timer = setTimeout(buscar, 350);
  });

  buscar();
}

// ── Ficha do aluno ────────────────────────────────────────────────────────

export function fichaAluno(container, alunoId) {
  const area = el('div');
  container.append(el('a', { classe: 'voltar', href: '#/alunos', texto: '← Voltar para os alunos' }), area);

  comEstados(area, () => api.aluno(alunoId), ({ aluno, cursos }) => {
    const abaixo = cursos.filter((c) => c.frequencia !== null && c.frequencia < META).length;

    const alerta = abaixo > 0
      ? el('p', {
        classe: 'aviso aviso--erro',
        texto: abaixo === 1
          ? 'Em 1 curso a frequência está abaixo de 75% — neste ritmo, o certificado não sai.'
          : `Em ${abaixo} cursos a frequência está abaixo de 75% — nesse ritmo, os certificados não saem.`,
      })
      : null;

    const corpo = cursos.length === 0
      ? vazio('Sem inscrições', 'Este aluno ainda não se inscreveu em nenhum curso pelo app.')
      : el('ul', { classe: 'lista' }, cursos.map((c) => {
        const faixa = faixaDeFrequencia(c.frequencia);
        return el('li', {}, [
          el('div', { classe: 'lista__principal' }, [
            el('p', { classe: 'lista__titulo' }, [el('a', { href: `#/cursos/${c.id}`, texto: c.titulo })]),
            el('p', {
              classe: 'lista__meta',
              texto: c.encontros_ate_hoje > 0
                ? `Presente em ${c.presencas} de ${plural(c.encontros_ate_hoje, 'encontro realizado', 'encontros realizados')}`
                : 'O curso ainda não teve encontros',
            }),
          ]),
          el('span', { classe: 'etiqueta etiqueta--neutra', texto: c.status }),
          el('span', { classe: faixa.classe, texto: faixa.texto }),
        ]);
      }));

    return el('div', {}, [
      el('header', { classe: 'topo' }, [
        el('div', {}, [
          el('h1', { texto: aluno.nome }),
          el('p', { texto: [aluno.escola, aluno.serie, aluno.cidade].filter(Boolean).join(', ') }),
          el('p', { classe: 'lista__meta', texto: `${aluno.email}, no programa desde ${dataBR(aluno.criado_em)}` }),
        ]),
        el('div', { classe: 'topo__acoes' }, [
          el('a', { classe: 'botao botao--contorno', href: `#/chat/${aluno.id}`, texto: 'Abrir conversa' }),
        ]),
      ]),

      el('dl', { classe: 'contadores' }, [
        el('div', {}, [el('dt', { texto: 'Cursos' }), el('dd', { texto: String(cursos.length) })]),
        el('div', {}, [el('dt', { texto: 'Pontos no app' }), el('dd', { texto: String(aluno.pontos) })]),
        el('div', {}, [el('dt', { texto: 'Nível' }), el('dd', { texto: aluno.nivel || '—' })]),
      ]),

      alerta,

      el('section', { classe: 'painel' }, [
        el('div', { classe: 'painel__cabeca' }, [
          el('div', {}, [
            el('h2', { texto: 'Frequência por curso' }),
            el('p', { texto: `Abaixo de ${META}% o aluno não recebe certificado` }),
          ]),
        ]),
        corpo,
      ]),
    ]);
  });
}
