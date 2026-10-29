// Static shell; the numbers are updated imperatively by useGame through the data-hud hooks.
export default function Hud() {
  return (
    <div id="hud">
      <div className="b"><small>藍軍</small><span data-hud="blue">0</span></div>
      <div className="t"><small>時間</small><span data-hud="t">0</span></div>
      <div className="k"><small>魔王</small><span data-hud="kills">0</span></div>
      <div className="r"><small>紅軍</small><span data-hud="red">0</span></div>
    </div>
  );
}
