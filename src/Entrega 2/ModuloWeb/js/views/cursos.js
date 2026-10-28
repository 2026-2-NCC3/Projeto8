/**
 * Cursos: lista, criação, edição, desativação — e, dentro de um curso, os
 * encontros, que é de onde sai o QR Code da presença.
 *
 * Curso nunca é apagado: o botão desativa. Presenças e certificados já
 * emitidos continuam valendo, e a ONG precisa desse histórico.
 */

import { api, cursosEmCache, invalidarCursos } from '../api.js';
import {
  el, comEstados, barra, vazio, avisar, confirmar, formularioEmDialogo,
  campoTexto, campoAreaTexto, campoSelecao, campoDupla, periodo, plural,
  dataBR, diaMes, hoje,
} from '../ui.js';

const CATEGORIAS = ['Tecnologia', 'Educação', 'Carreira', 'Idiomas', 'Finanças', 'Criatividade', 'Geral'];

/** Campos do curso, reaproveitados por criar e editar. */
function camposDoCurso(curso = {}) {
  return [
    campoTexto({ nome: 'titulo', rotulo: 'Nome do curso', valor: curso.titulo, obrigatorio: true, maxlength: 120 }),
    campoAreaTexto({
      nome: 'descricao', rotulo: 'Descrição', valor: curso.descricao,
      dica: 'Este texto aparece para o aluno na tela do curso, dentro do app.',
    }),
    campoDupla(
      campoSelecao({
        nome: 'categoria', rotulo: 'Categoria', valor: curso.categoria || 'Geral',
        opcoes: CATEGORIAS.map((c) => ({ valor: c, rotulo: c })),
      }),
      campoTexto({ nome: 'local', rotulo: 'Local', valor: curso.local || '', dica: 'Ex.: FECAP — Liberdade, ou Online' }),
    ),
    campoDupla(
      campoTexto({ nome: 'data_inicio', rotulo: 'Início', tipo: 'date', valor: (curso.data_inicio || hoje()).slice(0, 10), obrigatorio: true }),
      campoTexto({ nome: 'data_fim', rotulo: 'Término', tipo: 'date', valor: (curso.data_fim || '').slice(0, 10) }),
    ),
    campoDupla(
      campoTexto({ nome: 'carga_horaria', rotulo: 'Carga horária (horas)', tipo: 'number', min: '0', valor: curso.carga_horaria ?? 20 }),
      campoTexto({ nome: 'vagas_totais', rotulo: 'Vagas', tipo: 'number', min: '1', valor: curso.vagas_totais ?? 30 }),
    ),
    campoSelecao({
      nome: 'possui_certificado', rotulo: 'Emite certificado',
      valor: curso.possui_certificado === 0 ? 'nao' : 'sim',
      opcoes: [{ valor: 'sim', rotulo: 'Sim, com 75% de frequência' }, { valor: 'nao', rotulo: 'Não' }],
    }),
  ];
}

function normalizar(dados) {
  return {
    ...dados,
    carga_horaria: Number(dados.carga_horaria) || 0,
    vagas_totais: Number(dados.vagas_totais) || 30,
    possui_certificado: dados.possui_certificado === 'sim',
    data_fim: dados.data_fim || dados.data_inicio,
  };
}

async function novoCurso(recarregar) {
  const dados = await formularioEmDialogo({
    titulo: 'Novo curso',
    descricao: 'Ao salvar, todos os alunos recebem uma notificação de curso novo no app.',
    campos: camposDoCurso(),
    enviar: 'Criar curso',
  });
  if (!dados) return;
  try {
    const curso = await api.criarCurso(normalizar(dados));
    invalidarCursos();
    avisar(`Curso "${curso.titulo}" criado e anunciado no app.`);
    recarregar();
  } catch (e) {
    avisar(e.message, { tipo: 'erro' });
  }
}

