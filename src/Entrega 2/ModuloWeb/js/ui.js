/**
 * Peças de interface reaproveitadas pelas telas: criação de elementos,
 * formatação em português, estados de carregando/vazio/erro, avisos e o
 * diálogo (usado tanto para confirmar quanto para formulários).
 */

/** Cria um elemento. `texto` entra como textContent — nunca como HTML. */
export function el(tag, atributos = {}, filhos = []) {
  const no = document.createElement(tag);
  for (const [chave, valor] of Object.entries(atributos)) {
    if (valor === null || valor === undefined || valor === false) continue;
    if (chave === 'texto') no.textContent = valor;
    else if (chave === 'html') no.innerHTML = valor;
    else if (chave === 'classe') no.className = valor;
    else if (chave.startsWith('on')) no.addEventListener(chave.slice(2).toLowerCase(), valor);
    else if (chave === 'dataset') Object.assign(no.dataset, valor);
    else no.setAttribute(chave, valor === true ? '' : valor);
  }
  (Array.isArray(filhos) ? filhos : [filhos])
    .filter(Boolean)
    .forEach((f) => no.append(f));
  return no;
}

export function limpar(no) { no.replaceChildren(); return no; }

// ── Formatação ────────────────────────────────────────────────────────────

const MESES = ['jan', 'fev', 'mar', 'abr', 'mai', 'jun', 'jul', 'ago', 'set', 'out', 'nov', 'dez'];

/**
 * As datas chegam do SQLite como texto ("2026-09-01", "2026-09-01 18:00:00").
 * São lidas por pedaço, de propósito: passar por `new Date()` desloca o dia
 * conforme o fuso do computador e um encontro acabaria aparecendo na véspera.
 */
export function dataBR(iso) {
  if (!iso) return '—';
  const [ano, mes, dia] = String(iso).slice(0, 10).split('-');
  return `${dia}/${mes}/${ano}`;
}

export function diaMes(iso) {
  if (!iso) return { dia: '--', mes: '' };
  const [, mes, dia] = String(iso).slice(0, 10).split('-');
  return { dia, mes: MESES[Number(mes) - 1] || '' };
}

export function dataHoraBR(iso) {
  if (!iso) return '—';
  const texto = String(iso);
  const hora = texto.length > 10 ? texto.slice(11, 16) : '';
  return hora ? `${dataBR(texto)} às ${hora}` : dataBR(texto);
}

export function periodo(inicio, fim) {
  if (!inicio) return 'sem data definida';
  if (!fim || fim === inicio) return `em ${dataBR(inicio)}`;
  return `de ${dataBR(inicio)} a ${dataBR(fim)}`;
}

/** "3 alunos" / "1 aluno" — evita o "1 aluno(s)" dos formulários de escola. */
export function plural(quantidade, singular, pluralForma) {
  return `${quantidade} ${quantidade === 1 ? singular : pluralForma}`;
}

export function hoje() { return new Date().toISOString().slice(0, 10); }

// ── Estados de tela ───────────────────────────────────────────────────────

export function carregando(mensagem = 'Carregando…') {
  return el('div', { classe: 'carregando', texto: mensagem });
}

/** Tela vazia é convite para agir, por isso aceita um botão. */
export function vazio(titulo, texto, acao = null) {
  return el('div', { classe: 'estado' }, [
    el('h3', { texto: titulo }),
    el('p', { texto }),
    acao,
  ]);
}

export function erro(mensagem, aoTentarDeNovo = null) {
  return el('div', { classe: 'estado' }, [
    el('h3', { texto: 'Não foi possível carregar' }),
    el('p', { texto: mensagem }),
    aoTentarDeNovo
      ? el('button', { classe: 'botao botao--contorno', texto: 'Tentar de novo', onclick: aoTentarDeNovo })
      : null,
  ]);
}

/**
 * Roda uma busca na API e desenha o resultado, cuidando sozinha de
 * carregando / erro / nova tentativa. Evita repetir try/catch em cada tela.
 */
export async function comEstados(container, buscar, desenhar) {
  limpar(container).append(carregando());
  try {
    const dados = await buscar();
    limpar(container).append(desenhar(dados));
  } catch (e) {
    if (e.status === 401) return; // o app já está levando de volta ao login
    limpar(container).append(erro(e.message, () => comEstados(container, buscar, desenhar)));
  }
}

// ── Avisos flutuantes ─────────────────────────────────────────────────────

const MAX_AVISOS = 3;

export function avisar(mensagem, { tipo = 'ok', detalhe = null, segundos = 5 } = {}) {
  const caixa = document.getElementById('avisos');
  const toast = el('div', { classe: `toast${tipo === 'erro' ? ' toast--erro' : ''}` }, [
    el('strong', { texto: mensagem }),
    detalhe ? el('code', { texto: detalhe }) : null,
  ]);
  caixa.append(toast);
  // Mantém só os avisos mais recentes: empilhar seis cobre meia tela.
  while (caixa.children.length > MAX_AVISOS) caixa.firstElementChild.remove();
  setTimeout(() => toast.remove(), segundos * 1000);
}

