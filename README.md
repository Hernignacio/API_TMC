# Desplegar TMC en Vercel

Este proyecto contiene una web simple para consultar la API TMC de la CMF y una capa proxy serverless para exponer endpoints que la UI consume.

Estructura relevante:
- `/api/*.js` - funciones serverless (Vercel) que llaman a la API de la CMF
- `/public/index.html` - página estática simple (puedes reemplazarla por la UI completa)
- `vercel.json` - configuración de despliegue

Requisitos
- Tener la CLI de Vercel instalada (`npm i -g vercel`) y estar autenticado (`vercel login`).
- Definir la variable de entorno `TMC_APIKEY` en tu proyecto Vercel (preferible) o pasar `apikey` en la query.

Despliegue (pasos):
1) Iniciar sesión y linkear tu proyecto (o usar `vercel --prod`):
   vercel login
   vercel

2) Configurar variable de entorno en Vercel:
   En el dashboard > Settings > Environment Variables, añade `TMC_APIKEY` con tu clave.

3) Deploy:
   vercel --prod

Uso
- URLs:
  - /api/mes?year=2013&month=02&formato=xml
  - /api/anio?year=2013&formato=xml
  - /api/periodo?start=2010-01&end=2011-01&formato=xml
  - /api/anteriores?year=2013&month=02&formato=xml
  - /api/posteriores?year=2013&month=02&formato=xml

Notas de seguridad
- No almacenes la clave en el código fuente para producción. Usa variables de entorno en Vercel.
- El proxy reenvía la respuesta de la CMF tal cual. Si quieres filtrar o normalizar la salida (por ejemplo extraer sólo `TMC`), lo puedo implementar.

