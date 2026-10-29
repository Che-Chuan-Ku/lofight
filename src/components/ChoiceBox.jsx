// Pick-one-of-two panel shown when a "?" crate breaks; the game is paused meanwhile.
export default function ChoiceBox({ cards, onPick }) {
  return (
    <div id="choice">
      <h2>二選一</h2>
      <div className="cards">
        {(cards || []).map((c, i) => (
          <button key={i} className={'card ' + (c.good ? 'good' : 'bad')} type="button" onClick={() => onPick(i)}>
            <span className="ct">{c.title}</span><span className="cd">{c.desc}</span>
          </button>
        ))}
      </div>
    </div>
  );
}
