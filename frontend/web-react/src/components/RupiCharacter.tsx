export type RupiRole = 'default' | 'scientist' | 'mathematician' | 'reader' | 'matematica'

type Props = {
  className?: string
  mood?: 'idle' | 'jumping' | 'thinking' | 'cheering'
  role?: RupiRole
}

export function RupiCharacter({ className = '', mood = 'idle', role = 'default' }: Props) {
  const resolvedRole = role === 'matematica' ? 'mathematician' : role

  return (
    <svg
      className={`rupi-character mood-${mood} role-${resolvedRole} ${className}`}
      viewBox="0 0 160 170"
      aria-hidden="true"
      focusable="false"
    >
      {/* Sombra base en el suelo */}
      <ellipse className="rupi-ground-shadow" cx="81" cy="157" rx="47" ry="8" />

      {/* Cola de plumas andinas */}
      <g className="rupi-tail">
        <path d="M48 116 20 131q11 2 21-2l-13 13q18-2 30-15" />
        <path d="m47 119-18 15" />
      </g>

      {/* Cuerpo / torso base de Rupi */}
      <path
        className="rupi-body"
        d="M38 112c-7-20 0-45 18-56 16-10 42-9 58 5 17 15 20 42 11 62-9 19-29 28-52 26-19-2-31-14-35-37Z"
      />

      {/* =================================================================== */}
      {/* CAPA 1: TRAJES REALISTAS AJUSTADOS AL VIENTRE (Bata, Camisa, Chaleco)*/}
      {/* =================================================================== */}

      {/* 1. Científico: Bata blanca de laboratorio con solapas y bolsillo con bolígrafos */}
      {resolvedRole === 'scientist' && (
        <g className="rupi-scientist-costume">
          {/* Cuerpo completo de la bata blanca siguiendo la curvatura real del cuerpo */}
          <path
            className="rupi-lab-coat"
            d="M40 108 C35 120 38 132 46 142 C56 148 76 150 96 144 C112 138 122 128 123 118 C116 112 104 106 90 104 C72 102 54 104 40 108 Z"
            fill="#ffffff"
            stroke="#173e30"
            strokeWidth="3"
            strokeLinejoin="round"
          />
          {/* Solapa izquierda de la bata */}
          <path
            d="M52 105 L66 122 L54 125 L45 107 Z"
            fill="#edf5f0"
            stroke="#173e30"
            strokeWidth="2"
            strokeLinejoin="round"
          />
          {/* Solapa derecha de la bata */}
          <path
            d="M92 105 L78 122 L90 125 L99 107 Z"
            fill="#edf5f0"
            stroke="#173e30"
            strokeWidth="2"
            strokeLinejoin="round"
          />
          {/* Cuello interior en V y camisa azul de fondo */}
          <polygon points="68,105 76,105 72,115" fill="#246a91" stroke="#173e30" strokeWidth="1.2" />
          {/* Línea central de botones */}
          <line x1="72" y1="120" x2="72" y2="148" stroke="#173e30" strokeWidth="2" />
          {/* Botones verde esmeralda */}
          <circle cx="72" cy="126" r="2" fill="#31724b" stroke="#173e30" strokeWidth="1" />
          <circle cx="72" cy="134" r="2" fill="#31724b" stroke="#173e30" strokeWidth="1" />
          <circle cx="72" cy="142" r="2" fill="#31724b" stroke="#173e30" strokeWidth="1" />
          {/* Bolsillo en el pecho izquierdo */}
          <rect x="49" y="122" width="14" height="12" rx="1.5" fill="#ffffff" stroke="#173e30" strokeWidth="1.8" />
          {/* Dos bolígrafos en el bolsillo (uno verde y uno naranja) */}
          <rect x="52" y="116" width="2.6" height="8" rx="1" fill="#31724b" stroke="#173e30" strokeWidth="0.8" />
          <rect x="57" y="115" width="2.6" height="9" rx="1" fill="#f95738" stroke="#173e30" strokeWidth="0.8" />
          <line x1="54.6" y1="118" x2="54.6" y2="122" stroke="#ffd26a" strokeWidth="0.8" />
          <line x1="59.6" y1="117" x2="59.6" y2="122" stroke="#ffd26a" strokeWidth="0.8" />
        </g>
      )}

      {/* 2. Matemático: Camisa escolar con tirantes, cuello doblado, corbata y lápiz en bolsillo */}
      {resolvedRole === 'mathematician' && (
        <g className="rupi-math-costume">
          {/* Cuerpo de la camisa ajustado al vientre */}
          <path
            className="rupi-math-shirt-body"
            d="M40 108 C35 120 38 132 46 142 C56 148 76 150 96 144 C112 138 122 128 123 118 C116 112 104 106 90 104 C72 102 54 104 40 108 Z"
            fill="#f4f7f9"
            stroke="#173e30"
            strokeWidth="3"
            strokeLinejoin="round"
          />
          {/* Tirantes escolares azul marino */}
          <path d="M54 106 L57 146" stroke="#246a91" strokeWidth="3.5" strokeLinecap="round" />
          <path d="M90 105 L87 144" stroke="#246a91" strokeWidth="3.5" strokeLinecap="round" />
          {/* Broches metálicos dorados de los tirantes */}
          <rect x="52.5" y="128" width="3" height="4" rx="0.5" fill="#ffd26a" stroke="#173e30" strokeWidth="0.8" />
          <rect x="85.5" y="127" width="3" height="4" rx="0.5" fill="#ffd26a" stroke="#173e30" strokeWidth="0.8" />
          {/* Cuello camisero blanco doblado */}
          <polygon points="58,105 71,116 73,107 64,103" fill="#ffffff" stroke="#173e30" strokeWidth="1.8" strokeLinejoin="round" />
          <polygon points="86,105 73,116 71,107 80,103" fill="#ffffff" stroke="#173e30" strokeWidth="1.8" strokeLinejoin="round" />
          {/* Corbata escolar roja Rupi colgada naturalmente */}
          <polygon points="69,115 75,115 78,136 72,141 66,136" fill="#f95738" stroke="#173e30" strokeWidth="1.8" strokeLinejoin="round" />
          <polygon points="68,113 76,113 74,118 70,118" fill="#cc3a1d" stroke="#173e30" strokeWidth="1.2" />
          {/* Bolsillito de camisa con el lápiz bien guardado dentro (realista, no flotando) */}
          <rect x="46" y="122" width="12" height="11" rx="1.5" fill="#ffffff" stroke="#173e30" strokeWidth="1.5" />
          <rect x="49" y="116" width="3" height="8" rx="0.8" fill="#ffd26a" stroke="#173e30" strokeWidth="0.8" />
          <polygon points="49,116 52,116 50.5,112" fill="#3a2512" />
        </g>
      )}

      {/* 3. Lector: Chaleco escolar de punto azul andino con cuello en V ribeteado y camisa */}
      {resolvedRole === 'reader' && (
        <g className="rupi-reader-costume">
          {/* Cuerpo del chaleco ajustado */}
          <path
            className="rupi-reader-sweater"
            d="M40 108 C35 120 38 132 46 142 C56 148 76 150 96 144 C112 138 122 128 123 118 C116 112 104 106 90 104 C72 102 54 104 40 108 Z"
            fill="#246a91"
            stroke="#173e30"
            strokeWidth="3"
            strokeLinejoin="round"
          />
          {/* Borde inferior del chaleco */}
          <path d="M48 138 C68 144 88 143 104 137" stroke="#173e30" strokeWidth="1.8" fill="none" />
          {/* Cuello en V ribeteado en amarillo maíz escolar */}
          <path d="M56 106 L72 121 L88 106" fill="none" stroke="#ffd26a" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round" />
          {/* Cuello camisero blanco que asoma */}
          <polygon points="58,105 70,116 73,107 64,103" fill="#ffffff" stroke="#173e30" strokeWidth="1.8" strokeLinejoin="round" />
          <polygon points="86,105 74,116 71,107 80,103" fill="#ffffff" stroke="#173e30" strokeWidth="1.8" strokeLinejoin="round" />
          {/* Corbatita escolar roja */}
          <polygon points="70,116 74,116 73,124 71,124" fill="#f95738" stroke="#173e30" strokeWidth="1.2" />
          {/* Insignia escolar dorada bordada en el pecho */}
          <circle cx="53" cy="123" r="3.5" fill="#ffd26a" stroke="#173e30" strokeWidth="1" />
          <path d="M51.5 123 L54.5 123 M53 121.5 L53 124.5" stroke="#173e30" strokeWidth="0.8" />
        </g>
      )}

      {/* Ala de Rupi */}
      <path
        className="rupi-wing"
        d="M55 99c13-14 36-17 52-5 10 7 13 18 9 29-12 12-31 17-48 10-12-5-18-18-13-34Z"
      />

      {/* Copete de plumas de gallito de las rocas */}
      <path
        className="rupi-crest"
        d="M42 65c-12-7-10-20 4-24-4-13 8-22 21-17 3-15 19-18 29-7 12-11 27-3 26 11 15 3 17 17 7 25 7 10 0 21-12 22-16-12-42-15-63-7-8 3-14 1-17-3-10 7-21 0-20-10 0-4 3-8 8-10 5-2 12 0 17 3Z"
      />

      {/* Rostro y rasgos faciales */}
      <path className="rupi-face" d="M56 72c13-12 39-15 56-3 13 9 16 27 7 40-9 14-31 18-49 11-19-7-27-32-14-48Z" />
      <path className="rupi-beak" d="m111 77 38 13-37 12q-11-11-1-25Z" />
      <path className="rupi-beak-shine" d="m116 83 25 7-26 2" />
      <ellipse className="rupi-eye-white" cx="91" cy="83" rx="13" ry="16" />
      <ellipse className="rupi-eye-pupil" cx="95" cy="85" rx="6" ry="9" />
      <circle className="rupi-eye-glint" cx="97" cy="81" r="2.5" />

      {/* =================================================================== */}
      {/* CAPA 2: GAFAS TEMÁTICAS BIEN CALZADAS SOBRE EL ROSTRO              */}
      {/* =================================================================== */}

      {/* Gafas de Científico (Protección de laboratorio con montura verde y patilla) */}
      {resolvedRole === 'scientist' && (
        <g className="rupi-scientist-glasses">
          <circle cx="91" cy="83" r="14.5" fill="rgba(215, 245, 255, 0.4)" stroke="#173e30" strokeWidth="2.5" />
          <circle cx="91" cy="83" r="13" fill="none" stroke="#31724b" strokeWidth="1.8" />
          <path d="M105 82 L115 84" stroke="#173e30" strokeWidth="2.5" strokeLinecap="round" />
          <path d="M83 75 L88 72 M85 81 L93 74" stroke="#ffffff" strokeWidth="2" strokeLinecap="round" />
        </g>
      )}

      {/* Gafas de Lector (Montura redonda dorada académica con patilla firme) */}
      {resolvedRole === 'reader' && (
        <g className="rupi-reading-glasses">
          <circle cx="91" cy="83" r="14.5" fill="rgba(255, 255, 255, 0.25)" stroke="#173e30" strokeWidth="2.5" />
          <circle cx="91" cy="83" r="13" fill="none" stroke="#ffd26a" strokeWidth="1.8" />
          <path d="M105 82 Q111 80 115 83" fill="none" stroke="#173e30" strokeWidth="2.5" strokeLinecap="round" />
          <path d="M105 82 Q111 80 115 83" fill="none" stroke="#ffd26a" strokeWidth="1.2" strokeLinecap="round" />
          <path d="M83 75 L88 72 M85 81 L93 74" stroke="#ffffff" strokeWidth="2" strokeLinecap="round" />
        </g>
      )}

      {/* Mejilla sonrojada y patitas */}
      <path className="rupi-cheek" d="M67 102q9 8 17 1" />
      <path className="rupi-feet" d="m70 145-3 9m3-4-7 2m43-7 2 9m-2-4 7 2" />

      {/* =================================================================== */}
      {/* CAPA 3: OBJETOS SOSTENIDOS EN MANO (Sin flotar en el aire)         */}
      {/* =================================================================== */}

      {/* Lector: Libro escolar abierto sostenido firmemente con ambas alas al frente */}
      {resolvedRole === 'reader' && (
        <g className="rupi-reader-items" transform="translate(42, 114)">
          {/* Sombra del libro */}
          <path d="M3 3 Q33 13 36 0 Q39 13 69 3 L65 25 Q39 29 36 17 Q33 29 7 25 Z" fill="rgba(23, 62, 48, 0.2)" />
          {/* Portada exterior del libro escolar */}
          <path d="M0 0 Q30 10 33 -3 Q36 10 66 0 L62 22 Q36 26 33 14 Q30 26 4 22 Z" fill="#173e30" stroke="#173e30" strokeWidth="2" strokeLinejoin="round" />
          {/* Páginas interiores color marfil */}
          <path d="M4 -1 Q28 7 32 -3 L32 13 Q28 23 6 20 Z" fill="#fffef8" stroke="#173e30" strokeWidth="1.5" />
          <path d="M34 -3 Q38 7 62 -1 L60 20 Q38 23 34 13 Z" fill="#fffdf2" stroke="#173e30" strokeWidth="1.5" />
          {/* Líneas doradas de lectura */}
          <line x1="9" y1="5" x2="27" y2="7" stroke="#d4a34b" strokeWidth="1.5" strokeLinecap="round" />
          <line x1="9" y1="10" x2="25" y2="12" stroke="#d4a34b" strokeWidth="1.5" strokeLinecap="round" />
          <line x1="9" y1="15" x2="26" y2="17" stroke="#d4a34b" strokeWidth="1.5" strokeLinecap="round" />
          <line x1="39" y1="7" x2="57" y2="5" stroke="#d4a34b" strokeWidth="1.5" strokeLinecap="round" />
          <line x1="39" y1="12" x2="55" y2="10" stroke="#d4a34b" strokeWidth="1.5" strokeLinecap="round" />
          <line x1="39" y1="17" x2="56" y2="15" stroke="#d4a34b" strokeWidth="1.5" strokeLinecap="round" />
          {/* Cinta marcapáginas roja */}
          <path d="M33 14 Q34 25 38 30" stroke="#f95738" strokeWidth="2.5" fill="none" strokeLinecap="round" />
          {/* Alita izquierda sosteniendo el libro */}
          <ellipse cx="2" cy="11" rx="4" ry="6" transform="rotate(-15 2 11)" fill="#de5638" stroke="#9c382e" strokeWidth="2" />
          {/* Alita derecha sosteniendo el libro */}
          <ellipse cx="64" cy="11" rx="4" ry="6" transform="rotate(15 64 11)" fill="#de5638" stroke="#9c382e" strokeWidth="2" />
        </g>
      )}

      {/* Matemático: Calculadora escolar de mano sostenida firmemente por el ala */}
      {resolvedRole === 'mathematician' && (
        <g className="rupi-math-items" transform="translate(88, 106) rotate(4)">
          {/* Sombra de la calculadora */}
          <rect x="2" y="2" width="30" height="40" rx="4" fill="rgba(23, 62, 48, 0.25)" />
          {/* Carcasa física sólida verde bosque */}
          <rect x="0" y="0" width="30" height="40" rx="4" fill="#173e30" stroke="#ffd26a" strokeWidth="2" />
          {/* Pantalla digital LCD */}
          <rect x="3.5" y="4.5" width="23" height="9.5" rx="1.5" fill="#2d6447" stroke="#173e30" strokeWidth="1" />
          <text x="24.5" y="12" fill="#ffd26a" fontSize="7" fontWeight="900" fontFamily="monospace" textAnchor="end">123</text>
          {/* Teclas numéricas */}
          <rect x="4" y="16.5" width="5.5" height="4.5" rx="1" fill="#ffffff" stroke="#173e30" strokeWidth="0.8" />
          <rect x="12" y="16.5" width="5.5" height="4.5" rx="1" fill="#ffffff" stroke="#173e30" strokeWidth="0.8" />
          <rect x="20" y="16.5" width="5.5" height="4.5" rx="1" fill="#f95738" stroke="#173e30" strokeWidth="0.8" />
          <text x="22.7" y="20" fill="#ffffff" fontSize="4.5" fontWeight="900" textAnchor="middle">+</text>

          <rect x="4" y="23" width="5.5" height="4.5" rx="1" fill="#ffffff" stroke="#173e30" strokeWidth="0.8" />
          <rect x="12" y="23" width="5.5" height="4.5" rx="1" fill="#ffffff" stroke="#173e30" strokeWidth="0.8" />
          <rect x="20" y="23" width="5.5" height="4.5" rx="1" fill="#ffd26a" stroke="#173e30" strokeWidth="0.8" />
          <text x="22.7" y="26.5" fill="#173e30" fontSize="4.5" fontWeight="900" textAnchor="middle">×</text>

          <rect x="4" y="29.5" width="5.5" height="4.5" rx="1" fill="#ffffff" stroke="#173e30" strokeWidth="0.8" />
          <rect x="12" y="29.5" width="5.5" height="4.5" rx="1" fill="#ffffff" stroke="#173e30" strokeWidth="0.8" />
          <rect x="20" y="29.5" width="5.5" height="4.5" rx="1" fill="#246a91" stroke="#173e30" strokeWidth="0.8" />
          <text x="22.7" y="33" fill="#ffffff" fontSize="4.5" fontWeight="900" textAnchor="middle">=</text>

          {/* Ala de Rupi sosteniendo la calculadora por el lateral */}
          <ellipse cx="28" cy="22" rx="4" ry="8" transform="rotate(15 28 22)" fill="#de5638" stroke="#9c382e" strokeWidth="2" />
        </g>
      )}

      {/* Científico: Matraz Erlenmeyer sostenido firmemente por el ala */}
      {resolvedRole === 'scientist' && (
        <g className="rupi-science-items" transform="translate(104, 102)">
          {/* Cuello de vidrio del matraz */}
          <rect x="10" y="0" width="8" height="10" rx="1.5" fill="rgba(240, 250, 255, 0.85)" stroke="#173e30" strokeWidth="2" />
          <ellipse cx="14" cy="0" rx="5.5" ry="2" fill="#ffffff" stroke="#173e30" strokeWidth="2" />
          {/* Cuerpo cónico de vidrio */}
          <path d="M10 10 L0 30 Q-1 33 3 33 L25 33 Q29 33 28 30 L18 10 Z" fill="rgba(240, 250, 255, 0.85)" stroke="#173e30" strokeWidth="2" strokeLinejoin="round" />
          {/* Líquido químico verde esmeralda con menisco */}
          <path d="M3 24 Q14 22 25 24 L26 31 Q27 33 24 33 L4 33 Q1 33 2 31 Z" fill="#31724b" />
          <ellipse cx="14" cy="24" rx="10.5" ry="2.5" fill="#4fa871" />
          {/* Burbujitas químicas */}
          <circle cx="11" cy="28" r="1.8" fill="#ffd26a" />
          <circle cx="17" cy="26" r="2.2" fill="#ffd26a" />
          <circle cx="13" cy="20" r="1.5" fill="#65c98d" />
          {/* Ala de Rupi sosteniendo el cuello del matraz */}
          <ellipse cx="8" cy="12" rx="4" ry="7" transform="rotate(-20 8 12)" fill="#de5638" stroke="#9c382e" strokeWidth="2" />
        </g>
      )}
    </svg>
  )
}
