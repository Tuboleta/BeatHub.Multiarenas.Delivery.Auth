-- V6: Registro de rol BACKOFFICE y usuario de integración con autenticación local
-- Permite al entorno BackOffice autenticarse directamente mediante credenciales y operar en segundo plano

DO $$
DECLARE
    v_persona_id BIGINT;
    v_usuario_id BIGINT;
    v_rol_id BIGINT;
    v_arena1_id BIGINT := 1;
    v_arena2_id BIGINT := 2;
    v_au1_id BIGINT;
    v_au2_id BIGINT;
BEGIN
    -- 1. Rol BACKOFFICE
    SELECT id INTO v_rol_id FROM auth.rol WHERE nombre = 'BACKOFFICE';
    IF v_rol_id IS NULL THEN
        INSERT INTO auth.rol (nombre, descripcion, estado_id, arena_id, creacion_fecha)
        VALUES ('BACKOFFICE', 'Rol de integración y consumo de servicios para BackOffice', 1, NULL, NOW())
        RETURNING id INTO v_rol_id;
    ELSE
        UPDATE auth.rol 
        SET descripcion = 'Rol de integración y consumo de servicios para BackOffice',
            estado_id = 1
        WHERE id = v_rol_id;
    END IF;

    -- 2. Persona Base para el usuario BackOffice (sin forzar id numérico para evitar colisiones con persona_pkey)
    SELECT id INTO v_persona_id FROM auth.persona WHERE email = 'backoffice@beathub.com';
    IF v_persona_id IS NULL THEN
        INSERT INTO auth.persona (
            nombres, apellidos, tipo_documento_id, numero_documento, 
            email, telefono, pais_id, departamento_id, ciudad_id, estado_id, creacion_fecha
        ) VALUES (
            'BackOffice', 'System', 1, '9000000002', 
            'backoffice@beathub.com', '+573000000002', 1, 11, 11001, 1, NOW()
        )
        RETURNING id INTO v_persona_id;
    ELSE
        UPDATE auth.persona
        SET nombres = 'BackOffice',
            apellidos = 'System',
            estado_id = 1
        WHERE id = v_persona_id;
    END IF;

    -- 3. Usuario BackOffice (autenticación con contraseña)
    -- Contraseña por defecto: Admin12345!
    -- BCrypt hash: $2a$10$vFPRhqE.Xsll7SKkKynrBuMndTXmqzwdQX8QQgXvliz4lMFqlAqRG
    SELECT id INTO v_usuario_id FROM auth.usuario WHERE username = 'backoffice';
    IF v_usuario_id IS NULL THEN
        INSERT INTO auth.usuario (
            persona_id, username, password, sso_id, sso_provider, estado_id, arena_id, creacion_fecha
        ) VALUES (
            v_persona_id, 'backoffice', '$2a$10$vFPRhqE.Xsll7SKkKynrBuMndTXmqzwdQX8QQgXvliz4lMFqlAqRG', 
            NULL, 'LOCAL', 1, 1, NOW()
        )
        RETURNING id INTO v_usuario_id;
    ELSE
        UPDATE auth.usuario 
        SET password = '$2a$10$vFPRhqE.Xsll7SKkKynrBuMndTXmqzwdQX8QQgXvliz4lMFqlAqRG',
            estado_id = 1
        WHERE id = v_usuario_id;
    END IF;

    -- 4. Vinculación del usuario BackOffice a las Arenas Iniciales (DaviArena=1, MovistarArena=2)
    -- Arena 1
    SELECT id INTO v_au1_id FROM auth.arena_usuario WHERE arena_id = v_arena1_id AND usuario_id = v_usuario_id;
    IF v_au1_id IS NULL THEN
        INSERT INTO auth.arena_usuario (arena_id, usuario_id, estado_id, creacion_fecha)
        VALUES (v_arena1_id, v_usuario_id, 1, NOW())
        RETURNING id INTO v_au1_id;
    ELSE
        UPDATE auth.arena_usuario SET estado_id = 1 WHERE id = v_au1_id;
    END IF;

    -- Arena 2
    SELECT id INTO v_au2_id FROM auth.arena_usuario WHERE arena_id = v_arena2_id AND usuario_id = v_usuario_id;
    IF v_au2_id IS NULL THEN
        INSERT INTO auth.arena_usuario (arena_id, usuario_id, estado_id, creacion_fecha)
        VALUES (v_arena2_id, v_usuario_id, 1, NOW())
        RETURNING id INTO v_au2_id;
    ELSE
        UPDATE auth.arena_usuario SET estado_id = 1 WHERE id = v_au2_id;
    END IF;

    -- 5. Asignación de Rol BACKOFFICE en las arenas
    IF v_au1_id IS NOT NULL AND v_rol_id IS NOT NULL THEN
        IF NOT EXISTS (SELECT 1 FROM auth.rol_arena_usuario WHERE rol_id = v_rol_id AND arena_usuario_id = v_au1_id) THEN
            INSERT INTO auth.rol_arena_usuario (rol_id, arena_usuario_id, estado_id, arena_id, creacion_fecha)
            VALUES (v_rol_id, v_au1_id, 1, v_arena1_id, NOW());
        ELSE
            UPDATE auth.rol_arena_usuario SET estado_id = 1 WHERE rol_id = v_rol_id AND arena_usuario_id = v_au1_id;
        END IF;
    END IF;

    IF v_au2_id IS NOT NULL AND v_rol_id IS NOT NULL THEN
        IF NOT EXISTS (SELECT 1 FROM auth.rol_arena_usuario WHERE rol_id = v_rol_id AND arena_usuario_id = v_au2_id) THEN
            INSERT INTO auth.rol_arena_usuario (rol_id, arena_usuario_id, estado_id, arena_id, creacion_fecha)
            VALUES (v_rol_id, v_au2_id, 1, v_arena2_id, NOW());
        ELSE
            UPDATE auth.rol_arena_usuario SET estado_id = 1 WHERE rol_id = v_rol_id AND arena_usuario_id = v_au2_id;
        END IF;
    END IF;

    -- 6. Sincronizar secuencias de PostgreSQL
    PERFORM setval('auth.persona_id_seq', COALESCE((SELECT MAX(id) FROM auth.persona), 1));
    PERFORM setval('auth.usuario_id_seq', COALESCE((SELECT MAX(id) FROM auth.usuario), 1));
    PERFORM setval('auth.rol_id_seq', COALESCE((SELECT MAX(id) FROM auth.rol), 1));
    PERFORM setval('auth.arena_usuario_id_seq', COALESCE((SELECT MAX(id) FROM auth.arena_usuario), 1));
    PERFORM setval('auth.rol_arena_usuario_id_seq', COALESCE((SELECT MAX(id) FROM auth.rol_arena_usuario), 1));
END $$;
