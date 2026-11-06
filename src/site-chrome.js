// Site header drawer, and the mobile-home header that slides in after scroll.
// layout.md Round 8: IntersectionObserver on a sentinel, show at header height,
// hide 8px earlier, 200ms ease (CSS), prefers-reduced-motion disables the transition.

const HYST = 8;

function bindDrawer() {
  const btn = document.querySelector('.menu-btn');
  const drawer = document.getElementById('site-drawer');
  if (!btn || !drawer) return;
  btn.addEventListener('click', () => {
    const open = drawer.classList.toggle('open');
    btn.setAttribute('aria-expanded', open ? 'true' : 'false');
    btn.setAttribute('aria-label', open ? '關閉選單' : '開啟選單');
  });
}

function bindHomeHeader() {
  const header = document.querySelector('.site-header');
  const sentinel = document.querySelector('.header-sentinel');
  if (!header || !sentinel) return;
  const mq = window.matchMedia('(max-width: 767px)');
  const hideSentinel = document.createElement('div');
  hideSentinel.className = 'header-sentinel header-sentinel-hide';
  hideSentinel.setAttribute('aria-hidden', 'true');
  sentinel.parentNode.insertBefore(hideSentinel, sentinel.nextSibling);

  let shown = false;
  let showOut = false;
  let hideIn = true;

  const mobileHome = () => mq.matches && document.body.classList.contains('home');
  const limit = () => header.offsetHeight || 56;

  function syncHeights() {
    const h = limit();
    sentinel.style.height = h + 'px';
    hideSentinel.style.height = Math.max(1, h - HYST) + 'px';
  }
  function apply() {
    header.classList.toggle('is-shown', shown);
  }
  function fromScroll() {
    if (!mobileHome()) { shown = false; apply(); return; }
    const y = window.scrollY;
    const t = limit();
    if (!shown && y >= t) shown = true;
    else if (shown && y < t - HYST) shown = false;
    apply();
  }

  syncHeights();
  if (!('IntersectionObserver' in window)) {
    window.addEventListener('scroll', fromScroll, { passive: true });
    window.addEventListener('resize', syncHeights);
    mq.addEventListener('change', fromScroll);
    return;
  }

  const sync = () => {
    if (!mobileHome()) { shown = false; apply(); return; }
    if (showOut) shown = true;
    else if (hideIn) shown = false;
    apply();
  };
  // Show once the full-height sentinel has left the viewport (scrollY >= header height).
  const showIO = new IntersectionObserver((entries) => {
    showOut = entries[0] ? !entries[0].isIntersecting : false;
    sync();
  }, { threshold: 0 });
  // Hide only after the shorter sentinel is back in view (8px before the show line).
  const hideIO = new IntersectionObserver((entries) => {
    hideIn = entries[0] ? entries[0].isIntersecting : true;
    sync();
  }, { threshold: 0 });
  showIO.observe(sentinel);
  hideIO.observe(hideSentinel);
  window.addEventListener('resize', () => { syncHeights(); sync(); });
  mq.addEventListener('change', sync);
  new MutationObserver(sync).observe(document.body, { attributes: true, attributeFilter: ['class'] });
}

export function initSiteChrome() {
  bindDrawer();
  bindHomeHeader();
}
