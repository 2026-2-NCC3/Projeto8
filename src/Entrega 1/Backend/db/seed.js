// Popula o banco de desenvolvimento com dados FICTÍCIOS.
// Nunca usar dados reais de alunos aqui (regra do projeto / LGPD).
const bcrypt = require('bcryptjs');
const db = require('./connection');

const nomes = [
  'Ana Souza', 'Bruno Lima', 'Carla Mendes', 'Diego Alves', 'Elisa Rocha',
  'Fábio Nunes', 'Gabriela Dias', 'Henrique Melo', 'Isabela Cruz', 'João Pedro',
  'Karina Silva', 'Lucas Ferreira', 'Mariana Costa', 'Nicolas Barros', 'Olívia Ramos',
  'Pedro Teixeira', 'Queila Santos', 'Rafael Pinto', 'Sofia Martins', 'Thiago Rocha',
  'Ursula Carvalho', 'Vinícius Araújo', 'Wesley Gomes', 'Yasmin Ribeiro', 'Zeca Moreira',
  'Amanda Farias', 'Bruna Castro', 'Caio Correia', 'Débora Vieira', 'Eduardo Batista'
];
const escolas = ['EE Prof. João XXIII', 'EE Dom Pedro II', 'EE Castro Alves', 'EE Machado de Assis', 'EE Cora Coralina'];
const cidades = ['São Paulo', 'Guarulhos', 'Osasco', 'Santo André', 'Diadema'];
const series = ['1º EM', '2º EM', '3º EM'];

function reset() {
  db.exec(`
    DELETE FROM notificacoes;
    DELETE FROM mensagens;
    DELETE FROM resultados_perfil;
    DELETE FROM alternativas_perfil;
    DELETE FROM perguntas_perfil;
    DELETE FROM certificados;
    DELETE FROM presencas;
    DELETE FROM inscricoes;
    DELETE FROM atividades;
    DELETE FROM cursos;
    DELETE FROM alunos;
  `);
}

