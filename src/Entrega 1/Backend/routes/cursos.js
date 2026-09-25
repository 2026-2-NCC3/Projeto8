const express = require('express');
const db = require('../db/connection');
const router = express.Router();

// RFM03 - Cursos disponíveis
router.get('/cursos', (req, res) => {
  const cursos = db.prepare(`SELECT * FROM cursos WHERE ativo = 1 ORDER BY data_inicio ASC`).all();
  res.json(cursos);
});

// RFM03 - Detalhe de um curso
router.get('/cursos/:id', (req, res) => {
  const curso = db.prepare(`SELECT * FROM cursos WHERE id = ?`).get(req.params.id);
  if (!curso) return res.status(404).json({ erro: 'Curso não encontrado.' });
  const atividades = db.prepare(`SELECT * FROM atividades WHERE curso_id = ? ORDER BY data ASC`).all(req.params.id);
  res.json({ ...curso, atividades });
});

// RFM03 - Cursos em que o aluno está inscrito
router.get('/cursos/inscritos/meus', (req, res) => {
  const cursos = db.prepare(`
    SELECT c.*, i.status AS status_inscricao, i.criado_em AS inscrito_em
    FROM cursos c
    JOIN inscricoes i ON i.curso_id = c.id
    WHERE i.aluno_id = ?
    ORDER BY c.data_inicio ASC
  `).all(req.alunoId);
  res.json(cursos);
});

// Inscrição em um curso
router.post('/cursos/:id/inscricao', (req, res) => {
  const curso = db.prepare('SELECT * FROM cursos WHERE id = ?').get(req.params.id);
  if (!curso) return res.status(404).json({ erro: 'Curso não encontrado.' });
  if (curso.vagas_disponiveis <= 0) return res.status(409).json({ erro: 'Não há vagas disponíveis.' });

  try {
    db.prepare(`INSERT INTO inscricoes (aluno_id, curso_id, status) VALUES (?, ?, 'inscrito')`)
      .run(req.alunoId, req.params.id);
    db.prepare(`UPDATE cursos SET vagas_disponiveis = vagas_disponiveis - 1 WHERE id = ?`).run(req.params.id);
    res.status(201).json({ ok: true });
  } catch (e) {
    if (String(e).includes('UNIQUE')) {
      return res.status(409).json({ erro: 'Você já está inscrito neste curso.' });
    }
    res.status(500).json({ erro: 'Erro ao realizar inscrição.' });
  }
});

// RFM05 - Agenda individual (todos os encontros dos cursos em que o aluno está inscrito)
router.get('/agenda', (req, res) => {
  const eventos = db.prepare(`
    SELECT a.id, a.titulo, a.data, a.hora_inicio, a.hora_fim, a.local, c.titulo AS curso, c.id AS curso_id,
      EXISTS(SELECT 1 FROM presencas p WHERE p.atividade_id = a.id AND p.aluno_id = ?) AS presenca_confirmada
    FROM atividades a
    JOIN cursos c ON c.id = a.curso_id
    JOIN inscricoes i ON i.curso_id = c.id AND i.aluno_id = ?
    ORDER BY a.data ASC, a.hora_inicio ASC
  `).all(req.alunoId, req.alunoId);
  // SQLite/better-sqlite3 devolve EXISTS(...) como número (0/1), mas o app
  // Android espera um booleano JSON de verdade (true/false) nesse campo.
  const eventosComBooleano = eventos.map((e) => ({
    ...e,
    presenca_confirmada: Boolean(e.presenca_confirmada),
  }));
  res.json(eventosComBooleano);
});

module.exports = router;
