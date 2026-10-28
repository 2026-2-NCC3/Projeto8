const express = require('express');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const crypto = require('crypto');
const QRCode = require('qrcode');
const db = require('../db/connection');
const { autenticarAdmin, JWT_SECRET } = require('../middleware/auth');

const router = express.Router();

/**
 * Módulo web administrativo (Entrega 2).
 *
 * O documento do projeto pede "um módulo web administrativo e/ou uma camada de
 * integração que represente as funções necessárias para alimentar o
 * aplicativo". É isto: a organização cria cursos e encontros, acompanha
 * presença, dispara comunicados e emite certificados — e tudo isso aparece no
 * app do aluno na sincronização seguinte.
 */

// ---------------------------------------------------------------------------
// Login do administrador (rota pública)
// ---------------------------------------------------------------------------
router.post('/admin/login', (req, res) => {
  const { email, senha } = req.body;
  if (!email || !senha) return res.status(400).json({ erro: 'Informe e-mail e senha.' });

  const admin = db.prepare('SELECT * FROM administradores WHERE email = ?').get(String(email).toLowerCase());
  if (!admin || !bcrypt.compareSync(senha, admin.senha_hash)) {
    return res.status(401).json({ erro: 'E-mail ou senha inválidos.' });
  }

  const token = jwt.sign({ adminId: admin.id, tipo: 'admin' }, JWT_SECRET, { expiresIn: '1d' });
  res.json({ token, admin: { id: admin.id, nome: admin.nome, email: admin.email } });
});

// ---------------------------------------------------------------------------
// Daqui para baixo, tudo exige token de administrador
// ---------------------------------------------------------------------------
router.use('/admin', autenticarAdmin);

/** Painel inicial: números que a ONG acompanha. */
router.get('/admin/metricas', (req, res) => {
  const um = (sql, ...p) => db.prepare(sql).get(...p);

  const totalAlunos = um('SELECT COUNT(*) AS t FROM alunos').t;
  const totalCursos = um('SELECT COUNT(*) AS t FROM cursos WHERE ativo = 1').t;
  const totalInscricoes = um('SELECT COUNT(*) AS t FROM inscricoes').t;
  const totalPresencas = um('SELECT COUNT(*) AS t FROM presencas').t;
  const totalCertificados = um('SELECT COUNT(*) AS t FROM certificados').t;

  // Frequência média: presenças registradas dividido pelas presenças possíveis
  // (cada aluno inscrito em um curso poderia comparecer a todos os encontros).
  const possiveis = um(`
    SELECT COUNT(*) AS t
    FROM inscricoes i
    JOIN atividades a ON a.curso_id = i.curso_id
    WHERE a.data <= date('now')
  `).t;
  const frequenciaMedia = possiveis > 0 ? Math.round((totalPresencas / possiveis) * 100) : 0;

  const cursosPorOcupacao = db.prepare(`
    SELECT c.id, c.titulo, c.vagas_totais, c.vagas_disponiveis,
           (c.vagas_totais - c.vagas_disponiveis) AS ocupadas
    FROM cursos c WHERE c.ativo = 1
    ORDER BY ocupadas DESC LIMIT 10
  `).all();

  const proximosEncontros = db.prepare(`
    SELECT a.id, a.titulo, a.data, a.hora_inicio, c.titulo AS curso,
           (SELECT COUNT(*) FROM presencas p WHERE p.atividade_id = a.id) AS presencas
    FROM atividades a JOIN cursos c ON c.id = a.curso_id
    WHERE a.data >= date('now')
    ORDER BY a.data ASC, a.hora_inicio ASC LIMIT 10
  `).all();

  res.json({
    totalAlunos, totalCursos, totalInscricoes, totalPresencas, totalCertificados,
    frequenciaMedia, cursosPorOcupacao, proximosEncontros,
  });
});

// ---------------------------------------------------------------------------
// Cursos
// ---------------------------------------------------------------------------
router.get('/admin/cursos', (req, res) => {
  const cursos = db.prepare(`
    SELECT c.*,
           (SELECT COUNT(*) FROM inscricoes i WHERE i.curso_id = c.id) AS inscritos,
           (SELECT COUNT(*) FROM atividades a WHERE a.curso_id = c.id) AS encontros
    FROM cursos c ORDER BY c.data_inicio DESC
  `).all();
  res.json(cursos);
});

