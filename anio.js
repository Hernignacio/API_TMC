const { BASE, encode, proxyUrl } = require('./_lib');
module.exports = async (req, res) => {
  const { year, formato = 'xml', apikey } = req.query;
  if (!year) { res.statusCode = 400; res.end('Missing year'); return; }
  const url = `${BASE}/${year}?apikey=${apikey || process.env.TMC_APIKEY}&formato=${encode(formato)}`;
  await proxyUrl(url, res);
};

