-- ============================================
-- SCRIPT DE MIGRAÇÃO - QueroAssistir
-- ============================================

-- 1. Criar tabela de Histórico
-- ============================================
CREATE TABLE IF NOT EXISTS tb_historicos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    -- Dados da busca
    query TEXT NOT NULL,
    mood VARCHAR(50) NOT NULL,
    context VARCHAR(20) NOT NULL,
    duration VARCHAR(20),
    
    -- Dados do filme recomendado
    filme_id VARCHAR(255) NOT NULL,
    filme_titulo VARCHAR(255) NOT NULL,
    filme_imagem TEXT,
    
    -- Auditoria
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Índices para performance
    CONSTRAINT fk_historico_filme FOREIGN KEY (filme_id) REFERENCES tb_filmes(id) ON DELETE CASCADE
);

-- Índices para melhor performance de queries
CREATE INDEX IF NOT EXISTS idx_historicos_criado_em ON tb_historicos(criado_em DESC);
CREATE INDEX IF NOT EXISTS idx_historicos_mood ON tb_historicos(mood);
CREATE INDEX IF NOT EXISTS idx_historicos_filme_id ON tb_historicos(filme_id);

-- ============================================
-- 2. Adicionar suporte a Plataformas nos filmes
-- ============================================
-- Se a coluna não existir, adicionar
ALTER TABLE tb_filmes ADD COLUMN IF NOT EXISTS platforms TEXT[];

-- Criar tabela de relação (alternativa mais normalizada)
CREATE TABLE IF NOT EXISTS tb_filme_plataformas (
    filme_id VARCHAR(255) NOT NULL,
    plataforma VARCHAR(100) NOT NULL,
    
    PRIMARY KEY (filme_id, plataforma),
    CONSTRAINT fk_filme_plataforma FOREIGN KEY (filme_id) REFERENCES tb_filmes(id) ON DELETE CASCADE
);

-- ============================================
-- 3. Comentários e Documentação
-- ============================================
COMMENT ON TABLE tb_historicos IS 'Registra o histórico de buscas e recomendações de filmes dos usuários';
COMMENT ON COLUMN tb_historicos.id IS 'Identificador único da entrada de histórico';
COMMENT ON COLUMN tb_historicos.query IS 'Descrição/query original do usuário';
COMMENT ON COLUMN tb_historicos.mood IS 'Estado emocional: leve, emocional, intenso, divertido, nostalgico, tenso, inspirado, caotico';
COMMENT ON COLUMN tb_historicos.context IS 'Contexto de visualização: sozinho, amigos';
COMMENT ON COLUMN tb_historicos.duration IS 'Duração desejada: curto, longo, qualquer';
COMMENT ON COLUMN tb_historicos.filme_id IS 'Referência ao filme recomendado';
COMMENT ON COLUMN tb_historicos.criado_em IS 'Data e hora de criação do registro';

COMMENT ON TABLE tb_filme_plataformas IS 'Relação entre filmes e plataformas de streaming disponíveis';
COMMENT ON COLUMN tb_filme_plataformas.filme_id IS 'Referência ao filme';
COMMENT ON COLUMN tb_filme_plataformas.plataforma IS 'Nome da plataforma: Netflix, Amazon Prime, Disney+, etc.';

-- ============================================
-- 4. Exemplo de INSERT para teste
-- ============================================
-- INSERT INTO tb_historicos (query, mood, context, duration, filme_id, filme_titulo, filme_imagem)
-- VALUES (
--   'Estou me sentindo nostálgico e quero lembrar de filmes antigos',
--   'nostalgico',
--   'sozinho',
--   'qualquer',
--   'filme-id-123',
--   'Um Filme Nostálgico',
--   'https://example.com/image.jpg'
-- );
