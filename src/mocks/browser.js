// Dev-only MSW bootstrap. main.jsx imports this behind `import.meta.env.DEV`, so production builds never contain it.
import { setupWorker } from 'msw/browser';
import { handlers } from './handlers/index.js';
import { controls } from './db.js';

export async function startMocks() {
  const worker = setupWorker(...handlers);
  await worker.start({ onUnhandledRequest: 'bypass', quiet: true });
  window.__mockDb = controls;   // test controls for the Playwright suite (dev mock mode only)
  return worker;
}
