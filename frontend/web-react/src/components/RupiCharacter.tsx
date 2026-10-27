export type RupiRole = 'default' | 'scientist' | 'mathematician' | 'reader' | 'matematica'

export interface EquippedAccessories {
  sombrero?: string | null
  vestimenta?: string | null
  fondo?: string | null
}

type Props = {
  className?: string
  mood?: 'idle' | 'jumping' | 'thinking' | 'cheering'
  role?: RupiRole
  accessories?: EquippedAccessories
}

export function RupiCharacter({
  className = '',
  mood = 'idle',
  role = 'default',
  accessories,
}: Props) {
  const resolvedRole = role === 'matematica' ? 'mathematician' : role

  // Los accesorios equipados tienen prioridad visual sobre el rol predeterminado
  const activeSombrero = accessories?.sombrero || null
  const activeVestimenta = accessories?.vestimenta || null
  const activeFondo = accessories?.fondo || null

  return (
    <svg
      className={`rupi-character mood-${mood} role-${resolvedRole} ${className}`}
      viewBox="0 0 160 170"
      aria-hidden="true"
      focusable="false"
    >
      <defs>
        <radialGradient id="rupi-aura-grad" cx="50%" cy="50%" r="50%">
          <stop offset="0%" stopColor="#ffd26a" stopOpacity="0.75" />
          <stop offset="65%" stopColor="#f95738" stopOpacity="0.3" />
          <stop offset="100%" stopColor="#f95738" stopOpacity="0" />
        </radialGradient>
        <linearGradient id="rupi-poncho-grad" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stopColor="#d94432" />
          <stop offset="100%" stopColor="#a32819" />
        </linearGradient>
        <linearGradient id="rupi-chullo-grad" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stopColor="#c73c2a" />
          <stop offset="100%" stopColor="#872013" />
        </linearGradient>
      </defs>

      {/* =================================================================== */}
      {/* CAPA 0: FONDOS / EFECTOS ESPECIALES DESBLOQUEABLES                  */}
      {/* =================================================================== */}

      {/* Efecto 1: Aura Solar Inti */}
      {activeFondo === 'AURA_SOLAR' && (
        <g className="rupi-aura-solar">
          <circle cx="82" cy="85" r="62" fill="url(#rupi-aura-grad)" />
          <circle cx="82" cy="85" r="54" fill="none" stroke="#ffd26a" strokeWidth="2.5" strokeDasharray="6 5" opacity="0.7" />
        </g>
      )}

      {/* Efecto 2: Estrellas Andinas de la Cruz del Sur */}
      {activeFondo === 'ESTRELLAS_ANDINAS' && (
        <g className="rupi-estrellas-andinas" fill="#ffd26a" stroke="#c9911e" strokeWidth="0.8">
          <polygon points="28,45 31,35 34,45 44,48 34,51 31,61 28,51 18,48" opacity="0.85" />
          <polygon points="135,52 137,44 139,52 147,54 139,56 137,64 135,56 127,54" opacity="0.8" />
          <polygon points="22,112 24,106 26,112 32,114 26,116 24,122 22,116 16,114" opacity="0.85" />
          <polygon points="138,122 140,116 142,122 148,124 142,126 140,132 138,126 132,124" opacity="0.8" />
        </g>
      )}

      {/* Sombra base en el suelo */}
      <ellipse className="rupi-ground-shadow" cx="81" cy="157" rx="47" ry="8" />

      {/* Cola de plumas andinas */}
      <g className="rupi-tail">
        <path d="M48 116 20 131q11 2 21-2l-13 13q18-2 30-15" />
        <path d="m47 119-18 15" />
      </g>

      {/* Capa de Héroe (Detrás del cuerpo) */}
      {activeVestimenta === 'CAPA_SUPER_RUPI' && (
        <g className="rupi-hero-cape-back">
          <path
            d="M44 104 C20 114 10 138 6 156 C20 152 36 150 50 148 C44 136 42 122 44 104 Z"
            fill="#31724b"
            stroke="#173e30"
            strokeWidth="3"
            strokeLinejoin="round"
          />
        </g>
      )}

      {/* Cuerpo / torso base de Rupi */}
      <path
        className="rupi-body"
        d="M38 112c-7-20 0-45 18-56 16-10 42-9 58 5 17 15 20 42 11 62-9 19-29 28-52 26-19-2-31-14-35-37Z"
      />

      {/* =================================================================== */}
      {/* CAPA 1: VESTIMENTAS Y TRAJES (Personalizados o por Rol Escolar)     */}
      {/* =================================================================== */}

      {/* 1. Poncho Cusqueño de Fiesta */}
      {activeVestimenta === 'PONCHO_CUSQUENO' && (
        <g className="rupi-poncho-costume">
          <path
            d="M36 106 C30 120 34 135 44 146 C56 151 78 152 98 146 C114 140 125 128 126 116 C118 108 104 103 88 102 C70 100 52 102 36 106 Z"
            fill="url(#rupi-poncho-grad)"
            stroke="#173e30"
            strokeWidth="3"
            strokeLinejoin="round"
          />
          {/* Franja geométrica incaica (grecas amarillas) */}
          <path
            d="M42 124 L48 118 L54 124 L60 118 L66 124 L72 118 L78 124 L84 118 L90 124 L96 118 L102 124 L108 118 L114 124 L120 118"
            stroke="#ffd26a"
            strokeWidth="3"
            fill="none"
            strokeLinecap="round"
          />
          {/* Flecos del poncho al borde inferior */}
          <path
            d="M46 142 L48 148 M56 146 L58 152 M66 147 L68 153 M76 148 L78 154 M86 147 L88 153 M96 145 L98 151 M106 141 L108 147"
            stroke="#ffd26a"
            strokeWidth="2.2"
            strokeLinecap="round"
          />
          {/* Cuello en V abierto con ribete dorado */}
          <polygon points="66,102 75,116 84,102" fill="#ffd26a" stroke="#173e30" strokeWidth="1.5" />
        </g>
      )}

      {/* 2. Capa de Héroe Andino (Frente y broche solar) */}
      {activeVestimenta === 'CAPA_SUPER_RUPI' && (
        <g className="rupi-hero-cape-front">
          <circle cx="68" cy="107" r="5" fill="#ffd26a" stroke="#173e30" strokeWidth="1.5" />
          <circle cx="68" cy="107" r="2" fill="#f95738" />
        </g>
      )}

      {/* 3. Chaleco Escolar (Accesorio o rol reader) */}
      {(activeVestimenta === 'CHALECO_ESCOLAR' || (!activeVestimenta && resolvedRole === 'reader')) && (
        <g className="rupi-reader-costume">
          <path
            className="rupi-reader-sweater"
            d="M40 108 C35 120 38 132 46 142 C56 148 76 150 96 144 C112 138 122 128 123 118 C116 112 104 106 90 104 C72 102 54 104 40 108 Z"
            fill="#246a91"
            stroke="#173e30"
            strokeWidth="3"
            strokeLinejoin="round"
          />
          <path d="M48 138 C68 144 88 143 104 137" stroke="#173e30" strokeWidth="1.8" fill="none" />
          <path d="M56 106 L72 121 L88 106" fill="none" stroke="#ffd26a" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round" />
          <polygon points="58,105 70,116 73,107 64,103" fill="#ffffff" stroke="#173e30" strokeWidth="1.8" strokeLinejoin="round" />
          <polygon points="86,105 74,116 71,107 80,103" fill="#ffffff" stroke="#173e30" strokeWidth="1.8" strokeLinejoin="round" />
          <polygon points="70,116 74,116 73,124 71,124" fill="#f95738" stroke="#173e30" strokeWidth="1.2" />
          <circle cx="53" cy="123" r="3.5" fill="#ffd26a" stroke="#173e30" strokeWidth="1" />
        </g>
      )}

      {/* 4. Bata Blanca de Explorador / Científico (Accesorio o rol scientist) */}
      {(activeVestimenta === 'BATA_CIENTIFICA' || (!activeVestimenta && resolvedRole === 'scientist')) && (
        <g className="rupi-scientist-costume">
          <path
            className="rupi-lab-coat"
            d="M40 108 C35 120 38 132 46 142 C56 148 76 150 96 144 C112 138 122 128 123 118 C116 112 104 106 90 104 C72 102 54 104 40 108 Z"
            fill="#ffffff"
            stroke="#173e30"
            strokeWidth="3"
            strokeLinejoin="round"
          />
          <path d="M52 105 L66 122 L54 125 L45 107 Z" fill="#edf5f0" stroke="#173e30" strokeWidth="2" strokeLinejoin="round" />
          <path d="M92 105 L78 122 L90 125 L99 107 Z" fill="#edf5f0" stroke="#173e30" strokeWidth="2" strokeLinejoin="round" />
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

      {/* Mejilla sonrojada y patitas */}
      <path className="rupi-cheek" d="M67 102q9 8 17 1" />
      <path className="rupi-feet" d="m70 145-3 9m3-4-7 2m43-7 2 9m-2-4 7 2" />

      {/* =================================================================== */}
      {/* CAPA 2: SOMBREROS Y TOCADOS EN CABEZA                               */}
      {/* =================================================================== */}

      {/* 1. Chullo de Alpaca */}
      {activeSombrero === 'CHULLO_ANDINO' && (
        <g className="rupi-chullo-hat">
          <path
            d="M52 68 C50 44 64 28 84 28 C104 28 118 44 116 68 C108 65 95 62 84 62 C73 62 60 65 52 68 Z"
            fill="url(#rupi-chullo-grad)"
            stroke="#173e30"
            strokeWidth="3"
            strokeLinejoin="round"
          />
          <circle cx="84" cy="25" r="6" fill="#ffd26a" stroke="#173e30" strokeWidth="1.8" />
          <polygon points="70,48 74,43 78,48 74,53" fill="#ffd26a" />
          <polygon points="84,48 88,43 92,48 88,53" fill="#ffd26a" />
          <polygon points="98,48 102,43 106,48 102,53" fill="#ffd26a" />
          {/* Orejera izquierda con borla */}
          <path d="M54 66 L50 82 L60 82 Z" fill="#b93b2a" stroke="#173e30" strokeWidth="2" strokeLinejoin="round" />
          <line x1="55" y1="82" x2="55" y2="90" stroke="#ffd26a" strokeWidth="2" strokeLinecap="round" />
          <circle cx="55" cy="91" r="2.5" fill="#ffd26a" />
        </g>
      )}

      {/* 2. Birrete de Graduación */}
      {activeSombrero === 'BIRRETE_ACADEMICO' && (
        <g className="rupi-birrete-hat">
          <path d="M60 58 C60 48 106 48 106 58 Z" fill="#173e30" stroke="#173e30" strokeWidth="2" />
          <polygon points="83,28 128,42 83,56 38,42" fill="#212529" stroke="#173e30" strokeWidth="3" strokeLinejoin="round" />
          <circle cx="83" cy="42" r="3" fill="#ffd26a" stroke="#173e30" strokeWidth="1.2" />
          <path d="M83 42 Q105 48 112 60" fill="none" stroke="#ffd26a" strokeWidth="2.2" strokeLinecap="round" />
          <polygon points="109,60 115,60 114,72 110,72" fill="#ffd26a" stroke="#173e30" strokeWidth="1" />
        </g>
      )}

      {/* 3. Mascapaicha Imperial / Corona del Sol */}
      {activeSombrero === 'CORONA_INTI' && (
        <g className="rupi-corona-hat">
          <path d="M50 64 Q83 54 116 64 L114 54 Q83 45 52 54 Z" fill="#ffd26a" stroke="#173e30" strokeWidth="2.5" strokeLinejoin="round" />
          <polygon points="56,53 62,38 68,52" fill="#ffd26a" stroke="#173e30" strokeWidth="2" strokeLinejoin="round" />
          <polygon points="76,50 83,30 90,50" fill="#ffd26a" stroke="#173e30" strokeWidth="2" strokeLinejoin="round" />
          <polygon points="98,52 104,38 110,53" fill="#ffd26a" stroke="#173e30" strokeWidth="2" strokeLinejoin="round" />
          <circle cx="83" cy="42" r="3" fill="#246a91" stroke="#173e30" strokeWidth="1" />
          <rect x="80" y="52" width="6" height="8" rx="1" fill="#f95738" stroke="#173e30" strokeWidth="1.2" />
        </g>
      )}

      {/* 4. Sombrero de Paja Chalán */}
      {activeSombrero === 'SOMBRERO_CHALAN' && (
        <g className="rupi-chalan-hat">
          <path d="M62 56 L66 40 Q83 38 100 40 L104 56 Z" fill="#f2ebd4" stroke="#173e30" strokeWidth="2.5" strokeLinejoin="round" />
          <path d="M63 54 Q83 50 103 54" stroke="#212529" strokeWidth="3" fill="none" />
          <ellipse cx="83" cy="57" rx="42" ry="12" fill="#f8f4e6" stroke="#173e30" strokeWidth="2.8" />
        </g>
      )}
    </svg>
  )
}
