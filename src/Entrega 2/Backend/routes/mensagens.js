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

// RFM12 - Chat de dúvidas.
//
// Na Entrega 1 o histórico ficava em um Map na memória do processo e sumia a
// cada reinício do servidor (o plano gratuito do Render reinicia sozinho).
// Na Entrega 2 passou a ser gravado na tabela `chat_mensagens`, então a
// conversa sobrevive a reinícios e a equipe consegue responder pelo painel
// administrativo.
router.get('/chat', (req, res) => {
  const mensagens = db.prepare(`
    SELECT id, autor, texto, criado_em FROM chat_mensagens
    WHERE aluno_id = ? ORDER BY criado_em ASC
  `).all(req.alunoId);
  res.json(mensagens);
});

router.post('/chat', (req, res) => {
  const { texto } = req.body;
  if (!texto || !String(texto).trim()) {
    return res.status(400).json({ erro: 'Mensagem vazia.' });
  }

  const inserir = db.prepare('INSERT INTO chat_mensagens (aluno_id, autor, texto) VALUES (?, ?, ?)');
  inserir.run(req.alunoId, 'aluno', String(texto).trim());

  // Confirmação automática de recebimento. A resposta de verdade vem pelo
  // painel administrativo, mas o aluno precisa saber que a mensagem chegou —
  // e só enviamos esse aviso uma vez por conversa, para não repetir a cada
  // mensagem enviada.
  const jaRespondido = db.prepare(`
    SELECT 1 FROM chat_mensagens WHERE aluno_id = ? AND autor = 'equipe' LIMIT 1
  `).get(req.alunoId);

  if (!jaRespondido) {
    inserir.run(req.alunoId, 'equipe',
      'Recebemos sua mensagem! A equipe da Próxima Etapa responde em horário comercial.');
  }

  const mensagens = db.prepare(`
    SELECT id, autor, texto, criado_em FROM chat_mensagens
    WHERE aluno_id = ? ORDER BY criado_em ASC
  `).all(req.alunoId);

  res.status(201).json(mensagens);
});

module.exports = router;
