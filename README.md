# FECAP - Fundação de Comércio Álvares Penteado

<p align="center">
<a href= "https://www.fecap.br/"><img src="https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRhZPrRa89Kma0ZZogxm0pi-tCn_TLKeHGVxywp-LXAFGR3B1DPouAJYHgKZGV0XTEf4AE&usqp=CAU" alt="FECAP - Fundação de Comércio Álvares Penteado" border="0"></a>
</p>

# Próxima Etapa

## NullPointerSolo

## Integrantes: Gustavo

## Professores Orientadores: <a href="https://www.linkedin.com/in/katia-bossi/">Katia Milani Lara Bossi</a>, <a href="https://www.linkedin.com/in/marco-aurelio-lima-barbosa/">Marco Aurelio Lima Barbosa</a>, <a href="https://www.linkedin.com/in/rodrigo-da-rosa-phd/">Rodrigo da Rosa</a>, <a href="https://www.linkedin.com/in/victorbarq/">Victor Bruno Alexander Rosetti de Quiroz</a>

## Descrição

<p align="center">
  <a href="https://proximaetapa.org.br">
    Próxima Etapa
  </a>
</p>

O Próxima Etapa – App do Aluno é um aplicativo Android desenvolvido no Projeto Interdisciplinar do 3º semestre do curso de Ciência da Computação da FECAP, em parceria com a organização Próxima Etapa.
<br><br>
A solução tem como objetivo facilitar o acompanhamento da jornada educacional dos estudantes atendidos pela ONG, permitindo acesso a cursos, atividades, agenda, presença, certificados, mensagens e orientação profissional em um único ambiente digital.
<br><br>
O sistema é composto por um aplicativo mobile, uma API REST e um banco de dados integrados, possibilitando a sincronização das informações dos alunos. Conta com registro de presença por QR Code, emissão de certificados, notificações, teste de perfil educacional/profissional e canal de comunicação entre estudantes e a organização.
<br><br>

## 🚀 Entrega 2 — sistema completo e integrado

O sistema está **no ar**, com os quatro componentes integrados:

| Componente | Onde |
|---|---|
| API REST | <https://proxima-etapa-api.onrender.com/api> ([health](https://proxima-etapa-api.onrender.com/api/health)) |
| Painel web da organização | <https://proxima-etapa-api.onrender.com/> |
| App Android | código em [`src/Entrega 2/Frontend`](src/Entrega%202/Frontend) · [APK e instruções de geração](documentos/Entrega%202/Projeto%20Interdisciplinar%20(Aplicativo%20Movel)) |
| Banco de dados | SQLite, criado por `db/schema.sql` e populado por `db/seed.js` |

**Credenciais de demonstração** (fictícias, geradas pelo seed):
aluno `ana.souza@aluno.proximaetapa.org.br` / `123456` ·
painel `admin@proximaetapa.org.br` / `123456`

### A função principal da etapa: sincronização e presença offline

Os encontros da ONG acontecem em escolas e espaços comunitários onde o sinal
cai. Por isso o app foi construído **offline primeiro**: nenhuma tela consulta
a API para se desenhar — todas leem do banco local (Room), e a rede só
atualiza esse banco em segundo plano.

O aluno escaneia o QR Code do encontro e a presença é gravada **na hora** no
celular, com o horário real, mesmo sem internet. Quando a conexão volta, a
fila sobe em ordem de chegada e cada item só é apagado depois que o servidor
confirma. Cada check-in carrega um identificador único gerado no aparelho, o
que torna o reenvio seguro: o servidor reconhece a repetição em vez de
registrar a presença duas vezes.

### Documentos da Entrega 2

| Disciplina | Documento |
|---|---|
| Análise Descritiva de Dados | [planilha de dispersão, outliers e probabilidades](documentos/Entrega%202/Analise%20Descritiva%20de%20Dados/) |
| POO e Estrutura de Dados | [diagrama de classes completo](documentos/Entrega%202/POO%20e%20Estrutura%20de%20Dados/diagrama-classes-completo.md) · [README da UC](src/Entrega%202/POO-EstruturaDados/README.md) |
| Dispositivos Móveis | [relatório da Entrega 2](documentos/Entrega%202/Dispositivos%20Moveis/relatorio-entrega-2.md) |
| Projeto Interdisciplinar | [relatório de implantação](documentos/Entrega%202/Projeto%20Interdisciplinar%20(Aplicativo%20Movel)/relatorio-implantacao.md) |
| Apresentação | [roteiro do pitch de 4 minutos](documentos/Entrega%202/pitch-4-minutos.md) |

