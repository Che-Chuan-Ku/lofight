import { useEffect, useRef, useState } from 'react';
import { useGame } from './hooks/useGame.js';
import Hud from './components/Hud.jsx';
import Overlay from './components/Overlay.jsx';
import ChoiceBox from './components/ChoiceBox.jsx';
import RestBox from './components/RestBox.jsx';
import { leaderboardEnabled, fetchTop, submitScore, cleanName, loadName, saveName, defaultName } from './leaderboard.js';

export default function App() {
  const appRef = useRef(null), stageRef = useRef(null), canvasRef = useRef(null), startKeyRef = useRef(null);
  const { ui, hint, start, pickChoice, pickRest } = useGame({ canvasRef, appRef, stageRef, startKeyRef });
  const playing = ui.phase !== 'intro' && ui.phase !== 'end';

  // ---- nickname + leaderboard ----
  const enabled = leaderboardEnabled();
  const [name, setName] = useState(() => loadName() || defaultName());
  const [nameErr, setNameErr] = useState(false);
  const [showBoard, setShowBoard] = useState(false);
  const [board, setBoard] = useState({ status: 'loading', rows: [] });
  const [submitNote, setSubmitNote] = useState(null);
  const submittedRef = useRef(false);

  const loadBoard = () => {
    setBoard(b => ({ ...b, status: 'loading' }));
    fetchTop().then(rows => setBoard({ status: 'ok', rows })).catch(() => setBoard(b => ({ ...b, status: 'error' })));
  };
  const tryStart = () => {
    if (playing) return;
    const n = cleanName(name);
    if (!n) { setNameErr(true); return; }
    setName(n); saveName(n); setNameErr(false); setShowBoard(false); setSubmitNote(null);
    submittedRef.current = false;
    start();
  };
  startKeyRef.current = tryStart;

  // submit once per finished game, then show the fresh top list
  useEffect(() => {
    if (ui.phase !== 'end' || submittedRef.current) return;
    submittedRef.current = true;
    if (!enabled) return;
    setSubmitNote('上傳成績中…'); setBoard(b => ({ ...b, status: 'loading' }));
    const me = cleanName(name);
    submitScore({ name: me, seconds: Number(ui.time), win: ui.result === 'win' })
      .then(rows => {
        const rank = rows.findIndex(r => r.name === me);
        setBoard({ status: 'ok', rows });
        setSubmitNote(rank >= 0 ? '你的最佳成績：第 ' + (rank + 1) + ' 名' : '成績已上傳');
      })
      .catch(() => { setBoard(b => ({ ...b, status: 'error' })); setSubmitNote('成績上傳失敗'); });
  }, [ui.phase, ui.time, ui.result, name, enabled]);

  const toggleBoard = () => { const next = !showBoard; setShowBoard(next); if (next) loadBoard(); };

  return (
    <div id="app" ref={appRef}>
      <div id="stage" ref={stageRef}>
        <canvas id="c" ref={canvasRef} />
        <Hud />
        <div id="hint" style={{ opacity: hint ? 1 : 0 }}>左右拖曳移動砲台</div>
        {playing && (
          <button id="restart" type="button" aria-label="重新開始" title="重新開始"
            onClick={e => { tryStart(); e.currentTarget.blur(); }}>&#8635;</button>
        )}
        {ui.phase === 'rest' && <RestBox onPick={pickRest} />}
        {ui.phase === 'choice' && <ChoiceBox cards={ui.choiceCards} onPick={pickChoice} />}
        {!playing && (
          <Overlay phase={ui.phase} result={ui.result} time={ui.time} onStart={tryStart}
            name={name} onName={v => { setName(v); setNameErr(false); }} nameErr={nameErr}
            board={enabled ? board : null} showBoard={showBoard} onToggleBoard={toggleBoard} submitNote={submitNote} />
        )}
      </div>
    </div>
  );
}
