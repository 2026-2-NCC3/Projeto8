-- Próxima Etapa - App do Aluno
-- Banco de desenvolvimento (cópia própria, simplificada, com dados fictícios)
-- Baseado no inventário de banco de dados fornecido pela ONG, adaptado para
-- fins acadêmicos (SQLite ao invés do Postgres/Supabase de produção).

PRAGMA foreign_keys = ON;

CREATE TABLE IF NOT EXISTS alunos (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  nome TEXT NOT NULL,
  email TEXT NOT NULL UNIQUE,
  senha_hash TEXT NOT NULL,
  escola TEXT,
  serie TEXT,
  cidade TEXT,
  pontos INTEGER NOT NULL DEFAULT 0,
  nivel TEXT NOT NULL DEFAULT 'Explorador',
  avatar_url TEXT,
  criado_em TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS cursos (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  titulo TEXT NOT NULL,
  descricao TEXT,
  categoria TEXT,
  local TEXT,
  carga_horaria INTEGER NOT NULL DEFAULT 0,
  data_inicio TEXT NOT NULL,
  data_fim TEXT,
  vagas_totais INTEGER NOT NULL DEFAULT 30,
  vagas_disponiveis INTEGER NOT NULL DEFAULT 30,
  possui_certificado INTEGER NOT NULL DEFAULT 1,
  ativo INTEGER NOT NULL DEFAULT 1,
  criado_em TEXT NOT NULL DEFAULT (datetime('now'))
);

-- Encontros/atividades de um curso (aulas, oficinas, visitas etc.)
CREATE TABLE IF NOT EXISTS atividades (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  curso_id INTEGER NOT NULL REFERENCES cursos(id) ON DELETE CASCADE,
  titulo TEXT NOT NULL,
  data TEXT NOT NULL,       -- YYYY-MM-DD
  hora_inicio TEXT NOT NULL, -- HH:MM
  hora_fim TEXT NOT NULL,
  local TEXT,
  qr_code TEXT NOT NULL UNIQUE  -- código único usado no check-in por QR Code
);

CREATE TABLE IF NOT EXISTS inscricoes (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  aluno_id INTEGER NOT NULL REFERENCES alunos(id) ON DELETE CASCADE,
  curso_id INTEGER NOT NULL REFERENCES cursos(id) ON DELETE CASCADE,
  status TEXT NOT NULL DEFAULT 'inscrito' CHECK (status IN ('inscrito','cursando','concluido','desistente')),
  criado_em TEXT NOT NULL DEFAULT (datetime('now')),
  UNIQUE(aluno_id, curso_id)
);

CREATE TABLE IF NOT EXISTS presencas (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  aluno_id INTEGER NOT NULL REFERENCES alunos(id) ON DELETE CASCADE,
  atividade_id INTEGER NOT NULL REFERENCES atividades(id) ON DELETE CASCADE,
  registrado_em TEXT NOT NULL DEFAULT (datetime('now')),
  UNIQUE(aluno_id, atividade_id) -- evita duplicidade de presença no mesmo encontro
);

CREATE TABLE IF NOT EXISTS certificados (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  aluno_id INTEGER NOT NULL REFERENCES alunos(id) ON DELETE CASCADE,
  curso_id INTEGER NOT NULL REFERENCES cursos(id) ON DELETE CASCADE,
  certificado_url TEXT,
  emitido_em TEXT NOT NULL DEFAULT (datetime('now')),
  UNIQUE(aluno_id, curso_id)
);

CREATE TABLE IF NOT EXISTS perguntas_perfil (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  texto TEXT NOT NULL,
  ordem INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS alternativas_perfil (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  pergunta_id INTEGER NOT NULL REFERENCES perguntas_perfil(id) ON DELETE CASCADE,
  texto TEXT NOT NULL,
  perfil TEXT NOT NULL -- categoria/perfil que essa alternativa pontua (ex: 'exatas','humanas','saude','artes')
);

CREATE TABLE IF NOT EXISTS resultados_perfil (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  aluno_id INTEGER NOT NULL REFERENCES alunos(id) ON DELETE CASCADE,
  respostas_json TEXT NOT NULL,   -- {"1": 3, "2": 7, ...} pergunta_id -> alternativa_id
  perfil_resultado TEXT NOT NULL, -- ex: 'exatas'
  descricao TEXT,
  concluido_em TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS mensagens (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  titulo TEXT NOT NULL,
  corpo TEXT NOT NULL,
  destinatario_tipo TEXT NOT NULL DEFAULT 'todos' CHECK (destinatario_tipo IN ('todos','curso')),
  curso_id INTEGER REFERENCES cursos(id) ON DELETE CASCADE,
  criado_em TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS notificacoes (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  aluno_id INTEGER NOT NULL REFERENCES alunos(id) ON DELETE CASCADE,
  mensagem_id INTEGER REFERENCES mensagens(id) ON DELETE CASCADE,
  tipo TEXT NOT NULL DEFAULT 'mensagem', -- mensagem | curso_novo | agenda | certificado
  titulo TEXT NOT NULL,
  corpo TEXT,
  lida INTEGER NOT NULL DEFAULT 0,
  criado_em TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE INDEX IF NOT EXISTS idx_atividades_curso ON atividades(curso_id);
CREATE INDEX IF NOT EXISTS idx_inscricoes_aluno ON inscricoes(aluno_id);
CREATE INDEX IF NOT EXISTS idx_presencas_aluno ON presencas(aluno_id);
CREATE INDEX IF NOT EXISTS idx_notificacoes_aluno ON notificacoes(aluno_id);
