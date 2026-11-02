import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';
import { viteSingleFile } from 'vite-plugin-singlefile';
import { readFileSync } from 'node:fs';

const pkg = JSON.parse(readFileSync(new URL('./package.json', import.meta.url), 'utf8'));

// Single-file build so dist/index.html can be published as-is (same artifact URL as before).
export default defineConfig(({ command, mode }) => {
  const env = loadEnv(mode, process.cwd(), '');
  // /api/csg/v1 goes to the Spring Boot backend when the dev server is not mocking (VITE_API_MOCK=false)
  // and for `vite preview`. The backend has no CORS, so the browser must see one origin.
  const proxy = { '/api': { target: env.API_PROXY_TARGET || 'http://localhost:8080', changeOrigin: true } };
  return {
    plugins: [react(), viteSingleFile()],
    define: { __APP_VERSION__: JSON.stringify(pkg.version) },
    // public/ only holds the MSW service worker, which production never needs
    publicDir: command === 'serve' ? 'public' : false,
    server: { proxy: env.VITE_API_MOCK === 'true' ? undefined : proxy },
    preview: { proxy },
    build: { target: 'es2020', assetsInlineLimit: 1e9, cssCodeSplit: false }
  };
});
