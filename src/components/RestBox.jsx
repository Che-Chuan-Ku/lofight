// Rest screen before the last boss: widen one gate (index 0 left, 1 middle, 2 right).
const OPTIONS = [
  { i: 1, title: '中門加大', desc: '左右兩門縮小' },
  { i: 0, title: '左門加大', desc: '右門縮小' },
  { i: 2, title: '右門加大', desc: '左門縮小' }
];
export default function RestBox({ onPick }) {
  return (
    <div id="rest">
      <h2>休息</h2>
      <div className="cards">
        {OPTIONS.map(o => (
          <button key={o.i} className="card" type="button" onClick={() => onPick(o.i)}>
            <span className="ct">{o.title}</span><span className="cd">{o.desc}</span>
          </button>
        ))}
      </div>
    </div>
  );
}