router.post('/admin/cursos', (req, res) => {
  const {
    titulo, descricao, categoria, local, carga_horaria,
    data_inicio, data_fim, vagas_totais, possui_certificado,
  } = req.body;

  if (!titulo || !data_inicio) {
    return res.status(400).json({ erro: 'Título e data de início são obrigatórios.' });
  }

  const vagas = Number(vagas_totais) || 30;
  const info = db.prepare(`
    INSERT INTO cursos (titulo, descricao, categoria, local, carga_horaria,
                        data_inicio, data_fim, vagas_totais, vagas_disponiveis, possui_certificado)
    VALUES (@titulo, @descricao, @categoria, @local, @carga_horaria,
            @data_inicio, @data_fim, @vagas, @vagas, @possui_certificado)
  `).run({
    titulo,
    descricao: descricao || '',
    categoria: categoria || 'Geral',
    local: local || 'A definir',
    carga_horaria: Number(carga_horaria) || 0,
    data_inicio,
    data_fim: data_fim || data_inicio,
    vagas,
    possui_certificado: possui_certificado === false ? 0 : 1,
  });

  // Curso novo gera notificação para todos os alunos (RFM04).
  const curso = db.prepare('SELECT * FROM cursos WHERE id = ?').get(info.lastInsertRowid);
  notificarTodos('curso_novo', 'Novo curso disponível', `${curso.titulo} — inscrições abertas.`);

  res.status(201).json(curso);
});

router.patch('/admin/cursos/:id', (req, res) => {
  const curso = db.prepare('SELECT * FROM cursos WHERE id = ?').get(req.params.id);
  if (!curso) return res.status(404).json({ erro: 'Curso não encontrado.' });

  const permitidos = ['titulo', 'descricao', 'categoria', 'local', 'carga_horaria',
    'data_inicio', 'data_fim', 'vagas_totais', 'possui_certificado', 'ativo'];
  const campos = {};
  permitidos.forEach((c) => { if (req.body[c] !== undefined) campos[c] = req.body[c]; });
  if (Object.keys(campos).length === 0) {
    return res.status(400).json({ erro: 'Nenhum campo válido para atualizar.' });
  }

  const sets = Object.keys(campos).map((c) => `${c} = @${c}`).join(', ');
  db.prepare(`UPDATE cursos SET ${sets} WHERE id = @id`).run({ ...campos, id: req.params.id });
  res.json(db.prepare('SELECT * FROM cursos WHERE id = ?').get(req.params.id));
});

router.delete('/admin/cursos/:id', (req, res) => {
  // Desativa em vez de apagar: o histórico de presença e os certificados já
  // emitidos continuam válidos e precisam continuar existindo.
  const info = db.prepare('UPDATE cursos SET ativo = 0 WHERE id = ?').run(req.params.id);
  if (info.changes === 0) return res.status(404).json({ erro: 'Curso não encontrado.' });
  res.json({ ok: true, aviso: 'Curso desativado. O histórico foi preservado.' });
});

// ---------------------------------------------------------------------------
// Atividades (encontros) — é aqui que nasce o QR Code do check-in
// ---------------------------------------------------------------------------
router.get('/admin/cursos/:id/atividades', (req, res) => {
  const atividades = db.prepare(`
    SELECT a.*, (SELECT COUNT(*) FROM presencas p WHERE p.atividade_id = a.id) AS presencas
    FROM atividades a WHERE a.curso_id = ? ORDER BY a.data ASC, a.hora_inicio ASC
  `).all(req.params.id);
  res.json(atividades);
});

router.post('/admin/cursos/:id/atividades', (req, res) => {
  const curso = db.prepare('SELECT * FROM cursos WHERE id = ?').get(req.params.id);
  if (!curso) return res.status(404).json({ erro: 'Curso não encontrado.' });

  const { titulo, data, hora_inicio, hora_fim, local } = req.body;
  if (!titulo || !data || !hora_inicio) {
    return res.status(400).json({ erro: 'Título, data e hora de início são obrigatórios.' });
  }

  // Código aleatório e único por encontro. Como é sorteado (e não derivado do
  // id), ninguém consegue adivinhar o código do próximo encontro e registrar
  // presença sem estar lá.
  const qrCode = `PE-${crypto.randomBytes(6).toString('hex').toUpperCase()}`;

  const info = db.prepare(`
    INSERT INTO atividades (curso_id, titulo, data, hora_inicio, hora_fim, local, qr_code)
    VALUES (?, ?, ?, ?, ?, ?, ?)
  `).run(req.params.id, titulo, data, hora_inicio, hora_fim || hora_inicio, local || curso.local, qrCode);

  res.status(201).json(db.prepare('SELECT * FROM atividades WHERE id = ?').get(info.lastInsertRowid));
});

