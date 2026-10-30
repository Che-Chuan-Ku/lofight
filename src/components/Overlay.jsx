import Board from './Board.jsx';
import { NAME_MAX, APP_VERSION } from '../config.js';

// Intro and end screens share one panel; `phase` is 'intro' or 'end'.
// `result`: win | lose | secret (hidden boss killed) | secretfail (stage 8 cleared, died to the hidden boss).
export default function Overlay({ phase, result, time, clearTime, onStart, name, onName, nameErr, board, showBoard, onToggleBoard, submitNote }) {
  const ended = phase === 'end', win = ended && result !== 'lose', secret = result === 'secret';
  const title = !ended ? '人海攻城' : secret ? '㊙ Secret Clear!' : result === 'secretfail' ? '第八關通關' : win ? 'Victory!' : 'Defeat in Battle';
  const sub = !ended ? '左右移動砲台，守住陣地。'
    : secret ? '隱藏魔王也倒下了，用了 ' + time + ' 秒。'
    : result === 'secretfail' ? '成績 ' + clearTime + ' 秒已計入排行榜，隱藏魔王下次再戰。'
    : (win ? '用了 ' : '撐了 ') + time + ' 秒。';
  const hasBoard = !!board;
  return (
    <div id="overlay">
      <h1 className={ended && !win ? 'lose' : ''}>{title}</h1>
      <p>{sub}</p>
      {!ended && !showBoard && (
        <>
          <label className="nick"><span>暱稱</span>
            <input type="text" value={name} maxLength={NAME_MAX} placeholder="輸入暱稱" autoComplete="off" enterKeyHint="go"
              className={nameErr ? 'err' : ''} aria-label="暱稱" onChange={e => onName(e.target.value)} onFocus={e => e.target.select()} />
          </label>
          {nameErr && <small className="err">請先輸入暱稱（1 到 {NAME_MAX} 字）</small>}
        </>
      )}
      {hasBoard && (ended || showBoard) && <Board status={board.status} rows={board.rows} me={name} note={ended ? submitNote : null} />}
      {!ended && showBoard
        ? <button type="button" className="ghost" onClick={onToggleBoard}>返回</button>
        : <button type="button" onClick={onStart}>{ended ? '再來一局' : '開始'}</button>}
      {!ended && !showBoard && hasBoard && <button type="button" className="ghost" onClick={onToggleBoard}>排行榜</button>}
      <span className="ver">v{APP_VERSION}</span>
    </div>
  );
}
