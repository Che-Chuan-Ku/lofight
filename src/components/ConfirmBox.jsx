// Shown when the restart button is pressed mid-game; the simulation is paused meanwhile.
export default function ConfirmBox({ onYes, onNo }) {
  return (
    <div id="confirm">
      <h2>重新開始？</h2>
      <p>目前這局的進度會消失。</p>
      <div className="row">
        <button type="button" className="no" onClick={onNo}>取消</button>
        <button type="button" className="yes" onClick={onYes}>重新開始</button>
      </div>
    </div>
  );
}
