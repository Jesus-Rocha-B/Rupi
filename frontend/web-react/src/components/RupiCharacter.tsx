type Props = {
  className?: string
  mood?: 'idle' | 'jumping' | 'thinking' | 'cheering'
}

export function RupiCharacter({ className = '', mood = 'idle' }: Props) {
  return (
    <svg className={`rupi-character mood-${mood} ${className}`} viewBox="0 0 160 170" aria-hidden="true" focusable="false">
      <ellipse className="rupi-ground-shadow" cx="81" cy="157" rx="47" ry="8" />
      <g className="rupi-tail"><path d="M48 116 20 131q11 2 21-2l-13 13q18-2 30-15" /><path d="m47 119-18 15" /></g>
      <path className="rupi-body" d="M38 112c-7-20 0-45 18-56 16-10 42-9 58 5 17 15 20 42 11 62-9 19-29 28-52 26-19-2-31-14-35-37Z" />
      <path className="rupi-wing" d="M55 99c13-14 36-17 52-5 10 7 13 18 9 29-12 12-31 17-48 10-12-5-18-18-13-34Z" />
      <path className="rupi-crest" d="M42 65c-12-7-10-20 4-24-4-13 8-22 21-17 3-15 19-18 29-7 12-11 27-3 26 11 15 3 17 17 7 25 7 10 0 21-12 22-16-12-42-15-63-7-8 3-14 1-17-3-10 7-21 0-20-10 0-4 3-8 8-10 5-2 12 0 17 3Z" />
      <path className="rupi-face" d="M56 72c13-12 39-15 56-3 13 9 16 27 7 40-9 14-31 18-49 11-19-7-27-32-14-48Z" />
      <path className="rupi-beak" d="m111 77 38 13-37 12q-11-11-1-25Z" />
      <path className="rupi-beak-shine" d="m116 83 25 7-26 2" />
      <ellipse className="rupi-eye-white" cx="91" cy="83" rx="13" ry="16" />
      <ellipse className="rupi-eye-pupil" cx="95" cy="85" rx="6" ry="9" />
      <circle className="rupi-eye-glint" cx="97" cy="81" r="2.5" />
      <path className="rupi-cheek" d="M67 102q9 8 17 1" />
      <path className="rupi-feet" d="m70 145-3 9m3-4-7 2m43-7 2 9m-2-4 7 2" />
    </svg>
  )
}
