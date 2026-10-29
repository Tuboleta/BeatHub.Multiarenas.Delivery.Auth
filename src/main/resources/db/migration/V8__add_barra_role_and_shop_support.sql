-- V8: Agregar rol BARRA y soporte multi-tenant de asignación a Shop/Barra en auth.arena_usuario
DO $$
BEGIN
    -- 1. Rol BARRA en auth.rol
    IF NOT EXISTS (SELECT 1 FROM auth.rol WHERE nombre = 'BARRA') THEN
        INSERT INTO auth.rol (nombre, descripcion, estado_id, arena_id, creacion_fecha)
        VALUES ('BARRA', 'Operador de barra / punto de recogida responsable de despachos y entregas de mostrador', 1, NULL, NOW());
    END IF;

    -- 2. Columna shop_id y shop_nombre en auth.arena_usuario (Multi-tenant por arena)
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_schema = 'auth' AND table_name = 'arena_usuario' AND column_name = 'shop_id'
    ) THEN
        ALTER TABLE auth.arena_usuario ADD COLUMN shop_id BIGINT;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_schema = 'auth' AND table_name = 'arena_usuario' AND column_name = 'shop_nombre'
    ) THEN
        ALTER TABLE auth.arena_usuario ADD COLUMN shop_nombre VARCHAR(150);
    END IF;

    -- 3. Índice compuesto para consultas eficientes por arena y shop
    CREATE INDEX IF NOT EXISTS idx_arena_usuario_arena_shop ON auth.arena_usuario(arena_id, shop_id);
END $$;