### Testes

```bash
cd "src/Entrega 2/Backend"
npm install && npm run seed && npm start
# em outro terminal:
node test-api.js      # 11 verificações (Entrega 1)
node test-api-e2.js   # 34 verificações (Entrega 2)
```

## 🛠 Estrutura de pastas
```
📂 Projeto8/
│
├── 📄 README.md
├── 📄 LICENSE
├── 📄 .gitignore
├── 📄 .gitattributes
│
├── 📂 src/
│   ├── 📂 Entrega 1/
│       ├── 📂 Frontend/                 # Aplicativo Android (Java)
│       │   ├── 📂 app/
│       │   │   ├── 📄 build.gradle
│       │   │   └── 📂 src/main/
│       │   │       ├── 📄 AndroidManifest.xml
│       │   │       ├── 📂 java/br/org/proximaetapa/app/
│       │   │       │   ├── 📂 data/       (ApiClient, ApiService, SessionManager)
│       │   │       │   ├── 📂 model/      (Aluno, Curso, AgendaItem...)
│       │   │       │   └── 📂 ui/         (LoginActivity, CursosActivity, AgendaActivity...)
│       │   │       └── 📂 res/
│       │   ├── 📄 build.gradle
│       │   ├── 📄 settings.gradle
│       │   └── 📄 README.md
│       │
│       ├── 📂 Backend/                  # API Node.js + Express + SQLite
│       │   ├── 📂 db/
│       │   │   ├── 📄 connection.js
│       │   │   ├── 📄 schema.sql
│       │   │   └── 📄 seed.js           # Popula o banco com dados fictícios
│       │   ├── 📂 middleware/
│       │   ├── 📂 routes/
│       │   ├── 📄 server.js
│       │   ├── 📄 API.md                # Documentação completa dos endpoints
│       │   ├── 📄 test-api.js           # Smoke test automatizado
│       │   └── 📄 package.json
│       │
│       └── 📂 POO-EstruturaDados/       # Classes de domínio + estrutura de dados
│           ├── 📂 src/.../modelo/        (Aluno, Curso, Atividade, Inscrição, Presença)
│           ├── 📂 src/.../repositorio/   (RepositorioEmMemoria: ArrayList + HashMap)
│           └── 📂 src/.../app/Main.java  (demonstração de CRUD, busca e ordenação)
│
│   └── 📂 Entrega 2/
│       ├── 📂 Frontend/                 # App Android: Room, QR Code e sincronização
│       │   └── 📂 app/src/main/java/.../data/local/   (8 entidades Room + fila offline)
│       ├── 📂 Backend/                  # API + sincronização + rotas administrativas
│       │   ├── 📄 routes/sync.js         # GET /sync e POST /sync/presencas
│       │   ├── 📄 routes/admin.js        # painel da ONG, QR Code, certificados
│       │   └── 📄 test-api-e2.js         # 34 verificações automatizadas
│       ├── 📂 ModuloWeb/                # Painel web administrativo (HTML/CSS/JS)
│       └── 📂 POO-EstruturaDados/       # Java puro — entrega final da UC
│           ├── 📄 README.md              # POO e estruturas de dados (explicação)
│           ├── 📂 src/.../modelo/        (13 classes + Identificavel + ItemFeed)
│           ├── 📂 src/.../repositorio/   (ArrayList, HashMap, TreeMap, HashSet, Queue)
│           ├── 📂 src/.../servico/       (inscrição, presença, certificado, feed)
│           └── 📂 src/.../app/Main.java  (demonstração executável em 13 seções)
│
├── 📂 documentos/
│   ├── 📂 Entrega 1/
│   │   ├── 📂 Analise Descritiva de Dados/
│   │   │   └── 📂 Analise-Descritiva-Dados/
│   │   │       └── 📄 analise_descritiva_proxima_etapa.xlsx
│   │   ├── 📂 POO e Estrutura de Dados/
│   │   │   └── 📄 diagrama-classes.md    (Diagrama de classes, Mermaid)
│   │   ├── 📂 Dispositivos Moveis/
│   │   └── 📂 Projeto Interdisciplinar (Aplicativo Movel)/
│   └── 📂 Entrega 2/
│       ├── 📄 pitch-4-minutos.md         (roteiro cronometrado da apresentação)
│       ├── 📂 Analise Descritiva de Dados/
│       │   ├── 📄 analise_dispersao_proxima_etapa.xlsx
│       │   └── 📄 gerar_analise_dispersao.py
│       ├── 📂 POO e Estrutura de Dados/
│       │   └── 📄 diagrama-classes-completo.md  (13 classes, Mermaid)
│       ├── 📂 Dispositivos Moveis/
│       │   └── 📄 relatorio-entrega-2.md
│       └── 📂 Projeto Interdisciplinar (Aplicativo Movel)/
│           ├── 📄 relatorio-implantacao.md
│           └── 📄 COMO-GERAR-O-APK.md
│
└── 📂 imagens/
```

