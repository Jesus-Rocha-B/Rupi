-- Corrige exclusivamente el hash ficticio de la antigua semilla local.
USE rupi;
UPDATE identidad_cuenta_usuario SET hash_clave = 'pbkdf2_sha256$600000$rupi-dev-hu01$8LkMHYbhvu/QFEd17e3Ilkb37nI2vSgtJUg4jaCaNjE='
WHERE id = '11111111-1111-4111-8111-111111111111'
AND hash_clave = '$2a$10$w8.t5n6E1.1J5E1P1.1KuexL3q.8P8gN7g5q0u9q4x8z3v6y2t1W2';
