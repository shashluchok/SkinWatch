// OPFS (Origin Private File System) access inside the sqlite-wasm web worker requires
// SharedArrayBuffer, which browsers only expose in a cross-origin isolated context. Without these
// headers the dev server serves a non-isolated page, sqlite3's OPFS VFS silently fails to install,
// and `sqlite3.oo1.OpfsDb` never gets defined on the loaded module.
config.devServer = {
    ...config.devServer,
    headers: {
        ...(config.devServer && config.devServer.headers),
        "Cross-Origin-Opener-Policy": "same-origin",
        "Cross-Origin-Embedder-Policy": "require-corp",
    },
};
