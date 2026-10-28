# -*- coding: utf-8 -*-
"""
=============================================================================
 Projeto Interdisciplinar FECAP — "Próxima Etapa — App do Aluno"
 Unidade Curricular: Análise Descritiva de Dados — ENTREGA 2 (final)
=============================================================================

Objetivo da Entrega 2 (enunciado do professor):
  1. Avaliar a dispersão das variáveis quantitativas pelo Coeficiente de Variação.
  2. Indicar quais variáveis apresentam grande dispersão.
  3. Sugerir possíveis explicações e medidas para redução da variabilidade.
  4. Construir Box Plot e realizar análise exploratória dos outliers.
  5. Comparar resultados com e sem valores atípicos e concluir sobre
     concentração dos dados.
  6. Quando aplicável, determinar a probabilidade de ocorrência de eventos
     (baixa frequência, ausência, conclusão de cursos).

O que este script faz:
  - Simula uma base de dados FICTÍCIA coerente com o schema.sql e o seed.js do
    back-end do projeto (nenhum dado real de aluno é utilizado — exigência de
    LGPD do projeto), porém com volume ampliado (160 alunos e 12 cursos, contra
    os 30 alunos e 7 cursos do seed de demonstração) para dar significância
    estatística à análise.
  - Gera os gráficos (Box Plots e comparativo de CV) com matplotlib.
  - Monta a planilha Excel com fórmulas VIVAS (AVERAGE, STDEV.S, QUARTILE.INC,
    COUNTIF, AVERAGEIFS, BINOM.DIST ...), de modo que todos os indicadores se
    recalculam automaticamente se a base de dados for alterada.

Reprodutibilidade: semente fixa (random.seed / np.random.seed = 42).

Como executar:
    pip install --break-system-packages openpyxl pandas numpy matplotlib
    python3 gerar_analise_dispersao.py
=============================================================================
"""

import os
import random
import unicodedata

import numpy as np
import matplotlib

matplotlib.use("Agg")  # backend sem interface gráfica
import matplotlib.pyplot as plt

import openpyxl
from openpyxl.styles import Font, PatternFill, Alignment, Border, Side
from openpyxl.utils import get_column_letter
from openpyxl.formatting.rule import FormulaRule
from openpyxl.chart import BarChart, Reference
from openpyxl.drawing.image import Image as XLImage

# =============================================================================
# 0. CONFIGURAÇÃO GERAL
# =============================================================================

SEMENTE = 42
random.seed(SEMENTE)
np.random.seed(SEMENTE)

PASTA = os.path.dirname(os.path.abspath(__file__))
ARQ_XLSX = os.path.join(PASTA, "analise_dispersao_proxima_etapa.xlsx")

N_ALUNOS = 160
N_CURSOS = 12

# --- Identidade visual (mesma da Entrega 1) ---
AZUL = "1E3A5F"           # azul institucional usado nos cabeçalhos
CINZA_TXT = "555555"      # texto de apoio / subtítulos

# Paleta dos gráficos (superfície clara)
C_SUPERFICIE = "#fcfcfb"
C_TINTA = "#0b0b0b"
C_TINTA_2 = "#52514e"
C_MUDO = "#898781"
C_GRADE = "#e1e0d9"
C_SERIE = "#2a78d6"       # azul — série principal
C_BOM = "#0ca30c"         # status: baixa dispersão
C_ATENCAO = "#fab219"     # status: média dispersão
C_CRITICO = "#d03b3b"     # status: alta dispersão / outliers

# --- Estilos de célula ---
F_TITULO = Font(name="Arial", size=14, bold=True, color=AZUL)
F_SUB = Font(name="Arial", size=10, color=CINZA_TXT)
F_SECAO = Font(name="Arial", size=11, bold=True, color=AZUL)
F_CAB = Font(name="Arial", size=11, bold=True, color="FFFFFF")
F_NOTA = Font(name="Arial", size=9, italic=True, color=CINZA_TXT)
F_TXT = Font(name="Calibri", size=11)
F_NEG = Font(name="Calibri", size=11, bold=True)

FILL_CAB = PatternFill("solid", start_color=AZUL)
FILL_ALTA = PatternFill("solid", start_color="FCE4E4")   # alta dispersão
FILL_MEDIA = PatternFill("solid", start_color="FFF2CC")  # média dispersão
FILL_BAIXA = PatternFill("solid", start_color="E2EFDA")  # baixa dispersão

AL_CAB = Alignment(horizontal="center", vertical="center", wrap_text=True)
AL_TXT = Alignment(vertical="top", wrap_text=True)
AL_CENTRO = Alignment(horizontal="center", vertical="center")

_fino = Side(style="thin", color="FFFFFF")
B_CAB = Border(bottom=_fino, right=_fino)

# Prefixo exigido pelo formato OOXML para as funções introduzidas a partir do
# Excel 2010 (o Excel exibe "STDEV.S", "QUARTILE.INC", "BINOM.DIST" sem o prefixo).
XFN = "_xlfn."


# =============================================================================
# 1. SIMULAÇÃO DA BASE DE DADOS FICTÍCIA
#    (estrutura espelhada em src/Entrega 2/Backend/db/schema.sql)
# =============================================================================

PRENOMES = [
    "Ana", "Bruno", "Carla", "Diego", "Elisa", "Fábio", "Gabriela", "Henrique",
    "Isabela", "João", "Karina", "Lucas", "Mariana", "Nicolas", "Olívia",
    "Pedro", "Queila", "Rafael", "Sofia", "Thiago", "Úrsula", "Vinícius",
    "Wesley", "Yasmin", "Zeca", "Amanda", "Bruna", "Caio", "Débora", "Eduardo",
    "Felipe", "Giovana", "Heitor", "Ingrid", "Jonas", "Larissa", "Murilo",
    "Natália", "Otávio", "Paula", "Renan", "Sabrina", "Tatiane", "Vitor",
]
SOBRENOMES = [
    "Souza", "Lima", "Mendes", "Alves", "Rocha", "Nunes", "Dias", "Melo",
    "Cruz", "Silva", "Ferreira", "Costa", "Barros", "Ramos", "Teixeira",
    "Santos", "Pinto", "Martins", "Carvalho", "Araújo", "Gomes", "Ribeiro",
    "Moreira", "Farias", "Castro", "Correia", "Vieira", "Batista", "Monteiro",
]
ESCOLAS = [
    "EE Prof. João XXIII", "EE Dom Pedro II", "EE Castro Alves",
    "EE Machado de Assis", "EE Cora Coralina", "EE Anísio Teixeira",
    "EE Paulo Freire", "EE Carolina Maria de Jesus",
]
CIDADES = ["São Paulo", "Guarulhos", "Osasco", "Santo André", "Diadema", "São Bernardo do Campo"]
SERIES = ["1º EM", "2º EM", "3º EM"]


def sem_acento(txt):
    return "".join(
        c for c in unicodedata.normalize("NFD", txt) if unicodedata.category(c) != "Mn"
    )


def nivel_por_pontos(p):
    """Mesma regra de negócio do seed.js, estendida para a faixa ampliada."""
    if p > 350:
        return "Referência"
    if p > 150:
        return "Em trilha"
    return "Explorador"


# -----------------------------------------------------------------------------
# 1.1 Cursos
#     O curso 12 é um OUTLIER PROPOSITAL e realista: uma formação técnica longa
#     (120h / 24 encontros) ao lado de oficinas curtas de 8 a 30 horas.
# -----------------------------------------------------------------------------
CURSOS_BASE = [
    # titulo, categoria, local, carga_horaria, encontros, vagas_totais, atratividade, turma_encerrada
    ("Introdução à Programação",                 "Tecnologia", "FECAP - Liberdade",      20,  4, 30, 1.60, True),
    ("Oficina de Redação para o ENEM",           "Educação",   "Online",                 12,  3, 40, 1.45, True),
    ("Orientação Profissional",                  "Carreira",   "FECAP - Liberdade",       8,  2, 35, 0.45, True),
    ("Inglês para o Mundo do Trabalho",          "Idiomas",    "Online",                 30,  6, 25, 1.30, True),
    ("Design Thinking na Prática",               "Inovação",   "Espaço Próxima Etapa",   16,  4, 30, 0.95, True),
    ("Finanças Pessoais para Jovens",            "Carreira",   "Online",                 10,  2, 35, 1.20, True),
    ("Introdução a Dados e IA",                  "Tecnologia", "FECAP - Liberdade",      24,  5, 25, 1.70, True),
    ("Comunicação e Oratória",                   "Carreira",   "Espaço Próxima Etapa",   12,  3, 30, 1.00, True),
    ("Excel e Planilhas para o Trabalho",        "Tecnologia", "Online",                 18,  4, 40, 1.35, False),
    ("Preparatório para o Mercado de Trabalho",  "Carreira",   "FECAP - Liberdade",      14,  3, 30, 0.80, False),
    ("Cidadania e Projeto de Vida",              "Educação",   "Espaço Próxima Etapa",    8,  2, 35, 0.30, False),
    ("Formação Técnica em Desenvolvimento Web",  "Tecnologia", "FECAP - Liberdade",     120, 24, 20, 0.55, False),
]
assert len(CURSOS_BASE) == N_CURSOS

# Cursos com evasão/absenteísmo estruturalmente maior (carga longa ou turno difícil).
# Multiplicador aplicado à propensão de presença do aluno.
FATOR_CURSO = {4: 0.82, 9: 0.90, 12: 0.70, 3: 0.95}

cursos = []
for i, (tit, cat, loc, ch, enc, vt, atr, encerrada) in enumerate(CURSOS_BASE, start=1):
    cursos.append(
        dict(id=i, titulo=tit, categoria=cat, local=loc, carga_horaria=ch,
             num_encontros=enc, vagas_totais=vt, atratividade=atr,
             situacao="Encerrada" if encerrada else "Em andamento",
             encerrada=encerrada)
    )

# -----------------------------------------------------------------------------
# 1.2 Alunos
# -----------------------------------------------------------------------------
alunos = []
nomes_usados = set()
for i in range(1, N_ALUNOS + 1):
    while True:
        nome = f"{random.choice(PRENOMES)} {random.choice(SOBRENOMES)}"
        if nome not in nomes_usados:
            nomes_usados.add(nome)
            break

    # Idade: público-alvo é o Ensino Médio da rede pública (14 a 19 anos).
    idade = int(np.clip(round(np.random.normal(16.5, 1.55)), 14, 19))

    # Pontos (gamificação do app): distribuição fortemente assimétrica à direita.
    if random.random() < 0.82:
        pontos = int(np.random.gamma(shape=2.0, scale=95))
    else:
        pontos = int(np.random.normal(640, 130))
    pontos = max(0, pontos)

    # Quantidade de cursos em que o aluno se inscreveu.
    qtd_cursos = int(np.random.choice([1, 2, 3, 4, 5], p=[0.40, 0.30, 0.18, 0.09, 0.03]))

    # Propensão individual de presença (mistura de três perfis de engajamento):
    perfil = np.random.choice(["alto", "medio", "baixo"], p=[0.65, 0.23, 0.12])
    if perfil == "alto":
        prop = float(np.random.beta(10, 2))    # ~0,83
    elif perfil == "medio":
        prop = float(np.random.beta(5, 5))     # ~0,50
    else:
        prop = float(np.random.beta(1.5, 6))   # ~0,20 (frequência muito baixa)

    alunos.append(
        dict(id=i, nome=nome, idade=idade,
             escola=ESCOLAS[i % len(ESCOLAS)],
             serie=SERIES[i % len(SERIES)],
             cidade=CIDADES[i % len(CIDADES)],
             pontos=pontos, nivel=nivel_por_pontos(pontos),
             qtd_cursos=qtd_cursos, propensao=prop)
    )

# Outliers propositais e justificáveis na idade: jovens egressos/EJA atendidos
# pela ONG em caráter excepcional.
for idx, nova_idade in zip([7, 55, 118], [22, 23, 25]):
    alunos[idx]["idade"] = nova_idade

# Outliers propositais na pontuação: dois alunos "embaixadores" do app, com
# pontuação muito acima da média (indicam e acompanham turmas).
for idx, novos_pontos in zip([3, 91], [1480, 1610]):
    alunos[idx]["pontos"] = novos_pontos
    alunos[idx]["nivel"] = nivel_por_pontos(novos_pontos)

# -----------------------------------------------------------------------------
# 1.3 Inscrições, presenças e certificados
# -----------------------------------------------------------------------------
pesos = np.array([c["atratividade"] for c in cursos], dtype=float)
pesos = pesos / pesos.sum()

vagas_ocupadas = {c["id"]: 0 for c in cursos}
inscricoes = []
insc_id = 0

for a in alunos:
    escolhidos = list(
        np.random.choice([c["id"] for c in cursos], size=a["qtd_cursos"],
                         replace=False, p=pesos)
    )
    for cid in escolhidos:
        curso = cursos[cid - 1]
        if vagas_ocupadas[cid] >= curso["vagas_totais"]:
            continue  # turma lotada
        vagas_ocupadas[cid] += 1
        insc_id += 1

        n_enc = curso["num_encontros"]
        p_presenca = float(np.clip(a["propensao"] * FATOR_CURSO.get(cid, 1.0), 0.02, 0.98))
        presencas = int(np.random.binomial(n_enc, p_presenca))
        freq = round(presencas / n_enc * 100, 1)

        # Status (regra de negócio do projeto):
        #   frequência < 40%  -> desistente
        #   turma encerrada   -> concluido
        #   turma em andamento-> cursando
        if freq < 40:
            status = "desistente"
        elif curso["encerrada"]:
            status = "concluido"
        else:
            status = "cursando"

        # Certificado: só é emitido com conclusão E frequência mínima de 75%
        # (regra da ONG, espelhada na tabela `certificados`).
        certificado = "Sim" if (status == "concluido" and freq >= 75) else "Não"

        # Dias entre a inscrição e o primeiro encontro da turma.
        dias = int(np.clip(round(np.random.gamma(1.7, 11.5)), 0, 200))

        inscricoes.append(
            dict(id=insc_id, aluno_id=a["id"], curso_id=cid,
                 curso_titulo=curso["titulo"], status=status,
                 encontros=n_enc, presencas=presencas, frequencia=freq,
                 dias_insc=dias, certificado=certificado)
        )

