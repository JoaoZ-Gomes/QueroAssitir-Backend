-- ============================================
-- DADOS DE TESTE - Plataformas dos Filmes
-- ============================================

-- Inserir plataformas para os filmes demo
INSERT INTO tb_filme_plataformas (filme_id, plataforma) VALUES
-- O Guardião dos Sonhos
('guardiao', 'Netflix'),
('guardiao', 'Amazon Prime Video'),

-- A Última Memória
('ultima-memoria', 'Disney+'),
('ultima-memoria', 'Netflix'),

-- Sombras da Verdade
('sombras', 'Max (HBO)'),
('sombras', 'Apple TV+'),

-- O Labirinto
('labirinto', 'Netflix'),
('labirinto', 'Paramount+'),

-- Além do Horizonte
('horizonte', 'Amazon Prime Video'),
('horizonte', 'Max (HBO)'),

-- Caos Perfeito
('caos', 'Netflix'),

-- A Grande Escapada
('escapada', 'Disney+'),
('escapada', 'Paramount+')
ON CONFLICT DO NOTHING;

-- ============================================
-- Adicionar platforms array no tb_filmes (se usar array)
-- ============================================
UPDATE tb_filmes SET platforms = ARRAY[
  SELECT plataforma FROM tb_filme_plataformas WHERE filme_id = tb_filmes.id
] WHERE id IN ('guardiao', 'ultima-memoria', 'sombras', 'labirinto', 'horizonte', 'caos', 'escapada');
