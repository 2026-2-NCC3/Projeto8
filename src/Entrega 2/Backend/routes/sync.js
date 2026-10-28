const express = require('express');
const db = require('../db/connection');
const router = express.Router();

/**
 * RFM15 — Sincronização (função principal da Entrega 2).
 *
 * O app guarda os dados do aluno localmente (Room) para funcionar sem
 * internet. Estas duas rotas são as duas pontas dessa sincronização:
 *
 *  - GET  /api/sync            → servidor manda o que mudou (download)
 *  - POST /api/sync/presencas  → app manda a fila de check-ins feitos offline (upload)
 */

/** Data/hora do servidor no mesmo formato usado pelo SQLite (UTC). */
function agora() {
  return new Date().toISOString().replace('T', ' ').slice(0, 19);
}

/**
 * Baixa o estado do aluno em UMA requisição.
 *
 * Sem `desde`, devolve o pacote completo (primeiro login, ou app reinstalado).
 * Com `desde`, devolve só o que mudou depois daquele instante — é o que o app
 * usa no dia a dia, para gastar menos dados e bateria do aluno.
 *
 * A resposta sempre traz `servidor_em`: o app guarda esse valor e o envia como
 * `desde` na próxima vez. Usar o relógio do servidor (e não o do celular)
 * evita perder registros quando a hora do aparelho está errada.
 */
router.get('/sync', (req, res) => {
  const alunoId = req.alunoId;
  const desde = req.query.desde || null;
  const completo = !desde;
  const servidorEm = agora();

  // Filtro incremental aplicado às tabelas que têm carimbo de atualização.
  const filtro = (coluna) => (completo ? '' : ` AND ${coluna} > @desde`);
  const params = { alunoId, desde };

  const cursos = db.prepare(`
    SELECT * FROM cursos
    WHERE ativo = 1${filtro('atualizado_em')}
    ORDER BY data_inicio ASC
  `).all(params);

  const inscricoes = db.prepare(`
    SELECT i.id, i.curso_id, i.status, i.criado_em
    FROM inscricoes i
    WHERE i.aluno_id = @alunoId${filtro('i.atualizado_em')}
  `).all(params);

  const agenda = db.prepare(`
    SELECT a.id, a.curso_id, a.titulo, a.data, a.hora_inicio, a.hora_fim, a.local,
           c.titulo AS curso,
           EXISTS(SELECT 1 FROM presencas p
                  WHERE p.atividade_id = a.id AND p.aluno_id = @alunoId) AS presenca_confirmada
    FROM atividades a
    JOIN cursos c ON c.id = a.curso_id
    JOIN inscricoes i ON i.curso_id = c.id AND i.aluno_id = @alunoId
    WHERE 1 = 1${filtro('a.atualizado_em')}
    ORDER BY a.data ASC, a.hora_inicio ASC
  `).all(params).map((e) => ({ ...e, presenca_confirmada: Boolean(e.presenca_confirmada) }));

  // Presenças, certificados, mensagens e notificações são sempre enviados
  // por inteiro: o volume por aluno é pequeno e isso simplifica a conciliação
  // do lado do app (ele substitui a tabela local em vez de fazer merge).
  const presencas = db.prepare(`
    SELECT p.id, p.atividade_id, p.registrado_em, p.uuid_local,
           a.titulo AS atividade, a.data, c.titulo AS curso
    FROM presencas p
    JOIN atividades a ON a.id = p.atividade_id
    JOIN cursos c ON c.id = a.curso_id
    WHERE p.aluno_id = ?
    ORDER BY p.registrado_em DESC
  `).all(alunoId);

  const certificados = db.prepare(`
    SELECT cert.id, cert.curso_id, cert.certificado_url, cert.emitido_em,
           c.titulo AS curso, c.carga_horaria
    FROM certificados cert
    JOIN cursos c ON c.id = cert.curso_id
    WHERE cert.aluno_id = ?
    ORDER BY cert.emitido_em DESC
  `).all(alunoId);

  const mensagens = db.prepare(`
    SELECT m.* FROM mensagens m
    WHERE (m.destinatario_tipo = 'todos'
           OR m.curso_id IN (SELECT curso_id FROM inscricoes WHERE aluno_id = @alunoId))
      ${completo ? '' : 'AND m.atualizado_em > @desde'}
    ORDER BY m.criado_em DESC
  `).all(params);

  const notificacoes = db.prepare(`
    SELECT * FROM notificacoes WHERE aluno_id = ? ORDER BY criado_em DESC LIMIT 100
  `).all(alunoId);

  const aluno = db.prepare(`
    SELECT id, nome, email, escola, serie, cidade, pontos, nivel, avatar_url
    FROM alunos WHERE id = ?
  `).get(alunoId);

  res.json({
    servidor_em: servidorEm,
    completo,
    aluno,
    cursos,
    inscricoes,
    agenda,
    presencas,
    certificados,
    mensagens,
    notificacoes,
    totais: {
      cursos: cursos.length,
      agenda: agenda.length,
      presencas: presencas.length,
      certificados: certificados.length,
      mensagens: mensagens.length,
      notificacoes: notificacoes.length,
    },
  });
});

