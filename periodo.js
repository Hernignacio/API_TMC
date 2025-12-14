const { BASE, encode, proxyUrl } = require('./_lib');
module.exports = async (req, res) => {
  const { start, end, formato = 'xml', apikey } = req.query;
  if (!start || !end) { res.statusCode = 400; res.end('Missing start or end'); return; }
  // start and end expected as YYYY-MM
  const [y1,m1] = start.split('-');
  const [y2,m2] = end.split('-');
  if (!y1 || !m1 || !y2 || !m2) { res.statusCode = 400; res.end('Invalid start/end'); return; }
  const url = `${BASE}/periodo/${y1}/${m1}/${y2}/${m2}?apikey=${apikey || process.env.TMC_APIKEY}&formato=${encode(formato)}`;
  await proxyUrl(url, res);
};