async function editarCurso(curso, recarregar) {
  const dados = await formularioEmDialogo({
    titulo: 'Editar curso',
    descricao: curso.titulo,
    campos: camposDoCurso(curso),
    enviar: 'Salvar alterações',
  });
  if (!dados) return;
  try {
    await api.atualizarCurso(curso.id, normalizar(dados));
    invalidarCursos();
    avisar('Curso atualizado.');
    recarregar();
  } catch (e) {
    avisar(e.message, { tipo: 'erro' });
  }
}

async function desativarCurso(curso, recarregar) {
  const ok = await confirmar({
    titulo: 'Desativar este curso?',
    texto: `"${curso.titulo}" sai da lista de cursos do app e ninguém mais se inscreve. As presenças e os certificados já emitidos continuam guardados.`,
    confirmar: 'Desativar curso',
  });
  if (!ok) return;
  try {
    const r = await api.desativarCurso(curso.id);
    invalidarCursos();
    avisar(r.aviso || 'Curso desativado.');
    recarregar();
  } catch (e) {
    avisar(e.message, { tipo: 'erro' });
  }
}

// ── Lista de cursos ───────────────────────────────────────────────────────

export function listaCursos(container) {
  let mostrarInativos = false;

  const area = el('div');

  const botaoNovo = el('button', {
    classe: 'botao botao--principal', texto: 'Novo curso',
    onclick: () => novoCurso(desenhar),
  });

  const alternar = el('button', {
    classe: 'botao botao--contorno', texto: 'Mostrar desativados',
    'aria-pressed': 'false',
    onclick: () => {
      mostrarInativos = !mostrarInativos;
      alternar.textContent = mostrarInativos ? 'Ocultar desativados' : 'Mostrar desativados';
      alternar.setAttribute('aria-pressed', String(mostrarInativos));
      desenhar();
    },
  });

  container.append(
    el('header', { classe: 'topo' }, [
      el('div', {}, [
        el('h1', { texto: 'Cursos e encontros' }),
        el('p', { texto: 'Tudo o que a ONG publica no app começa aqui. Abra um curso para marcar encontros e gerar o QR Code da presença.' }),
      ]),
      el('div', { classe: 'topo__acoes' }, [alternar, botaoNovo]),
    ]),
    area,
  );

  function desenhar() {
    comEstados(area, () => cursosEmCache({ recarregar: true }), (todos) => {
      const cursos = mostrarInativos ? todos : todos.filter((c) => c.ativo === 1);

      if (cursos.length === 0) {
        return el('section', { classe: 'painel' }, [
          vazio(
            mostrarInativos ? 'Nenhum curso cadastrado' : 'Nenhum curso ativo',
            'Crie o primeiro curso para que ele apareça no aplicativo dos alunos.',
            el('button', { classe: 'botao botao--principal', texto: 'Novo curso', onclick: () => novoCurso(desenhar) }),
          ),
        ]);
      }

      return el('section', { classe: 'painel' }, [
        el('div', { classe: 'painel__cabeca' }, [
          el('div', {}, [
            el('h2', { texto: mostrarInativos ? 'Todos os cursos' : 'Cursos ativos' }),
            el('p', { texto: plural(cursos.length, 'curso na lista', 'cursos na lista') }),
          ]),
        ]),
        el('ul', { classe: 'lista' }, cursos.map((c) => linhaCurso(c, desenhar))),
      ]);
    });
  }

  desenhar();
}

