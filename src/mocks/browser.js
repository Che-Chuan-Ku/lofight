// Dev-only MSW bootstrap. main.jsx imports this behind `import.meta.env.DEV`, so production builds never contain it.
import { setupWorker } from 'msw/browser';
import { handlers } from './handlers/index.js';
import { controls, simulateEcpay } from './db.js';
import { installMockAds } from './ads.js';

export async function startMocks() {
  const worker = setupWorker(...handlers);
  await worker.start({ onUnhandledRequest: 'bypass', quiet: true });
  window.__mockDb = controls;   // test controls for the Playwright suite (dev mock mode only)
  installMockAds();             // Google Ad Placement stand-in + test publisher id (window.__mockAds)
  // Mocked ECPay checkout: a form posted to <origin>/mock-ecpay settles the order (db.topupOutcome) and returns to /?topup=<id>.
  document.addEventListener('submit', e => {
    const f = e.target;
    if (!(f instanceof HTMLFormElement) || f.action !== location.origin + '/mock-ecpay') return;
    e.preventDefault();
    const id = (f.elements.namedItem('MerchantTradeNo') || {}).value;
    if (window.__mockEcpayHold) return;   // tests may keep the player "on the ECPay page"
    simulateEcpay(id);
    location.assign('/?topup=' + encodeURIComponent(id));
  });
  return worker;
}