// ── Diálogo ───────────────────────────────────────────────────────────────

const dialogo = () => document.getElementById('dialogo');

function montarDialogo(titulo, descricao, corpo, rodape) {
  const d = dialogo();
  limpar(d).append(
    el('div', { classe: 'dialogo__cabeca' }, [
      el('h2', { id: 'dialogo-titulo', texto: titulo }),
      descricao ? el('p', { texto: descricao }) : null,
    ]),
    el('div', { classe: 'dialogo__corpo' }, corpo),
    el('div', { classe: 'dialogo__rodape' }, rodape),
  );
  return d;
}

/**
 * Confirmação antes de algo irreversível. Usa <dialog> nativo para ganhar de
 * graça o foco preso, o Esc e o fundo inerte — acessibilidade sem código extra.
 */
export function confirmar({ titulo, texto, confirmar: rotulo = 'Confirmar', perigo = true }) {
  return new Promise((resolve) => {
    let resposta = false;
    const d = montarDialogo(titulo, null, [el('p', { texto, style: 'margin:0' })], [
      el('button', {
        classe: 'botao botao--contorno', texto: 'Cancelar',
        onclick: () => d.close(),
      }),
      el('button', {
        classe: `botao ${perigo ? 'botao--perigo' : 'botao--principal'}`, texto: rotulo,
        onclick: () => { resposta = true; d.close(); },
      }),
    ]);
    d.addEventListener('close', () => resolve(resposta), { once: true });
    d.showModal();
  });
}

/**
 * Formulário em diálogo. `campos` desenha o conteúdo; devolve os dados ao
 * confirmar, ou `null` se a pessoa cancelar.
 */
export function formularioEmDialogo({ titulo, descricao, campos, enviar = 'Salvar' }) {
  return new Promise((resolve) => {
    let dados = null;

    const form = el('form', { id: 'form-dialogo', novalidate: true });
    form.append(...campos);

    const erroEl = el('p', { classe: 'aviso aviso--erro', hidden: true });

    const d = montarDialogo(titulo, descricao, [erroEl, form], [
      el('button', { classe: 'botao botao--contorno', texto: 'Cancelar', onclick: () => d.close() }),
      el('button', { classe: 'botao botao--principal', texto: enviar, type: 'submit', form: 'form-dialogo' }),
    ]);

    form.addEventListener('submit', (ev) => {
      ev.preventDefault();
      if (!form.checkValidity()) {
        erroEl.textContent = 'Preencha os campos obrigatórios destacados.';
        erroEl.hidden = false;
        form.reportValidity();
        return;
      }
      dados = Object.fromEntries(new FormData(form).entries());
      d.close();
    });

    d.addEventListener('close', () => resolve(dados), { once: true });
    d.showModal();
    // Foco no primeiro campo: quem usa teclado começa a digitar direto.
    form.querySelector('input, select, textarea')?.focus();
  });
}

// ── Campos prontos para os formulários ────────────────────────────────────

let contador = 0;
const novoId = (nome) => `campo-${nome}-${++contador}`;

export function campoTexto({ nome, rotulo, valor = '', tipo = 'text', obrigatorio = false, dica = '', ...resto }) {
  const id = novoId(nome);
  return el('div', { classe: 'campo' }, [
    el('label', { for: id, texto: rotulo + (obrigatorio ? ' *' : '') }),
    el('input', { id, name: nome, type: tipo, value: valor ?? '', required: obrigatorio, ...resto }),
    dica ? el('p', { classe: 'campo__dica', texto: dica }) : null,
  ]);
}

export function campoAreaTexto({ nome, rotulo, valor = '', obrigatorio = false, dica = '' }) {
  const id = novoId(nome);
  return el('div', { classe: 'campo' }, [
    el('label', { for: id, texto: rotulo + (obrigatorio ? ' *' : '') }),
    el('textarea', { id, name: nome, required: obrigatorio, texto: valor ?? '' }),
    dica ? el('p', { classe: 'campo__dica', texto: dica }) : null,
  ]);
}

export function campoSelecao({ nome, rotulo, opcoes, valor = '', obrigatorio = false, ...resto }) {
  const id = novoId(nome);
  const select = el('select', { id, name: nome, required: obrigatorio, ...resto });
  opcoes.forEach((o) => select.append(
    el('option', { value: o.valor, texto: o.rotulo, selected: String(o.valor) === String(valor) }),
  ));
  return el('div', { classe: 'campo' }, [el('label', { for: id, texto: rotulo + (obrigatorio ? ' *' : '') }), select]);
}

export function campoDupla(a, b) {
  return el('div', { classe: 'campo-duplo' }, [a, b]);
}

/** Barra de proporção com rótulo acessível. */
export function barra(valor, total, { cheiaEmAmbar = true } = {}) {
  const pct = total > 0 ? Math.min(100, Math.round((valor / total) * 100)) : 0;
  return el('div', {
    classe: `barra${cheiaEmAmbar && pct >= 100 ? ' barra--cheia' : ''}`,
    role: 'img',
    'aria-label': `${pct}% de ${total}`,
  }, [el('span', { style: `width:${pct}%` })]);
}