function linhaCurso(c, recarregar) {
  const apoio = [c.categoria, `${c.carga_horaria} h`, periodo(c.data_inicio, c.data_fim), c.local]
    .filter(Boolean).join(', ');

  return el('li', { classe: c.ativo ? '' : 'ausente' }, [
    el('div', { classe: 'lista__principal' }, [
      el('p', { classe: 'lista__titulo' }, [
        el('a', { href: `#/cursos/${c.id}`, texto: c.titulo }),
        c.ativo ? null : el('span', { classe: 'etiqueta etiqueta--neutra', texto: 'Desativado', style: 'margin-left:8px' }),
      ]),
      el('p', { classe: 'lista__meta', texto: apoio }),
    ]),

    el('div', { classe: 'ocupacao' }, [
      el('p', { classe: 'ocupacao__texto' }, [
        el('span', { texto: 'Inscritos' }),
        el('span', { texto: `${c.vagas_totais - c.vagas_disponiveis} de ${c.vagas_totais}` }),
      ]),
      barra(c.vagas_totais - c.vagas_disponiveis, c.vagas_totais),
    ]),

    el('p', { classe: 'lista__numero', texto: String(c.encontros) }, [
      el('small', { texto: c.encontros === 1 ? 'encontro' : 'encontros' }),
    ]),

    el('div', { classe: 'lista__acoes' }, [
      el('a', { classe: 'botao botao--contorno botao--pequeno', href: `#/cursos/${c.id}`, texto: 'Abrir' }),
      el('button', { classe: 'botao botao--contorno botao--pequeno', texto: 'Editar', onclick: () => editarCurso(c, recarregar) }),
      c.ativo
        ? el('button', { classe: 'botao botao--perigo botao--pequeno', texto: 'Desativar', onclick: () => desativarCurso(c, recarregar) })
        : null,
    ]),
  ]);
}

// ── Detalhe do curso: ficha + encontros ───────────────────────────────────

async function novoEncontro(curso, recarregar) {
  const dados = await formularioEmDialogo({
    titulo: 'Novo encontro',
    descricao: `${curso.titulo} — o QR Code da presença é gerado automaticamente.`,
    campos: [
      campoTexto({ nome: 'titulo', rotulo: 'Título do encontro', obrigatorio: true, maxlength: 120, valor: `Encontro ${curso.encontros + 1} - ${curso.titulo}` }),
      campoTexto({ nome: 'data', rotulo: 'Data', tipo: 'date', valor: hoje(), obrigatorio: true }),
      campoDupla(
        campoTexto({ nome: 'hora_inicio', rotulo: 'Começa às', tipo: 'time', valor: '18:00', obrigatorio: true }),
        campoTexto({ nome: 'hora_fim', rotulo: 'Termina às', tipo: 'time', valor: '20:00' }),
      ),
      campoTexto({ nome: 'local', rotulo: 'Local', valor: curso.local || '' }),
    ],
    enviar: 'Criar encontro',
  });
  if (!dados) return;
  try {
    await api.criarAtividade(curso.id, dados);
    invalidarCursos();
    avisar('Encontro criado. O QR Code já pode ser projetado.');
    recarregar();
  } catch (e) {
    avisar(e.message, { tipo: 'erro' });
  }
}

async function excluirEncontro(atividade, recarregar) {
  const ok = await confirmar({
    titulo: 'Excluir este encontro?',
    texto: `"${atividade.titulo}" será removido junto com as ${plural(atividade.presencas, 'presença registrada', 'presenças registradas')}. Não dá para desfazer.`,
    confirmar: 'Excluir encontro',
  });
  if (!ok) return;
  try {
    await api.excluirAtividade(atividade.id);
    invalidarCursos();
    avisar('Encontro excluído.');
    recarregar();
  } catch (e) {
    avisar(e.message, { tipo: 'erro' });
  }
}

