import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class TmcClient {
    private static final String BASE = "https://api.cmfchile.cl/api-sbifv3/recursos_api/tmc";
    // API key incluida según solicitud del usuario. Considera usar variables de entorno para mayor seguridad.
    private static final String DEFAULT_APIKEY = "7d37e5ea3b6789fe8220aaa2a8cb9d71cdc5838e";

    public static String buildUrl(String resource, String value, String apikey, String formato, String callback) {
        // Prioridad en la siguiente orden: argumento apikey -> variable de entorno TMC_APIKEY -> DEFAULT_APIKEY
        if (apikey == null || apikey.isEmpty()) {
            String envKey = System.getenv("TMC_APIKEY");
            if (envKey != null && !envKey.isEmpty()) {
                apikey = envKey;
            } else {
                apikey = DEFAULT_APIKEY;
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append(BASE);
        switch (resource) {
            case "anio":
                sb.append("/").append(value);
                break;
            case "mes":
                String[] p = value.split("-");
                sb.append("/").append(p[0]).append("/").append(p[1]);
                break;
            case "anteriores":
                String[] pa = value.split("-");
                sb.append("/anteriores/").append(pa[0]).append("/").append(pa[1]);
                break;
            case "posteriores":
                String[] pp = value.split("-");
                sb.append("/posteriores/").append(pp[0]).append("/").append(pp[1]);
                break;
            case "periodo":
                String[] pr = value.split("_");
                String[] a1 = pr[0].split("-");
                String[] a2 = pr[1].split("-");
                sb.append("/periodo/").append(a1[0]).append("/").append(a1[1]).append("/").append(a2[0]).append("/").append(a2[1]);
                break;
            default:
                throw new IllegalArgumentException("Recurso desconocido: " + resource);
        }

        // Parámetros GET
        String sep = "?";
        if (apikey != null && !apikey.isEmpty()) {
            sb.append(sep).append("apikey=").append(encode(apikey));
            sep = "&";
        }
        if (formato != null && !formato.isEmpty()) {
            // normalizar a minúsculas para que JSON/JSON/XML también funcionen
            String fmt = formato.trim().toLowerCase();
            sb.append(sep).append("formato=").append(encode(fmt));
            sep = "&";
        }
        if (callback != null && !callback.isEmpty()) {
            sb.append(sep).append("callback=").append(encode(callback));
        }

        return sb.toString();
    }

    // Nuevo: devuelve el cuerpo completo de la URL (sin truncar). Retorna null en error.
    public static String fetchUrl(String urlStr) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            int code = conn.getResponseCode();
            InputStream is = (code >= 200 && code < 400) ? conn.getInputStream() : conn.getErrorStream();
            if (is == null) return null;

            BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            StringBuilder body = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                body.append(line).append('\n');
            }
            br.close();
            return body.toString();
        } catch (Exception ex) {
            System.err.println("Error al obtener URL: " + ex.getMessage());
            return null;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private static String encode(String s) {
        try {
            return URLEncoder.encode(s, StandardCharsets.UTF_8.name());
        } catch (Exception ex) {
            return s;
        }
    }

    public static void callUrl(String urlStr) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            int code = conn.getResponseCode();
            System.out.println("HTTP/1.x " + code + " " + conn.getResponseMessage());

            InputStream is = (code >= 200 && code < 400) ? conn.getInputStream() : conn.getErrorStream();
            if (is == null) {
                System.out.println("(sin cuerpo de respuesta)");
                return;
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            StringBuilder body = new StringBuilder();
            String line;
            int total = 0;
            int limit = 8 * 1024; // 8 KB
            while ((line = br.readLine()) != null && total < limit) {
                body.append(line).append('\n');
                total += line.length();
            }
            br.close();

            System.out.println("----- CUERPO (hasta " + limit + " bytes) -----");
            System.out.println(body.toString());
            if (total >= limit) {
                System.out.println("...respuesta truncada (demasiado larga)");
            }
        } catch (Exception ex) {
            System.err.println("Error al llamar URL: " + ex.getMessage());
        } finally {
            if (conn != null) conn.disconnect();
        }
    }
}
