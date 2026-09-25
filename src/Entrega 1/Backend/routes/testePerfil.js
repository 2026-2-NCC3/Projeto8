const express = require('express');
const db = require('../db/connection');
const router = express.Router();

const DESCRICOES = {
  exatas: 'Você tem afinidade com raciocínio lógico, matemática e tecnologia. Cursos de exatas e computação podem combinar com você.',
  humanas: 'Você se destaca em comunicação, argumentação e temas sociais. Cursos de humanas e comunicação podem combinar com você.',
  saude: 'Você demonstra cuidado com o próximo e interesse por biologia e saúde. Cursos da área da saúde podem combinar com você.',
  artes: 'Você tem perfil criativo e expressivo. Cursos de artes, design e comunicação visual podem combinar com você.',
};

// RFM10 - Perguntas do teste de perfil
router.get('/perfil-teste/perguntas', (req, res) => {
  const perguntas = db.prepare('SELECT id, texto, ordem FROM perguntas_perfil ORDER BY ordem').all();
  const comAlternativas = perguntas.map((p) => ({
    ...p,
    alternativas: db.prepare('SELECT id, texto FROM alternativas_perfil WHERE pergunta_id = ?').all(p.id),
  }));
  res.json(comAlternativas);
});

// RFM10 - Enviar respostas e obter resultado
// body: { respostas: { "1": 3, "2": 7, "3": 10 } }  (pergunta_id -> alternativa_id)
router.post('/perfil-teste/respostas', (req, res) => {
  const { respostas } = req.body;
  if (!respostas || typeof respostas !== 'object') {
    return res.status(400).json({ erro: 'Envie as respostas do teste.' });
  }

  const contagem = {};
  Object.values(respostas).forEach((altId) => {
    const alt = db.prepare('SELECT perfil FROM alternativas_perfil WHERE id = ?').get(altId);
    if (alt) contagem[alt.perfil] = (contagem[alt.perfil] || 0) + 1;
  });

  const perfilResultado = Object.entries(contagem).sort((a, b) => b[1] - a[1])[0]?.[0] || 'indefinido';

  db.prepare(`INSERT INTO resultados_perfil (aluno_id, respostas_json, perfil_resultado, descricao)
    VALUES (?, ?, ?, ?)`).run(req.alunoId, JSON.stringify(respostas), perfilResultado, DESCRICOES[perfilResultado] || '');

  res.status(201).json({
    perfil: perfilResultado,
    descricao: DESCRICOES[perfilResultado] || 'Resultado calculado com base nas suas respostas.',
    aviso: 'Este resultado tem finalidade de orientação educacional/profissional e não é um diagnóstico psicológico.',
  });
});

// Histórico de resultados do aluno
router.get('/perfil-teste/resultados', (req, res) => {
  const resultados = db.prepare(`
    SELECT id, perfil_resultado, descricao, concluido_em FROM resultados_perfil
    WHERE aluno_id = ? ORDER BY concluido_em DESC
  `).all(req.alunoId);
  res.json(resultados);
});

module.exports = router;
