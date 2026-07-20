<?php

declare(strict_types=1);

require_once __DIR__ . '/db.php';

try {
    getConnection()->query('SELECT 1');
    respond(['success' => true, 'message' => 'API and database are ready']);
} catch (Throwable $exception) {
    respond(['success' => false, 'message' => 'Database unavailable'], 503);
}
