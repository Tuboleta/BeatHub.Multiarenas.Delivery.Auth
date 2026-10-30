-- V9: Crear tabla auth.runner_zona para zonificación de runners
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.tables 
        WHERE table_schema = 'auth' AND table_name = 'runner_zona'
    ) THEN
        CREATE TABLE auth.runner_zona (
            id BIGSERIAL PRIMARY KEY,
            usuario_id BIGINT NOT NULL REFERENCES auth.usuario(id) ON DELETE CASCADE,
            arena_id BIGINT NOT NULL,
            zona_id BIGINT NOT NULL,
            prioridad INT NOT NULL DEFAULT 1,
            activo BOOLEAN NOT NULL DEFAULT true,
            estado_id INT NOT NULL DEFAULT 1 REFERENCES public.estado(id),
            creacion_fecha TIMESTAMP NOT NULL DEFAULT NOW(),
            creacion_usuario BIGINT,
            actualizacion_fecha TIMESTAMP,
            actualizacion_usuario BIGINT,
            CONSTRAINT uk_runner_arena_zona UNIQUE (usuario_id, arena_id, zona_id)
        );

        CREATE INDEX IF NOT EXISTS idx_runner_zona_lookup ON auth.runner_zona(arena_id, zona_id, activo);
        CREATE INDEX IF NOT EXISTS idx_runner_zona_usuario ON auth.runner_zona(usuario_id, arena_id);
    END IF;
END $$;

