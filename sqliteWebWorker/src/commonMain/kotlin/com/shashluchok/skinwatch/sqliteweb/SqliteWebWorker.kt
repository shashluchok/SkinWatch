package com.shashluchok.skinwatch.sqliteweb

import androidx.sqlite.driver.web.WebWorkerSQLiteDriver

/**
 * Builds a [WebWorkerSQLiteDriver] backed by this module's vendored `worker.js` (see
 * `sqliteWebWorker/worker/`), which runs SQLite WASM inside a real browser Web Worker and persists
 * to the Origin Private File System. Adapted with attribution from the Room team's own KMP web
 * sample (`danysantiago/room-web-demo`).
 */
expect fun createSqliteWebWorkerDriver(): WebWorkerSQLiteDriver
