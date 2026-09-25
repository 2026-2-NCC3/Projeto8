const express = require('express');
const db = require('../db/connection');
const router = express.Router();

// RFM11 - Mensagens e comunicados
router.get('/mensagens', (req, res) => {
  const mensagens = db.prepare(`
    SELECT m.* FROM mensagens m
    WHERE m.destinatario_tipo = 'todos'
       OR m.curso_id IN (SELECT curso_id FROM inscricoes WHERE aluno_id = ?)
    ORDER BY m.criado_em DESC
  `).all(req.alunoId);
  res.json(mensagens);
});

// RFM13 - Notificações do aluno
router.get('/notificacoes', (req, res) => {
  const notificacoes = db.prepare(`
    SELECT * FROM notificacoes WHERE aluno_id = ? ORDER BY criado_em DESC
  `).all(req.alunoId);
  res.json(notificacoes);
});

router.patch('/notificacoes/:id/lida', (req, res) => {
  const info = db.prepare(`UPDATE notificacoes SET lida = 1 WHERE id = ? AND aluno_id = ?`)
    .run(req.params.id, req.alunoId);
  if (info.changes === 0) return res.status(404).json({ erro: 'Notificação não encontrada.' });
  res.json({ ok: true });
});

router.patch('/notificacoes/lidas/todas', (req, res) => {
  db.prepare(`UPDATE notificacoes SET lida = 1 WHERE aluno_id = ?`).run(req.alunoId);
  res.json({ ok: true });
});

// RFM12 - Chat de dúvidas (versão simples: mensagens de texto armazenadas por aluno)
// Para o protótipo, tratamos como uma lista simples em memória por aluno.
const chatPorAluno = new Map();

router.get('/chat', (req, res) => {
  res.json(chatPorAluno.get(req.alunoId) || []);
});

router.post('/chat', (req, res) => {
  const { texto } = req.body;
  if (!texto) return res.status(400).json({ erro: 'Mensagem vazia.' });
  const lista = chatPorAluno.get(req.alunoId) || [];
  lista.push({ autor: 'aluno', texto, data: new Date().toISOString() });
  // resposta automática simples (placeholder para o canal real da equipe de suporte)
  lista.push({ autor: 'equipe', texto: 'Obrigado pela mensagem! Nossa equipe responderá em breve.', data: new Date().toISOString() });
  chatPorAluno.set(req.alunoId, lista);
  res.status(201).json(lista);
});

module.exports = router;
