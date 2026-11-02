import React from 'react';
import { createRoot } from 'react-dom/client';
import App from './App.jsx';
import './styles.css';
import './menu.css';

async function boot() {
  // Dev only: MSW answers /api/csg/v1 from src/mocks. The condition is written inline (not imported) so the
  // bundler sees a literal `false` in production and drops this import and every mock file from dist/.
  if (import.meta.env.DEV && import.meta.env.VITE_API_MOCK === 'true') {
    const { startMocks } = await import('./mocks/browser.js');
    await startMocks();
  }
  createRoot(document.getElementById('root')).render(
    <React.StrictMode>
      <App />
    </React.StrictMode>
  );
}
boot();
