import { useRef } from 'react';
import { useGame } from './hooks/useGame.js';
import Hud from './components/Hud.jsx';
import Overlay from './components/Overlay.jsx';
import ChoiceBox from './components/ChoiceBox.jsx';
import RestBox from './components/RestBox.jsx';

export default function App() {
  const appRef = useRef(null), stageRef = useRef(null), canvasRef = useRef(null);
  const { ui, hint, start, pickChoice, pickRest } = useGame({ canvasRef, appRef, stageRef });
  const playing = ui.phase !== 'intro' && ui.phase !== 'end';
  return (
    <div id="app" ref={appRef}>
      <div id="stage" ref={stageRef}>
        <canvas id="c" ref={canvasRef} />
        <Hud hud={ui.hud} />
        <div id="hint" style={{ opacity: hint ? 1 : 0 }}>左右拖曳移動砲台</div>
        {playing && (
          <button id="restart" type="button" aria-label="重新開始" title="重新開始"
            onClick={e => { start(); e.currentTarget.blur(); }}>&#8635;</button>
        )}
        {ui.phase === 'rest' && <RestBox onPick={pickRest} />}
        {ui.phase === 'choice' && <ChoiceBox cards={ui.choiceCards} onPick={pickChoice} />}
        {!playing && <Overlay phase={ui.phase} result={ui.result} time={ui.time} onStart={start} />}
      </div>
    </div>
  );
}