/**
 * QR Code do encontro, desenhado como SVG.
 *
 * É assim que o check-in acontece na prática: a equipe projeta (ou imprime)
 * esta imagem no encontro e os alunos apontam a câmera do app para ela.
 * Gerado no servidor para que o painel não dependa de nenhuma biblioteca
 * externa no navegador.
 */
router.get('/admin/atividades/:id/qrcode.svg', async (req, res) => {
  const atividade = db.prepare('SELECT * FROM atividades WHERE id = ?').get(req.params.id);
  if (!atividade) return res.status(404).json({ erro: 'Encontro não encontrado.' });

  try {
    const svg = await QRCode.toString(atividade.qr_code, {
      type: 'svg',
      margin: 1,
      width: 320,
      color: { dark: '#1E6F5C', light: '#FFFFFF' },
    });
    res.type('image/svg+xml').send(svg);
  } catch (e) {
    res.status(500).json({ erro: 'Não foi possível gerar o QR Code.' });
  }
});

router.delete('/admin/atividades/:id', (req, res) => {
  const info = db.prepare('DELETE FROM atividades WHERE id = ?').run(req.params.id);
  if (info.changes === 0) return res.status(404).json({ erro: 'Encontro não encontrado.' });
  res.json({ ok: true });
});

/** Lista de presença de um encontro — quem compareceu e quem faltou. */
router.get('/admin/atividades/:id/presencas', (req, res) => {
  const atividade = db.prepare('SELECT * FROM atividades WHERE id = ?').get(req.params.id);
  if (!atividade) return res.status(404).json({ erro: 'Encontro não encontrado.' });

  const lista = db.prepare(`
    SELECT al.id, al.nome, al.email,
           p.registrado_em,
           CASE WHEN p.id IS NULL THEN 0 ELSE 1 END AS presente
    FROM inscricoes i
    JOIN alunos al ON al.id = i.aluno_id
    LEFT JOIN presencas p ON p.aluno_id = al.id AND p.atividade_id = @atividadeId
    WHERE i.curso_id = @cursoId
    ORDER BY presente DESC, al.nome ASC
  `).all({ atividadeId: atividade.id, cursoId: atividade.curso_id });

  res.json({
    atividade,
    total: lista.length,
    presentes: lista.filter((a) => a.presente).length,
    lista: lista.map((a) => ({ ...a, presente: Boolean(a.presente) })),
  });
});

// ---------------------------------------------------------------------------
// Alunos
// ---------------------------------------------------------------------------
router.get('/admin/alunos', (req, res) => {
  const busca = req.query.busca ? `%${req.query.busca}%` : '%';
  const alunos = db.prepare(`
    SELECT a.id, a.nome, a.email, a.escola, a.serie, a.cidade, a.pontos, a.nivel,
           (SELECT COUNT(*) FROM inscricoes i WHERE i.aluno_id = a.id) AS cursos,
           (SELECT COUNT(*) FROM presencas p WHERE p.aluno_id = a.id) AS presencas
    FROM alunos a
    WHERE a.nome LIKE ? OR a.email LIKE ?
    ORDER BY a.nome ASC
  `).all(busca, busca);
  res.json(alunos);
});

