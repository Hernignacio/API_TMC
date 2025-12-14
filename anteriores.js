const { BASE, encode, proxyUrl } = require('./_lib');
module.exports = async (req, res) => {
  const { year, month, formato = 'xml', apikey } = req.query;
  if (!year || !month) { res.statusCode = 400; res.end('Missing year or month'); return; }
  const m = month.length === 1 ? '0'+month : month;
  const url = `${BASE}/anteriores/${year}/${m}?apikey=${apikey || process.env.TMC_APIKEY}&formato=${encode(formato)}`;
  await proxyUrl(url, res);
};

