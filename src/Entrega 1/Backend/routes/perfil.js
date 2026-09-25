const express = require('express');
const db = require('../db/connection');
const router = express.Router();

// RFM02 - Página inicial: resumo da jornada do aluno
router.get('/home', (req, res) => {
  const alunoId = req.alunoId;
  const proximaAtividade = db.prepare(`
    SELECT a.id, a.titulo, a.data, a.hora_inicio, c.titulo AS curso
    FROM atividades a
    JOIN cursos c ON c.id = a.curso_id
    JOIN inscricoes i ON i.curso_id = c.id AND i.aluno_id = ?
    WHERE a.data >= date('now')
    ORDER BY a.data ASC, a.hora_inicio ASC
    LIMIT 3
  `).all(alunoId);

  const cursosInscritos = db.prepare(`
    SELECT COUNT(*) AS total FROM inscricoes WHERE aluno_id = ?
  `).get(alunoId).total;

  const notificacoesNaoLidas = db.prepare(`
    SELECT COUNT(*) AS total FROM notificacoes WHERE aluno_id = ? AND lida = 0
  `).get(alunoId).total;

  const aluno = db.prepare('SELECT id, nome, pontos, nivel FROM alunos WHERE id = ?').get(alunoId);

  res.json({ aluno, proximasAtividades: proximaAtividade, cursosInscritos, notificacoesNaoLidas });
});

// RFM14 - Perfil: consultar e atualizar dados pessoais
router.get('/perfil', (req, res) => {
  const aluno = db.prepare('SELECT id, nome, email, escola, serie, cidade, pontos, nivel, avatar_url FROM alunos WHERE id = ?').get(req.alunoId);
  res.json(aluno);
});

router.patch('/perfil', (req, res) => {
  const campos = ['escola', 'serie', 'cidade', 'avatar_url'];
  const atualizacoes = {};
  campos.forEach((c) => { if (req.body[c] !== undefined) atualizacoes[c] = req.body[c]; });
  if (Object.keys(atualizacoes).length === 0) {
    return res.status(400).json({ erro: 'Nenhum campo válido para atualizar.' });
  }
  const sets = Object.keys(atualizacoes).map((c) => `${c} = @${c}`).join(', ');
  db.prepare(`UPDATE alunos SET ${sets} WHERE id = @id`).run({ ...atualizacoes, id: req.alunoId });
  const aluno = db.prepare('SELECT id, nome, email, escola, serie, cidade, pontos, nivel, avatar_url FROM alunos WHERE id = ?').get(req.alunoId);
  res.json(aluno);
});

// RFM08 - Card digital do aluno
router.get('/card', (req, res) => {
  const aluno = db.prepare('SELECT id, nome, escola, serie, pontos, nivel, avatar_url FROM alunos WHERE id = ?').get(req.alunoId);
  const cursosAtivos = db.prepare(`
    SELECT COUNT(*) AS total FROM inscricoes WHERE aluno_id = ? AND status IN ('inscrito','cursando')
  `).get(req.alunoId).total;
  res.json({
    ...aluno,
    cursosAtivos,
    codigoCard: `PE-${String(aluno.id).padStart(6, '0')}`,
    validoDesde: new Date().toISOString().slice(0, 10),
  });
});

module.exports = router;