router.get('/admin/alunos/:id', (req, res) => {
  const aluno = db.prepare(`
    SELECT id, nome, email, escola, serie, cidade, pontos, nivel, criado_em
    FROM alunos WHERE id = ?
  `).get(req.params.id);
  if (!aluno) return res.status(404).json({ erro: 'Aluno não encontrado.' });

  const cursos = db.prepare(`
    SELECT c.id, c.titulo, i.status,
           (SELECT COUNT(*) FROM atividades a WHERE a.curso_id = c.id AND a.data <= date('now')) AS encontros_ate_hoje,
           (SELECT COUNT(*) FROM presencas p
            JOIN atividades a2 ON a2.id = p.atividade_id
            WHERE p.aluno_id = @alunoId AND a2.curso_id = c.id) AS presencas
    FROM inscricoes i JOIN cursos c ON c.id = i.curso_id
    WHERE i.aluno_id = @alunoId
  `).all({ alunoId: req.params.id });

  // Frequência por curso: o critério que decide quem pode receber certificado.
  const comFrequencia = cursos.map((c) => ({
    ...c,
    frequencia: c.encontros_ate_hoje > 0
      ? Math.round((c.presencas / c.encontros_ate_hoje) * 100)
      : null,
  }));

  res.json({ aluno, cursos: comFrequencia });
});

// ---------------------------------------------------------------------------
// Comunicados e notificações
// ---------------------------------------------------------------------------
function notificarTodos(tipo, titulo, corpo, mensagemId = null) {
  const alunos = db.prepare('SELECT id FROM alunos').all();
  const inserir = db.prepare(`
    INSERT INTO notificacoes (aluno_id, mensagem_id, tipo, titulo, corpo)
    VALUES (?, ?, ?, ?, ?)
  `);
  const lote = db.transaction((lista) => {
    lista.forEach((a) => inserir.run(a.id, mensagemId, tipo, titulo, corpo));
  });
  lote(alunos);
  return alunos.length;
}

function notificarCurso(cursoId, tipo, titulo, corpo, mensagemId = null) {
  const alunos = db.prepare('SELECT aluno_id AS id FROM inscricoes WHERE curso_id = ?').all(cursoId);
  const inserir = db.prepare(`
    INSERT INTO notificacoes (aluno_id, mensagem_id, tipo, titulo, corpo)
    VALUES (?, ?, ?, ?, ?)
  `);
  const lote = db.transaction((lista) => {
    lista.forEach((a) => inserir.run(a.id, mensagemId, tipo, titulo, corpo));
  });
  lote(alunos);
  return alunos.length;
}

router.get('/admin/mensagens', (req, res) => {
  const mensagens = db.prepare(`
    SELECT m.*, c.titulo AS curso,
           (SELECT COUNT(*) FROM notificacoes n WHERE n.mensagem_id = m.id) AS enviadas,
           (SELECT COUNT(*) FROM notificacoes n WHERE n.mensagem_id = m.id AND n.lida = 1) AS lidas
    FROM mensagens m LEFT JOIN cursos c ON c.id = m.curso_id
    ORDER BY m.criado_em DESC
  `).all();
  res.json(mensagens);
});

router.post('/admin/mensagens', (req, res) => {
  const { titulo, corpo, destinatario_tipo, curso_id } = req.body;
  if (!titulo || !corpo) return res.status(400).json({ erro: 'Título e corpo são obrigatórios.' });

  const tipo = destinatario_tipo === 'curso' ? 'curso' : 'todos';
  if (tipo === 'curso' && !curso_id) {
    return res.status(400).json({ erro: 'Selecione o curso de destino.' });
  }

  const info = db.prepare(`
    INSERT INTO mensagens (titulo, corpo, destinatario_tipo, curso_id)
    VALUES (?, ?, ?, ?)
  `).run(titulo, corpo, tipo, tipo === 'curso' ? curso_id : null);

  // O comunicado vira notificação na caixa de cada aluno alcançado (RFM13).
  const alcance = tipo === 'curso'
    ? notificarCurso(curso_id, 'mensagem', titulo, corpo, info.lastInsertRowid)
    : notificarTodos('mensagem', titulo, corpo, info.lastInsertRowid);

  res.status(201).json({
    mensagem: db.prepare('SELECT * FROM mensagens WHERE id = ?').get(info.lastInsertRowid),
    notificados: alcance,
  });
});

