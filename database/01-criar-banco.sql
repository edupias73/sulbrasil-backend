-- ============================================================
-- Sul Brasil - Catálogo de Autopeças
-- Crie o banco ANTES de subir o Spring Boot.
-- As tabelas e os dados de teste são criados pela aplicação.
-- ============================================================

CREATE DATABASE IF NOT EXISTS catalogo_autopecas
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE catalogo_autopecas;

SELECT 'Banco catalogo_autopecas pronto. Agora suba o backend (Spring Boot).' AS mensagem;
