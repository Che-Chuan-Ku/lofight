// Pick-one panel: two cards when a "?" crate breaks, three before the hidden stage; the game is paused meanwhile.
export default function ChoiceBox({ title = '二選一', cards, onPick }) {
  return (
    <div id="choice">
      <h2>{title}</h2>
      <div className={'cards' + ((cards || []).length > 2 ? ' three' : '')}>
        {(cards || []).map((c, i) => (
          <button key={i} className={'card ' + (c.good ? 'good' : 'bad')} type="button" onClick={() => onPick(i)}>
            <span className="ct">{c.title}</span><span className="cd">{c.desc}</span>
          </button>
        ))}
      </div>
    </div>
  );
}