// ---------------------------------------------------------------------------
// Certificados
// ---------------------------------------------------------------------------
/** Alunos que já cumpriram o critério de frequência e ainda não têm certificado. */
router.get('/admin/certificados/elegiveis', (req, res) => {
  const minimo = Number(req.query.frequencia_minima) || 75;

  const candidatos = db.prepare(`
    SELECT al.id AS aluno_id, al.nome AS aluno, c.id AS curso_id, c.titulo AS curso,
           c.carga_horaria,
           (SELECT COUNT(*) FROM atividades a WHERE a.curso_id = c.id AND a.data <= date('now')) AS encontros,
           (SELECT COUNT(*) FROM presencas p
            JOIN atividades a2 ON a2.id = p.atividade_id
            WHERE p.aluno_id = al.id AND a2.curso_id = c.id) AS presencas
    FROM inscricoes i
    JOIN alunos al ON al.id = i.aluno_id
    JOIN cursos c ON c.id = i.curso_id
    WHERE c.possui_certificado = 1
      AND NOT EXISTS (SELECT 1 FROM certificados ce
                      WHERE ce.aluno_id = al.id AND ce.curso_id = c.id)
  `).all();

  const elegiveis = candidatos
    .filter((c) => c.encontros > 0)
    .map((c) => ({ ...c, frequencia: Math.round((c.presencas / c.encontros) * 100) }))
    .filter((c) => c.frequencia >= minimo)
    .sort((a, b) => b.frequencia - a.frequencia);

  res.json({ frequencia_minima: minimo, total: elegiveis.length, elegiveis });
});

router.post('/admin/certificados', (req, res) => {
  const { aluno_id, curso_id } = req.body;
  if (!aluno_id || !curso_id) return res.status(400).json({ erro: 'Informe aluno e curso.' });

  const curso = db.prepare('SELECT * FROM cursos WHERE id = ?').get(curso_id);
  if (!curso) return res.status(404).json({ erro: 'Curso não encontrado.' });

  try {
    const url = `https://proximaetapa.org.br/certificados/${crypto.randomBytes(8).toString('hex')}`;
    db.prepare('INSERT INTO certificados (aluno_id, curso_id, certificado_url) VALUES (?, ?, ?)')
      .run(aluno_id, curso_id, url);

    db.prepare(`
      INSERT INTO notificacoes (aluno_id, tipo, titulo, corpo)
      VALUES (?, 'certificado', 'Certificado disponível', ?)
    `).run(aluno_id, `Seu certificado de ${curso.titulo} já está disponível no app.`);

    res.status(201).json({ ok: true, certificado_url: url });
  } catch (e) {
    if (String(e).includes('UNIQUE')) {
      return res.status(409).json({ erro: 'Este aluno já possui certificado deste curso.' });
    }
    res.status(500).json({ erro: 'Erro ao emitir certificado.' });
  }
});

// ---------------------------------------------------------------------------
// Chat de dúvidas — lado da equipe (RFM12)
// ---------------------------------------------------------------------------
router.get('/admin/chat', (req, res) => {
  const conversas = db.prepare(`
    SELECT al.id AS aluno_id, al.nome AS aluno,
           (SELECT texto FROM chat_mensagens cm WHERE cm.aluno_id = al.id
            ORDER BY cm.criado_em DESC LIMIT 1) AS ultima_mensagem,
           (SELECT criado_em FROM chat_mensagens cm WHERE cm.aluno_id = al.id
            ORDER BY cm.criado_em DESC LIMIT 1) AS ultima_em,
           (SELECT COUNT(*) FROM chat_mensagens cm WHERE cm.aluno_id = al.id) AS total
    FROM alunos al
    WHERE EXISTS (SELECT 1 FROM chat_mensagens cm WHERE cm.aluno_id = al.id)
    ORDER BY ultima_em DESC
  `).all();
  res.json(conversas);
});

router.get('/admin/chat/:alunoId', (req, res) => {
  const mensagens = db.prepare(`
    SELECT * FROM chat_mensagens WHERE aluno_id = ? ORDER BY criado_em ASC
  `).all(req.params.alunoId);
  res.json(mensagens);
});

router.post('/admin/chat/:alunoId', (req, res) => {
  const { texto } = req.body;
  if (!texto) return res.status(400).json({ erro: 'Mensagem vazia.' });

  db.prepare('INSERT INTO chat_mensagens (aluno_id, autor, texto) VALUES (?, ?, ?)')
    .run(req.params.alunoId, 'equipe', texto);

  db.prepare(`
    INSERT INTO notificacoes (aluno_id, tipo, titulo, corpo)
    VALUES (?, 'mensagem', 'Resposta da equipe', ?)
  `).run(req.params.alunoId, texto.slice(0, 120));

  res.status(201).json({ ok: true });
});

module.exports = router;
