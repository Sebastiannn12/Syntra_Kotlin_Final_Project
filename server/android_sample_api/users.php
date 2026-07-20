<?php

declare(strict_types=1);

require_once __DIR__ . '/db.php';

header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, Authorization');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    respond(['success' => true, 'message' => 'OK']);
}

$pdo = getConnection();
$method = $_SERVER['REQUEST_METHOD'];
$id = isset($_GET['id']) ? (int) $_GET['id'] : 0;
$action = $_GET['action'] ?? '';
$input = jsonInput();

function requireFields(array $input, array $fields): void
{
    foreach ($fields as $field) {
        if (!isset($input[$field]) || trim((string) $input[$field]) === '') {
            respond([
                'success' => false,
                'message' => "{$field} is required",
            ], 422);
        }
    }
}

function validateAccountInput(array $input, bool $passwordRequired): void
{
    if (!filter_var($input['email'] ?? '', FILTER_VALIDATE_EMAIL)) {
        respond(['success' => false, 'message' => 'Enter a valid email address'], 422);
    }
    $password = (string) ($input['password'] ?? '');
    if (($passwordRequired || $password !== '') && (
        strlen($password) < 8 || !preg_match('/[A-Z]/', $password)
        || !preg_match('/[a-z]/', $password) || !preg_match('/\d/', $password)
        || !preg_match('/[^A-Za-z0-9]/', $password)
    )) {
        respond(['success' => false, 'message' => 'Use 8+ characters with uppercase, lowercase, number, and symbol'], 422);
    }
}

function ensureUserExists(PDO $pdo, int $id): void
{
    $stmt = $pdo->prepare('SELECT id FROM tblusers WHERE id = ?');
    $stmt->execute([$id]);

    if (!$stmt->fetch()) {
        respond(['success' => false, 'message' => 'User not found'], 404);
    }
}

try {
    $authenticatedUser = requireAuthenticatedUser($pdo);

    if ($method === 'POST' && $action === 'restore' && $id > 0) {
        ensureUserExists($pdo, $id);
        $pdo->prepare('UPDATE tblusers SET is_active = 1 WHERE id = ?')->execute([$id]);
        respond(['success' => true, 'message' => 'User restored']);
    }

    if ($method === 'GET' && $id > 0) {
        $stmt = $pdo->prepare('SELECT * FROM tblusers WHERE id = ?');
        $stmt->execute([$id]);
        $user = $stmt->fetch();

        if (!$user) {
            respond(['success' => false, 'message' => 'User not found'], 404);
        }

        respond(['success' => true, 'user' => publicUser($user)]);
    }

    if ($method === 'GET') {
        $query = trim((string) ($_GET['search'] ?? ''));
        $sort = $_GET['sort'] ?? 'newest';
        $order = match ($sort) {
            'name' => 'first_name ASC, last_name ASC',
            'oldest' => 'date_created ASC',
            default => 'date_created DESC',
        };
        $sql = 'SELECT * FROM tblusers';
        $params = [];
        if ($query !== '') {
            $sql .= ' WHERE username LIKE ? OR first_name LIKE ? OR last_name LIKE ? OR email LIKE ?';
            $term = '%' . $query . '%';
            $params = [$term, $term, $term, $term];
        }
        $sql .= " ORDER BY {$order}";
        $stmt = $pdo->prepare($sql);
        $stmt->execute($params);
        $users = array_map('publicUser', $stmt->fetchAll());

        respond(['success' => true, 'users' => $users]);
    }

    if ($method === 'POST') {
        requireFields($input, ['username', 'last_name', 'first_name', 'email', 'password']);
        validateAccountInput($input, true);

        $stmt = $pdo->prepare(
            'INSERT INTO tblusers (username, last_name, first_name, middle_name, email, password, photo)
             VALUES (?, ?, ?, ?, ?, ?, ?)'
        );
        $stmt->execute([
            trim((string) $input['username']),
            trim((string) $input['last_name']),
            trim((string) $input['first_name']),
            trim((string) ($input['middle_name'] ?? '')),
            trim((string) $input['email']),
            password_hash((string) $input['password'], PASSWORD_DEFAULT),
            trim((string) ($input['photo'] ?? '')),
        ]);

        respond([
            'success' => true,
            'message' => 'User created',
            'id' => (int) $pdo->lastInsertId(),
        ], 201);
    }

    if ($method === 'PUT' && $id > 0) {
        ensureUserExists($pdo, $id);
        requireFields($input, ['username', 'last_name', 'first_name', 'email']);
        validateAccountInput($input, false);

        if (isset($input['password']) && trim((string) $input['password']) !== '') {
            $stmt = $pdo->prepare(
                'UPDATE tblusers
                 SET username = ?, last_name = ?, first_name = ?, middle_name = ?, email = ?, password = ?, photo = ?
                 WHERE id = ?'
            );
            $stmt->execute([
                trim((string) $input['username']),
                trim((string) $input['last_name']),
                trim((string) $input['first_name']),
                trim((string) ($input['middle_name'] ?? '')),
                trim((string) $input['email']),
                password_hash((string) $input['password'], PASSWORD_DEFAULT),
                trim((string) ($input['photo'] ?? '')),
                $id,
            ]);
        } else {
            $stmt = $pdo->prepare(
                'UPDATE tblusers
                 SET username = ?, last_name = ?, first_name = ?, middle_name = ?, email = ?, photo = ?
                 WHERE id = ?'
            );
            $stmt->execute([
                trim((string) $input['username']),
                trim((string) $input['last_name']),
                trim((string) $input['first_name']),
                trim((string) ($input['middle_name'] ?? '')),
                trim((string) $input['email']),
                trim((string) ($input['photo'] ?? '')),
                $id,
            ]);
        }

        respond(['success' => true, 'message' => 'User updated']);
    }

    if ($method === 'DELETE' && $id > 0) {
        ensureUserExists($pdo, $id);
        if ((int) $authenticatedUser['id'] === $id) {
            respond(['success' => false, 'message' => 'You cannot disable the account currently signed in'], 422);
        }
        $pdo->prepare('UPDATE tblusers SET is_active = 0 WHERE id = ?')->execute([$id]);
        $pdo->prepare('DELETE FROM api_tokens WHERE user_id = ?')->execute([$id]);
        respond(['success' => true, 'message' => 'User disabled']);
    }

    respond(['success' => false, 'message' => 'Unsupported request'], 405);
} catch (PDOException $exception) {
    if ($exception->getCode() === '23000') {
        respond(['success' => false, 'message' => 'Username or email already exists'], 409);
    }

    respond(['success' => false, 'message' => 'Database error'], 500);
}
