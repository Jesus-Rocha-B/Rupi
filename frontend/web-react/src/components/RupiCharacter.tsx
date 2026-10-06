export type RupiRole = 'default' | 'scientist' | 'mathematician' | 'reader'

type Props = {
  className?: string
  mood?: 'idle' | 'jumping' | 'thinking' | 'cheering'
  role?: RupiRole
}

export function RupiCharacter({ className = '', mood = 'idle', role = 'default' }: Props) {
  return (
    <svg className={`rupi-character mood-${mood} role-${role} ${className}`} viewBox="0 0 160 170" aria-hidden="true" focusable="false">
      <ellipse className="rupi-ground-shadow" cx="81" cy="157" rx="47" ry="8" />
      <g className="rupi-tail"><path d="M48 116 20 131q11 2 21-2l-13 13q18-2 30-15" /><path d="m47 119-18 15" /></g>
      <path className="rupi-body" d="M38 112c-7-20 0-45 18-56 16-10 42-9 58 5 17 15 20 42 11 62-9 19-29 28-52 26-19-2-31-14-35-37Z" />
      <path className="rupi-wing" d="M55 99c13-14 36-17 52-5 10 7 13 18 9 29-12 12-31 17-48 10-12-5-18-18-13-34Z" />
      <path className="rupi-crest" d="M42 65c-12-7-10-20 4-24-4-13 8-22 21-17 3-15 19-18 29-7 12-11 27-3 26 11 15 3 17 17 7 25 7 10 0 21-12 22-16-12-42-15-63-7-8 3-14 1-17-3-10 7-21 0-20-10 0-4 3-8 8-10 5-2 12 0 17 3Z" />

      {/* Sombrero académico para Rupi Matemático */}
      {role === 'mathematician' && (
        <g className="rupi-academic-cap">
          <polygon points="85,30 114,38 85,46 56,38" fill="#173e30" stroke="#ffd26a" strokeWidth="1.8" />
          <path d="M68 41.5 L68 49 Q85 55 102 49 L102 41.5" fill="#173e30" stroke="#ffd26a" strokeWidth="1.2" />
          <circle cx="85" cy="38" r="2.2" fill="#ffd26a" />
          <path d="M85 38 Q102 42 108 52" fill="none" stroke="#ffd26a" strokeWidth="1.8" strokeLinecap="round" />
        </g>
      )}

      <path className="rupi-face" d="M56 72c13-12 39-15 56-3 13 9 16 27 7 40-9 14-31 18-49 11-19-7-27-32-14-48Z" />
      <path className="rupi-beak" d="m111 77 38 13-37 12q-11-11-1-25Z" />
      <path className="rupi-beak-shine" d="m116 83 25 7-26 2" />
      <ellipse className="rupi-eye-white" cx="91" cy="83" rx="13" ry="16" />
      <ellipse className="rupi-eye-pupil" cx="95" cy="85" rx="6" ry="9" />
      <circle className="rupi-eye-glint" cx="97" cy="81" r="2.5" />

      {/* Lentes de investigador para Rupi Científico */}
      {role === 'scientist' && (
        <g className="rupi-scientist-glasses">
          <circle cx="91" cy="83" r="14" className="glasses-lens" fill="rgba(200, 240, 255, 0.35)" stroke="#31724b" strokeWidth="2.5" />
          <circle cx="91" cy="83" r="16.5" fill="none" stroke="#ffd26a" strokeWidth="1.2" />
          <path d="M105 82 Q112 80 115 83" fill="none" stroke="#31724b" strokeWidth="2.5" strokeLinecap="round" />
          <path d="M83 75 L88 72 M85 81 L94 74" stroke="#ffffff" strokeWidth="2" strokeLinecap="round" />
        </g>
      )}

      <path className="rupi-cheek" d="M67 102q9 8 17 1" />
      <path className="rupi-feet" d="m70 145-3 9m3-4-7 2m43-7 2 9m-2-4 7 2" />

      {/* Accesorios para Rupi Lector (Comunicación) */}
      {role === 'reader' && (
        <g className="rupi-reader-items">
          {/* Libro escolar abierto sostenido con cariño */}
          <g className="rupi-open-book">
            <path d="M44 116 Q74 125 77 112 Q80 125 110 116 L106 138 Q80 143 77 129 Q74 143 48 138 Z" fill="#246a91" stroke="#173e30" strokeWidth="2.5" />
            <path d="M48 115 Q72 123 76 112 L76 128 Q72 138 50 135 Z" fill="#fffef8" stroke="#173e30" strokeWidth="1.5" />
            <path d="M78 112 Q82 123 106 115 L104 135 Q82 138 78 128 Z" fill="#fffdf2" stroke="#173e30" strokeWidth="1.5" />
            <line x1="53" y1="121" x2="71" y2="123" stroke="#d4a34b" strokeWidth="1.5" strokeLinecap="round" />
            <line x1="53" y1="125" x2="69" y2="127" stroke="#d4a34b" strokeWidth="1.5" strokeLinecap="round" />
            <line x1="83" y1="123" x2="101" y2="121" stroke="#d4a34b" strokeWidth="1.5" strokeLinecap="round" />
            <line x1="83" y1="127" x2="99" y2="125" stroke="#d4a34b" strokeWidth="1.5" strokeLinecap="round" />
            <path d="M77 128 Q78 139 82 145" stroke="#f95738" strokeWidth="2.5" fill="none" strokeLinecap="round" />
          </g>
          {/* Pluma de escribir andina */}
          <g className="rupi-reading-quill">
            <path d="M108 100 Q122 86 128 76 Q125 88 116 102 Z" fill="#ffd26a" stroke="#a67114" strokeWidth="1.5" />
            <line x1="108" y1="100" x2="128" y2="76" stroke="#8c5806" strokeWidth="1.2" />
          </g>
          {/* Letras mágicas flotantes */}
          <text x="122" y="70" className="reading-glyph glyph-a" fill="#246a91" fontSize="13" fontWeight="900" fontFamily="Nunito">A</text>
          <text x="134" y="87" className="reading-glyph glyph-b" fill="#f95738" fontSize="11" fontWeight="900" fontFamily="Nunito">B</text>
          <text x="32" y="108" className="reading-glyph glyph-c" fill="#31724b" fontSize="11" fontWeight="900" fontFamily="Nunito">C</text>
        </g>
      )}

      {/* Accesorios para Rupi Matemático (Matemática) */}
      {role === 'mathematician' && (
        <g className="rupi-math-items">
          {/* Lápiz escolar alzado triunfante */}
          <g className="rupi-math-pencil" transform="rotate(22 120 72)">
            <polygon points="120,58 124,66 116,66" fill="#ffd26a" stroke="#a77519" strokeWidth="1.5" />
            <polygon points="120,58 122,62 118,62" fill="#3a2512" />
            <rect x="116" y="66" width="8" height="24" rx="1" fill="#f95738" stroke="#a9322c" strokeWidth="1.5" />
            <rect x="116" y="86" width="8" height="4" fill="#dbe8d2" />
            <rect x="116" y="90" width="8" height="6" rx="1.5" fill="#f096a5" stroke="#c45d71" strokeWidth="1" />
          </g>
          {/* Regla escolar graduada */}
          <g className="rupi-math-ruler" transform="rotate(-30 46 128)">
            <rect x="36" y="120" width="30" height="9" rx="2" fill="#ffd26a" stroke="#a77519" strokeWidth="1.5" />
            <line x1="42" y1="120" x2="42" y2="124" stroke="#754b08" strokeWidth="1" />
            <line x1="47" y1="120" x2="47" y2="126" stroke="#754b08" strokeWidth="1.5" />
            <line x1="52" y1="120" x2="52" y2="124" stroke="#754b08" strokeWidth="1" />
            <line x1="57" y1="120" x2="57" y2="126" stroke="#754b08" strokeWidth="1.5" />
            <line x1="62" y1="120" x2="62" y2="124" stroke="#754b08" strokeWidth="1" />
          </g>
          {/* Símbolos matemáticos flotantes */}
          <text x="126" y="52" className="math-symbol sym-plus" fill="#31724b" fontSize="15" fontWeight="900" fontFamily="Nunito">+</text>
          <text x="24" y="98" className="math-symbol sym-times" fill="#f95738" fontSize="14" fontWeight="900" fontFamily="Nunito">×</text>
          <text x="136" y="80" className="math-symbol sym-equal" fill="#246a91" fontSize="14" fontWeight="900" fontFamily="Nunito">=</text>
          <text x="32" y="72" className="math-symbol sym-div" fill="#98591e" fontSize="14" fontWeight="900" fontFamily="Nunito">÷</text>
        </g>
      )}

      {/* Accesorios para Rupi Científico (Ciencia y Tecnología - CYT) */}
      {role === 'scientist' && (
        <g className="rupi-science-items">
          {/* Matraz Erlenmeyer con líquido burbujeante */}
          <g className="rupi-flask-container">
            <ellipse cx="127" cy="116" rx="18" ry="7" transform="rotate(-28 127 116)" className="atom-orbit orbit-1" fill="none" stroke="#246a91" strokeWidth="1.5" strokeDasharray="3 3" />
            <ellipse cx="127" cy="116" rx="18" ry="7" transform="rotate(35 127 116)" className="atom-orbit orbit-2" fill="none" stroke="#ffd26a" strokeWidth="1.5" strokeDasharray="3 3" />
            <circle cx="140" cy="108" r="2.2" className="atom-particle" fill="#ffd26a" />
            {/* Matraz de vidrio */}
            <rect x="123" y="103" width="8" height="11" rx="1.5" fill="rgba(240, 250, 255, 0.7)" stroke="#235538" strokeWidth="2" />
            <ellipse cx="127" cy="103" rx="5.5" ry="2" fill="#ffffff" stroke="#235538" strokeWidth="2" />
            <path d="M123 114 L112 135 Q111 138 115 138 L139 138 Q143 138 142 135 L131 114 Z" fill="rgba(240, 250, 255, 0.7)" stroke="#235538" strokeWidth="2" />
            {/* Líquido mágico verde esmeralda */}
            <path d="M116 128 Q127 126 138 128 L139 136 Q140 138 137 138 L117 138 Q114 138 115 136 Z" fill="#31724b" />
            <ellipse cx="127" cy="128" rx="11" ry="3" fill="#4fa871" />
            {/* Burbujitas subiendo */}
            <circle cx="125" cy="122" r="2.2" className="flask-bubble b1" fill="#ffd26a" />
            <circle cx="129" cy="114" r="2.6" className="flask-bubble b2" fill="#ffd26a" />
            <circle cx="124" cy="98" r="3" className="flask-bubble b3" fill="#65c98d" />
            <circle cx="130" cy="90" r="2.2" className="flask-bubble b4" fill="#ffd26a" />
          </g>
        </g>
      )}
    </svg>
  )
}
