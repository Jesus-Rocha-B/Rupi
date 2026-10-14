import { GraduationCap, Sparkles } from 'lucide-react'
import './StudentAvatar.css'

interface StudentAvatarProps {
  name: string
  role?: 'ESTUDIANTE' | 'DOCENTE'
  size?: 'sm' | 'md' | 'lg'
  showBadge?: boolean
  className?: string
}

export function StudentAvatar({
  name,
  role = 'ESTUDIANTE',
  size = 'md',
  showBadge = false,
  className = '',
}: StudentAvatarProps) {
  const isTeacher = role === 'DOCENTE'

  // Paleta de colores cálidos andinos según la inicial del nombre para darle personalidad individual
  const colorVariants = [
    { bg: '#ffd26a', accent: '#f95738', cap: '#173e30' }, // Sol / Naranja / Verde
    { bg: '#ffb703', accent: '#246a91', cap: '#31724b' }, // Maíz / Azul / Hoja
    { bg: '#f4a261', accent: '#e76f51', cap: '#264653' }, // Arcilla / Coral / Petróleo
    { bg: '#a7c957', accent: '#386641', cap: '#bc4749' }, // Valle / Bosque / Retablo
    { bg: '#90e0ef', accent: '#0077b6', cap: '#03045e' }, // Cielo / Lago / Noche
  ]

  const charCode = (name || 'E').charCodeAt(0) + (name || 'E').length
  const variant = colorVariants[charCode % colorVariants.length]

  return (
    <div
      className={`student-avatar-root size-${size} ${isTeacher ? 'is-teacher' : ''} ${className}`}
      aria-label={`Avatar de ${name} (${isTeacher ? 'Docente' : 'Estudiante'})`}
      role="img"
    >
      <div
        className="student-avatar-frame"
        style={{
          backgroundColor: variant.bg,
          borderColor: isTeacher ? '#173e30' : variant.cap,
        }}
      >
        <svg
          viewBox="0 0 100 100"
          className="student-avatar-svg"
          xmlns="http://www.w3.org/2000/svg"
          aria-hidden="true"
        >
          {/* Fondo circular interno con textura cálida */}
          <circle cx="50" cy="50" r="48" fill={variant.bg} />

          {/* Cuerpo / Uniforme escolar o poncho andino */}
          <path
            d="M20 92 C20 70, 32 66, 50 66 C68 66, 80 70, 80 92 Z"
            fill={isTeacher ? '#173e30' : variant.accent}
          />
          {/* Cuello o pañuelo escolar */}
          <path
            d="M42 66 L50 78 L58 66 Z"
            fill="#ffffff"
            stroke={variant.cap}
            strokeWidth="2"
          />

          {/* Cabeza / Rostro empático */}
          <ellipse cx="50" cy="46" rx="22" ry="24" fill="#fed0bb" />

          {/* Ojos curiosos y vivaces */}
          <circle cx="42" cy="45" r="3.2" fill="#2b1810" />
          <circle cx="58" cy="45" r="3.2" fill="#2b1810" />
          {/* Brillos en los ojos */}
          <circle cx="43" cy="43.8" r="1.1" fill="#ffffff" />
          <circle cx="59" cy="43.8" r="1.1" fill="#ffffff" />

          {/* Mejillas sonrientes (rubor escolar andino) */}
          <ellipse cx="37" cy="49" rx="3.5" ry="2.2" fill="#f28482" opacity="0.6" />
          <ellipse cx="63" cy="49" rx="3.5" ry="2.2" fill="#f28482" opacity="0.6" />

          {/* Sonrisa amable */}
          <path
            d="M44 54 Q50 60 56 54"
            fill="none"
            stroke="#2b1810"
            strokeWidth="2.5"
            strokeLinecap="round"
          />

          {/* Cabello o accesorio según rol */}
          {isTeacher ? (
            /* Birrete de maestro con borla dorada */
            <g>
              <polygon points="50,15 84,27 50,39 16,27" fill="#173e30" />
              <rect x="36" y="27" width="28" height="10" rx="3" fill="#112d23" />
              <line x1="50" y1="27" x2="78" y2="38" stroke="#ffd26a" strokeWidth="2.5" />
              <circle cx="78" cy="40" r="3" fill="#ffd26a" />
            </g>
          ) : (
            /* Gorro explorador escolar andino / chullo con orejeras */
            <g>
              {/* Gorro explorador */}
              <path
                d="M26 40 C26 20, 74 20, 74 40 Z"
                fill={variant.cap}
              />
              {/* Banda decorativa con patrón andino */}
              <rect x="26" y="36" width="48" height="6" fill="#ffd26a" rx="1" />
              {/* Pluma del Gallito de las Rocas como emblema de explorador */}
              <path
                d="M62 26 C68 12, 75 14, 73 28 Z"
                fill="#f95738"
              />
            </g>
          )}
        </svg>
      </div>

      {/* Insignia pequeña en la esquina si aplica */}
      {showBadge && (
        <span
          className={`avatar-badge-corner ${isTeacher ? 'teacher-badge' : 'student-badge'}`}
          title={isTeacher ? 'Docente de Primaria' : 'Explorador Escolar'}
        >
          {isTeacher ? <GraduationCap size={12} /> : <Sparkles size={11} />}
        </span>
      )}
    </div>
  )
}
