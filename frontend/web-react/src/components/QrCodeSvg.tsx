import { useMemo } from 'react'
import { buildQrMatrix } from '../utils/qrCode'

interface QrCodeSvgProps {
  value: string
  size?: number
  label?: string
}

export function QrCodeSvg({ value, size = 196, label = 'Código QR de doble factor' }: QrCodeSvgProps) {
  const { count, pathData } = useMemo(() => {
    const matrix = buildQrMatrix(value)
    const parts: string[] = []
    for (let r = 0; r < matrix.length; r++) {
      for (let c = 0; c < matrix.length; c++) {
        if (matrix[r][c]) {
          parts.push(`M${c},${r}h1v1h-1z`)
        }
      }
    }
    return { count: matrix.length, pathData: parts.join('') }
  }, [value])

  const viewSize = count + 8 // 4 módulos de margen blanco (quiet zone) por lado

  return (
    <svg
      width={size}
      height={size}
      viewBox={`0 0 ${viewSize} ${viewSize}`}
      role="img"
      aria-label={label}
      shapeRendering="crispEdges"
      style={{
        display: 'block',
        background: '#ffffff',
        borderRadius: '14px',
        border: '2px solid #d9d1c2',
        boxShadow: '0 4px 0 #e6dec8',
      }}
    >
      <rect width={viewSize} height={viewSize} fill="#ffffff" />
      <g transform="translate(4, 4)">
        <path d={pathData} fill="#173e30" />
      </g>
    </svg>
  )
}