## 🧩 POO e Estrutura de Dados (UC — Entrega 2)

A entrega da UC **Programação Orientada a Objetos e Estrutura de Dados** fica em
**[`src/Entrega 2/POO-EstruturaDados`](./src/Entrega%202/POO-EstruturaDados)** —
veja o **[README da UC](./src/Entrega%202/POO-EstruturaDados/README.md)**, que
explica em detalhe os princípios de POO e as estruturas de dados empregados, e o
**[diagrama de classes completo](./documentos/Entrega%202/POO%20e%20Estrutura%20de%20Dados/diagrama-classes-completo.md)**.

É Java puro (sem Android e sem dependências externas), compilado com `javac` e
executado com `java`. As classes espelham o
[`schema.sql`](./src/Entrega%202/Backend/db/schema.sql) do projeto.

**Princípios de POO aplicados**

- **Encapsulamento** — atributos privados com invariantes protegidos:
  `Curso.vagasDisponiveis` só muda por `ocuparVaga()`/`liberarVaga()`;
  `Aluno.adicionarPontos()` recalcula o nível junto; `Certificado` tem
  construtor privado e só é criado pela fábrica `emitir()`, que recusa
  frequência abaixo de 75%.
- **Herança** — `ItemFeed` é a superclasse abstrata de `Mensagem` e
  `Notificacao` (título, corpo e data são idênticos nas duas tabelas do banco);
  `RepositorioAtividade` estende `RepositorioEmMemoria<Atividade>` para
  acrescentar um índice por QR Code.
- **Polimorfismo** — `rotuloTipo()` é abstrato em `ItemFeed` e implementado por
  cada subclasse, o que permite ao feed da tela inicial ser uma única
  `List<ItemFeed>` ordenada e impressa sem nenhum `instanceof`.
- **Abstração** — a interface `Identificavel` reduz toda entidade ao que o
  repositório genérico precisa saber (ela tem um id), eliminando o extrator de
  id que a Entrega 1 exigia no construtor.

**Estruturas de dados empregadas**

| Estrutura | Onde | Para quê | Custo |
|---|---|---|---|
| `ArrayList` + `HashMap` | `RepositorioEmMemoria<T>` | Ordem de cadastro + índice por id | localizar O(1), listar O(n) |
| `HashMap<String, Atividade>` | `RepositorioAtividade` | Achar o encontro pelo QR Code no check-in | O(1) |
| `TreeMap` | `AgendaDoAluno`, `TestePerfil` | Agenda sempre cronológica e consultas de intervalo (`tailMap`/`subMap`) | O(log n) |
| `HashSet` | `ControleDePresenca`, inscrições, certificados | Espelha os `UNIQUE` do banco: sem presença duplicada no mesmo encontro | O(1) |
| `Queue`/`LinkedList` | `FilaDeSincronizacao` | Check-ins feitos offline sobem em ordem FIFO quando a conexão volta | O(1) |
| `Comparator` | `Comparadores` | Ranking por pontos, cursos por data, feed por recência | O(n log n) |

**Decisão de estrutura de dados destacada:** indexar cada entidade em duas
estruturas ao mesmo tempo (lista para a ordem, hash para o id) e estender a
ideia a um índice por QR Code. Com isso, o check-in — a operação mais
frequente do app — é resolvido por três verificações O(1) em sequência
(QR Code existe, aluno inscrito, presença ainda não registrada), e seu tempo
deixa de depender do tamanho do histórico de presenças.

```bash
cd "src/Entrega 2/POO-EstruturaDados"
javac -encoding UTF-8 -d out $(find src -name "*.java")
java -Dfile.encoding=UTF-8 -cp out br.org.proximaetapa.poo.app.Main
```

# Instalação

## Windows

1. **Acesse o repositório no GitHub**
   Abra o navegador e acesse: `https://github.com/2026-2-NCC3/Projeto8`

2. **Copie a URL HTTPS do repositório**
   Clique no botão verde **"Code"**, selecione **HTTPS** e copie a URL (`https://github.com/2026-2-NCC3/Projeto8.git`).

