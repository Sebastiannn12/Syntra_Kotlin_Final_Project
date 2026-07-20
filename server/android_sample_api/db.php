<?php

declare(strict_types=1);

function getConnection(): PDO
{
    $fileConfig = is_file(__DIR__ . '/config.php') ? require __DIR__ . '/config.php' : [];
    $host = $fileConfig['host'] ?? getenv('DB_HOST') ?: '127.0.0.1';
    $port = $fileConfig['port'] ?? getenv('DB_PORT') ?: '3306';
    $database = $fileConfig['database'] ?? getenv('DB_NAME') ?: 'android_sample';
    $username = $fileConfig['username'] ?? getenv('DB_USER') ?: 'root';
    $password = $fileConfig['password'] ?? getenv('DB_PASS') ?: '';

    return new PDO(
        "mysql:host={$host};port={$port};dbname={$database};charset=utf8mb4",
        $username,
        $password,
        [
            PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
            PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
            PDO::ATTR_TIMEOUT => 10,
            PDO::ATTR_EMULATE_PREPARES => false,
        ]
    );
}

function respond(array $data, int $status = 200): void
{
    http_response_code($status);
    header('Content-Type: application/json');
    header('Access-Control-Allow-Origin: *');
    header('Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS');
    header('Access-Control-Allow-Headers: Content-Type, Authorization');
    echo json_encode($data);
    exit;
}

function jsonInput(): array
{
    $decoded = json_decode(file_get_contents('php://input'), true);
    return is_array($decoded) ? $decoded : [];
}

function bearerToken(): string
{
    $header = $_SERVER['HTTP_AUTHORIZATION'] ?? '';
    if (!preg_match('/^Bearer\s+(.+)$/i', $header, $matches)) {
        respond(['success' => false, 'message' => 'Authentication required'], 401);
    }
    return trim($matches[1]);
}

function requireAuthenticatedUser(PDO $pdo): array
{
    $tokenHash = hash('sha256', bearerToken());
    $stmt = $pdo->prepare(
        'SELECT u.* FROM api_tokens t
         INNER JOIN tblusers u ON u.id = t.user_id
         WHERE t.token_hash = ? AND t.expires_at > NOW() AND u.is_active = 1'
    );
    $stmt->execute([$tokenHash]);
    $user = $stmt->fetch();

    if (!$user) {
        respond(['success' => false, 'message' => 'Session expired. Please sign in again.'], 401);
    }
    return $user;
}

function publicUser(array $row): array
{
    unset($row['password']);
    return $row;
}
