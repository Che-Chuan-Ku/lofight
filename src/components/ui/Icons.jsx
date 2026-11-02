// Inline SVG icons copied from the prototype (decorative: aria-hidden).
export const IcBack = () => (
  <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M15 4 7 12l8 8" fill="none" stroke="currentColor" strokeWidth="3.2" strokeLinecap="round" strokeLinejoin="round" /></svg>
);
export const IcClose = () => (
  <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M6 6l12 12M18 6 6 18" fill="none" stroke="currentColor" strokeWidth="3.2" strokeLinecap="round" /></svg>
);
export const IcWarn = () => (
  <svg className="ico" viewBox="0 0 40 40" aria-hidden="true"><path d="M20 5 37 34H3z" fill="none" stroke="currentColor" strokeWidth="3" strokeLinejoin="round" /><path d="M20 15v9M20 28v2" stroke="currentColor" strokeWidth="3" strokeLinecap="round" /></svg>
);
export const IcEmptyLedger = () => (
  <svg className="ico" viewBox="0 0 40 40" aria-hidden="true"><circle cx="20" cy="20" r="16" fill="none" stroke="currentColor" strokeWidth="3" strokeDasharray="6 5" /></svg>
);
export const IcEmptyBoard = () => (
  <svg className="ico" viewBox="0 0 40 40" aria-hidden="true"><path d="M10 36V6h18l-4 6 4 6H10" fill="none" stroke="currentColor" strokeWidth="3" /></svg>
);
export const GLogo = () => (
  <svg viewBox="0 0 48 48" aria-hidden="true">
    <path fill="#EA4335" d="M24 9.5c3.5 0 6.6 1.2 9 3.5l6.7-6.7C35.6 2.4 30.2 0 24 0 14.6 0 6.6 5.4 2.7 13.3l7.8 6C12.4 13.7 17.7 9.5 24 9.5z" />
    <path fill="#4285F4" d="M46.1 24.5c0-1.6-.1-3.1-.4-4.5H24v9h12.4c-.5 2.9-2.2 5.3-4.6 6.9l7.4 5.7c4.3-4 6.9-9.9 6.9-17.1z" />
    <path fill="#FBBC05" d="M10.5 28.7c-.5-1.4-.8-3-.8-4.7s.3-3.3.8-4.7l-7.8-6C1 16.6 0 20.2 0 24s1 7.4 2.7 10.7l7.8-6z" />
    <path fill="#34A853" d="M24 48c6.5 0 11.9-2.1 15.8-5.8l-7.4-5.7c-2.1 1.4-4.8 2.3-8.4 2.3-6.3 0-11.6-4.2-13.5-9.9l-7.8 6C6.6 42.6 14.6 48 24 48z" />
  </svg>
);

// Pixel-art icons drawn on a 16x16 grid to match the Kenney Tiny Dungeon tiles: one <rect> per cell,
// colors are menu.css tokens. Grid key: o = outline, b = body, s = shadow, h = highlight, . = empty.
function PixelArt({ rows, palette, className, size }) {
  const cells = [];
  rows.forEach((row, y) => {
    for (let x = 0; x < row.length; x++) {
      const fill = palette[row[x]];
      if (fill) cells.push(<rect key={x + '-' + y} x={x} y={y} width="1" height="1" style={{ fill }} />);
    }
  });
  return (
    <svg className={className} viewBox={`0 0 ${rows[0].length} ${rows.length}`} width={size} height={size}
      shapeRendering="crispEdges" aria-hidden="true" focusable="false">{cells}</svg>
  );
}

const COIN_ROWS = [
  '.....oooooo.....',
  '...oohhbbbboo...',
  '..ohhbbbbbbbso..',
  '.ohbbbbbbbbbbso.',
  'ohbbbssssssbbbso',
  'ohbbsbbbbbbhbbso',
  'ohbbsbbbbbbhbbso',
  'ohbbsbbbbbbhbbso',
  'ohbbsbbbbbbhbbso',
  'ohbbsbbbbbbhbbso',
  'ohbbsbbbbbbhbbso',
  'ohbbbhhhhhhbbbso',
  '.obbbbbbbbbbsso.',
  '..obbbbbbbssso..',
  '...oossssssoo...',
  '.....oooooo.....'
];
const COIN_PALETTE = { o: 'var(--color-accent-deep)', b: 'var(--color-accent)', s: 'var(--color-bronze)', h: 'var(--color-text)' };

/** Gold coin; size comes from the .coin / .coin.lg classes. */
export const PixelCoin = ({ className = '' }) => (
  <PixelArt rows={COIN_ROWS} palette={COIN_PALETTE} className={('coin ' + className).trim()} />
);

const TROPHY_ROWS = [
  '..oooooooooooo..',
  '..ohhbbbbbbsso..',
  'ooohhbbbbbbssooo',
  'o.ohbbbbbbbbso.o',
  'o.ohbbbbbbbbso.o',
  '.o.ohbbbbbbso.o.',
  '..oohbbbbbssoo..',
  '...oohbbbbsoo...',
  '....oohbbsoo....',
  '......obso......',
  '......obso......',
  '.....oobsoo.....',
  '....oooooooo....',
  '....ohbbbbso....',
  '...ohbbbbbbso...',
  '...oooooooooo...'
];
const TROPHY_PALETTES = {
  bronze: { o: 'var(--color-bronze-shadow)', b: 'var(--color-bronze)', s: 'var(--color-bronze-deep)', h: 'var(--sand)' },
  silver: { o: 'var(--color-silver-shadow)', b: 'var(--color-silver)', s: 'var(--color-silver-deep)', h: 'var(--color-card-edge)' }
};

/** Tier trophy: same cup, bronze or silver colors. */
export const PixelTrophy = ({ tier, size = 40 }) => (
  <PixelArt rows={TROPHY_ROWS} palette={TROPHY_PALETTES[tier]} className="trophy" size={size} />
);
