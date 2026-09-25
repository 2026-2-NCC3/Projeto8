const express = require('express');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const db = require('../db/connection');
const { JWT_SECRET } = require('../middleware/auth');

const router = express.Router();

// RFM01 - Autenticação
router.post('/login', (req, res) => {
  const { email, senha } = req.body;
  if (!email || !senha) {
    return res.status(400).json({ erro: 'Informe e-mail e senha.' });
  }
  const aluno = db.prepare('SELECT * FROM alunos WHERE email = ?').get(email.toLowerCase());
  if (!aluno || !bcrypt.compareSync(senha, aluno.senha_hash)) {
    return res.status(401).json({ erro: 'E-mail ou senha inválidos.' });
  }
  const token = jwt.sign({ alunoId: aluno.id }, JWT_SECRET, { expiresIn: '7d' });
  delete aluno.senha_hash;
  res.json({ token, aluno });
});

// Logout é stateless aqui: o app descarta o token localmente.
router.post('/logout', (req, res) => {
  res.json({ ok: true });
});

module.exports = router;