function seed() {
  reset();
  const senhaHash = bcrypt.hashSync('123456', 8); // senha padrão de todos os alunos fictícios (demo)

  const insertAluno = db.prepare(`INSERT INTO alunos (nome, email, senha_hash, escola, serie, cidade, pontos, nivel)
    VALUES (@nome, @email, @senha_hash, @escola, @serie, @cidade, @pontos, @nivel)`);

  const alunoIds = [];
  nomes.forEach((nome, i) => {
    const email = nome.toLowerCase().normalize('NFD').replace(/[̀-ͯ]/g, '').replace(/\s+/g, '.') + '@aluno.proximaetapa.org.br';
    const pontos = Math.floor(Math.random() * 500);
    const nivel = pontos > 350 ? 'Referência' : pontos > 150 ? 'Em trilha' : 'Explorador';
    const info = insertAluno.run({
      nome, email, senha_hash: senhaHash,
      escola: escolas[i % escolas.length],
      serie: series[i % series.length],
      cidade: cidades[i % cidades.length],
      pontos, nivel
    });
    alunoIds.push(info.lastInsertRowid);
  });

  const cursosData = [
    { titulo: 'Introdução à Programação', categoria: 'Tecnologia', carga_horaria: 20, local: 'FECAP - Liberdade', encontros: 4 },
    { titulo: 'Oficina de Redação para o ENEM', categoria: 'Educação', carga_horaria: 12, local: 'Online', encontros: 3 },
    { titulo: 'Orientação Profissional', categoria: 'Carreira', carga_horaria: 8, local: 'FECAP - Liberdade', encontros: 2 },
    { titulo: 'Inglês para o Mundo do Trabalho', categoria: 'Idiomas', carga_horaria: 30, local: 'Online', encontros: 6 },
    { titulo: 'Design Thinking na Prática', categoria: 'Inovação', carga_horaria: 16, local: 'Espaço Próxima Etapa', encontros: 4 },
    { titulo: 'Finanças Pessoais para Jovens', categoria: 'Carreira', carga_horaria: 10, local: 'Online', encontros: 2 },
    { titulo: 'Introdução a Dados e IA', categoria: 'Tecnologia', carga_horaria: 24, local: 'FECAP - Liberdade', encontros: 5 },
  ];

  const insertCurso = db.prepare(`INSERT INTO cursos (titulo, descricao, categoria, local, carga_horaria, data_inicio, data_fim, vagas_totais, vagas_disponiveis, possui_certificado)
    VALUES (@titulo, @descricao, @categoria, @local, @carga_horaria, @data_inicio, @data_fim, @vagas_totais, @vagas_disponiveis, 1)`);
  const insertAtividade = db.prepare(`INSERT INTO atividades (curso_id, titulo, data, hora_inicio, hora_fim, local, qr_code)
    VALUES (@curso_id, @titulo, @data, @hora_inicio, @hora_fim, @local, @qr_code)`);

  const cursoIds = [];
  const atividadesByCurso = {};
  const base = new Date('2026-09-01T00:00:00');

  cursosData.forEach((c, idx) => {
    const inicio = new Date(base.getTime() + idx * 3 * 24 * 3600 * 1000);
    const fim = new Date(inicio.getTime() + c.encontros * 7 * 24 * 3600 * 1000);
    const vagasTotais = 25 + (idx % 3) * 5;
    const vagasOcupadas = Math.floor(Math.random() * vagasTotais);
    const info = insertCurso.run({
      titulo: c.titulo,
      descricao: `Curso de ${c.titulo}, oferecido pela Próxima Etapa.`,
      categoria: c.categoria,
      local: c.local,
      carga_horaria: c.carga_horaria,
      data_inicio: inicio.toISOString().slice(0, 10),
      data_fim: fim.toISOString().slice(0, 10),
      vagas_totais: vagasTotais,
      vagas_disponiveis: Math.max(vagasTotais - vagasOcupadas, 0),
    });
    const cursoId = info.lastInsertRowid;
    cursoIds.push(cursoId);
    atividadesByCurso[cursoId] = [];

    for (let e = 0; e < c.encontros; e++) {
      const dataEncontro = new Date(inicio.getTime() + e * 7 * 24 * 3600 * 1000);
      const infoAt = insertAtividade.run({
        curso_id: cursoId,
        titulo: `Encontro ${e + 1} - ${c.titulo}`,
        data: dataEncontro.toISOString().slice(0, 10),
        hora_inicio: '18:00',
        hora_fim: '20:00',
        local: c.local,
        qr_code: `QR-${cursoId}-${e + 1}-${Math.random().toString(36).slice(2, 8)}`
      });
      atividadesByCurso[cursoId].push(infoAt.lastInsertRowid);
    }
  });

  // Inscrições, presenças e certificados fictícios
  const insertInscricao = db.prepare(`INSERT OR IGNORE INTO inscricoes (aluno_id, curso_id, status) VALUES (?, ?, ?)`);
  const insertPresenca = db.prepare(`INSERT OR IGNORE INTO presencas (aluno_id, atividade_id) VALUES (?, ?)`);
  const insertCertificado = db.prepare(`INSERT OR IGNORE INTO certificados (aluno_id, curso_id, certificado_url) VALUES (?, ?, ?)`);

  alunoIds.forEach((alunoId) => {
    const nCursos = 1 + Math.floor(Math.random() * 3);
    const cursosEscolhidos = [...cursoIds].sort(() => 0.5 - Math.random()).slice(0, nCursos);
    cursosEscolhidos.forEach((cursoId) => {
      const status = ['inscrito', 'cursando', 'concluido'][Math.floor(Math.random() * 3)];
      insertInscricao.run(alunoId, cursoId, status);

      const atividades = atividadesByCurso[cursoId];
      atividades.forEach((atividadeId) => {
        if (Math.random() < 0.7) insertPresenca.run(alunoId, atividadeId);
      });

      if (status === 'concluido') {
        insertCertificado.run(alunoId, cursoId, `https://proximaetapa.org.br/certificados/demo-${alunoId}-${cursoId}.pdf`);
      }
    });
  });

  // Teste de perfil
  const perguntas = [
    { texto: 'Qual atividade combina mais com você?', alternativas: [
      { texto: 'Resolver problemas de lógica e matemática', perfil: 'exatas' },
      { texto: 'Escrever textos e debater ideias', perfil: 'humanas' },
      { texto: 'Cuidar de pessoas e da saúde', perfil: 'saude' },
      { texto: 'Criar desenhos, músicas ou vídeos', perfil: 'artes' },
    ]},
    { texto: 'Em um trabalho em grupo, você prefere...', alternativas: [
      { texto: 'Organizar dados e planilhas', perfil: 'exatas' },
      { texto: 'Apresentar e argumentar', perfil: 'humanas' },
      { texto: 'Ajudar colegas com dificuldades', perfil: 'saude' },
      { texto: 'Cuidar da parte visual/criativa', perfil: 'artes' },
    ]},
    { texto: 'Qual matéria você mais gosta?', alternativas: [
      { texto: 'Matemática ou Física', perfil: 'exatas' },
      { texto: 'História ou Sociologia', perfil: 'humanas' },
      { texto: 'Biologia', perfil: 'saude' },
      { texto: 'Artes', perfil: 'artes' },
    ]},
  ];
  const insertPergunta = db.prepare(`INSERT INTO perguntas_perfil (texto, ordem) VALUES (?, ?)`);
  const insertAlternativa = db.prepare(`INSERT INTO alternativas_perfil (pergunta_id, texto, perfil) VALUES (?, ?, ?)`);
  perguntas.forEach((p, i) => {
    const info = insertPergunta.run(p.texto, i);
    p.alternativas.forEach((a) => insertAlternativa.run(info.lastInsertRowid, a.texto, a.perfil));
  });

  // Mensagens e notificações
  const insertMensagem = db.prepare(`INSERT INTO mensagens (titulo, corpo, destinatario_tipo, curso_id) VALUES (?, ?, ?, ?)`);
  const m1 = insertMensagem.run('Novo curso disponível!', 'Abrimos vagas para Introdução a Dados e IA. Inscreva-se!', 'todos', null);
  const m2 = insertMensagem.run('Lembrete de encontro', 'Não esqueça do encontro desta semana.', 'todos', null);

  const insertNotificacao = db.prepare(`INSERT INTO notificacoes (aluno_id, mensagem_id, tipo, titulo, corpo) VALUES (?, ?, ?, ?, ?)`);
  alunoIds.forEach((alunoId) => {
    insertNotificacao.run(alunoId, m1.lastInsertRowid, 'curso_novo', 'Novo curso disponível!', 'Abrimos vagas para Introdução a Dados e IA.');
    if (Math.random() < 0.5) {
      insertNotificacao.run(alunoId, m2.lastInsertRowid, 'agenda', 'Lembrete de encontro', 'Não esqueça do encontro desta semana.');
    }
  });

  console.log(`Seed concluído: ${alunoIds.length} alunos, ${cursoIds.length} cursos.`);
  console.log('Login de teste: qualquer e-mail gerado acima, senha "123456"');
  console.log('Ex.: ana.souza@aluno.proximaetapa.org.br / 123456');
}

seed();
