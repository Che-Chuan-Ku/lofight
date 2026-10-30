// Shown once the last planned boss falls: stop here, or go on to the hidden boss (the clear time is kept either way).
export default function OfferBox({ onYes, onNo }) {
  return (
    <div id="offer">
      <h2>隱藏關卡</h2>
      <p>要挑戰隱藏魔王嗎？</p>
      <div className="row">
        <button type="button" className="no" onClick={onNo}>見好就收</button>
        <button type="button" className="yes" onClick={onYes}>挑戰 ㊙</button>
      </div>
    </div>
  );
}
