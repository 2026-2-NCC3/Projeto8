require('dotenv').config();
const path = require('path');
const express = require('express');
const cors = require('cors');
const morgan = require('morgan');

const { autenticar } = require('./middleware/auth');
const authRoutes = require('./routes/auth');
const adminRoutes = require('./routes/admin');
const perfilRoutes = require('./routes/perfil');
const cursosRoutes = require('./routes/cursos');
const presencasRoutes = require('./routes/presencas');
const certificadosRoutes = require('./routes/certificados');
const testePerfilRoutes = require('./routes/testePerfil');
const mensagensRoutes = require('./routes/mensagens');
const syncRoutes = require('./routes/sync');

const app = express();
app.use(cors());
app.use(express.json());
app.use(morgan('dev'));

app.get('/api/health', (req, res) => res.json({
  status: 'ok',
  servico: 'Próxima Etapa API',
  versao: '2.0',
  hora: new Date().toISOString(),
}));

// Rotas públicas: login do aluno e login do administrador.
// (adminRoutes aplica seu próprio middleware de administrador internamente,
// deixando apenas /admin/login aberto.)
app.use('/api/auth', authRoutes);
app.use('/api', adminRoutes);

// A partir daqui, todas as rotas do aluno exigem token (Authorization: Bearer <token>)
app.use('/api', autenticar, perfilRoutes);
app.use('/api', autenticar, cursosRoutes);
app.use('/api', autenticar, presencasRoutes);
app.use('/api', autenticar, certificadosRoutes);
app.use('/api', autenticar, testePerfilRoutes);
app.use('/api', autenticar, mensagensRoutes);
app.use('/api', autenticar, syncRoutes);

// Módulo web administrativo: servido pelo próprio backend, na raiz do
// domínio. Assim o deploy continua sendo um serviço só — a ONG abre a URL do
// servidor no navegador e cai direto no painel.
app.use(express.static(path.join(__dirname, '..', 'ModuloWeb')));

app.use('/api', (req, res) => res.status(404).json({ erro: 'Rota não encontrada.' }));

// eslint-disable-next-line no-unused-vars
app.use((err, req, res, next) => {
  console.error(err);
  res.status(500).json({ erro: 'Erro interno no servidor.' });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Próxima Etapa API (v2) rodando em http://localhost:${PORT}`);
  console.log(`Painel administrativo em http://localhost:${PORT}/`);
});
