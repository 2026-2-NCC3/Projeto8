const express = require('express');
const db = require('../db/connection');
const router = express.Router();

// RFM06 - Registrar presença via QR Code
router.post('/presencas/checkin', (req, res) => {
  const { qr_code } = req.body;
  if (!qr_code) return res.status(400).json({ erro: 'Código QR não informado.' });

  const atividade = db.prepare('SELECT * FROM atividades WHERE qr_code = ?').get(qr_code);
  if (!atividade) return res.status(404).json({ erro: 'QR Code inválido ou expirado.' });

  const inscrito = db.prepare('SELECT 1 FROM inscricoes WHERE aluno_id = ? AND curso_id = ?')
    .get(req.alunoId, atividade.curso_id);
  if (!inscrito) return res.status(403).json({ erro: 'Você não está inscrito neste curso.' });

  try {
    db.prepare('INSERT INTO presencas (aluno_id, atividade_id) VALUES (?, ?)').run(req.alunoId, atividade.id);
    res.status(201).json({ ok: true, atividade: atividade.titulo, data: atividade.data });
  } catch (e) {
    if (String(e).includes('UNIQUE')) {
      return res.status(409).json({ erro: 'Presença já registrada para este encontro.' });
    }
    res.status(500).json({ erro: 'Erro ao registrar presença.' });
  }
});

// RFM07 - Histórico de presença
router.get('/presencas/historico', (req, res) => {
  const historico = db.prepare(`
    SELECT p.id, p.registrado_em, a.titulo AS atividade, a.data, c.titulo AS curso
    FROM presencas p
    JOIN atividades a ON a.id = p.atividade_id
    JOIN cursos c ON c.id = a.curso_id
    WHERE p.aluno_id = ?
    ORDER BY a.data DESC
  `).all(req.alunoId);
  res.json(historico);
});

module.exports = router;