# Outliers propositais no intervalo inscrição -> 1º encontro: alunos que se
# inscreveram com muita antecedência em turmas que só abriram meses depois.
for idx, novos_dias in zip([12, 140, 255], [118, 132, 147]):
    if idx < len(inscricoes):
        inscricoes[idx]["dias_insc"] = novos_dias

for c in cursos:
    c["vagas_ocupadas"] = vagas_ocupadas[c["id"]]

# Consistência da base: "qtd_cursos" passa a ser o número de inscrições EFETIVADAS
# (pode ser menor que o pretendido quando a turma escolhida já estava lotada —
# situação real, que o próprio app enfrenta).
contagem_insc = {a["id"]: 0 for a in alunos}
for i0 in inscricoes:
    contagem_insc[i0["aluno_id"]] += 1
for a in alunos:
    a["qtd_cursos"] = contagem_insc[a["id"]]

N_INSC = len(inscricoes)

# Linhas finais de cada base (usadas nas fórmulas)
LIN_AL = N_ALUNOS + 1      # alunos:      linhas 2..161
LIN_CU = N_CURSOS + 1      # cursos:      linhas 2..13
LIN_IN = N_INSC + 1        # inscrições:  linhas 2..N_INSC+1


# =============================================================================
# 2. NOMES DAS ABAS E DEFINIÇÃO DAS VARIÁVEIS QUANTITATIVAS ANALISADAS
# =============================================================================

AB_APRES = "0. Apresentação"
AB_ALUNOS = "1. Base - Alunos"
AB_CURSOS = "2. Base - Cursos"
AB_INSC = "3. Base - Inscrições"
AB_DIC = "4. Dicionário de Variáveis"
AB_CV = "5. Dispersão - CV"
AB_BOX = "6. Box Plot e Outliers"
AB_SEM = "7. Com e Sem Outliers"
AB_AUX = "8. Aux - Sem Outliers"
AB_PROB = "9. Probabilidades"
AB_CONC = "10. Conclusões"


class Variavel:
    """Variável quantitativa analisada: liga o nome ao intervalo real na planilha."""

    def __init__(self, nome, unidade, aba, col, lin_fim, dados, justificativa, decimais=1):
        self.nome = nome
        self.unidade = unidade
        self.aba = aba
        self.col = col
        self.lin_fim = lin_fim
        self.dados = np.array(dados, dtype=float)
        self.justificativa = justificativa
        self.decimais = decimais

    @property
    def rng(self):
        """Intervalo absoluto, pronto para uso em fórmula (ex.: '1. Base - Alunos'!C$2:C$161)."""
        return f"'{self.aba}'!${self.col}$2:${self.col}${self.lin_fim}"

    def cel(self, linha):
        return f"'{self.aba}'!${self.col}${linha}"


col_freq = [i["frequencia"] for i in inscricoes]
col_dias = [i["dias_insc"] for i in inscricoes]
taxa_ocup = [round(c["vagas_ocupadas"] / c["vagas_totais"] * 100, 1) for c in cursos]

VARIAVEIS = [
    Variavel("Idade do aluno", "anos", AB_ALUNOS, "C", LIN_AL,
             [a["idade"] for a in alunos],
             "Define o público atendido e condiciona a oferta de turmas por série."),
    Variavel("Pontos acumulados no app", "pontos", AB_ALUNOS, "G", LIN_AL,
             [a["pontos"] for a in alunos],
             "Principal indicador de engajamento com a gamificação do aplicativo."),
    Variavel("Nº de cursos por aluno", "cursos", AB_ALUNOS, "I", LIN_AL,
             [a["qtd_cursos"] for a in alunos],
             "Mede a profundidade do vínculo do jovem com a ONG."),
    Variavel("Carga horária do curso", "horas", AB_CURSOS, "E", LIN_CU,
             [c["carga_horaria"] for c in cursos],
             "Dimensiona o esforço exigido do aluno e o custo de oferta da turma."),
    Variavel("Nº de encontros por curso", "encontros", AB_CURSOS, "F", LIN_CU,
             [c["num_encontros"] for c in cursos],
             "Base de cálculo da frequência e do número de check-ins por QR Code."),
    Variavel("Vagas totais por curso", "vagas", AB_CURSOS, "G", LIN_CU,
             [c["vagas_totais"] for c in cursos],
             "Capacidade planejada da turma; define o custo fixo de sala e de educador."),
    Variavel("Vagas ocupadas por curso", "vagas", AB_CURSOS, "H", LIN_CU,
             [c["vagas_ocupadas"] for c in cursos],
             "Mede a procura real por cada curso ofertado."),
    Variavel("Taxa de ocupação do curso", "%", AB_CURSOS, "J", LIN_CU,
             taxa_ocup,
             "Indica aproveitamento das vagas e orienta a abertura/encerramento de turmas."),
    Variavel("Percentual de frequência (por inscrição)", "%", AB_INSC, "H", LIN_IN,
             col_freq,
             "Variável central do projeto: condiciona a conclusão e a emissão do certificado."),
    Variavel("Dias entre inscrição e 1º encontro", "dias", AB_INSC, "I", LIN_IN,
             col_dias,
             "Mede o tempo de espera do jovem, fator associado à desistência precoce."),
]

LIN_V0 = 5  # primeira linha de dados nas abas de análise
N_VAR = len(VARIAVEIS)
LIN_VF = LIN_V0 + N_VAR - 1


# =============================================================================
# 3. CÁLCULOS DE CONFERÊNCIA EM PYTHON
#    (servem para validar, ao final, o que as fórmulas do Excel devolvem)
# =============================================================================

def estatisticas(x):
    """Estatísticas descritivas no mesmo padrão das funções do Excel."""
    x = np.asarray(x, dtype=float)
    n = x.size
    media = float(np.mean(x))
    dp = float(np.std(x, ddof=1))                 # STDEV.S (amostral)
    cv = dp / media * 100 if media else float("nan")
    q1 = float(np.percentile(x, 25, method="linear"))   # QUARTILE.INC
    q2 = float(np.percentile(x, 50, method="linear"))
    q3 = float(np.percentile(x, 75, method="linear"))
    iqr = q3 - q1
    li = q1 - 1.5 * iqr
    ls = q3 + 1.5 * iqr
    out_b = x[x < li]
    out_a = x[x > ls]
    limpo = x[(x >= li) & (x <= ls)]
    m2 = float(np.mean(limpo)) if limpo.size else float("nan")
    dp2 = float(np.std(limpo, ddof=1)) if limpo.size > 1 else float("nan")
    cv2 = dp2 / m2 * 100 if limpo.size > 1 and m2 else float("nan")
    return dict(n=n, min=float(np.min(x)), max=float(np.max(x)), media=media, dp=dp, cv=cv,
                q1=q1, q2=q2, q3=q3, iqr=iqr, li=li, ls=ls,
                out_b=out_b, out_a=out_a, n_out=out_b.size + out_a.size,
                limpo=limpo, n_limpo=limpo.size, media2=m2, dp2=dp2, cv2=cv2)


STATS = {v.nome: estatisticas(v.dados) for v in VARIAVEIS}


def classificar(cv):
    if cv < 15:
        return "Baixa dispersão"
    if cv <= 30:
        return "Média dispersão"
    return "Alta dispersão"


# =============================================================================
# 4. GRÁFICOS (matplotlib)
# =============================================================================

plt.rcParams.update({
    "font.family": "DejaVu Sans",
    "font.size": 9,
    "figure.facecolor": C_SUPERFICIE,
    "axes.facecolor": C_SUPERFICIE,
    "axes.edgecolor": C_GRADE,
    "axes.labelcolor": C_TINTA_2,
    "text.color": C_TINTA,
    "xtick.color": C_MUDO,
    "ytick.color": C_MUDO,
    "savefig.facecolor": C_SUPERFICIE,
})


def _estilo_box(ax, bp):
    """Marcas finas, caixa azul, atípicos em vermelho (status) — padrão único."""
    for caixa in bp["boxes"]:
        caixa.set(facecolor=C_SERIE, alpha=0.25, edgecolor=C_SERIE, linewidth=1.6)
    for elem in ("whiskers", "caps"):
        for art in bp[elem]:
            art.set(color=C_MUDO, linewidth=1.3)
    for med in bp["medians"]:
        med.set(color=C_SERIE, linewidth=2.2)
    for fly in bp["fliers"]:
        fly.set(marker="D", markersize=4.5, markerfacecolor=C_CRITICO,
                markeredgecolor=C_SUPERFICIE, markeredgewidth=0.6, alpha=0.9)
    for m in bp.get("means", []):
        m.set(marker="o", markersize=5, markerfacecolor="#ffffff",
              markeredgecolor=C_TINTA_2, markeredgewidth=1.2)
    ax.spines["top"].set_visible(False)
    ax.spines["right"].set_visible(False)
    ax.spines["left"].set_visible(False)
    ax.grid(axis="x", color=C_GRADE, linewidth=0.8)
    ax.set_axisbelow(True)


