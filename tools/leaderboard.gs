// Leaderboard backend for 人海攻城 (Google Apps Script bound to a Google Sheet).
// Deploy: Extensions > Apps Script > paste this file > Deploy > New deployment > Web app,
// "Execute as: Me", "Who has access: Anyone". Put the web-app URL into src/config.js (LEADERBOARD_URL).
// Sheet "scores" columns: time, version, name, seconds, win. One sheet keeps every version;
// the game only reads rows matching its own version, so each release starts a fresh board.
var SHEET_NAME = 'scores';
var TOP_N = 50;

function sheet_() {
  var ss = SpreadsheetApp.getActiveSpreadsheet();
  var sh = ss.getSheetByName(SHEET_NAME);
  if (!sh) { sh = ss.insertSheet(SHEET_NAME); sh.appendRow(['time', 'version', 'name', 'seconds', 'win']); }
  return sh;
}
// strip control chars and leading formula characters so a nickname can never become a spreadsheet formula
function clean_(s, n) { return String(s || '').replace(/[\u0000-\u001f]/g, '').replace(/^[=+\-@]+/, '').trim().slice(0, n); }

function doGet(e) {
  var version = clean_(e && e.parameter && e.parameter.version, 20);
  return json_({ ok: true, top: top_(version) });
}

function doPost(e) {
  var d;
  try { d = JSON.parse(e.postData.contents); } catch (err) { return json_({ ok: false, error: 'bad json' }); }
  var name = clean_(d.name, 12), version = clean_(d.version, 20);
  var seconds = Math.max(0, Math.min(99999, Math.round(Number(d.seconds) || 0)));
  var win = d.win ? 1 : 0;
  if (!name || !version) return json_({ ok: false, error: 'missing fields' });
  var lock = LockService.getScriptLock();
  lock.waitLock(10000);
  try { sheet_().appendRow([new Date(), version, name, seconds, win]); } finally { lock.releaseLock(); }
  return json_({ ok: true, top: top_(version) });
}

// best entry per nickname for this version, ranked by better_()
function top_(version) {
  var rows = sheet_().getDataRange().getValues().slice(1);
  var best = {};
  for (var i = 0; i < rows.length; i++) {
    var r = rows[i];
    if (String(r[1]) !== version) continue;
    var e = { name: String(r[2]), seconds: Number(r[3]) || 0, win: Number(r[4]) ? 1 : 0 };
    var b = best[e.name];
    if (!b || better_(e, b)) best[e.name] = e;
  }
  var list = Object.keys(best).map(function (k) { return best[k]; });
  list.sort(function (a, b) { return better_(a, b) ? -1 : better_(b, a) ? 1 : 0; });
  return list.slice(0, TOP_N);
}
// wins rank above losses; among wins fewer seconds is better; among losses more seconds survived is better
function better_(a, b) {
  if (a.win !== b.win) return a.win > b.win;
  return a.win ? a.seconds < b.seconds : a.seconds > b.seconds;
}
function json_(o) { return ContentService.createTextOutput(JSON.stringify(o)).setMimeType(ContentService.MimeType.JSON); }
