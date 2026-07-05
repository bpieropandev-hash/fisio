-- Executado antes das migrações no bootstrap do PostgreSQL em Docker/Podman.
-- Cria o schema usado pela aplicação e o define como padrão do database,
-- para que os arquivos V*.sql (não qualificados) criem os objetos no lugar certo.
CREATE SCHEMA IF NOT EXISTS master;
ALTER DATABASE physio_manager SET search_path TO master, public;
SET search_path TO master, public;
