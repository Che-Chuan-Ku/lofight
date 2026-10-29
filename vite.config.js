import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import { viteSingleFile } from 'vite-plugin-singlefile';

// Single-file build so dist/index.html can be published as-is (same artifact URL as before).
export default defineConfig({
  plugins: [react(), viteSingleFile()],
  build: { target: 'es2020', assetsInlineLimit: 1e9, cssCodeSplit: false }
});
