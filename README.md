# QueroAssitir-Backend

Backend Spring Boot do projeto QueroAssistir.

## Deploy no Render

O projeto esta preparado para deploy como Web Service Docker no Render.

Variaveis obrigatorias no painel do Render:

```env
DATABASE_URL=jdbc:postgresql://host:5432/database?sslmode=require
DATABASE_USERNAME=usuario
DATABASE_PASSWORD=senha
GEMINI_API_KEY=sua_chave_gemini
TMDB_API_KEY=sua_chave_tmdb
```

O `render.yaml` declara essas variaveis com `sync: false`, entao os valores devem ser preenchidos no Dashboard do Render e nao ficam salvos no GitHub.

Para banco Render Postgres, converta a connection string para o formato JDBC antes de salvar em `DATABASE_URL`:

```text
postgresql://usuario:senha@host:5432/database
```

vira:

```text
jdbc:postgresql://host:5432/database?sslmode=require
```

mantendo o usuario em `DATABASE_USERNAME` e a senha em `DATABASE_PASSWORD`.
