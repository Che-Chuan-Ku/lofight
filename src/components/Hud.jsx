export default function Hud({ hud }) {
  return (
    <div id="hud">
      <div className="b"><small>藍軍</small><span>{hud.blue}</span></div>
      <div className="t"><small>時間</small><span>{hud.t}</span></div>
      <div className="k"><small>魔王</small><span>{hud.kills}</span></div>
      <div className="r"><small>紅軍</small><span>{hud.red}</span></div>
    </div>
  );
}
