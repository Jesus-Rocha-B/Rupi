# Ficha de Tokens de Diseño · RUPI

Variables y valores de diseño para mantener la coherencia artesanal en todo el frontend de RUPI.

```css
:root {
  /* Identidad Cromática RUPI */
  --rupi-primary-dark: #173e30;     /* Bosque nuboso profundo */
  --rupi-primary: #31724b;          /* Verde hoja andina */
  --rupi-primary-light: #eef5e8;    /* Prado claro (fondos de tarjeta) */
  --rupi-primary-border: #dbe8d2;   /* Borde natural de prado */

  --rupi-accent-orange: #f95738;    /* Naranja pluma de gallito */
  --rupi-accent-orange-dark: #cc3a1d; /* Sombra sólida para botones */
  
  --rupi-sun-yellow: #ffd26a;       /* Sol andino / Maíz */
  --rupi-sun-yellow-dark: #dca331;  /* Borde de moneda y camino */

  --rupi-sky-blue: #246a91;         /* Cielo serrano despejado */
  --rupi-sky-blue-dark: #154562;    /* Sombra parada disponible */

  --rupi-earth-ink: #1a2e22;        /* Texto principal de alto contraste */
  --rupi-earth-muted: #43614c;      /* Texto secundario y descriptivo */
  --rupi-earth-paper: #faf8f5;      /* Fondo general apergaminado */

  /* Tipografía */
  --font-family-display: 'Nunito', system-ui, -apple-system, sans-serif;
  --font-family-body: 'Nunito', system-ui, -apple-system, sans-serif;

  /* Elevaciones Físicas (Sombras táctiles sólidas) */
  --shadow-tactile-sm: 0 3px 0 rgba(23, 62, 48, 0.15);
  --shadow-tactile-md: 0 5px 0 var(--rupi-accent-orange-dark);
  --shadow-tactile-node: 0 5px 0 #154562;

  /* Radios de curvatura orgánica */
  --radius-sm: 10px;
  --radius-md: 16px;
  --radius-lg: 24px;
  --radius-full: 9999px;
}
```

## Reglas de Uso en Componentes
1. **Nunca usar colores hexadecimales quemados directamente en los componentes sin pasar por tokens.**
2. **Las sombras no deben ser oscuras ni difusas en exceso:** priorizar sombras de 0 a 6px de desplazamiento con opacidad baja o con un color de sombra específico (ej. el naranja usa sombra naranja oscura).
3. **El contraste mínimo de texto frente al fondo debe ser 4.5:1** para texto normal y **3:1** para texto grande/interactivo en cumplimiento estricto con los criterios de accesibilidad.