def grafico_painel_boxplots(caminho):
    """Painel: um Box Plot por variável quantitativa (cada um na sua própria escala)."""
    ncols = 3
    nrows = -(-len(VARIAVEIS) // ncols)
    fig, axes = plt.subplots(nrows, ncols, figsize=(13.2, 3.05 * nrows))
    eixos = axes.ravel()
    for ax in eixos[len(VARIAVEIS):]:
        ax.set_visible(False)
    for ax, v in zip(eixos, VARIAVEIS):
        s = STATS[v.nome]
        bp = ax.boxplot(v.dados, orientation="horizontal", widths=0.55,
                        patch_artist=True, showmeans=True, whis=1.5)
        _estilo_box(ax, bp)
        ax.set_yticks([])
        ax.set_title(f"{v.nome}  ({v.unidade})", fontsize=9.5, color=C_TINTA,
                     loc="left", pad=8)
        ax.set_xlabel(
            f"n = {s['n']}   |   CV = {s['cv']:.1f}%   |   "
            f"{s['n_out']} atípico(s)", fontsize=8, color=C_TINTA_2)
    fig.suptitle(
        "Box Plot das variáveis quantitativas — Próxima Etapa (App do Aluno)",
        fontsize=13, fontweight="bold", color=C_TINTA, x=0.012, ha="left", y=0.985)
    fig.text(0.012, 0.962,
             "Critério de Tukey: atípico é todo valor fora de [Q1 − 1,5·IQR ; Q3 + 1,5·IQR]. "
             "Losango vermelho = valor atípico; círculo branco = média; linha azul = mediana.",
             fontsize=8.5, color=C_TINTA_2, ha="left")
    fig.tight_layout(rect=[0, 0, 1, 0.948])
    fig.savefig(caminho, dpi=150)
    plt.close(fig)


def grafico_box_detalhe(var, caminho, titulo, nota):
    """Box Plot ampliado de uma variável, com os limites de Tukey anotados."""
    s = STATS[var.nome]
    fig, ax = plt.subplots(figsize=(10.4, 3.5))
    bp = ax.boxplot(var.dados, orientation="horizontal", widths=0.45,
                    patch_artist=True, showmeans=True, whis=1.5)
    _estilo_box(ax, bp)
    ax.set_yticks([])
    for valor, rotulo, cor in [
        (s["li"], f"Lim. inf. {s['li']:.1f}", C_MUDO),
        (s["q1"], f"Q1 {s['q1']:.1f}", C_SERIE),
        (s["q2"], f"Mediana {s['q2']:.1f}", C_SERIE),
        (s["q3"], f"Q3 {s['q3']:.1f}", C_SERIE),
        (s["ls"], f"Lim. sup. {s['ls']:.1f}", C_CRITICO),
    ]:
        ax.axvline(valor, color=cor, linestyle=":", linewidth=1.1, alpha=0.75)
        ax.annotate(rotulo, xy=(valor, 1.33), fontsize=7.8, color=cor,
                    rotation=90, ha="center", va="bottom")
    ax.set_ylim(0.55, 1.85)
    ax.set_xlabel(f"{var.nome} ({var.unidade})", fontsize=9, color=C_TINTA_2)
    ax.set_title(titulo, fontsize=11.5, fontweight="bold", color=C_TINTA, loc="left", pad=10)
    fig.text(0.012, 0.015, nota, fontsize=8, color=C_TINTA_2, ha="left")
    fig.tight_layout(rect=[0, 0.06, 1, 1])
    fig.savefig(caminho, dpi=150)
    plt.close(fig)


def grafico_cv(caminho):
    """Barras horizontais do CV, coloridas pelo grau de dispersão (status + rótulo)."""
    ordenadas = sorted(VARIAVEIS, key=lambda v: STATS[v.nome]["cv"])
    nomes = [f"{v.nome} ({v.unidade})" for v in ordenadas]
    cvs = [STATS[v.nome]["cv"] for v in ordenadas]
    cores = [C_BOM if c < 15 else (C_ATENCAO if c <= 30 else C_CRITICO) for c in cvs]

    fig, ax = plt.subplots(figsize=(11.2, 5.4))
    y = np.arange(len(nomes))
    ax.barh(y, cvs, height=0.62, color=cores, edgecolor=C_SUPERFICIE, linewidth=1.4)
    ax.set_yticks(y)
    ax.set_yticklabels(nomes, fontsize=9, color=C_TINTA)

    for i, (c, cor) in enumerate(zip(cvs, cores)):
        ax.text(c + max(cvs) * 0.012, i, f"{c:.1f}%  {classificar(c).split()[0].lower()}",
                va="center", fontsize=8.6, color=C_TINTA_2)

    ax.set_ylim(-0.7, len(nomes) + 0.55)
    for lim, txt, desloc in [(15, "15% — limite da baixa dispersão", 0.52),
                             (30, "30% — limite da média dispersão", 0.10)]:
        ax.axvline(lim, color=C_MUDO, linestyle="--", linewidth=1.1)
        ax.text(lim + max(cvs) * 0.012, len(nomes) - desloc, txt, fontsize=7.8,
                color=C_MUDO, ha="left", va="center")

    ax.set_xlim(0, max(cvs) * 1.28)
    ax.set_xlabel("Coeficiente de Variação (%)", fontsize=9.5, color=C_TINTA_2)
    ax.spines["top"].set_visible(False)
    ax.spines["right"].set_visible(False)
    ax.spines["left"].set_visible(False)
    ax.grid(axis="x", color=C_GRADE, linewidth=0.8)
    ax.set_axisbelow(True)
    ax.set_title("Dispersão relativa das variáveis quantitativas (CV)",
                 fontsize=13, fontweight="bold", color=C_TINTA, loc="left", pad=12)
    fig.text(0.012, 0.015,
             "Verde = baixa dispersão (CV < 15%)   |   Amarelo = média (15% a 30%)   "
             "|   Vermelho = alta (CV > 30%). A classificação também está escrita ao lado de cada barra.",
             fontsize=8, color=C_TINTA_2, ha="left")
    fig.tight_layout(rect=[0, 0.055, 1, 1])
    fig.savefig(caminho, dpi=150)
    plt.close(fig)


def grafico_comparativo_cv(caminho):
    """Barras agrupadas: CV com e sem valores atípicos."""
    alvos = [v for v in VARIAVEIS if STATS[v.nome]["n_out"] > 0]
    nomes = [v.nome for v in alvos]
    com = [STATS[v.nome]["cv"] for v in alvos]
    sem = [STATS[v.nome]["cv2"] for v in alvos]

    fig, ax = plt.subplots(figsize=(11.2, 0.72 * len(nomes) + 1.9))
    y = np.arange(len(nomes))[::-1]   # primeira variável no topo
    larg = 0.37
    ax.barh(y + larg / 2, com, larg, label="Com valores atípicos",
            color=C_CRITICO, edgecolor=C_SUPERFICIE, linewidth=1.4)
    ax.barh(y - larg / 2, sem, larg, label="Sem valores atípicos",
            color=C_SERIE, edgecolor=C_SUPERFICIE, linewidth=1.4)
    desloc = max(com) * 0.012
    for yi, (a, b) in zip(y, zip(com, sem)):
        ax.text(a + desloc, yi + larg / 2, f"{a:.1f}%", va="center", fontsize=8.2, color=C_TINTA_2)
        ax.text(b + desloc, yi - larg / 2, f"{b:.1f}%", va="center", fontsize=8.2, color=C_TINTA_2)
    ax.set_yticks(y)
    ax.set_yticklabels(nomes, fontsize=9, color=C_TINTA)
    ax.set_xlim(0, max(com) * 1.16)
    ax.set_xlabel("Coeficiente de Variação (%)", fontsize=9.5, color=C_TINTA_2)
    ax.legend(frameon=False, fontsize=9, loc="lower right")
    ax.spines["top"].set_visible(False)
    ax.spines["right"].set_visible(False)
    ax.spines["left"].set_visible(False)
    ax.grid(axis="x", color=C_GRADE, linewidth=0.8)
    ax.set_axisbelow(True)
    ax.set_title("Efeito da remoção dos valores atípicos sobre a dispersão",
                 fontsize=13, fontweight="bold", color=C_TINTA, loc="left", pad=12)
    fig.tight_layout()
    fig.savefig(caminho, dpi=150)
    plt.close(fig)


IMG_PAINEL = os.path.join(PASTA, "boxplot_painel_variaveis.png")
IMG_FREQ = os.path.join(PASTA, "boxplot_frequencia.png")
IMG_CH = os.path.join(PASTA, "boxplot_carga_horaria.png")
IMG_CV = os.path.join(PASTA, "grafico_coeficiente_variacao.png")
IMG_COMP = os.path.join(PASTA, "grafico_com_sem_outliers.png")

def var_por_nome(nome):
    return next(v for v in VARIAVEIS if v.nome == nome)


VAR_FREQ = var_por_nome("Percentual de frequência (por inscrição)")
VAR_CH = var_por_nome("Carga horária do curso")

grafico_painel_boxplots(IMG_PAINEL)
grafico_box_detalhe(
    VAR_FREQ, IMG_FREQ,
    "Box Plot em detalhe — Percentual de frequência por inscrição",
    "Leitura: a cauda inferior concentra as inscrições de frequência muito baixa — "
    "justamente o grupo que não alcança os 75% exigidos para o certificado.")
grafico_box_detalhe(
    VAR_CH, IMG_CH,
    "Box Plot em detalhe — Carga horária dos cursos",
    "Leitura: a Formação Técnica em Desenvolvimento Web (120h) é um atípico legítimo — "
    "não é erro de digitação, é um curso de natureza diferente das oficinas curtas.")
grafico_cv(IMG_CV)
grafico_comparativo_cv(IMG_COMP)
print("Gráficos gerados com matplotlib.")


# =============================================================================
# 5. MONTAGEM DA PLANILHA
# =============================================================================

wb = openpyxl.Workbook()
wb.remove(wb.active)


def titulo_aba(ws, titulo, subtitulo=None):
    ws["A1"] = titulo
    ws["A1"].font = F_TITULO
    ws.row_dimensions[1].height = 19
    if subtitulo:
        ws["A2"] = subtitulo
        ws["A2"].font = F_SUB


def cabecalho(ws, linha, titulos, larguras=None, altura=30):
    for j, t in enumerate(titulos, start=1):
        c = ws.cell(linha, j, t)
        c.font, c.fill, c.alignment, c.border = F_CAB, FILL_CAB, AL_CAB, B_CAB
    ws.row_dimensions[linha].height = altura
    if larguras:
        for j, w in enumerate(larguras, start=1):
            ws.column_dimensions[get_column_letter(j)].width = w


def texto(ws, cel, valor, fonte=None, larg=None, altura=None, fmt=None, alinh=None):
    c = ws[cel]
    c.value = valor
    c.font = fonte or F_TXT
    if alinh:
        c.alignment = alinh
    if fmt:
        c.number_format = fmt
    return c


def paragrafo(ws, linha, txt, col_fim="H", fonte=None, altura=None):
    """Escreve um parágrafo mesclado de A{linha} até {col_fim}{linha}."""
    ws.merge_cells(f"A{linha}:{col_fim}{linha}")
    c = ws[f"A{linha}"]
    c.value = txt
    c.font = fonte or F_TXT
    c.alignment = Alignment(vertical="top", wrap_text=True)
    if altura:
        ws.row_dimensions[linha].height = altura
    return c


# -----------------------------------------------------------------------------
# 5.0 ABA — APRESENTAÇÃO
# -----------------------------------------------------------------------------
ws = wb.create_sheet(AB_APRES)
titulo_aba(ws, "Análise Descritiva de Dados — Entrega 2 (final)",
           "Projeto Interdisciplinar FECAP · Sistema “Próxima Etapa — App do Aluno”")
ws.column_dimensions["A"].width = 20
for col in "BCDEFGH":
    ws.column_dimensions[col].width = 16

linhas_apres = [
    ("sec", "1. O que é analisado nesta entrega"),
    ("txt", "Esta planilha dá continuidade à Entrega 1 (dicionário de variáveis, medidas de tendência "
            "central e percentil 95). Aqui o foco passa da posição dos dados para a sua VARIABILIDADE: "
            "coeficiente de variação, análise de outliers por Box Plot, comparação com e sem valores "
            "atípicos e cálculo de probabilidades de eventos de interesse da ONG."),
    ("sec", "2. Origem dos dados e LGPD"),
    ("txt", "Todos os dados desta planilha são FICTÍCIOS, gerados por simulação. Nenhum dado real de "
            "aluno foi utilizado — é uma exigência de LGPD assumida pelo grupo no projeto. A estrutura "
            "das tabelas reproduz fielmente o banco do sistema (src/Entrega 2/Backend/db/schema.sql): "
            "alunos, cursos, atividades (encontros), inscrições, presenças e certificados."),
    ("sec", "3. Por que a simulação foi AMPLIADA"),
    ("txt", f"O script de seed do back-end (seed.js) popula o banco com 30 alunos e 7 cursos — volume "
            f"suficiente para demonstrar o aplicativo, mas pequeno demais para uma análise de dispersão "
            f"ter significância estatística (com 7 cursos, por exemplo, o cálculo de quartis e a detecção "
            f"de outliers ficam instáveis). Por isso a simulação desta entrega usa o MESMO MODELO de "
            f"dados, com volume ampliado: {N_ALUNOS} alunos, {N_CURSOS} cursos e {N_INSC} inscrições. "
            f"As regras de negócio (faixa de carga horária, nº de encontros, presença por QR Code, "
            f"status da inscrição, certificado com 75% de frequência) são as mesmas do sistema."),
    ("sec", "4. Reprodutibilidade"),
    ("txt", f"A simulação usa semente fixa (random.seed = np.random.seed = {SEMENTE}). Executar novamente "
            f"o script gerar_analise_dispersao.py produz exatamente a mesma base e os mesmos resultados. "
            f"O script acompanha esta planilha na mesma pasta."),
    ("sec", "5. Fórmulas vivas"),
    ("txt", "Todos os indicadores das abas 5 a 9 são FÓRMULAS do Excel (AVERAGE, STDEV.S, QUARTILE.INC, "
            "COUNTIF, COUNTIFS, AVERAGEIFS, BINOM.DIST …) que apontam para as abas de base de dados. "
            "Se qualquer valor das abas 1, 2 ou 3 for alterado, médias, desvios padrão, coeficientes de "
            "variação, quartis, limites de outliers e probabilidades se recalculam automaticamente."),
    ("sec", "6. Critério de classificação da dispersão adotado"),
    ("txt", "CV < 15% → baixa dispersão (dados homogêneos) · 15% ≤ CV ≤ 30% → dispersão média · "
            "CV > 30% → alta dispersão (dados heterogêneos). Critério usual em estatística descritiva "
            "aplicada, adotado de forma uniforme em toda a planilha."),
    ("sec", "7. Roteiro das abas"),
]

lin = 4
for tipo, txt in linhas_apres:
    if tipo == "sec":
        ws[f"A{lin}"] = txt
        ws[f"A{lin}"].font = F_SECAO
        lin += 1
    else:
        alt = 15 * (1 + len(txt) // 115)
        paragrafo(ws, lin, txt, "H", F_TXT, max(30, alt))
        lin += 2

roteiro = [
    (AB_ALUNOS, f"Base de dados simulada dos alunos ({N_ALUNOS} registros)."),
    (AB_CURSOS, f"Base de dados simulada dos cursos ofertados ({N_CURSOS} registros)."),
    (AB_INSC, f"Base de dados das inscrições, com presenças e frequência ({N_INSC} registros)."),
    (AB_DIC, "Dicionário de variáveis: tipo, subtipo, unidade e descrição."),
    (AB_CV, "Medidas de dispersão e Coeficiente de Variação, com classificação do grau de dispersão."),
    (AB_BOX, "Quartis, IQR, limites de Tukey, contagem e listagem dos outliers + Box Plots."),
    (AB_SEM, "Comparação das medidas com e sem os valores atípicos."),
    (AB_AUX, "Área auxiliar de cálculo (série de cada variável já sem os valores atípicos)."),
    (AB_PROB, "Probabilidades de eventos relevantes para a ONG, incluindo distribuição binomial."),
    (AB_CONC, "Conclusões, explicações da variabilidade e recomendações práticas."),
]
cabecalho(ws, lin, ["Aba", "Conteúdo"], None, 24)
ws.column_dimensions["A"].width = 32
ws.merge_cells(f"B{lin}:H{lin}")
lin += 1
for nome, desc in roteiro:
    ws[f"A{lin}"] = nome
    ws[f"A{lin}"].font = F_NEG
    ws.merge_cells(f"B{lin}:H{lin}")
    ws[f"B{lin}"] = desc
    ws[f"B{lin}"].font = F_TXT
    ws[f"B{lin}"].alignment = AL_TXT
    lin += 1


# -----------------------------------------------------------------------------
# 5.1 ABA — BASE: ALUNOS
# -----------------------------------------------------------------------------
ws = wb.create_sheet(AB_ALUNOS)
cabecalho(ws, 1, ["id", "nome", "idade", "escola", "serie", "cidade", "pontos",
                  "nivel", "qtd_cursos", "media_frequencia_pct"],
          [6, 24, 8, 27, 8, 22, 9, 13, 11, 20], 32)
for k, a in enumerate(alunos):
    r = 2 + k
    ws.cell(r, 1, a["id"])
    ws.cell(r, 2, a["nome"])
    ws.cell(r, 3, a["idade"])
    ws.cell(r, 4, a["escola"])
    ws.cell(r, 5, a["serie"])
    ws.cell(r, 6, a["cidade"])
    ws.cell(r, 7, a["pontos"])
    ws.cell(r, 8, a["nivel"])
    # Nº de cursos: contado ao vivo na aba de inscrições (garante consistência).
    ws.cell(r, 9, f"=COUNTIF('{AB_INSC}'!$B$2:$B${LIN_IN},$A{r})")
    # Frequência média do aluno: calculada ao vivo a partir da aba de inscrições.
    c = ws.cell(r, 10,
                f"=IFERROR(ROUND(AVERAGEIF('{AB_INSC}'!$B$2:$B${LIN_IN},$A{r},"
                f"'{AB_INSC}'!$H$2:$H${LIN_IN}),1),\"\")")
    c.number_format = "0.0"
ws.freeze_panes = "A2"
ws.auto_filter.ref = f"A1:J{LIN_AL}"


# -----------------------------------------------------------------------------
# 5.2 ABA — BASE: CURSOS
# -----------------------------------------------------------------------------
ws = wb.create_sheet(AB_CURSOS)
cabecalho(ws, 1, ["id", "titulo", "categoria", "local", "carga_horaria",
                  "num_encontros", "vagas_totais", "vagas_ocupadas",
                  "vagas_disponiveis", "taxa_ocupacao_pct", "situacao_turma"],
          [6, 40, 13, 23, 14, 14, 13, 15, 16, 17, 16], 32)
for k, c0 in enumerate(cursos):
    r = 2 + k
    ws.cell(r, 1, c0["id"])
    ws.cell(r, 2, c0["titulo"])
    ws.cell(r, 3, c0["categoria"])
    ws.cell(r, 4, c0["local"])
    ws.cell(r, 5, c0["carga_horaria"])
    ws.cell(r, 6, c0["num_encontros"])
    ws.cell(r, 7, c0["vagas_totais"])
    ws.cell(r, 8, c0["vagas_ocupadas"])
    ws.cell(r, 9, f"=$G{r}-$H{r}")                        # vagas disponíveis (viva)
    cc = ws.cell(r, 10, f"=ROUND($H{r}/$G{r}*100,1)")     # taxa de ocupação (viva)
    cc.number_format = "0.0"
    ws.cell(r, 11, c0["situacao"])
ws.freeze_panes = "A2"
ws.auto_filter.ref = f"A1:K{LIN_CU}"


# -----------------------------------------------------------------------------
# 5.3 ABA — BASE: INSCRIÇÕES
# -----------------------------------------------------------------------------
ws = wb.create_sheet(AB_INSC)
cabecalho(ws, 1, ["id", "aluno_id", "curso_id", "curso_titulo", "status",
                  "encontros_curso", "presencas", "frequencia_pct",
                  "dias_insc_1o_encontro", "certificado_emitido"],
          [6, 10, 10, 40, 13, 15, 11, 14, 20, 18], 32)
for k, i0 in enumerate(inscricoes):
    r = 2 + k
    ws.cell(r, 1, i0["id"])
    ws.cell(r, 2, i0["aluno_id"])
    ws.cell(r, 3, i0["curso_id"])
    ws.cell(r, 4, i0["curso_titulo"])
    ws.cell(r, 5, i0["status"])
    ws.cell(r, 6, i0["encontros"])
    ws.cell(r, 7, i0["presencas"])
    cc = ws.cell(r, 8, f"=ROUND($G{r}/$F{r}*100,1)")      # frequência (viva)
    cc.number_format = "0.0"
    ws.cell(r, 9, i0["dias_insc"])
    ws.cell(r, 10, i0["certificado"])
ws.freeze_panes = "A2"
ws.auto_filter.ref = f"A1:J{LIN_IN}"


# -----------------------------------------------------------------------------
# 5.4 ABA — DICIONÁRIO DE VARIÁVEIS
# -----------------------------------------------------------------------------
ws = wb.create_sheet(AB_DIC)
titulo_aba(ws, "Dicionário de Variáveis — Entrega 2",
           "Continuação do dicionário da Entrega 1, agora com unidade de medida e marcação das "
           "variáveis efetivamente submetidas à análise de dispersão.")
cabecalho(ws, 4, ["Variável", "Origem (tabela.campo)", "Aba · coluna", "Tipo",
                  "Subtipo", "Unidade", "Descrição / observações",
                  "Analisada na dispersão?"],
          [34, 26, 26, 15, 22, 12, 56, 17], 34)

DICIONARIO = [
    # nome, origem, aba.coluna, tipo, subtipo, unidade, descricao, analisada
    ("id do aluno", "alunos.id", f"{AB_ALUNOS} · A", "Quantitativa", "Discreta (identificador)",
     "—", "Chave primária. Não tem significado estatístico.", "Não"),
    ("Nome do aluno", "alunos.nome", f"{AB_ALUNOS} · B", "Qualitativa", "Nominal",
     "—", "Identificação fictícia do aluno.", "Não"),
    ("Idade do aluno", "alunos (derivado de data de nascimento)", f"{AB_ALUNOS} · C",
     "Quantitativa", "Discreta", "anos",
     "Idade em anos completos. Público-alvo: Ensino Médio da rede pública (14 a 19 anos).", "Sim"),
    ("Escola", "alunos.escola", f"{AB_ALUNOS} · D", "Qualitativa", "Nominal",
     "—", "8 escolas públicas parceiras.", "Não"),
    ("Série escolar", "alunos.serie", f"{AB_ALUNOS} · E", "Qualitativa", "Ordinal",
     "—", "1º EM < 2º EM < 3º EM (há ordem natural).", "Não"),
    ("Cidade", "alunos.cidade", f"{AB_ALUNOS} · F", "Qualitativa", "Nominal",
     "—", "6 municípios da Região Metropolitana de São Paulo.", "Não"),
    ("Pontos acumulados no app", "alunos.pontos", f"{AB_ALUNOS} · G",
     "Quantitativa", "Discreta", "pontos",
     "Pontuação da gamificação (check-ins, conclusão de cursos, teste de perfil).", "Sim"),
    ("Nível de engajamento", "alunos.nivel", f"{AB_ALUNOS} · H", "Qualitativa", "Ordinal",
     "—", "Explorador < Em trilha < Referência. Derivado dos pontos.", "Não"),
    ("Nº de cursos por aluno", "COUNT(inscricoes) por aluno", f"{AB_ALUNOS} · I",
     "Quantitativa", "Discreta", "cursos",
     "Cursos em que a inscrição foi EFETIVADA (0 a 5). Pode ser zero quando todas as turmas "
     "pretendidas pelo jovem já estavam lotadas — situação real que o app enfrenta.", "Sim"),
    ("Frequência média do aluno", "AVG(presencas/encontros) por aluno", f"{AB_ALUNOS} · J",
     "Quantitativa", "Contínua", "%",
     "Média das frequências do aluno em todos os cursos. Usada na aba de probabilidades.", "Não"),
    ("id do curso", "cursos.id", f"{AB_CURSOS} · A", "Quantitativa", "Discreta (identificador)",
     "—", "Chave primária do curso.", "Não"),
    ("Título do curso", "cursos.titulo", f"{AB_CURSOS} · B", "Qualitativa", "Nominal",
     "—", "Nome do curso ofertado.", "Não"),
    ("Categoria do curso", "cursos.categoria", f"{AB_CURSOS} · C", "Qualitativa", "Nominal",
     "—", "Tecnologia, Educação, Carreira, Idiomas, Inovação.", "Não"),
    ("Local do curso", "cursos.local", f"{AB_CURSOS} · D", "Qualitativa", "Nominal",
     "—", "FECAP - Liberdade, Online ou Espaço Próxima Etapa.", "Não"),
    ("Carga horária do curso", "cursos.carga_horaria", f"{AB_CURSOS} · E",
     "Quantitativa", "Contínua (registrada em horas cheias)", "horas",
     "Duração total do curso. Vai de oficinas de 8h a uma formação técnica de 120h.", "Sim"),
    ("Nº de encontros por curso", "COUNT(atividades) por curso", f"{AB_CURSOS} · F",
     "Quantitativa", "Discreta", "encontros",
     "Número de atividades/aulas da turma. É o denominador da frequência.", "Sim"),
    ("Vagas totais por curso", "cursos.vagas_totais", f"{AB_CURSOS} · G", "Quantitativa", "Discreta",
     "vagas", "Capacidade máxima planejada da turma (20 a 40 vagas).", "Sim"),
    ("Vagas ocupadas por curso", "cursos.vagas_totais − vagas_disponiveis", f"{AB_CURSOS} · H",
     "Quantitativa", "Discreta", "vagas",
     "Número de inscrições efetivadas na turma.", "Sim"),
    ("Vagas disponíveis", "cursos.vagas_disponiveis", f"{AB_CURSOS} · I",
     "Quantitativa", "Discreta", "vagas", "Vagas ainda abertas (fórmula = totais − ocupadas).", "Não"),
    ("Taxa de ocupação do curso", "vagas_ocupadas / vagas_totais", f"{AB_CURSOS} · J",
     "Quantitativa", "Contínua", "%",
     "Percentual de preenchimento da turma. Orienta abrir, manter ou cancelar turmas.", "Sim"),
    ("Situação da turma", "derivado de cursos.data_fim", f"{AB_CURSOS} · K",
     "Qualitativa", "Nominal", "—", "Encerrada ou Em andamento.", "Não"),
    ("id da inscrição", "inscricoes.id", f"{AB_INSC} · A", "Quantitativa", "Discreta (identificador)",
     "—", "Chave primária da inscrição (par aluno × curso).", "Não"),
    ("Status da inscrição", "inscricoes.status", f"{AB_INSC} · E", "Qualitativa", "Ordinal",
     "—", "desistente < cursando < concluido. Regra: frequência < 40% ⇒ desistente; "
          "turma encerrada ⇒ concluido; turma em andamento ⇒ cursando.", "Não"),
    ("Nº de presenças", "COUNT(presencas) por inscrição", f"{AB_INSC} · G",
     "Quantitativa", "Discreta", "presenças",
     "Check-ins por QR Code registrados pelo aluno naquele curso.", "Não"),
    ("Percentual de frequência (por inscrição)", "presencas / atividades do curso",
     f"{AB_INSC} · H", "Quantitativa", "Contínua", "%",
     "Variável central da entrega: define conclusão e emissão de certificado (mínimo de 75%).", "Sim"),
    ("Dias entre inscrição e 1º encontro", "atividades.data − inscricoes.criado_em",
     f"{AB_INSC} · I", "Quantitativa", "Discreta (contagem de dias)", "dias",
     "Tempo de espera do jovem entre se inscrever e a primeira aula.", "Sim"),
    ("Certificado emitido", "EXISTS(certificados)", f"{AB_INSC} · J",
     "Qualitativa", "Nominal (dicotômica)", "—",
     "Sim/Não. Exige status 'concluido' E frequência ≥ 75%.", "Não"),
]

for k, linha in enumerate(DICIONARIO):
    r = 5 + k
    for j, val in enumerate(linha, start=1):
        c = ws.cell(r, j, val)
        c.font = F_TXT
        c.alignment = AL_TXT
    if linha[7] == "Sim":
        for j in range(1, 9):
            ws.cell(r, j).fill = PatternFill("solid", start_color="EDF3FA")
        ws.cell(r, 1).font = F_NEG
        ws.cell(r, 8).font = F_NEG
    ws.row_dimensions[r].height = 30
ws.freeze_panes = "A5"

lin = 5 + len(DICIONARIO) + 1
paragrafo(ws, lin,
          f"Observação: as {N_VAR} variáveis destacadas em azul são as submetidas à análise de dispersão, "
          "Box Plot e probabilidades. Identificadores (id) e variáveis qualitativas não entram no "
          "cálculo do Coeficiente de Variação — o CV só tem sentido para variáveis quantitativas "
          "medidas em escala de razão, com média diferente de zero.",
          "H", F_NOTA, 32)


# -----------------------------------------------------------------------------
# 5.5 ABA — DISPERSÃO / COEFICIENTE DE VARIAÇÃO
# -----------------------------------------------------------------------------
ws = wb.create_sheet(AB_CV)
titulo_aba(ws, "Medidas de dispersão e Coeficiente de Variação (CV)",
           "Todas as células de cálculo são fórmulas vivas que apontam para as abas 1, 2 e 3. "
           "Critério de classificação: CV < 15% = baixa dispersão · 15% ≤ CV ≤ 30% = média "
           "dispersão · CV > 30% = alta dispersão.")
cabecalho(ws, 4, ["Variável quantitativa", "Unidade", "n (observações)", "Mínimo", "Máximo",
                  "Amplitude", "Média", "Desvio padrão (amostral)", "Variância",
                  "Coeficiente de Variação (%)", "Classificação do grau de dispersão",
                  "Por que esta variável importa para a ONG"],
          [40, 11, 14, 10, 10, 11, 11, 15, 13, 16, 24, 52], 44)

for k, v in enumerate(VARIAVEIS):
    r = LIN_V0 + k
    ws.cell(r, 1, v.nome).font = F_NEG
    ws.cell(r, 2, v.unidade).alignment = AL_CENTRO
    ws.cell(r, 3, f"=COUNT({v.rng})")
    ws.cell(r, 4, f"=MIN({v.rng})")
    ws.cell(r, 5, f"=MAX({v.rng})")
    ws.cell(r, 6, f"=$E{r}-$D{r}")
    ws.cell(r, 7, f"=ROUND(AVERAGE({v.rng}),2)")
    ws.cell(r, 8, f"=ROUND({XFN}STDEV.S({v.rng}),2)")
    ws.cell(r, 9, f"=ROUND({XFN}VAR.S({v.rng}),2)")
    ws.cell(r, 10, f"=ROUND({XFN}STDEV.S({v.rng})/AVERAGE({v.rng})*100,2)")
    ws.cell(r, 11, f'=IF($J{r}<15,"Baixa dispersão",IF($J{r}<=30,"Média dispersão","Alta dispersão"))')
    ws.cell(r, 12, v.justificativa)

    for j in (4, 5, 6, 7, 8, 9):
        ws.cell(r, j).number_format = "0.00"
    ws.cell(r, 10).number_format = '0.00"%"'
    ws.cell(r, 10).font = F_NEG
    ws.cell(r, 11).alignment = AL_CENTRO
    ws.cell(r, 12).font = F_TXT
    ws.cell(r, 12).alignment = AL_TXT
    ws.row_dimensions[r].height = 30
ws.freeze_panes = "A5"

# Destaque visual VIVO: formatação condicional sobre a linha inteira, baseada no CV.
faixa = f"A{LIN_V0}:L{LIN_VF}"
ws.conditional_formatting.add(
    faixa, FormulaRule(formula=[f"$J{LIN_V0}>30"], fill=FILL_ALTA,
                       font=Font(name="Calibri", size=11, color="9C0006"), stopIfTrue=False))
ws.conditional_formatting.add(
    faixa, FormulaRule(formula=[f"AND($J{LIN_V0}>=15,$J{LIN_V0}<=30)"], fill=FILL_MEDIA,
                       font=Font(name="Calibri", size=11, color="9C6500"), stopIfTrue=False))
ws.conditional_formatting.add(
    faixa, FormulaRule(formula=[f"$J{LIN_V0}<15"], fill=FILL_BAIXA,
                       font=Font(name="Calibri", size=11, color="375623"), stopIfTrue=False))

lin = LIN_VF + 2
ws[f"A{lin}"] = "Legenda do critério de classificação (destaque aplicado automaticamente pelo Excel)"
ws[f"A{lin}"].font = F_SECAO
lin += 1
for faixa_txt, desc, fill, cor in [
    ("CV < 15%", "Baixa dispersão — dados homogêneos; a média representa bem o conjunto.",
     FILL_BAIXA, "375623"),
    ("15% ≤ CV ≤ 30%", "Dispersão média — há variabilidade relevante, mas ainda controlada.",
     FILL_MEDIA, "9C6500"),
    ("CV > 30%", "ALTA DISPERSÃO — dados heterogêneos; a média isolada é um resumo pobre e "
                 "a gestão precisa olhar subgrupos.", FILL_ALTA, "9C0006"),
]:
    ws[f"A{lin}"] = faixa_txt
    ws[f"A{lin}"].font = Font(name="Calibri", size=11, bold=True, color=cor)
    ws[f"A{lin}"].fill = fill
    ws[f"A{lin}"].alignment = AL_CENTRO
    ws.merge_cells(f"B{lin}:L{lin}")
    ws[f"B{lin}"] = desc
    ws[f"B{lin}"].font = F_TXT
    ws[f"B{lin}"].alignment = AL_TXT
    ws[f"B{lin}"].fill = fill
    lin += 1

lin += 1
ws[f"A{lin}"] = "Resposta ao item 2 do enunciado — variáveis com GRANDE dispersão"
ws[f"A{lin}"].font = F_SECAO
lin += 1
altas = [v for v in VARIAVEIS if STATS[v.nome]["cv"] > 30]
medias = [v for v in VARIAVEIS if 15 <= STATS[v.nome]["cv"] <= 30]
baixas = [v for v in VARIAVEIS if STATS[v.nome]["cv"] < 15]
txt_altas = "; ".join(f"{v.nome} (CV = {STATS[v.nome]['cv']:.2f}%)" for v in altas)
paragrafo(ws, lin,
          f"Apresentam ALTA dispersão (CV > 30%): {txt_altas}. "
          f"Dispersão média: " + ("; ".join(f"{v.nome} (CV = {STATS[v.nome]['cv']:.2f}%)" for v in medias) or "nenhuma") +
          ". Baixa dispersão: " + ("; ".join(f"{v.nome} (CV = {STATS[v.nome]['cv']:.2f}%)" for v in baixas) or "nenhuma") +
          ". As explicações e as medidas de redução da variabilidade estão na aba "
          f"“{AB_CONC}”.", "L", F_TXT, 48)
lin += 2

# Gráfico NATIVO do Excel (recalcula junto com as fórmulas)
graf = BarChart()
graf.type = "bar"
graf.title = "Coeficiente de Variação por variável (%)"
graf.y_axis.title = "CV (%)"
graf.x_axis.title = None
graf.height, graf.width = 10.5, 20
dados_ref = Reference(ws, min_col=10, min_row=4, max_row=LIN_VF)
cats_ref = Reference(ws, min_col=1, min_row=LIN_V0, max_row=LIN_VF)
graf.add_data(dados_ref, titles_from_data=True)
graf.set_categories(cats_ref)
graf.legend = None
ws.add_chart(graf, f"A{lin}")

# Gráfico em imagem (matplotlib), com a classificação escrita em cada barra
img = XLImage(IMG_CV)
img.width, img.height = 900, 434
ws.add_image(img, f"A{lin + 22}")


# -----------------------------------------------------------------------------
# 5.6 ABA — BOX PLOT E OUTLIERS
# -----------------------------------------------------------------------------
ws = wb.create_sheet(AB_BOX)
titulo_aba(ws, "Box Plot e análise exploratória dos outliers",
           "Critério de Tukey (o mesmo que o Box Plot desenha): é atípico todo valor fora do "
           "intervalo [Q1 − 1,5·IQR ; Q3 + 1,5·IQR], em que IQR = Q3 − Q1. Quartis calculados com "
           "QUARTILE.INC, idêntico ao método usado pelo Box Plot do matplotlib.")
cabecalho(ws, 4, ["Variável quantitativa", "Unidade", "n", "Mínimo", "Q1", "Mediana (Q2)", "Q3",
                  "Máximo", "IQR (Q3 − Q1)", "Limite inferior (Q1 − 1,5·IQR)",
                  "Limite superior (Q3 + 1,5·IQR)", "Outliers abaixo", "Outliers acima",
                  "Total de outliers", "% de outliers", "Valores atípicos observados"],
          [40, 10, 8, 9, 10, 12, 10, 9, 13, 18, 18, 11, 11, 12, 11, 60], 46)


def listar_outliers(v):
    s = STATS[v.nome]
    if s["n_out"] == 0:
        return "Nenhum valor atípico identificado por este critério."
    dec = 0 if v.unidade in ("anos", "pontos", "cursos", "horas", "encontros", "vagas", "dias") else 1
    baixos = sorted(s["out_b"])
    altos = sorted(s["out_a"], reverse=True)
    partes = []
    if baixos:
        amostra = baixos[:8]
        txt = ", ".join(f"{x:.{dec}f}" for x in amostra)
        if len(baixos) > 8:
            txt += f" … (+{len(baixos) - 8})"
        partes.append(f"Abaixo do limite ({len(baixos)}): {txt}")
    if altos:
        amostra = altos[:8]
        txt = ", ".join(f"{x:.{dec}f}" for x in amostra)
        if len(altos) > 8:
            txt += f" … (+{len(altos) - 8})"
        partes.append(f"Acima do limite ({len(altos)}): {txt}")
    return " | ".join(partes)


for k, v in enumerate(VARIAVEIS):
    r = LIN_V0 + k
    ws.cell(r, 1, v.nome).font = F_NEG
    ws.cell(r, 2, v.unidade).alignment = AL_CENTRO
    ws.cell(r, 3, f"=COUNT({v.rng})")
    ws.cell(r, 4, f"=MIN({v.rng})")
    ws.cell(r, 5, f"=ROUND({XFN}QUARTILE.INC({v.rng},1),2)")
    ws.cell(r, 6, f"=ROUND(MEDIAN({v.rng}),2)")
    ws.cell(r, 7, f"=ROUND({XFN}QUARTILE.INC({v.rng},3),2)")
    ws.cell(r, 8, f"=MAX({v.rng})")
    ws.cell(r, 9, f"=ROUND($G{r}-$E{r},2)")
    ws.cell(r, 10, f"=ROUND($E{r}-1.5*$I{r},2)")
    ws.cell(r, 11, f"=ROUND($G{r}+1.5*$I{r},2)")
    ws.cell(r, 12, f'=COUNTIF({v.rng},"<"&$J{r})')
    ws.cell(r, 13, f'=COUNTIF({v.rng},">"&$K{r})')
    ws.cell(r, 14, f"=$L{r}+$M{r}")
    ws.cell(r, 15, f"=ROUND($N{r}/$C{r}*100,2)")
    ws.cell(r, 16, listar_outliers(v))

    for j in (4, 5, 6, 7, 8, 9, 10, 11):
        ws.cell(r, j).number_format = "0.00"
    ws.cell(r, 15).number_format = '0.00"%"'
    ws.cell(r, 14).font = F_NEG
    for j in (3, 12, 13, 14, 15):
        ws.cell(r, j).alignment = AL_CENTRO
    ws.cell(r, 16).font = F_TXT
    ws.cell(r, 16).alignment = AL_TXT
    ws.row_dimensions[r].height = 32
ws.freeze_panes = "A5"

ws.conditional_formatting.add(
    f"A{LIN_V0}:P{LIN_VF}",
    FormulaRule(formula=[f"$N{LIN_V0}>0"], fill=FILL_ALTA, stopIfTrue=False))

lin = LIN_VF + 2
ws[f"A{lin}"] = "Análise exploratória: o que cada grupo de outliers significa no contexto do projeto"
ws[f"A{lin}"].font = F_SECAO
lin += 1
cabecalho(ws, lin, ["Variável", "Natureza do atípico", "Interpretação e decisão de tratamento"],
          None, 26)
ws.column_dimensions["A"].width = 40
lin += 1

INTERPRETA = [
    ("Idade do aluno",
     "Atípico legítimo (não é erro)",
     "Jovens acima de 20 anos atendidos em caráter excepcional (egressos / EJA), fora da faixa "
     "típica de 14 a 19 anos do Ensino Médio. Devem ser MANTIDOS na base, pois representam um "
     "público real da ONG, mas convém reportar as estatísticas de idade separando os dois grupos. "
     "Atenção metodológica: em variáveis discretas de amplitude curta como esta, o IQR é pequeno e "
     "o critério de Tukey tende a sinalizar como atípicos valores que são apenas pouco frequentes — "
     "por isso a leitura do Box Plot nunca substitui o conhecimento do contexto."),
    ("Pontos acumulados no app",
     "Atípico legítimo de cauda longa",
     "Dois alunos “embaixadores” com pontuação muito acima da média: usam o app intensamente, "
     "indicam colegas e comparecem a quase todos os encontros. Puxam a média para cima e tornam o "
     "CV altíssimo. Para comunicar engajamento típico, a MEDIANA é mais honesta que a média."),
    ("Carga horária do curso",
     "Atípico estrutural",
     "A Formação Técnica em Desenvolvimento Web (120h / 24 encontros) é de natureza diferente das "
     "oficinas de 8h a 30h. Não é erro de digitação: é outra categoria de produto. O correto é "
     "analisar carga horária separando “oficinas curtas” de “formações longas”."),
    ("Nº de encontros por curso",
     "Atípico estrutural (mesma causa)",
     "Decorre do mesmo curso de 24 encontros. Afeta diretamente o cálculo de frequência: em uma "
     "turma de 24 encontros é muito mais difícil manter 75% de presença do que em uma de 2."),
    ("Dias entre inscrição e 1º encontro",
     "Atípico de processo",
     "Inscrições feitas com mais de 100 dias de antecedência, em turmas que só abriram meses "
     "depois. É um sinal de risco: espera longa está associada a desistência precoce. Aqui o "
     "atípico aponta um problema de PROCESSO a ser corrigido, não um dado a ser descartado."),
    ("Nº de cursos por aluno",
     "Atípico de engajamento",
     "Alunos inscritos em 5 cursos simultâneos, enquanto a maioria cursa 1 ou 2. São jovens "
     "altamente engajados — mas também o grupo de maior risco de sobrecarga e, por consequência, "
     "de frequência baixa em algum dos cursos. Vale acompanhá-los individualmente pelo app."),
    ("Vagas ocupadas / Taxa de ocupação",
     "Atípico de demanda",
     "As turmas com procura muito baixa (Orientação Profissional e Cidadania e Projeto de Vida) "
     "destoam do restante do catálogo. Não é erro: é sinal de desalinhamento entre a oferta e o "
     "interesse do público, e é o insight mais acionável da análise — o curso existe, mas não "
     "chega a quem se interessaria por ele."),
    ("Percentual de frequência (por inscrição)",
     "Verificar na tabela ao lado",
     "A frequência tem cauda inferior longa (alunos que praticamente abandonaram o curso). "
     "Dependendo dos quartis, o critério de Tukey pode não sinalizar esses casos como atípicos — "
     "o que não os torna menos graves: eles são tratados na aba de probabilidades."),
]
for nome, natureza, interp in INTERPRETA:
    ws.cell(lin, 1, nome).font = F_NEG
    ws.cell(lin, 1).alignment = AL_TXT
    ws.cell(lin, 2, natureza).font = F_TXT
    ws.cell(lin, 2).alignment = AL_TXT
    ws.column_dimensions["B"].width = 28
    ws.merge_cells(f"C{lin}:P{lin}")
    ws.cell(lin, 3, interp).font = F_TXT
    ws.cell(lin, 3).alignment = AL_TXT
    ws.row_dimensions[lin].height = 46
    lin += 1

lin += 1
ws[f"A{lin}"] = f"Box Plot das {N_VAR} variáveis quantitativas analisadas"
ws[f"A{lin}"].font = F_SECAO
lin += 1
paragrafo(ws, lin,
          "Gráficos gerados com matplotlib pelo script gerar_analise_dispersao.py e incorporados à "
          "planilha (o Excel, via openpyxl, não possui Box Plot nativo). Os arquivos .png também "
          "ficam salvos na mesma pasta desta planilha, para uso no relatório do projeto.",
          "P", F_NOTA, 30)
lin += 2

_lp, _ap = XLImage(IMG_PAINEL).width, XLImage(IMG_PAINEL).height
img = XLImage(IMG_PAINEL)
img.width, img.height = 1060, int(1060 * _ap / _lp)
ws.add_image(img, f"A{lin}")
lin += int(img.height / 19) + 3

img = XLImage(IMG_FREQ)
img.width, img.height = 900, 303
ws.add_image(img, f"A{lin}")
lin += 17

img = XLImage(IMG_CH)
img.width, img.height = 900, 303
ws.add_image(img, f"A{lin}")


# -----------------------------------------------------------------------------
# 5.7 ABA — ÁREA AUXILIAR (SÉRIES SEM OS VALORES ATÍPICOS)
# -----------------------------------------------------------------------------
ws_aux = wb.create_sheet(AB_AUX)
titulo_aba(ws_aux, "Área auxiliar de cálculo — séries sem os valores atípicos",
           "Cada coluna repete a variável original, porém devolve vazio (\"\") quando o valor cai fora "
           "dos limites de Tukey calculados na aba “6. Box Plot e Outliers”. As funções AVERAGE, "
           "STDEV.S e COUNT ignoram células de texto, de modo que a aba 7 obtém as medidas SEM "
           "atípicos com fórmulas vivas, sem precisar apagar nenhum dado da base original.")
cabecalho(ws_aux, 4, [f"{v.nome} ({v.unidade})" for v in VARIAVEIS],
          [20] * N_VAR, 46)

MAX_LIN_AUX = max(int(STATS[v.nome]["n"]) for v in VARIAVEIS)
for k, v in enumerate(VARIAVEIS):
    col = k + 1
    n = int(STATS[v.nome]["n"])
    lim_inf = f"'{AB_BOX}'!$J${LIN_V0 + k}"
    lim_sup = f"'{AB_BOX}'!$K${LIN_V0 + k}"
    for i in range(n):
        linha_origem = 2 + i
        ref = v.cel(linha_origem)
        c = ws_aux.cell(5 + i, col,
                        f"=IF(AND({ref}>={lim_inf},{ref}<={lim_sup}),{ref},\"\")")
        c.number_format = "0.0"
ws_aux.freeze_panes = "A5"
LIN_AUX_FIM = 5 + MAX_LIN_AUX - 1


# -----------------------------------------------------------------------------
# 5.8 ABA — COMPARAÇÃO COM E SEM OUTLIERS
# -----------------------------------------------------------------------------
ws = wb.create_sheet(AB_SEM)
titulo_aba(ws, "Comparação das medidas COM e SEM os valores atípicos",
           "As colunas “com” leem a base original; as colunas “sem” leem a aba auxiliar 8, onde os "
           "valores fora dos limites de Tukey foram suprimidos. Toda a comparação é feita por "
           "fórmulas vivas — nenhum dado foi apagado da base.")
cabecalho(ws, 4, ["Variável quantitativa", "Unidade",
                  "n (com)", "Média (com)", "Desvio padrão (com)", "CV (com) %",
                  "n (sem)", "Média (sem)", "Desvio padrão (sem)", "CV (sem) %",
                  "Nº de atípicos removidos", "Variação da média (%)",
                  "Variação do desvio padrão (%)", "Variação do CV (p.p.)",
                  "Conclusão sobre a concentração dos dados"],
          [40, 10, 10, 12, 14, 12, 10, 12, 14, 12, 14, 14, 16, 14, 62], 48)

for k, v in enumerate(VARIAVEIS):
    r = LIN_V0 + k
    col_aux = get_column_letter(k + 1)
    rng_aux = f"'{AB_AUX}'!${col_aux}$5:${col_aux}${LIN_AUX_FIM}"
    s = STATS[v.nome]

    ws.cell(r, 1, v.nome).font = F_NEG
    ws.cell(r, 2, v.unidade).alignment = AL_CENTRO
    ws.cell(r, 3, f"=COUNT({v.rng})")
    ws.cell(r, 4, f"=ROUND(AVERAGE({v.rng}),2)")
    ws.cell(r, 5, f"=ROUND({XFN}STDEV.S({v.rng}),2)")
    ws.cell(r, 6, f"=ROUND({XFN}STDEV.S({v.rng})/AVERAGE({v.rng})*100,2)")
    ws.cell(r, 7, f"=COUNT({rng_aux})")
    ws.cell(r, 8, f"=ROUND(AVERAGE({rng_aux}),2)")
    ws.cell(r, 9, f"=ROUND({XFN}STDEV.S({rng_aux}),2)")
    ws.cell(r, 10, f"=ROUND({XFN}STDEV.S({rng_aux})/AVERAGE({rng_aux})*100,2)")
    ws.cell(r, 11, f"=$C{r}-$G{r}")
    ws.cell(r, 12, f"=IF($D{r}=0,\"\",ROUND(($H{r}-$D{r})/$D{r}*100,2))")
    ws.cell(r, 13, f"=IF($E{r}=0,\"\",ROUND(($I{r}-$E{r})/$E{r}*100,2))")
    ws.cell(r, 14, f"=ROUND($J{r}-$F{r},2)")

    if s["n_out"] == 0:
        concl = ("Sem valores atípicos: as medidas não mudam. Os dados já estavam contidos nos "
                 "limites de Tukey, o que indica distribuição razoavelmente concentrada.")
    else:
        red = (s["cv"] - s["cv2"])
        concl = (
            f"Retirando {s['n_out']} valor(es) atípico(s), o CV cai de {s['cv']:.2f}% para "
            f"{s['cv2']:.2f}% ({red:.2f} p.p. de redução, "
            f"{red / s['cv'] * 100:.1f}% do CV original) e o desvio padrão cai "
            f"{(s['dp'] - s['dp2']) / s['dp'] * 100:.1f}%. "
            + ("Ou seja: quase toda a variabilidade medida vinha de poucas observações extremas — "
               "o corpo dos dados é bem mais concentrado do que a média e o desvio padrão brutos "
               "sugerem." if red / s["cv"] > 0.25 else
               "A redução é modesta: a variabilidade é intrínseca à variável e está distribuída "
               "ao longo de toda a série, não concentrada em poucos casos extremos."))
    ws.cell(r, 15, concl).font = F_TXT
    ws.cell(r, 15).alignment = AL_TXT

    for j in (4, 5, 8, 9, 12, 13):
        ws.cell(r, j).number_format = "0.00"
    for j in (6, 10):
        ws.cell(r, j).number_format = '0.00"%"'
    ws.cell(r, 14).number_format = '+0.00";"-0.00";"0.00'
    for j in (3, 7, 11):
        ws.cell(r, j).alignment = AL_CENTRO
    ws.row_dimensions[r].height = 48
ws.freeze_panes = "A5"

ws.conditional_formatting.add(
    f"J{LIN_V0}:J{LIN_VF}",
    FormulaRule(formula=[f"$J{LIN_V0}<15"], fill=FILL_BAIXA, stopIfTrue=False))
ws.conditional_formatting.add(
    f"F{LIN_V0}:F{LIN_VF}",
    FormulaRule(formula=[f"$F{LIN_V0}>30"], fill=FILL_ALTA, stopIfTrue=False))

lin = LIN_VF + 2
ws[f"A{lin}"] = "Conclusão geral sobre a concentração dos dados (item 5 do enunciado)"
ws[f"A{lin}"].font = F_SECAO
lin += 1

maior_efeito = max(VARIAVEIS, key=lambda v: STATS[v.nome]["cv"] - STATS[v.nome]["cv2"]
                   if STATS[v.nome]["n_out"] else -1)
se = STATS[maior_efeito.nome]
total_out = sum(STATS[v.nome]["n_out"] for v in VARIAVEIS)
total_obs = sum(int(STATS[v.nome]["n"]) for v in VARIAVEIS)

TEXTOS_CONC = [
    f"Ao todo, o critério de Tukey identificou {total_out} valores atípicos em {total_obs} "
    f"observações analisadas ({total_out / total_obs * 100:.2f}% do total). É uma proporção pequena, "
    f"mas com efeito desproporcional sobre as medidas de dispersão — e é exatamente isso que a "
    f"comparação acima demonstra.",

    f"O caso mais expressivo é “{maior_efeito.nome}”: com os atípicos, o CV é de {se['cv']:.2f}%; "
    f"sem eles, cai para {se['cv2']:.2f}% — uma redução de {se['cv'] - se['cv2']:.2f} pontos "
    f"percentuais. A média sai de {se['media']:.2f} para {se['media2']:.2f} e o desvio padrão de "
    f"{se['dp']:.2f} para {se['dp2']:.2f}. Poucas observações, portanto, respondiam pela maior "
    f"parte da variabilidade aparente.",

    "Conclusão sobre concentração: nas variáveis em que a remoção dos atípicos derruba o CV de "
    "forma acentuada (pontos acumulados, carga horária, nº de encontros, dias de espera), os dados "
    "são na verdade BEM CONCENTRADOS em torno de um valor central — o CV alto era produzido por uma "
    "cauda curta de casos extremos, e não por heterogeneidade geral. Já nas variáveis em que o CV "
    "permanece alto mesmo sem atípicos (percentual de frequência e nº de cursos por aluno), a "
    "heterogeneidade é REAL e estrutural: existem de fato perfis de aluno muito diferentes "
    "convivendo na mesma base, e nenhuma limpeza de dados vai eliminar isso.",

    "Consequência prática para a análise: a média aritmética, isolada, é um resumo ruim para a "
    "maioria dessas variáveis. Relatórios de gestão da ONG devem apresentar mediana e quartis ao "
    "lado da média, e segmentar os indicadores por perfil de curso (oficina curta × formação longa) "
    "e por perfil de aluno (alta, média e baixa frequência).",

    "Decisão metodológica adotada: NENHUM valor atípico foi excluído da base. Todos foram "
    "classificados como legítimos (idade excepcional, aluno embaixador, curso de natureza "
    "diferente, espera longa por abertura de turma). A remoção aqui é apenas um exercício "
    "analítico, feito por fórmula na aba auxiliar, para medir o quanto esses casos influenciam os "
    "indicadores — e não uma faxina nos dados.",
]
for t in TEXTOS_CONC:
    paragrafo(ws, lin, t, "O", F_TXT, 15 * (1 + len(t) // 150) + 15)
    lin += 2

_lc, _ac = XLImage(IMG_COMP).width, XLImage(IMG_COMP).height
img = XLImage(IMG_COMP)
img.width, img.height = 980, int(980 * _ac / _lc)
ws.add_image(img, f"A{lin}")


# -----------------------------------------------------------------------------
# 5.9 ABA — PROBABILIDADES
# -----------------------------------------------------------------------------
ws = wb.create_sheet(AB_PROB)
titulo_aba(ws, "Probabilidade de ocorrência de eventos de interesse da ONG",
           "Bloco A: probabilidades empíricas (frequência relativa) — P(A) = casos favoráveis / "
           "casos possíveis. Bloco B: modelo probabilístico (distribuição binomial) aplicado à "
           "presença em encontros. Todos os resultados são fórmulas vivas.")

RNG_FREQ = f"'{AB_INSC}'!$H$2:$H${LIN_IN}"
RNG_STATUS = f"'{AB_INSC}'!$E$2:$E${LIN_IN}"
RNG_CERT = f"'{AB_INSC}'!$J$2:$J${LIN_IN}"
RNG_PRES = f"'{AB_INSC}'!$G$2:$G${LIN_IN}"
RNG_ENC = f"'{AB_INSC}'!$F$2:$F${LIN_IN}"
RNG_SIT = f"'{AB_CURSOS}'!$K$2:$K${LIN_CU}"
RNG_TAXA = f"'{AB_CURSOS}'!$J$2:$J${LIN_CU}"
RNG_MEDFREQ = f"'{AB_ALUNOS}'!$J$2:$J${LIN_AL}"

ws["A3"] = "Bloco A — Probabilidades empíricas (frequência relativa observada na base)"
ws["A3"].font = F_SECAO
cabecalho(ws, 4, ["Nº", "Evento", "Definição do evento", "Fórmula aplicada (notação Excel)",
                  "Casos favoráveis", "Casos possíveis", "Probabilidade (decimal)",
                  "Probabilidade (%)", "Interpretação para a gestão da ONG"],
          [6, 34, 44, 60, 14, 14, 15, 15, 58], 46)

EVENTOS = [
    dict(
        nome="A₁ — Baixa frequência",
        definicao="Uma inscrição sorteada ao acaso ter frequência ABAIXO de 75% "
                  "(mínimo exigido para o certificado).",
        formula=f'COUNTIF({RNG_FREQ};"<75") / COUNT({RNG_FREQ})',
        fav=f'=COUNTIF({RNG_FREQ},"<75")',
        pos=f"=COUNT({RNG_FREQ})",
        interp="Mede o tamanho do problema de evasão silenciosa: é a fatia de inscrições que, "
               "mantido o quadro, NÃO gera certificado. É o indicador que justifica os lembretes "
               "automáticos por notificação no app.",
    ),
    dict(
        nome="A₂ — Conclusão do curso",
        definicao="Uma inscrição em turma JÁ ENCERRADA terminar com status “concluido”. "
                  "Turmas em andamento são excluídas do denominador, pois ainda não tiveram "
                  "chance de concluir.",
        formula='COUNTIF(status;"concluido") / Σ COUNTIF(curso_id; id de cada turma encerrada)',
        fav=f'=COUNTIF({RNG_STATUS},"concluido")',
        pos="=" + "+".join(
            f"COUNTIF('{AB_INSC}'!$C$2:$C${LIN_IN},{c['id']})"
            for c in cursos if c["encerrada"]),
        interp="Taxa de conclusão entre as turmas que já terminaram. É o indicador de efetividade "
               "do programa que a ONG reporta a financiadores.",
    ),
    dict(
        nome="A₃ — Ausência em um encontro",
        definicao="Um aluno inscrito FALTAR a um encontro específico escolhido ao acaso. "
                  "Complementar da taxa global de presença.",
        formula=f"1 − SUM({RNG_PRES}) / SUM({RNG_ENC})",
        fav=f"=SUM({RNG_ENC})-SUM({RNG_PRES})",
        pos=f"=SUM({RNG_ENC})",
        interp="Probabilidade de a cadeira ficar vazia. Serve para dimensionar material, coffee "
               "break e, principalmente, para calibrar o envio de lembretes na véspera.",
    ),
    dict(
        nome="A₄ — Turma com baixa ocupação",
        definicao="Um curso ofertado ficar com MENOS DE 70% das vagas preenchidas.",
        formula=f'COUNTIF({RNG_TAXA};"<70") / COUNT({RNG_TAXA})',
        fav=f'=COUNTIF({RNG_TAXA},"<70")',
        pos=f"=COUNT({RNG_TAXA})",
        interp="Risco de abrir turma com baixa procura — custo fixo de sala e educador para poucos "
               "alunos. Orienta divulgação segmentada pelo teste de perfil do app.",
    ),
    dict(
        nome="A₅ — Certificado emitido",
        definicao="Uma inscrição qualquer resultar em certificado "
                  "(exige conclusão E frequência ≥ 75%).",
        formula=f'COUNTIF({RNG_CERT};"Sim") / COUNT({RNG_FREQ})',
        fav=f'=COUNTIF({RNG_CERT},"Sim")',
        pos=f"=COUNT({RNG_FREQ})",
        interp="Resultado final entregue ao jovem. A diferença entre A₂ (conclusão) e A₅ "
               "(certificado) é exatamente o grupo que terminou o curso mas não atingiu os 75% "
               "de presença — o alvo mais barato de recuperar.",
    ),
    dict(
        nome="A₆ — Desistência",
        definicao="Uma inscrição qualquer terminar com status “desistente” "
                  "(frequência inferior a 40%).",
        formula=f'COUNTIF({RNG_STATUS};"desistente") / COUNT({RNG_FREQ})',
        fav=f'=COUNTIF({RNG_STATUS},"desistente")',
        pos=f"=COUNT({RNG_FREQ})",
        interp="Evasão formal. Combinada com a variável “dias entre inscrição e 1º encontro”, "
               "indica que parte das desistências é decidida ANTES do curso começar.",
    ),
    dict(
        nome="A₇ — Aluno com frequência média baixa",
        definicao="Um ALUNO (e não uma inscrição) ter frequência MÉDIA inferior a 75% "
                  "considerando todos os seus cursos.",
        formula=f'COUNTIF({RNG_MEDFREQ};"<75") / COUNT({RNG_MEDFREQ})',
        fav=f'=COUNTIF({RNG_MEDFREQ},"<75")',
        pos=f"=COUNT({RNG_MEDFREQ})",
        interp="Identifica o jovem (não o curso) em situação de risco. É a lista de quem a ONG "
               "deve procurar individualmente — o app já permite notificação direcionada.",
    ),
]

for k, ev in enumerate(EVENTOS):
    r = LIN_V0 + k
    ws.cell(r, 1, f"A{k + 1}").alignment = AL_CENTRO
    ws.cell(r, 2, ev["nome"]).font = F_NEG
    ws.cell(r, 2).alignment = AL_TXT
    ws.cell(r, 3, ev["definicao"]).alignment = AL_TXT
    ws.cell(r, 4, ev["formula"]).alignment = AL_TXT
    ws.cell(r, 4).font = Font(name="Consolas", size=9, color="1F4E79")
    ws.cell(r, 5, ev["fav"]).alignment = AL_CENTRO
    ws.cell(r, 6, ev["pos"]).alignment = AL_CENTRO
    ws.cell(r, 7, f"=ROUND($E{r}/$F{r},4)")
    ws.cell(r, 7).number_format = "0.0000"
    ws.cell(r, 7).alignment = AL_CENTRO
    ws.cell(r, 8, f"=ROUND($G{r}*100,2)")
    ws.cell(r, 8).number_format = '0.00"%"'
    ws.cell(r, 8).font = F_NEG
    ws.cell(r, 8).alignment = AL_CENTRO
    ws.cell(r, 9, ev["interp"]).alignment = AL_TXT
    for j in (3, 9):
        ws.cell(r, j).font = F_TXT
    ws.row_dimensions[r].height = 62

lin = LIN_V0 + len(EVENTOS) + 1
paragrafo(ws, lin,
          "Observação metodológica: as probabilidades do Bloco A são EMPÍRICAS — estimadas pela "
          "frequência relativa observada na base simulada. Com 160 alunos e "
          f"{N_INSC} inscrições, a estimativa é razoavelmente estável; com os 30 alunos do seed de "
          "demonstração ela oscilaria muito de uma execução para outra, o que justifica a ampliação "
          "da simulação descrita na aba de apresentação.", "I", F_NOTA, 32)
lin += 2

# --- Bloco B: distribuição binomial ---
ws[f"A{lin}"] = "Bloco B — Modelo probabilístico: distribuição binomial aplicada à presença"
ws[f"A{lin}"].font = F_SECAO
lin += 1
paragrafo(ws, lin,
          "Por que a BINOMIAL é a distribuição adequada aqui: a presença de um aluno em um encontro "
          "é um experimento de Bernoulli (comparece / não comparece); um curso tem um número fixo e "
          "conhecido de encontros (n); e, como aproximação de trabalho, assume-se que os encontros "
          "são independentes entre si e que a probabilidade p de comparecer é a mesma em cada um. "
          "Sob essas hipóteses, o número X de presenças em n encontros segue X ~ Binomial(n, p), com "
          "P(X = k) = C(n,k) · pᵏ · (1−p)ⁿ⁻ᵏ. No Excel: BINOM.DIST(k; n; p; FALSO) para a "
          "probabilidade pontual e BINOM.DIST(k; n; p; VERDADEIRO) para a acumulada.",
          "I", F_TXT, 76)
lin += 2

LIN_P = lin
ws.cell(LIN_P, 1, "p — probabilidade de presença em um encontro (estimada da base)").font = F_NEG
ws.merge_cells(f"A{LIN_P}:D{LIN_P}")
ws.cell(LIN_P, 5, f"=ROUND(SUM({RNG_PRES})/SUM({RNG_ENC}),4)").number_format = "0.0000"
ws.cell(LIN_P, 5).font = F_NEG
ws.cell(LIN_P, 5).alignment = AL_CENTRO
ws.cell(LIN_P, 6, "← total de presenças ÷ total de vagas-encontro").font = F_NOTA

LIN_N = LIN_P + 1
ws.cell(LIN_N, 1, "n — nº de encontros do curso considerado (curso típico de 5 encontros)").font = F_NEG
ws.merge_cells(f"A{LIN_N}:D{LIN_N}")
ws.cell(LIN_N, 5, 5).font = F_NEG
ws.cell(LIN_N, 5).alignment = AL_CENTRO
ws.cell(LIN_N, 6, "← altere esta célula para simular turmas de outro tamanho").font = F_NOTA

lin = LIN_N + 2
cabecalho(ws, lin, ["k (nº de presenças)", "Frequência correspondente (k/n)",
                    "P(X = k) — BINOM.DIST(k;n;p;FALSO)", "P(X = k) em %",
                    "P(X ≤ k) — acumulada", "P(X ≤ k) em %",
                    "Leitura do resultado"], None, 46)
LIN_BIN0 = lin + 1
for k in range(0, 6):
    r = LIN_BIN0 + k
    ws.cell(r, 1, k).alignment = AL_CENTRO
    ws.cell(r, 2, f"=ROUND($A{r}/$E${LIN_N}*100,1)").number_format = '0.0"%"'
    ws.cell(r, 2).alignment = AL_CENTRO
    ws.cell(r, 3, f"=ROUND({XFN}BINOM.DIST($A{r},$E${LIN_N},$E${LIN_P},FALSE),4)")
    ws.cell(r, 3).number_format = "0.0000"
    ws.cell(r, 3).alignment = AL_CENTRO
    ws.cell(r, 4, f"=ROUND($C{r}*100,2)").number_format = '0.00"%"'
    ws.cell(r, 4).alignment = AL_CENTRO
    ws.cell(r, 5, f"=ROUND({XFN}BINOM.DIST($A{r},$E${LIN_N},$E${LIN_P},TRUE),4)")
    ws.cell(r, 5).number_format = "0.0000"
    ws.cell(r, 5).alignment = AL_CENTRO
    ws.cell(r, 6, f"=ROUND($E{r}*100,2)").number_format = '0.00"%"'
    ws.cell(r, 6).alignment = AL_CENTRO
    leituras = {
        0: "Ausência TOTAL: o aluno não aparece em nenhum encontro.",
        1: "Presença em apenas 1 de 5 encontros — desistência de fato.",
        2: "40% de frequência — limite do status “desistente”.",
        3: "60% de frequência — participou, mas fica sem certificado.",
        4: "80% de frequência — já supera o mínimo de 75%.",
        5: "Frequência integral (100%).",
    }
    ws.cell(r, 7, leituras[k]).font = F_TXT
    ws.cell(r, 7).alignment = AL_TXT
    ws.row_dimensions[r].height = 18
LIN_BINF = LIN_BIN0 + 5

lin = LIN_BINF + 2
ws[f"A{lin}"] = "Eventos derivados do modelo binomial (n e p das células acima)"
ws[f"A{lin}"].font = F_SECAO
lin += 1
cabecalho(ws, lin, ["Nº", "Evento", "Fórmula aplicada (notação Excel)", "Probabilidade (decimal)",
                    "Probabilidade (%)", "Interpretação"], None, 34)
lin += 1

DERIVADOS = [
    ("B₁", "P(X ≥ 4) — aluno atingir pelo menos 75% de frequência em um curso de 5 encontros "
           "(condição do certificado)",
     f"1 − BINOM.DIST(3; n; p; VERDADEIRO)",
     f"=ROUND(1-{XFN}BINOM.DIST(3,$E${LIN_N},$E${LIN_P},TRUE),4)",
     "Probabilidade de o aluno “típico” conquistar o certificado. Comparar com a taxa real "
     "observada (A₅) mostra o quanto a heterogeneidade entre alunos afasta a realidade do modelo."),
    ("B₂", "P(X = 0) — ausência total: o aluno se inscreve e não comparece a nenhum encontro",
     f"BINOM.DIST(0; n; p; FALSO)",
     f"=ROUND({XFN}BINOM.DIST(0,$E${LIN_N},$E${LIN_P},FALSE),4)",
     "“Vaga fantasma”. Sob o modelo é raro, mas na base real ocorre mais do que o modelo prevê — "
     "evidência de que p NÃO é igual para todos os alunos."),
    ("B₃", "P(X ≤ 2) — aluno comparecer a no máximo 40% dos encontros (faixa de desistência)",
     f"BINOM.DIST(2; n; p; VERDADEIRO)",
     f"=ROUND({XFN}BINOM.DIST(2,$E${LIN_N},$E${LIN_P},TRUE),4)",
     "Risco de desistência estimado pelo modelo. É o gatilho natural para acionar o fluxo de "
     "lembretes do app a partir da segunda falta."),
    ("B₄", "P(X = n) — frequência integral (100% de presença)",
     f"BINOM.DIST(n; n; p; FALSO)",
     f"=ROUND({XFN}BINOM.DIST($E${LIN_N},$E${LIN_N},$E${LIN_P},FALSE),4)",
     "Público “exemplar”, candidato natural a monitor/embaixador — papel que explica os outliers "
     "de pontuação observados na aba 6."),
]
for sigla, evento, formula_txt, formula, interp in DERIVADOS:
    ws.cell(lin, 1, sigla).alignment = AL_CENTRO
    ws.cell(lin, 2, evento).font = F_TXT
    ws.cell(lin, 2).alignment = AL_TXT
    ws.cell(lin, 3, formula_txt).font = Font(name="Consolas", size=9, color="1F4E79")
    ws.cell(lin, 3).alignment = AL_TXT
    ws.cell(lin, 4, formula).number_format = "0.0000"
    ws.cell(lin, 4).alignment = AL_CENTRO
    ws.cell(lin, 5, f"=ROUND($D{lin}*100,2)").number_format = '0.00"%"'
    ws.cell(lin, 5).font = F_NEG
    ws.cell(lin, 5).alignment = AL_CENTRO
    ws.cell(lin, 6, interp).font = F_TXT
    ws.cell(lin, 6).alignment = AL_TXT
    ws.row_dimensions[lin].height = 52
    lin += 1

lin += 1
paragrafo(ws, lin,
          "Limitação honesta do modelo binomial neste contexto: a hipótese de p constante entre "
          "alunos NÃO se sustenta — a aba 5 mostra que o percentual de frequência tem alta "
          "dispersão, ou seja, existem perfis de engajamento muito diferentes. O resultado é que o "
          "modelo binomial com um p médio subestima a cauda de frequência muito baixa. Uma "
          "modelagem mais fiel usaria uma mistura (ou uma Beta-Binomial) com um p por perfil de "
          "aluno. Ainda assim, o modelo é útil como referência: o afastamento entre o previsto (B₁) "
          "e o observado (A₅) é, ele próprio, uma medida da heterogeneidade do público.",
          "I", F_NOTA, 60)
ws.freeze_panes = "A5"


# -----------------------------------------------------------------------------
# 5.10 ABA — CONCLUSÕES E RECOMENDAÇÕES
# -----------------------------------------------------------------------------
ws = wb.create_sheet(AB_CONC)
titulo_aba(ws, "Conclusões e recomendações",
           "Síntese dos achados da Entrega 2 e ligação de cada um a uma ação concreta, "
           "preferencialmente usando recursos que o sistema “Próxima Etapa” já possui.")
for col, w in zip("ABCDEFGH", [26, 20, 20, 20, 20, 20, 20, 20]):
    ws.column_dimensions[col].width = w

cv_txt = {v.nome: STATS[v.nome]["cv"] for v in VARIAVEIS}


def fmt_cv(nome):
    return f"{cv_txt[nome]:.2f}%"


BLOCOS = [
    ("sec", "1. Síntese da dispersão (itens 1 e 2 do enunciado)"),
    ("txt", f"Foram analisadas {N_VAR} variáveis quantitativas do sistema. Pelo critério adotado "
            f"(CV < 15% baixa; 15–30% média; > 30% alta), {len(altas)} variáveis apresentam ALTA "
            f"dispersão, {len(medias)} apresentam dispersão média e {len(baixas)} apresentam baixa "
            f"dispersão."),
    ("txt", "As variáveis com GRANDE dispersão são: "
            + "; ".join(f"{v.nome} — CV = {fmt_cv(v.nome)}" for v in altas) + "."),
    ("txt", "Na outra ponta, a idade do aluno é a variável mais homogênea da base "
            f"(CV = {fmt_cv('Idade do aluno')}): o público é muito bem definido — jovens de Ensino "
            "Médio da rede pública —, o que é uma boa notícia para o desenho pedagógico dos cursos, "
            "porque permite material único para toda a turma."),

    ("sec", "2. Possíveis explicações para a alta variabilidade (item 3 do enunciado)"),
    ("txt", f"a) Pontos acumulados no app (CV = {fmt_cv('Pontos acumulados no app')}). A pontuação é "
            "cumulativa e sem teto: quem entrou há mais tempo, faz mais cursos e comparece mais "
            "acumula de forma composta. Somam-se dois “embaixadores” com pontuação altíssima. "
            "Resultado: distribuição fortemente assimétrica à direita, em que a média não "
            "representa o aluno típico."),
    ("txt", f"b) Carga horária e nº de encontros dos cursos (CV = {fmt_cv('Carga horária do curso')} "
            f"e {fmt_cv('Nº de encontros por curso')}). O catálogo mistura produtos de naturezas "
            "diferentes: oficinas pontuais de 8 horas e uma formação técnica de 120 horas. A "
            "dispersão não é defeito de dados, é reflexo de um portfólio heterogêneo."),
    ("txt", f"c) Percentual de frequência (CV = {fmt_cv('Percentual de frequência (por inscrição)')}). "
            "Convivem na mesma base três perfis bem distintos: alunos de presença quase integral, "
            "alunos intermitentes e alunos que praticamente abandonam após o primeiro encontro. "
            "Causas plausíveis no contexto: custo e tempo de transporte, conflito com a escola ou "
            "com trabalho informal, cursos no contraturno e expectativa desalinhada no momento da "
            "inscrição."),
    ("txt", f"d) Dias entre inscrição e 1º encontro (CV = {fmt_cv('Dias entre inscrição e 1º encontro')}). "
            "As turmas abrem em datas fixas, mas a inscrição fica aberta continuamente: quem se "
            "inscreve logo após o fechamento de uma turma espera muito mais. É um problema de "
            "PROCESSO de agendamento, e a espera longa alimenta a desistência antes do início."),
    ("txt", f"e) Nº de cursos por aluno (CV = {fmt_cv('Nº de cursos por aluno')}) e vagas ocupadas / "
            f"taxa de ocupação (CV = {fmt_cv('Vagas ocupadas por curso')} e "
            f"{fmt_cv('Taxa de ocupação do curso')}). A procura é muito concentrada em alguns temas "
            "(tecnologia e preparação para o ENEM) e fraca em outros, o que gera turmas cheias ao "
            "lado de turmas semivazias."),

    ("sec", "3. Medidas para reduzir a variabilidade (item 3 do enunciado)"),
    ("txt", "1) Segmentar o catálogo em trilhas por duração — “oficinas curtas” (até 20h) e "
            "“formações longas” (acima de 40h) — e passar a reportar carga horária, frequência e "
            "conclusão SEPARADAMENTE por trilha. Isso remove de uma vez a maior fonte de dispersão "
            "artificial dos indicadores de curso."),
    ("txt", "2) Reduzir o tempo de espera: publicar um calendário de turmas com data de início "
            "conhecida no ato da inscrição e abrir a inscrição apenas em janelas próximas ao "
            "início. Meta sugerida: manter o intervalo inscrição → 1º encontro abaixo de 30 dias "
            "para 90% das inscrições."),
    ("txt", "3) Usar as notificações que o app JÁ possui (tabelas mensagens e notificacoes, tipo "
            "“agenda”) de forma automática: lembrete na véspera de cada encontro para todos, e "
            "lembrete direcionado ao aluno que acumular duas faltas. O gatilho sai direto da "
            "tabela de presenças."),
    ("txt", "4) Expor a barra de frequência do aluno dentro do app, com o limiar de 75% marcado. "
            "Tornar a meta visível reduz a variabilidade de comportamento porque o aluno passa a "
            "saber exatamente quanto lhe falta para o certificado."),
    ("txt", "5) Fazer o matching entre o resultado do teste de perfil (tabela resultados_perfil) e "
            "a divulgação de novas turmas. Inscrição mais alinhada ao interesse real reduz a "
            "evasão precoce e, ao mesmo tempo, equilibra a ocupação entre os cursos."),
    ("txt", "6) Rever a pontuação da gamificação: adotar teto por período ou normalizar os pontos "
            "pelo tempo de cadastro. Isso impede que a métrica de engajamento seja dominada por "
            "poucos alunos antigos e a torna comparável entre turmas."),
    ("txt", "7) Padronizar o tamanho das turmas (vagas totais hoje variam de 20 a 40) e abrir a "
            "turma apenas com ocupação mínima garantida, reduzindo a dispersão da taxa de ocupação "
            "e o desperdício de recurso."),

    ("sec", "4. Outliers: o que foram e o que fazer com eles (item 4 do enunciado)"),
    ("txt", f"O critério de Tukey identificou {total_out} valores atípicos no conjunto das "
            f"{N_VAR} variáveis ({total_out / total_obs * 100:.2f}% das observações). A análise "
            "caso a caso (aba 6) mostrou que NENHUM deles é erro de registro: são a formação "
            "técnica de 120h, os dois alunos embaixadores, os jovens de 22 a 25 anos atendidos em "
            "caráter excepcional e as inscrições feitas com mais de 100 dias de antecedência."),
    ("txt", "Decisão: manter todos na base e tratá-los analiticamente — reportar mediana e quartis "
            "ao lado da média, e segmentar os relatórios por trilha de curso e por perfil de aluno. "
            "Excluir esses registros melhoraria os indicadores no papel e esconderia da ONG "
            "justamente os casos que ela precisa enxergar: o curso mais caro do portfólio, os "
            "alunos mais engajados e o gargalo de agendamento."),

    ("sec", "5. Concentração dos dados com e sem atípicos (item 5 do enunciado)"),
    ("txt", f"A comparação da aba 7 mostra que a retirada dos atípicos reduz drasticamente o CV das "
            f"variáveis de catálogo e de processo — o caso extremo é "
            f"“{maior_efeito.nome}”, cujo CV cai de {se['cv']:.2f}% para {se['cv2']:.2f}%. Nessas "
            f"variáveis, os dados são, na verdade, bem concentrados: a dispersão aparente vinha de "
            f"poucas observações extremas."),
    ("txt", "Já o percentual de frequência e o nº de cursos por aluno mantêm CV elevado mesmo sem "
            "atípicos. Aqui a heterogeneidade é real e estrutural — não há limpeza de dados que a "
            "resolva, apenas política de engajamento. Essa é a distinção prática mais importante "
            "desta entrega: dispersão causada por casos extremos se trata com segmentação; "
            "dispersão estrutural se trata com intervenção."),

    ("sec", "6. Probabilidades e o que elas dizem à ONG (item 6 do enunciado)"),
    ("txt", "As probabilidades empíricas da aba 9 transformam as medidas de dispersão em risco "
            "operacional: a chance de uma inscrição não alcançar os 75% de frequência, a chance de "
            "conclusão em turmas encerradas, a chance de a cadeira ficar vazia em um encontro "
            "qualquer e a chance de uma turma abrir com menos de 70% das vagas. São quatro números "
            "que cabem num painel de gestão e que já existem no banco do app."),
    ("txt", "O modelo binomial acrescenta a dimensão prospectiva: dado o p médio de presença "
            "observado, qual a chance de um aluno atingir o certificado em uma turma de 5 "
            "encontros, qual a chance de ausência total e qual a chance de cair na faixa de "
            "desistência. A diferença entre o que o modelo prevê e o que a base registra é, ela "
            "mesma, uma medida de quão heterogêneo é o público — e reforça a conclusão do item 5."),

    ("sec", "7. Limitações do estudo"),
    ("txt", "Os dados são simulados: as conclusões numéricas valem como demonstração de método, não "
            "como diagnóstico da ONG real. A simulação, porém, foi construída a partir do schema e "
            "das regras de negócio do sistema efetivamente implementado, e com semente fixa, de "
            "modo que é integralmente reprodutível. Aplicar o mesmo conjunto de fórmulas sobre a "
            "base de produção exigiria apenas substituir o conteúdo das abas 1, 2 e 3 — todos os "
            "indicadores se recalculariam automaticamente."),
]

lin = 4
for tipo, txt in BLOCOS:
    if tipo == "sec":
        lin += 1
        ws[f"A{lin}"] = txt
        ws[f"A{lin}"].font = F_SECAO
        lin += 1
    else:
        alt = 15 * (1 + len(txt) // 112)
        paragrafo(ws, lin, txt, "H", F_TXT, max(30, alt))
        lin += 2

lin += 1
ws[f"A{lin}"] = "8. Quadro de recomendações: achado → ação → recurso do sistema"
ws[f"A{lin}"].font = F_SECAO
lin += 1
cabecalho(ws, lin, ["Achado estatístico", "Ação recomendada para a ONG",
                    "Recurso do “Próxima Etapa” que viabiliza"], None, 28)
ws.column_dimensions["A"].width = 46
lin += 1

RECOMENDA = [
    (f"Frequência com alta dispersão (CV = {fmt_cv('Percentual de frequência (por inscrição)')}) e "
     "probabilidade relevante de ficar abaixo de 75%",
     "Lembrete automático na véspera de cada encontro e alerta direcionado após a 2ª falta.",
     "Tabelas mensagens / notificacoes (tipo “agenda”) + presenças registradas por QR Code."),
    ("Cauda de alunos com frequência muito baixa",
     "Busca ativa individual (ligação/WhatsApp) para a lista de alunos com frequência média abaixo "
     "de 75%, antes do fim da turma.",
     "Coluna “media_frequencia_pct” da aba 1 + notificação direcionada por aluno."),
    (f"Dias entre inscrição e 1º encontro com alta dispersão "
     f"(CV = {fmt_cv('Dias entre inscrição e 1º encontro')}) e atípicos acima de 100 dias",
     "Publicar data de início no ato da inscrição e abrir inscrições só em janelas próximas ao "
     "começo da turma.",
     "Campos cursos.data_inicio e atividades.data, já exibidos na agenda do app."),
    (f"Carga horária e nº de encontros com alta dispersão "
     f"(CV = {fmt_cv('Carga horária do curso')} e {fmt_cv('Nº de encontros por curso')})",
     "Separar o catálogo em trilhas (oficina curta × formação longa) e reportar os indicadores por "
     "trilha.",
     "Campos cursos.categoria e cursos.carga_horaria; filtro por categoria na listagem de cursos."),
    (f"Taxa de ocupação desigual entre turmas (CV = {fmt_cv('Taxa de ocupação do curso')})",
     "Divulgação segmentada das turmas com baixa procura e exigência de ocupação mínima para abrir "
     "a turma.",
     "Teste de perfil (resultados_perfil) + notificação do tipo “curso_novo”."),
    (f"Pontuação com dispersão altíssima (CV = {fmt_cv('Pontos acumulados no app')}) por efeito "
     "cumulativo e por 2 embaixadores",
     "Normalizar os pontos por período e reportar o engajamento pela mediana, não pela média. "
     "Convidar os embaixadores a atuar como monitores.",
     "Campo alunos.pontos e alunos.nivel (Explorador / Em trilha / Referência)."),
    ("Probabilidade de conclusão abaixo do desejado em turmas encerradas",
     "Definir meta institucional de conclusão por trilha e acompanhar mês a mês num painel.",
     "Campo inscricoes.status + tabela certificados."),
]
for achado, acao, recurso in RECOMENDA:
    ws.cell(lin, 1, achado).font = F_NEG
    ws.cell(lin, 1).alignment = AL_TXT
    ws.merge_cells(f"B{lin}:D{lin}")
    ws.cell(lin, 2, acao).font = F_TXT
    ws.cell(lin, 2).alignment = AL_TXT
    ws.merge_cells(f"E{lin}:H{lin}")
    ws.cell(lin, 5, recurso).font = F_TXT
    ws.cell(lin, 5).alignment = AL_TXT
    ws.row_dimensions[lin].height = 50
    lin += 1

lin += 2
paragrafo(ws, lin,
          f"Planilha gerada automaticamente por gerar_analise_dispersao.py · semente = {SEMENTE} · "
          f"{N_ALUNOS} alunos, {N_CURSOS} cursos, {N_INSC} inscrições · dados fictícios (LGPD).",
          "H", F_NOTA, 20)


# -----------------------------------------------------------------------------
# 5.11 Metadados e gravação
# -----------------------------------------------------------------------------
# A aba auxiliar precisou ser criada antes da aba 7 (que lê os intervalos dela),
# mas deve aparecer DEPOIS dela na ordem das guias.
wb.move_sheet(AB_AUX, offset=1)

wb.properties.title = "Análise Descritiva de Dados — Entrega 2 — Próxima Etapa"
wb.properties.subject = "Dispersão, Coeficiente de Variação, Box Plot, outliers e probabilidades"
wb.properties.description = ("Projeto Interdisciplinar FECAP — dados fictícios simulados a partir do "
                             "schema do sistema Próxima Etapa (App do Aluno).")
wb.calculation.fullCalcOnLoad = True

wb.save(ARQ_XLSX)
print(f"Planilha gravada em: {ARQ_XLSX}")


# =============================================================================
# 6. RELATÓRIO DE CONFERÊNCIA NO CONSOLE
# =============================================================================
print("\n" + "=" * 78)
print("CONFERÊNCIA — valores calculados em Python (devem bater com as fórmulas do Excel)")
print("=" * 78)
print(f"{'Variável':<44}{'n':>5}{'Média':>10}{'DP':>9}{'CV %':>8}{'Class.':>8}{'Out':>5}")
for v in VARIAVEIS:
    s = STATS[v.nome]
    print(f"{v.nome[:43]:<44}{int(s['n']):>5}{s['media']:>10.2f}{s['dp']:>9.2f}"
          f"{s['cv']:>8.2f}{classificar(s['cv']).split()[0][:7]:>8}{s['n_out']:>5}")

print("\nComparação COM e SEM outliers (CV %):")
for v in VARIAVEIS:
    s = STATS[v.nome]
    if s["n_out"]:
        print(f"  {v.nome[:45]:<46} {s['cv']:>7.2f}  ->  {s['cv2']:>7.2f}"
              f"   ({s['cv2'] - s['cv']:+.2f} p.p.)")

p_pres = sum(i["presencas"] for i in inscricoes) / sum(i["encontros"] for i in inscricoes)
n_conc = sum(1 for i in inscricoes if i["status"] == "concluido")
n_enc_tot = sum(1 for i in inscricoes if cursos[i["curso_id"] - 1]["encerrada"])
print("\nProbabilidades empíricas:")
print(f"  A1 P(frequência < 75%)            = {sum(1 for i in inscricoes if i['frequencia'] < 75) / N_INSC * 100:6.2f}%")
print(f"  A2 P(concluir | turma encerrada)  = {n_conc / n_enc_tot * 100:6.2f}%")
print(f"  A3 P(ausência em um encontro)     = {(1 - p_pres) * 100:6.2f}%")
print(f"  A4 P(ocupação < 70%)              = {sum(1 for t in taxa_ocup if t < 70) / N_CURSOS * 100:6.2f}%")
print(f"  A5 P(certificado emitido)         = {sum(1 for i in inscricoes if i['certificado'] == 'Sim') / N_INSC * 100:6.2f}%")
print(f"  A6 P(desistência)                 = {sum(1 for i in inscricoes if i['status'] == 'desistente') / N_INSC * 100:6.2f}%")
print(f"\n  p (presença por encontro) = {p_pres:.4f}")
print(f"  Total de outliers = {total_out} em {total_obs} observações")
print("=" * 78)
