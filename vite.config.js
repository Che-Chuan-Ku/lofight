import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import { viteSingleFile } from 'vite-plugin-singlefile';
import { readFileSync } from 'node:fs';

const pkg = JSON.parse(readFileSync(new URL('./package.json', import.meta.url), 'utf8'));

// Single-file build so dist/index.html can be published as-is (same artifact URL as before).
export default defineConfig({
  plugins: [react(), viteSingleFile()],
  // The leaderboard is scoped per app version, so every release starts a fresh board.
  define: { __APP_VERSION__: JSON.stringify(pkg.version) },
  build: { target: 'es2020', assetsInlineLimit: 1e9, cssCodeSplit: false }
});
