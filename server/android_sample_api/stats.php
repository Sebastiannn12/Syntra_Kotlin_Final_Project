<?php

declare(strict_types=1);

require_once __DIR__ . '/db.php';

$pdo = getConnection();
requireAuthenticatedUser($pdo);

$counts = $pdo->query(
    'SELECT COUNT(*) AS total,
            SUM(is_active = 1) AS active,
            SUM(is_active = 0) AS disabled
     FROM tblusers'
)->fetch();
$latest = $pdo->query(
    'SELECT * FROM tblusers WHERE is_active = 1 ORDER BY date_created DESC LIMIT 3'
)->fetchAll();

respond([
    'success' => true,
    'stats' => [
        'total' => (int) $counts['total'],
        'active' => (int) $counts['active'],
        'disabled' => (int) $counts['disabled'],
    ],
    'users' => array_map('publicUser', $latest),
]);
