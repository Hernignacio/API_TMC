// Helper para los endpoints serverless en Vercel
const BASE = 'https://api.cmfchile.cl/api-sbifv3/recursos_api/tmc';

function encode(s) { return encodeURIComponent(s); }

async function proxyUrl(url, res) {
  try {
    const r = await fetch(url);
    const ct = r.headers.get('content-type') || 'text/plain; charset=utf-8';
    const body = await r.text();
    res.statusCode = r.status;
    res.setHeader('Content-Type', ct);
    res.end(body);
  } catch (err) {
    res.statusCode = 502;
    res.setHeader('Content-Type', 'text/plain; charset=utf-8');
    res.end('Error proxying request: ' + err.message);
  }
}

module.exports = { BASE, encode, proxyUrl };

