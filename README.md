# Quero Assistir - Backend

Backend do **Quero Assistir**, responsavel por gerar recomendacoes de filmes usando inteligencia artificial e integrar dados reais do TMDB.

A API recebe o humor, contexto, duracao e busca do usuario, consulta o Gemini para gerar uma recomendacao e busca os dados tecnicos do filme no TMDB.

## Funcionalidades

- Endpoint REST para recomendacao de filmes.
- Integracao com Google Gemini via Spring AI.
- Integracao com TMDB para dados reais de filmes.
- Retorno de filme principal e alternativas.
- Informacoes de titulo, sinopse, poster, generos, nota, duracao, ano, diretor e plataformas.
- Tratamento global de erros.
- Configuracao CORS para frontend local e Vercel.
- Documentacao Swagger/OpenAPI.
- Deploy via Docker no Render.

## Tecnologias

- Java 21
- Spring Boot
- Spring Web MVC
- Spring Data JPA
- Spring AI
- Google Gemini
- TMDB API
- PostgreSQL
- Lombok
- Springdoc OpenAPI
- Docker

## Variaveis de ambiente

Crie um arquivo `.env` ou `.env.local` na raiz do backend:

```env
DATABASE_URL=jdbc:postgresql://host:5432/database?sslmode=require
DATABASE_USERNAME=usuario
DATABASE_PASSWORD=senha
GEMINI_API_KEY=sua_chave_gemini
TMDB_API_KEY=sua_chave_tmdb
CORS_ALLOWED_ORIGIN_PATTERNS=http://localhost:*,https://*.vercel.app
```

Os arquivos reais de ambiente nao devem ser versionados. Use `.env.example` como modelo.

## Como rodar localmente

Suba o banco local, se necessario:

```bash
docker compose up -d
```

Execute o backend:

```bash
./mvnw spring-boot:run
```

No Windows:

```bash
mvnw.cmd spring-boot:run
```

## Endpoint principal

```http
POST /api/recommendations
```

Exemplo de request:

```json
{
  "mood": "leve",
  "context": "sozinho",
  "duration": "qualquer",
  "query": "quero assistir um filme de terror"
}
```

Exemplo de response:

```json
{
  "primary": {
    "id": "348",
    "title": "Dracula",
    "description": "...",
    "image": "https://image.tmdb.org/t/p/w500/...",
    "rating": 7.2,
    "genres": ["Terror"],
    "duration": "1h 14min",
    "durationMinutes": 74,
    "year": 1931,
    "director": "Tod Browning",
    "platforms": []
  },
  "alternatives": [],
  "matchReason": "Justificativa da recomendacao.",
  "mood": "leve",
  "query": "quero assistir um filme de terror",
  "context": "sozinho"
}
```

## Swagger

Com a aplicacao rodando:

```text
http://localhost:8080/swagger-ui.html
```

Em producao:

```text
https://queroassitir-backend.onrender.com/swagger-ui.html
```

## Deploy

O backend esta preparado para deploy no Render como Web Service Docker.

Variaveis obrigatorias no Render:

```env
DATABASE_URL=jdbc:postgresql://host:5432/database?sslmode=require
DATABASE_USERNAME=usuario
DATABASE_PASSWORD=senha
GEMINI_API_KEY=sua_chave_gemini
TMDB_API_KEY=sua_chave_tmdb
CORS_ALLOWED_ORIGIN_PATTERNS=https://*.vercel.app,http://localhost:*
```

## Autor

Projeto desenvolvido por Joao Alves Gomes.

---
