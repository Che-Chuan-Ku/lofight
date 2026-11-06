// Crop the Kenney sheet (indexed PNG in src/game/tiles.js) into dist/assets for static pages.
// No extra image library: decode the palette PNG, encode small RGBA PNGs.
import { deflateSync, inflateSync } from 'node:zlib';
import { mkdirSync, writeFileSync } from 'node:fs';

const COLS = 12;
const TILE = 16;

function paeth(a, b, c) {
  const p = a + b - c, pa = Math.abs(p - a), pb = Math.abs(p - b), pc = Math.abs(p - c);
  return pa <= pb && pa <= pc ? a : pb <= pc ? b : c;
}

function decodeIndexed(buf) {
  let o = 8, w = 0, h = 0, plte, trns = Buffer.alloc(0);
  const idats = [];
  while (o < buf.length) {
    const len = buf.readUInt32BE(o); o += 4;
    const type = buf.toString('ascii', o, o + 4); o += 4;
    const chunk = buf.subarray(o, o + len); o += len + 4;
    if (type === 'IHDR') { w = chunk.readUInt32BE(0); h = chunk.readUInt32BE(4); }
    else if (type === 'PLTE') plte = chunk;
    else if (type === 'tRNS') trns = chunk;
    else if (type === 'IDAT') idats.push(chunk);
    else if (type === 'IEND') break;
  }
  const raw = inflateSync(Buffer.concat(idats));
  const idx = Buffer.alloc(w * h);
  let p = 0;
  for (let y = 0; y < h; y++) {
    const filter = raw[p++];
    const row = idx.subarray(y * w, (y + 1) * w);
    const prev = y ? idx.subarray((y - 1) * w, y * w) : null;
    for (let x = 0; x < w; x++) {
      const v = raw[p++];
      const a = x ? row[x - 1] : 0, b = prev ? prev[x] : 0, c = prev && x ? prev[x - 1] : 0;
      let out = v;
      if (filter === 1) out = (v + a) & 255;
      else if (filter === 2) out = (v + b) & 255;
      else if (filter === 3) out = (v + ((a + b) >> 1)) & 255;
      else if (filter === 4) out = (v + paeth(a, b, c)) & 255;
      row[x] = out;
    }
  }
  const rgba = Buffer.alloc(w * h * 4);
  for (let i = 0; i < w * h; i++) {
    const k = idx[i];
    rgba[i * 4] = plte[k * 3];
    rgba[i * 4 + 1] = plte[k * 3 + 1];
    rgba[i * 4 + 2] = plte[k * 3 + 2];
    rgba[i * 4 + 3] = k < trns.length ? trns[k] : 255;
  }
  return { w, h, rgba };
}

function crc32(buf) {
  let c = 0xffffffff;
  for (const b of buf) {
    c ^= b;
    for (let k = 0; k < 8; k++) c = (c >>> 1) ^ (0xedb88320 & -(c & 1));
  }
  return (c ^ 0xffffffff) >>> 0;
}

function chunk(type, data) {
  const t = Buffer.from(type);
  const len = Buffer.alloc(4);
  len.writeUInt32BE(data.length);
  const body = Buffer.concat([t, data]);
  const c = Buffer.alloc(4);
  c.writeUInt32BE(crc32(body));
  return Buffer.concat([len, body, c]);
}

function encodePng(w, h, rgba) {
  const raw = Buffer.alloc((w * 4 + 1) * h);
  for (let y = 0; y < h; y++) {
    raw[y * (w * 4 + 1)] = 0;
    rgba.copy(raw, y * (w * 4 + 1) + 1, y * w * 4, (y + 1) * w * 4);
  }
  const ihdr = Buffer.alloc(13);
  ihdr.writeUInt32BE(w, 0);
  ihdr.writeUInt32BE(h, 4);
  ihdr[8] = 8;
  ihdr[9] = 6;
  const sig = Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]);
  return Buffer.concat([sig, chunk('IHDR', ihdr), chunk('IDAT', deflateSync(raw)), chunk('IEND', Buffer.alloc(0))]);
}

function crop(sheet, index) {
  const col = index % COLS, row = (index / COLS) | 0;
  const out = Buffer.alloc(TILE * TILE * 4);
  for (let y = 0; y < TILE; y++) {
    for (let x = 0; x < TILE; x++) {
      const si = ((row * TILE + y) * sheet.w + (col * TILE + x)) * 4;
      const di = (y * TILE + x) * 4;
      sheet.rgba.copy(out, di, si, si + 4);
    }
  }
  return encodePng(TILE, TILE, out);
}

// Tile 0 is a flat dirt cell (repeats cleanly). Tile 36 is a stone course used as the h2 rule.
export const GROUND_TILE = 0;
export const WALL_TILE = 36;
// Blue soldier, decorative mark beside the DayWar wordmark.
export const LOGO_TILE = 96;

export function writeTileAssets(dir, sheetBytes) {
  mkdirSync(dir, { recursive: true });
  writeFileSync(dir + '/tiles.png', sheetBytes);
  const sheet = decodeIndexed(sheetBytes);
  writeFileSync(dir + '/ground.png', crop(sheet, GROUND_TILE));
  writeFileSync(dir + '/wall.png', crop(sheet, WALL_TILE));
}
