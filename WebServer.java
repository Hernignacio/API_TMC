import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class WebServer {
    public static void main(String[] args) throws Exception {
        int port = 8000;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/", new StaticHandler());
        server.createContext("/api/mes", new ApiHandler("mes"));
        server.createContext("/api/anio", new ApiHandler("anio"));
        server.createContext("/api/anteriores", new ApiHandler("anteriores"));
        server.createContext("/api/posteriores", new ApiHandler("posteriores"));
        server.createContext("/api/periodo", new ApiHandler("periodo"));

        server.setExecutor(null);
        System.out.println("Servidor web iniciado en http://localhost:" + port);
        server.start();
    }

    static class StaticHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/")) path = "/web/index.html";
            String fsPath = "src" + path.replaceFirst("/", File.separator);
            File f = new File(fsPath);
            if (!f.exists() || f.isDirectory()) {
                byte[] notFound = "404 Not Found".getBytes();
                exchange.sendResponseHeaders(404, notFound.length);
                exchange.getResponseBody().write(notFound);
                exchange.close();
                return;
            }
            Headers h = exchange.getResponseHeaders();
            if (fsPath.endsWith(".html")) h.add("Content-Type", "text/html; charset=utf-8");
            else if (fsPath.endsWith(".css")) h.add("Content-Type", "text/css");
            else h.add("Content-Type", "application/octet-stream");

            byte[] bytes = Files.readAllBytes(Paths.get(fsPath));
            exchange.sendResponseHeaders(200, bytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();
        }
    }

    static class ApiHandler implements HttpHandler {
        private final String resource;
        ApiHandler(String resource) { this.resource = resource; }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            URI uri = exchange.getRequestURI();
            String query = uri.getRawQuery();
            Map<String,String> q = parseQuery(query);

            String formato = q.getOrDefault("formato", "xml").toLowerCase();
            String apikey = q.get("apikey");

            String value = null;
            try {
                switch (resource) {
                    case "mes": {
                        String year = q.get("year");
                        String month = q.get("month");
                        if (year == null || month == null) {
                            sendBadRequest(exchange, "Se requieren parametros year y month para recurso mes");
                            return;
                        }
                        if (month.length() == 1) month = "0" + month;
                        value = year + "-" + month;
                        break;
                    }
                    case "anio": {
                        String year = q.get("year");
                        if (year == null) { sendBadRequest(exchange, "Se requiere parametro year para recurso anio"); return; }
                        value = year;
                        break;
                    }
                    case "anteriores":
                    case "posteriores": {
                        String year = q.get("year");
                        String month = q.get("month");
                        if (year == null || month == null) { sendBadRequest(exchange, "Se requieren parametros year y month"); return; }
                        if (month.length() == 1) month = "0" + month;
                        value = year + "-" + month;
                        break;
                    }
                    case "periodo": {
                        String start = q.get("start");
                        String end = q.get("end");
                        if (start == null || end == null) { sendBadRequest(exchange, "Se requieren start y end para periodo"); return; }
                        value = start + "_" + end;
                        break;
                    }
                }
            } catch (Exception ex) {
                sendBadRequest(exchange, "Parametros invalidos: " + ex.getMessage());
                return;
            }

            String url = TmcClient.buildUrl(resource, value, apikey, formato, null);
            String body = TmcClient.fetchUrl(url);
            if (body == null) {
                sendServerError(exchange, "Error al obtener datos desde la API");
                return;
            }

            Headers h = exchange.getResponseHeaders();
            if (formato.equals("json")) h.add("Content-Type", "application/json; charset=utf-8");
            else h.add("Content-Type", "application/xml; charset=utf-8");

            byte[] out = body.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, out.length);
            exchange.getResponseBody().write(out);
            exchange.close();
        }

        private void sendBadRequest(HttpExchange ex, String msg) throws IOException {
            byte[] b = msg.getBytes();
            ex.sendResponseHeaders(400, b.length);
            ex.getResponseBody().write(b);
            ex.close();
        }

        private void sendServerError(HttpExchange ex, String msg) throws IOException {
            byte[] b = msg.getBytes();
            ex.sendResponseHeaders(502, b.length);
            ex.getResponseBody().write(b);
            ex.close();
        }

        private Map<String,String> parseQuery(String q) {
            Map<String,String> m = new HashMap<>();
            if (q == null || q.isEmpty()) return m;
            String[] parts = q.split("&");
            for (String p: parts) {
                int idx = p.indexOf('=');
                if (idx < 0) continue;
                String k = p.substring(0, idx);
                String v = p.substring(idx+1);
                m.put(k, decode(v));
            }
            return m;
        }

        private String decode(String s) {
            try { return java.net.URLDecoder.decode(s, "UTF-8"); } catch (Exception ex) { return s; }
        }
    }
}