/**
 * Sobe a fila de check-ins que o aluno fez sem internet.
 *
 * Body: { presencas: [{ uuid_local, qr_code, registrado_em }] }
 *
 * Cada item é tratado isoladamente e recebe seu próprio status, porque um
 * QR Code inválido no meio da fila não pode impedir que os outros entrem.
 * O app usa esse status item a item para decidir o que apagar da fila local.
 *
 * Status possíveis por item:
 *   sincronizado  — registrado agora
 *   duplicado     — já existia (reenvio ou check-in repetido): some da fila também
 *   nao_inscrito  — aluno não está inscrito no curso daquela atividade
 *   qr_invalido   — código não corresponde a nenhuma atividade
 */
router.post('/sync/presencas', (req, res) => {
  const { presencas } = req.body;
  if (!Array.isArray(presencas)) {
    return res.status(400).json({ erro: 'Envie a lista de presenças em `presencas`.' });
  }
  if (presencas.length > 200) {
    return res.status(413).json({ erro: 'Envie no máximo 200 presenças por vez.' });
  }

  const buscarAtividade = db.prepare('SELECT * FROM atividades WHERE qr_code = ?');
  const buscarPorUuid = db.prepare('SELECT id FROM presencas WHERE uuid_local = ?');
  const estaInscrito = db.prepare('SELECT 1 FROM inscricoes WHERE aluno_id = ? AND curso_id = ?');
  const inserir = db.prepare(`
    INSERT INTO presencas (aluno_id, atividade_id, registrado_em, uuid_local)
    VALUES (?, ?, ?, ?)
  `);

  // Uma transação só: ou a fila inteira entra, ou nada entra. Evita um estado
  // intermediário se o processo cair no meio do lote.
  const processarLote = db.transaction((itens) => {
    const resultados = [];
    for (const item of itens) {
      const { uuid_local, qr_code, registrado_em } = item || {};

      if (!qr_code) {
        resultados.push({ uuid_local, status: 'qr_invalido', erro: 'QR Code não informado.' });
        continue;
      }

      // Reenvio do mesmo check-in: responde sucesso sem duplicar (idempotência).
      if (uuid_local && buscarPorUuid.get(uuid_local)) {
        resultados.push({ uuid_local, status: 'duplicado', erro: 'Presença já sincronizada.' });
        continue;
      }

      const atividade = buscarAtividade.get(qr_code);
      if (!atividade) {
        resultados.push({ uuid_local, status: 'qr_invalido', erro: 'QR Code inválido ou expirado.' });
        continue;
      }

      if (!estaInscrito.get(req.alunoId, atividade.curso_id)) {
        resultados.push({ uuid_local, status: 'nao_inscrito', erro: 'Você não está inscrito neste curso.' });
        continue;
      }

      try {
        // Preserva o horário em que o aluno realmente fez o check-in no
        // celular — não a hora em que a internet voltou. Sem isso, toda
        // presença offline seria registrada com a hora errada.
        inserir.run(req.alunoId, atividade.id, registrado_em || agora(), uuid_local || null);
        resultados.push({
          uuid_local,
          status: 'sincronizado',
          atividade: atividade.titulo,
          atividade_id: atividade.id,
          data: atividade.data,
        });
      } catch (e) {
        if (String(e).includes('UNIQUE')) {
          resultados.push({ uuid_local, status: 'duplicado', erro: 'Presença já registrada para este encontro.' });
        } else {
          resultados.push({ uuid_local, status: 'erro', erro: 'Falha ao registrar presença.' });
        }
      }
    }
    return resultados;
  });

  const resultados = processarLote(presencas);
  const sincronizadas = resultados.filter((r) => r.status === 'sincronizado').length;

  res.status(200).json({
    servidor_em: agora(),
    recebidas: presencas.length,
    sincronizadas,
    resultados,
  });
});

module.exports = router;
