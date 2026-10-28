const jwt = require('jsonwebtoken');

const JWT_SECRET = process.env.JWT_SECRET || 'dev-secret-troque-em-producao';

/** Extrai o token do cabeçalho `Authorization: Bearer <token>`. */
function extrairToken(req) {
  const header = req.headers.authorization || '';
  return header.startsWith('Bearer ') ? header.slice(7) : null;
}

/** Protege as rotas do aluno (app Android). */
function autenticar(req, res, next) {
  const token = extrairToken(req);
  if (!token) {
    return res.status(401).json({ erro: 'Token não informado. Faça login novamente.' });
  }
  try {
    const payload = jwt.verify(token, JWT_SECRET);
    if (payload.tipo && payload.tipo !== 'aluno') {
      return res.status(403).json({ erro: 'Este token não pertence a um aluno.' });
    }
    req.alunoId = payload.alunoId;
    next();
  } catch (e) {
    return res.status(401).json({ erro: 'Sessão inválida ou expirada. Faça login novamente.' });
  }
}

/**
 * Protege as rotas do módulo web administrativo (Entrega 2).
 * O token do administrador carrega `tipo: 'admin'`, então um token de aluno
 * não abre o painel e vice-versa — mesmo segredo, públicos separados.
 */
function autenticarAdmin(req, res, next) {
  const token = extrairToken(req);
  if (!token) {
    return res.status(401).json({ erro: 'Token não informado. Faça login no painel.' });
  }
  try {
    const payload = jwt.verify(token, JWT_SECRET);
    if (payload.tipo !== 'admin') {
      return res.status(403).json({ erro: 'Acesso restrito à equipe da organização.' });
    }
    req.adminId = payload.adminId;
    next();
  } catch (e) {
    return res.status(401).json({ erro: 'Sessão inválida ou expirada. Faça login novamente.' });
  }
}

module.exports = { autenticar, autenticarAdmin, JWT_SECRET };
