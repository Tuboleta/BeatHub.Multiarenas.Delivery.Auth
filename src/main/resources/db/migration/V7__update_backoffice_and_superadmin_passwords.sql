-- V7: Actualizar hash BCrypt exacto para 'Admin12345!' en backoffice y superadmin
UPDATE auth.usuario 
SET password = '$2a$10$vFPRhqE.Xsll7SKkKynrBuMndTXmqzwdQX8QQgXvliz4lMFqlAqRG'
WHERE username IN ('backoffice', 'superadmin');
