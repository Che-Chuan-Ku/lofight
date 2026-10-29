// Intro and end screens share one panel; `phase` is 'intro' or 'end'.
export default function Overlay({ phase, result, time, onStart }) {
  const ended = phase === 'end', win = result === 'win';
  const title = !ended ? '人海攻城' : win ? 'Victory!' : 'Defeat in Battle';
  const sub = !ended ? '左右移動砲台，守住陣地。' : (win ? '用了 ' : '撐了 ') + time + ' 秒。';
  return (
    <div id="overlay">
      <h1 className={ended && !win ? 'lose' : ''}>{title}</h1>
      <p>{sub}</p>
      <button type="button" onClick={onStart}>{ended ? '再來一局' : '開始'}</button>
    </div>
  );
}
