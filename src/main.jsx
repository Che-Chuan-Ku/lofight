import React from 'react';
import { createRoot } from 'react-dom/client';
import App from './App.jsx';
import { initSiteChrome } from './site-chrome.js';
import './styles.css';
import './menu.css';

initSiteChrome();

async function boot() {
  // Dev only: MSW answers /api/csg/v1 from src/mocks. The condition is written inline (not imported) so the
  // bundler sees a literal `false` in production and drops this import and every mock file from dist/.
  if (import.meta.env.DEV && import.meta.env.VITE_API_MOCK === 'true') {
    const { startMocks } = await import('./mocks/browser.js');
    await startMocks();
  }
  // No ad script at boot (FE-56): lib/ads.js loads it on the shop page, once listShopItems.adReward.isEnabled is true.
  createRoot(document.getElementById('root')).render(
    <React.StrictMode>
      <App />
    </React.StrictMode>
  );
}
boot();