3. **Abra o Android Studio**
   Se ainda não tiver instalado, veja a seção [Testando no PC](#testando-no-pc) antes de continuar.

4. **Selecione "Clone Repository"**
   Na tela inicial do Android Studio, clique em **"Clone Repository"**.

5. **Cole a URL do GitHub** no campo **URL**.

6. **Escolha a pasta de destino** no campo **Directory**.

7. **Clique em "Clone"** e aguarde o download.

8. **No Android Studio, abra a pasta certa**
   Como o projeto Android fica dentro de `src/Entrega 1/Frontend`, use **File → Open** e selecione essa subpasta (não a raiz do repositório).

9. **Aguarde o Gradle sincronizar** (barra de progresso na parte inferior — pode demorar alguns minutos na primeira vez).

10. **Resolva dependências faltantes**
    Se aparecer aviso de SDK ausente ou versão do Gradle incompatível, clique nos links sugeridos pelo próprio Android Studio.

11. **Abra o Device Manager**
    **View → Tool Windows → Device Manager**.

12. **Crie um emulador Android** (caso não exista nenhum)
    **"Create Device"** → escolha um modelo (ex.: Pixel 6) → **Next** → selecione uma imagem de sistema → **Finish**.

13. **Inicie o emulador** clicando no ícone de play (▶) ao lado dele.

14. **Selecione o dispositivo** no topo do Android Studio.

15. **Rode o backend** (em um terminal, na pasta `src/Entrega 1/Backend`):
    ```
    npm install
    npm run seed
    npm start
    ```

16. **Compile e rode o app**
    Clique no botão verde **Run (▶)** no topo do Android Studio.

17. **Verifique erros no Logcat**, se o app fechar sozinho ou algo não funcionar.

> **Observação para iniciantes:** a primeira sincronização do Gradle demora — ele está baixando as bibliotecas pela primeira vez. Depois disso fica bem mais rápido.

---

# Testando no PC

Indicado para quem nunca usou o Android Studio.

1. **Instale o Android Studio** em [developer.android.com/studio](https://developer.android.com/studio), mantendo as opções padrão (isso já instala o SDK e o emulador).
2. **Clone o projeto** seguindo os passos 1 a 7 da seção Windows acima.
3. **Abra o projeto**: **File → Open** e selecione `src/Entrega 1/Frontend`.
4. **Aguarde a sincronização do Gradle** finalizar.
5. **Crie um emulador Android** (passos 11 e 12 acima), caso não tenha um.
6. **Rode o backend** (passo 15 acima).
7. **Execute o app** clicando em **Run (▶)**.
8. **Teste as funcionalidades**: login, lista de cursos, detalhe do curso, agenda.
9. **Leia o Logcat** se algo falhar.

---

# Testando no Celular

O backend está publicado e o APK está no repositório, então dá para instalar
direto no aparelho, sem Android Studio:

1. Gere o APK da Entrega 2 seguindo
   [estas instruções](documentos/Entrega%202/Projeto%20Interdisciplinar%20(Aplicativo%20Movel)/COMO-GERAR-O-APK.md)
   (o APK que está no repositório é o da Entrega 1).
2. Transfira para o celular e abra o arquivo.
3. O Android vai pedir autorização para instalar de fora da Play Store —
   é esperado em um APK de desenvolvimento.
4. Entre com `ana.souza@aluno.proximaetapa.org.br` / `123456`.

> A primeira abertura pode demorar até 50 segundos: o servidor roda no plano
> gratuito do Render, que hiberna após alguns minutos sem uso. Depois disso
> fica rápido — e, como o app guarda tudo localmente, as telas seguintes
> abrem na hora, inclusive sem internet.

## 📋 Licença/License
Este projeto está licenciado sob a licença MIT — veja o arquivo
[LICENSE](./LICENSE) para o texto completo.

O nome, o logotipo e a identidade visual da ONG Próxima Etapa são de
propriedade da organização e não estão cobertos por esta licença,
sendo utilizados neste projeto acadêmico com fins de identificação
do parceiro institucional.

## 🔗 Referências e Materiais de Apoio

### 📚 Acadêmicas e Técnicas (Material da Disciplina)
1. **Documentação Oficial Android** – [developer.android.com](https://developer.android.com/)
2. **DEITEL, Paul.** *Android: Como Programar*. Bookman, 2ª Ed, 2015
3. **DEITEL, Paul et al.** *Android para Programadores: Uma Abordagem Baseada em Aplicativos*. Bookman, 2ª Ed, 2015
4. **Roteiro do Projeto Interdisciplinar** – *Próxima Etapa – App do Aluno* (Versão FINAL 2026)
5. **Inventário de Banco de Dados e Documentação da API** – material fornecido pela organização parceira
