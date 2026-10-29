// Top list. `status`: loading | ok | error. `me` highlights the player's own row.
import { BOARD_SHOW } from '../config.js';

export default function Board({ status, rows, me, note }) {
  const list = (rows || []).slice(0, BOARD_SHOW);
  return (
    <div className="board">
      <h3>排行榜</h3>
      {note && <div className="msg">{note}</div>}
      {status === 'loading' && <div className="msg">載入中…</div>}
      {status === 'error' && <div className="msg">排行榜暫時無法連線</div>}
      {status === 'ok' && list.length === 0 && <div className="msg">還沒有人上榜，來當第一個</div>}
      {status === 'ok' && list.length > 0 && (
        <ol>
          {list.map((r, i) => (
            <li key={r.name + i} className={r.name === me ? 'me' : ''}>
              <span className="rk">{i + 1}</span>
              <span className="nm">{r.name}</span>
              {r.win ? <span className="w">通關</span> : null}
              <span className="sc">{r.seconds} 秒</span>
            </li>
          ))}
        </ol>
      )}
    </div>
  );
}
