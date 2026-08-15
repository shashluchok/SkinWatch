package com.shashluchok.skinwatch.sqliteweb

import androidx.sqlite.driver.web.WebWorkerSQLiteDriver
import org.w3c.dom.Worker

actual fun createSqliteWebWorkerDriver(): WebWorkerSQLiteDriver =
    WebWorkerSQLiteDriver(Worker(js("""new URL("sqlite-wasm-worker/worker.js", import.meta.url)""")))
