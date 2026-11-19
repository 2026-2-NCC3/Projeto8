/**
 * Alunos: busca, lista e ficha.
 *
 * Na ficha, a frequência por curso é o dado que importa — é ela que decide o
 * certificado. Quem está abaixo de 75% aparece marcado em brasa, para a
 * equipe conseguir ligar para a família antes do curso acabar.
 */

import { api } from '../api.js';
import {
  el, limpar, comEstados, vazio, plural, dataBR,
  avisar, confirmar, formularioEmDialogo, campoTexto, campoSelecao, campoDupla,
} from '../ui.js';

const META = 75;

// ── Cadastro: o mesmo formulário serve para criar e para editar ───────────

const SERIES = ['1º EM', '2º EM', '3º EM', 'Ensino médio concluído'];

function camposDoAluno(aluno = null) {
  const novo = aluno === null;
  const a = aluno || {};

  // Se a série gravada não estiver na lista (dado antigo, digitado à mão),
  // ela entra como opção para não sumir ao abrir a edição.
  const series = a.serie && !SERIES.includes(a.serie) ? [a.serie, ...SERIES] : SERIES;

  return [
    campoTexto({ nome: 'nome', rotulo: 'Nome completo', valor: a.nome, obrigatorio: true, maxlength: 120, autocomplete: 'off' }),
    campoTexto({
      nome: 'email', rotulo: 'E-mail', tipo: 'email', valor: a.email, obrigatorio: true, autocomplete: 'off',
      dica: 'É o login do aluno no app.',
    }),
    campoDupla(
      campoTexto({ nome: 'escola', rotulo: 'Escola', valor: a.escola || '' }),
      campoSelecao({
        nome: 'serie', rotulo: 'Série', valor: a.serie || '',
        opcoes: [{ valor: '', rotulo: 'Não informada' }, ...series.map((s) => ({ valor: s, rotulo: s }))],
      }),
    ),
    campoTexto({ nome: 'cidade', rotulo: 'Cidade', valor: a.cidade || '' }),
    campoTexto({
      nome: 'senha',
      rotulo: novo ? 'Senha inicial' : 'Nova senha',
      tipo: 'password',
      obrigatorio: novo,
      minlength: '6',
      autocomplete: 'new-password',
      dica: novo
        ? 'Mínimo de 6 caracteres. Entregue ao aluno junto com o e-mail.'
        : 'Deixe em branco para manter a senha atual. Preencha só se o aluno esqueceu a dele.',
    }),
  ];
}

async function novoAluno() {
  const dados = await formularioEmDialogo({
    titulo: 'Novo aluno',
    descricao: 'Cria a conta que o jovem vai usar para entrar no app.',
    campos: camposDoAluno(),
    enviar: 'Cadastrar aluno',
  });
  if (!dados) return;
  try {
    const aluno = await api.criarAluno(dados);
    avisar(`Cadastro de ${aluno.nome} criado. O acesso ao app é pelo e-mail ${aluno.email}.`);
    location.hash = `#/alunos/${aluno.id}`;
  } catch (e) {
    avisar(e.message, { tipo: 'erro' });
  }
}

async function editarAluno(aluno, recarregar) {
  const dados = await formularioEmDialogo({
    titulo: 'Editar cadastro',
    descricao: aluno.nome,
    campos: camposDoAluno(aluno),
    enviar: 'Salvar alterações',
  });
  if (!dados) return;
  try {
    const r = await api.atualizarAluno(aluno.id, dados);
    avisar(r.senhaRedefinida
      ? 'Cadastro atualizado e senha redefinida. Avise o aluno da senha nova.'
      : 'Cadastro atualizado.');
    recarregar();
  } catch (e) {
    avisar(e.message, { tipo: 'erro' });
  }
}

async function excluirAluno(aluno) {
  const ok = await confirmar({
    titulo: 'Excluir este aluno?',
    texto: `A conta de ${aluno.nome} será apagada com todo o histórico: inscrições, presenças, `
      + 'certificados e conversas. O acesso ao app é cortado na hora. Não dá para desfazer.',
    confirmar: 'Excluir definitivamente',
  });
  if (!ok) return;
  try {
    const r = await api.excluirAluno(aluno.id);
    avisar(`Cadastro de ${r.nome} excluído, com ${plural(r.removidos.presencas, 'presença', 'presenças')} `
      + `e ${plural(r.removidos.certificados, 'certificado', 'certificados')}.`);
    location.hash = '#/alunos';
  } catch (e) {
    avisar(e.message, { tipo: 'erro' });
  }
}

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
      el('div', { classe: 'topo__acoes' }, [
        el('button', { classe: 'botao botao--principal', type: 'button', texto: 'Novo aluno', onclick: novoAluno }),
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
            : 'Use "Novo aluno" para criar a conta que o jovem vai usar no app.'),
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

  // Depois de editar, redesenha a ficha com os dados novos do servidor.
  const recarregar = () => { limpar(container); fichaAluno(container, alunoId); };

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
          el('button', {
            classe: 'botao botao--contorno', type: 'button', texto: 'Editar cadastro',
            onclick: () => editarAluno(aluno, recarregar),
          }),
          el('button', {
            classe: 'botao botao--perigo', type: 'button', texto: 'Excluir aluno',
            onclick: () => excluirAluno(aluno),
          }),
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
