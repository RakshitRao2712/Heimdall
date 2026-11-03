import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';
import tailwindcss from '@tailwindcss/vite';
import { fileURLToPath, URL } from 'node:url';
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '');
  // Discovery is bounded/cancellable by the backend; do not drop valid runs at five minutes.
  const proxy = { '/api': { target: env.API_TARGET || 'http://localhost:8080', changeOrigin: true, timeout: 0, proxyTimeout: 0 } };
  return { plugins: [react(), tailwindcss()], resolve: { alias: { '@': fileURLToPath(new URL('./src', import.meta.url)) } }, server: { host: '127.0.0.1', port: 5174, strictPort: true, proxy }, preview: { host: '127.0.0.1', port: 4174, proxy } };
});
