const jwt = require('jsonwebtoken');

const JWT_SECRET = process.env.JWT_SECRET || 'dev-secret-troque-em-producao';

function autenticar(req, res, next) {
  const header = req.headers.authorization || '';
  const token = header.startsWith('Bearer ') ? header.slice(7) : null;
  if (!token) {
    return res.status(401).json({ erro: 'Token não informado. Faça login novamente.' });
  }
  try {
    const payload = jwt.verify(token, JWT_SECRET);
    req.alunoId = payload.alunoId;
    next();
  } catch (e) {
    return res.status(401).json({ erro: 'Sessão inválida ou expirada. Faça login novamente.' });
  }
}

module.exports = { autenticar, JWT_SECRET };