export function detalheCurso(container, cursoId) {
  const area = el('div');
  container.append(el('a', { classe: 'voltar', href: '#/cursos', texto: '← Voltar para os cursos' }), area);

  async function buscar() {
    const [cursos, atividades] = await Promise.all([
      cursosEmCache({ recarregar: true }),
      api.atividades(cursoId),
    ]);
    const curso = cursos.find((c) => Number(c.id) === Number(cursoId));
    if (!curso) throw new Error('Este curso não existe mais.');
    return { curso, atividades };
  }

  function desenhar({ curso, atividades }) {
    const recarregar = () => comEstados(area, buscar, desenhar);
    const inscritos = curso.vagas_totais - curso.vagas_disponiveis;

    const ficha = el('header', { classe: 'topo' }, [
      el('div', {}, [
        el('h1', { texto: curso.titulo }),
        el('p', { texto: curso.descricao || 'Sem descrição cadastrada.' }),
        el('p', { classe: 'lista__meta', texto: [
          curso.categoria, `${curso.carga_horaria} h`, periodo(curso.data_inicio, curso.data_fim), curso.local,
          `${inscritos} de ${curso.vagas_totais} vagas`,
          curso.possui_certificado ? 'emite certificado' : 'sem certificado',
        ].filter(Boolean).join(', ') }),
      ]),
      el('div', { classe: 'topo__acoes' }, [
        curso.ativo ? null : el('span', { classe: 'etiqueta etiqueta--neutra', texto: 'Curso desativado' }),
        el('button', { classe: 'botao botao--contorno', texto: 'Editar curso', onclick: () => editarCurso(curso, recarregar) }),
        el('button', { classe: 'botao botao--principal', texto: 'Novo encontro', onclick: () => novoEncontro({ ...curso, encontros: atividades.length }, recarregar) }),
      ]),
    ]);

    const corpo = atividades.length === 0
      ? vazio(
        'Nenhum encontro marcado',
        'O QR Code da presença nasce do encontro. Marque o primeiro para a turma poder fazer check-in pelo app.',
        el('button', { classe: 'botao botao--principal', texto: 'Novo encontro', onclick: () => novoEncontro({ ...curso, encontros: 0 }, recarregar) }),
      )
      : el('ul', { classe: 'lista' }, atividades.map((a) => {
        const d = diaMes(a.data);
        return el('li', {}, [
          el('div', { classe: 'data-bloco' }, [el('strong', { texto: d.dia }), el('span', { texto: d.mes })]),
          el('div', { classe: 'lista__principal' }, [
            el('p', { classe: 'lista__titulo', texto: a.titulo }),
            el('p', {
              classe: 'lista__meta',
              texto: `${dataBR(a.data)}, das ${String(a.hora_inicio).slice(0, 5)} às ${String(a.hora_fim || a.hora_inicio).slice(0, 5)}, em ${a.local || 'local a definir'}`,
            }),
          ]),
          el('p', { classe: 'lista__numero', texto: String(a.presencas) }, [
            el('small', { texto: a.presencas === 1 ? 'presença' : 'presenças' }),
          ]),
          el('div', { classe: 'lista__acoes' }, [
            el('a', { classe: 'botao botao--principal botao--pequeno', href: `#/encontros/${a.id}/qrcode`, texto: 'Projetar QR Code' }),
            el('a', { classe: 'botao botao--contorno botao--pequeno', href: `#/encontros/${a.id}/chamada`, texto: 'Chamada' }),
            el('button', { classe: 'botao botao--perigo botao--pequeno', texto: 'Excluir', onclick: () => excluirEncontro(a, recarregar) }),
          ]),
        ]);
      }));

    return el('div', {}, [
      ficha,
      el('section', { classe: 'painel' }, [
        el('div', { classe: 'painel__cabeca' }, [
          el('div', {}, [
            el('h2', { texto: 'Encontros' }),
            el('p', { texto: 'Cada encontro tem o próprio QR Code e a própria lista de presença' }),
          ]),
        ]),
        corpo,
      ]),
    ]);
  }

  comEstados(area, buscar, desenhar);
}

/** Usado pela tela de comunicados para montar o seletor de curso. */
export async function opcoesDeCurso() {
  const cursos = await cursosEmCache();
  return cursos.filter((c) => c.ativo === 1).map((c) => ({ valor: c.id, rotulo: c.titulo }));
}
