// Teste de fumaça (smoke test) da API — roda contra um servidor já em execução.
// Uso: node server.js &  (em outro terminal)  node test-api.js
const BASE = process.env.API_URL || 'http://localhost:3000/api';

let falhas = 0;
function ok(cond, msg) {
  if (cond) console.log(`  OK  - ${msg}`);
  else { console.log(`  FALHOU - ${msg}`); falhas++; }
}

async function main() {
  console.log('Testando API em', BASE);

  const health = await fetch(`${BASE}/health`).then(r => r.json());
  ok(health.status === 'ok', 'GET /health responde ok');

  const semToken = await fetch(`${BASE}/cursos`);
  ok(semToken.status === 401, 'GET /cursos sem token retorna 401');

  const login = await fetch(`${BASE}/auth/login`, {
    method: 'POST', headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email: 'ana.souza@aluno.proximaetapa.org.br', senha: '123456' })
  }).then(r => r.json());
  ok(!!login.token, 'POST /auth/login retorna token válido');

  const loginErrado = await fetch(`${BASE}/auth/login`, {
    method: 'POST', headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email: 'ana.souza@aluno.proximaetapa.org.br', senha: 'errada' })
  });
  ok(loginErrado.status === 401, 'POST /auth/login com senha errada retorna 401');

  const auth = { Authorization: `Bearer ${login.token}` };

  const cursos = await fetch(`${BASE}/cursos`, { headers: auth }).then(r => r.json());
  ok(Array.isArray(cursos) && cursos.length > 0, `GET /cursos retorna lista (${cursos.length} cursos)`);

  const agenda = await fetch(`${BASE}/agenda`, { headers: auth }).then(r => r.json());
  ok(Array.isArray(agenda), `GET /agenda retorna lista (${agenda.length} eventos)`);

  const card = await fetch(`${BASE}/card`, { headers: auth }).then(r => r.json());
  ok(!!card.codigoCard, 'GET /card retorna card digital');

  const certificados = await fetch(`${BASE}/certificados`, { headers: auth }).then(r => r.json());
  ok(Array.isArray(certificados), 'GET /certificados retorna lista');

  const perguntas = await fetch(`${BASE}/perfil-teste/perguntas`, { headers: auth }).then(r => r.json());
  ok(Array.isArray(perguntas) && perguntas.length > 0, `GET /perfil-teste/perguntas retorna ${perguntas.length} perguntas`);

  const respostas = {};
  perguntas.forEach(p => { respostas[p.id] = p.alternativas[0].id; });
  const resultado = await fetch(`${BASE}/perfil-teste/respostas`, {
    method: 'POST', headers: { ...auth, 'Content-Type': 'application/json' },
    body: JSON.stringify({ respostas })
  }).then(r => r.json());
  ok(!!resultado.perfil, `POST /perfil-teste/respostas calcula resultado (${resultado.perfil})`);

  const mensagens = await fetch(`${BASE}/mensagens`, { headers: auth }).then(r => r.json());
  ok(Array.isArray(mensagens), 'GET /mensagens retorna lista');

  console.log(falhas === 0 ? '\nTodos os testes passaram.' : `\n${falhas} teste(s) falharam.`);
  process.exit(falhas === 0 ? 0 : 1);
}

main().catch((e) => { console.error(e); process.exit(1); });
