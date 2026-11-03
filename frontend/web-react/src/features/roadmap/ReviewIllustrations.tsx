/** Ilustraciones de repaso, decorativas: la acción está en el botón de cada tarjeta. */
export function ExplorerBackpack() {
  return <svg className="review-backpack" viewBox="0 0 160 170" aria-hidden="true" focusable="false">
    <circle cx="80" cy="80" r="66" fill="#fffdf4" stroke="#173e30" strokeWidth="3" />
    <ellipse cx="81" cy="157" rx="48" ry="6" fill="#b3c49b" opacity=".5" />
    <g className="backpack-supplies" stroke="#55492b" strokeWidth="2.5" strokeLinejoin="round">
      <path d="M37 100 Q10 63 26 42 Q37 35 44 49 L58 84" fill="#f95738" />
      <path d="M91 84 Q112 43 123 40 L133 49 L109 99" fill="#f95738" />
      <path d="M30 33 L67 19 L93 82 L51 99 Z" fill="#efdc9e" />
      <ellipse cx="47" cy="28" rx="19" ry="7" transform="rotate(-20 47 28)" fill="#d1c18d" />
      <path d="M36 31 Q45 20 55 25 M45 42 L54 54 L47 64 L66 67 L70 79 M65 35 L72 48" fill="none" stroke="#8f9665" strokeDasharray="4 4" />
      <path d="M111 69 L134 17 L143 22 L120 74 L110 83 Z" fill="#ffd26a" />
      <path d="M134 17 L137 11 Q139 8 143 11 L147 13 L143 22 Z" fill="#ee9d99" />
      <path d="M132 23 L141 28 M110 83 L113 73 L120 74" fill="none" />
    </g>
    <g stroke="#873c20" strokeWidth="3" strokeLinejoin="round">
      <path d="M42 87 Q20 106 22 139 Q26 151 43 141 L51 126" fill="none" stroke="#d95a27" strokeWidth="9" />
      <path d="M39 70 Q76 56 117 70 L129 137 Q128 155 84 158 Q42 159 36 140 Z" fill="#f46a2c" />
      <path d="M40 72 Q77 87 117 70 L115 96 Q78 111 41 96 Z" fill="#ff9a43" />
      <rect x="54" y="111" width="61" height="37" rx="12" fill="#ffd26a" />
      <path d="M55 120 Q85 132 114 120" fill="none" />
      <rect x="79" y="119" width="13" height="15" rx="3" fill="#f3b842" />
      <rect x="30" y="111" width="17" height="30" rx="7" fill="#ffd26a" />
    </g>
    <g fill="#173e30"><ellipse cx="67" cy="92" rx="3.4" ry="5" /><ellipse cx="95" cy="90" rx="3.4" ry="5" /></g>
    <path d="M76 96 Q82 104 88 95" fill="#fffdf4" stroke="#873c20" strokeWidth="2" />
    <g className="review-sparkles" fill="#ffd26a" stroke="#b98628" strokeWidth="2" strokeLinejoin="round">
      <path d="M108 3 L113 13 L124 14 L116 22 L118 33 L108 28 L98 34 L100 22 L92 15 L103 13 Z" />
      <path d="M144 59 L148 67 L156 69 L150 75 L151 84 L144 80 L137 84 L138 75 L132 69 L140 67 Z" />
    </g>
  </svg>
}

export function HappyReviewBook() {
  return <svg className="review-happy-book" viewBox="0 0 160 170" aria-hidden="true" focusable="false">
    <ellipse cx="80" cy="151" rx="52" ry="7" fill="#d9ca9b" opacity=".55" />
    <g className="happy-book-pages" stroke="#55472b" strokeWidth="3" strokeLinejoin="round">
      <path d="M16 45 Q48 34 80 57 Q111 34 144 45 L146 133 Q111 124 80 145 Q48 124 14 133 Z" fill="#31724b" />
      <path d="M22 37 Q52 31 80 51 Q108 31 138 37 L138 125 Q107 117 80 137 Q52 117 22 125 Z" fill="#fff8de" />
      <path d="M80 51 L80 137 M28 118 Q50 114 72 127 M88 127 Q110 114 132 118" fill="none" stroke="#c5b68b" strokeWidth="2" />
      <path d="M40 76 Q47 64 54 76 M105 76 Q112 64 119 76" fill="none" stroke="#55472b" strokeWidth="4" strokeLinecap="round" />
      <path d="M65 94 Q80 90 96 94 Q94 115 80 115 Q66 114 65 94 Z" fill="#873c34" />
      <path d="M70 108 Q81 99 91 108 Q80 119 70 108" fill="#ed8e7c" stroke="none" />
      <ellipse cx="44" cy="91" rx="9" ry="5" fill="#f5b6a1" stroke="none" /><ellipse cx="117" cy="91" rx="9" ry="5" fill="#f5b6a1" stroke="none" />
      <path d="M105 37 L105 58 L111 53 L117 58 L117 35" fill="#f95738" strokeWidth="2" />
    </g>
    <g className="review-sparkles" fill="#ffd26a" stroke="#b98628" strokeWidth="2"><path d="M20 11 L23 20 L32 23 L23 26 L20 35 L17 26 L8 23 L17 20 Z" /><path d="M139 12 L142 20 L150 23 L142 26 L139 34 L136 26 L128 23 L136 20 Z" /></g>
  </svg>
}
