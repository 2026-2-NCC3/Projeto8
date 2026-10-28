const express = require('express');
const db = require('../db/connection');
const router = express.Router();

// RFM09 - Certificados do aluno
router.get('/certificados', (req, res) => {
  const certificados = db.prepare(`
    SELECT cert.id, cert.certificado_url, cert.emitido_em, c.titulo AS curso, c.carga_horaria
    FROM certificados cert
    JOIN cursos c ON c.id = cert.curso_id
    WHERE cert.aluno_id = ?
    ORDER BY cert.emitido_em DESC
  `).all(req.alunoId);
  res.json(certificados);
});

module.exports = router;
