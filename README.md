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
│   └── 📂 Entrega 1/
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
│       ├── 📂 Analise Descritiva de Dados/
│       ├── 📂 POO e Estrutura de Dados/
│       ├── 📂 Dispositivos Moveis/
│       └── 📂 Projeto Interdisciplinar (Aplicativo Movel)/
│
└── 📂 imagens/
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

## Em breve

A versão para testes diretamente em dispositivos móveis será disponibilizada futuramente (Entrega 2), junto com o backend publicado em ambiente acessível na nuvem.

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
