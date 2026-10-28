const path = require('path');
const fs = require('fs');
const Database = require('better-sqlite3');

const DB_PATH = process.env.DB_PATH || path.join(__dirname, 'proxima_etapa.db');
const SCHEMA_PATH = path.join(__dirname, 'schema.sql');

const db = new Database(DB_PATH);
db.pragma('journal_mode = WAL');
db.pragma('foreign_keys = ON');

// Aplica o schema sempre (CREATE TABLE IF NOT EXISTS é idempotente)
const schema = fs.readFileSync(SCHEMA_PATH, 'utf8');
db.exec(schema);

/**
 * Migração leve de colunas (Entrega 2).
 *
 * CREATE TABLE IF NOT EXISTS não acrescenta colunas novas em tabelas que já
 * existem, e ALTER TABLE ... ADD COLUMN falha se a coluna já estiver lá.
 * Então conferimos o PRAGMA table_info antes de alterar — assim o banco de um
 * ambiente que já rodava a Entrega 1 é atualizado sem perder dados.
 */
function garantirColuna(tabela, coluna, definicao) {
  const colunas = db.prepare(`PRAGMA table_info(${tabela})`).all();
  if (!colunas.some((c) => c.name === coluna)) {
    db.exec(`ALTER TABLE ${tabela} ADD COLUMN ${coluna} ${definicao}`);
  }
}

// `atualizado_em` sustenta a sincronização incremental (delta sync): o app
// manda a data da última sincronização e recebe de volta só o que mudou.
garantirColuna('cursos', 'atualizado_em', "TEXT NOT NULL DEFAULT '1970-01-01 00:00:00'");
garantirColuna('atividades', 'atualizado_em', "TEXT NOT NULL DEFAULT '1970-01-01 00:00:00'");
garantirColuna('mensagens', 'atualizado_em', "TEXT NOT NULL DEFAULT '1970-01-01 00:00:00'");
garantirColuna('inscricoes', 'atualizado_em', "TEXT NOT NULL DEFAULT '1970-01-01 00:00:00'");

// `uuid_local` é o identificador que o app gera no celular para cada check-in
// feito offline. Ele torna o envio da fila idempotente: se o app reenviar o
// mesmo check-in (porque a resposta se perdeu no caminho), o servidor
// reconhece que já registrou aquele evento em vez de criar outro.
garantirColuna('presencas', 'uuid_local', 'TEXT');
db.exec('CREATE UNIQUE INDEX IF NOT EXISTS idx_presencas_uuid ON presencas(uuid_local) WHERE uuid_local IS NOT NULL');

// Gatilhos para manter `atualizado_em` sempre correto sem depender de cada
// rota lembrar de preencher a coluna.
db.exec(`
  CREATE TRIGGER IF NOT EXISTS trg_cursos_atualizado
  AFTER UPDATE ON cursos FOR EACH ROW
  BEGIN UPDATE cursos SET atualizado_em = datetime('now') WHERE id = NEW.id; END;

  CREATE TRIGGER IF NOT EXISTS trg_cursos_inserido
  AFTER INSERT ON cursos FOR EACH ROW
  BEGIN UPDATE cursos SET atualizado_em = datetime('now') WHERE id = NEW.id; END;

  CREATE TRIGGER IF NOT EXISTS trg_atividades_atualizado
  AFTER UPDATE ON atividades FOR EACH ROW
  BEGIN UPDATE atividades SET atualizado_em = datetime('now') WHERE id = NEW.id; END;

  CREATE TRIGGER IF NOT EXISTS trg_atividades_inserido
  AFTER INSERT ON atividades FOR EACH ROW
  BEGIN UPDATE atividades SET atualizado_em = datetime('now') WHERE id = NEW.id; END;

  CREATE TRIGGER IF NOT EXISTS trg_mensagens_inserido
  AFTER INSERT ON mensagens FOR EACH ROW
  BEGIN UPDATE mensagens SET atualizado_em = datetime('now') WHERE id = NEW.id; END;

  CREATE TRIGGER IF NOT EXISTS trg_inscricoes_inserido
  AFTER INSERT ON inscricoes FOR EACH ROW
  BEGIN UPDATE inscricoes SET atualizado_em = datetime('now') WHERE id = NEW.id; END;

  CREATE TRIGGER IF NOT EXISTS trg_inscricoes_atualizado
  AFTER UPDATE ON inscricoes FOR EACH ROW
  BEGIN UPDATE inscricoes SET atualizado_em = datetime('now') WHERE id = NEW.id; END;
`);

module.exports = db;
