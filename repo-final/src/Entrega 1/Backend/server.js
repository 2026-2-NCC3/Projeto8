require('dotenv').config();
const express = require('express');
const cors = require('cors');
const morgan = require('morgan');

const { autenticar } = require('./middleware/auth');
const authRoutes = require('./routes/auth');
const perfilRoutes = require('./routes/perfil');
const cursosRoutes = require('./routes/cursos');
const presencasRoutes = require('./routes/presencas');
const certificadosRoutes = require('./routes/certificados');
const testePerfilRoutes = require('./routes/testePerfil');
const mensagensRoutes = require('./routes/mensagens');

const app = express();
app.use(cors());
app.use(express.json());
app.use(morgan('dev'));

app.get('/api/health', (req, res) => res.json({ status: 'ok', servico: 'Próxima Etapa API', hora: new Date().toISOString() }));

// Rotas públicas
app.use('/api/auth', authRoutes);

// A partir daqui, todas as rotas exigem token (Authorization: Bearer <token>)
app.use('/api', autenticar, perfilRoutes);
app.use('/api', autenticar, cursosRoutes);
app.use('/api', autenticar, presencasRoutes);
app.use('/api', autenticar, certificadosRoutes);
app.use('/api', autenticar, testePerfilRoutes);
app.use('/api', autenticar, mensagensRoutes);

app.use((req, res) => res.status(404).json({ erro: 'Rota não encontrada.' }));

// eslint-disable-next-line no-unused-vars
app.use((err, req, res, next) => {
  console.error(err);
  res.status(500).json({ erro: 'Erro interno no servidor.' });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Próxima Etapa API rodando em http://localhost:${PORT}`);
});
