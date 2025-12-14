import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class Main {
    public static void main(String[] args) throws Exception {
        // Si se ejecuta sin argumentos, arrancamos directamente en modo interactivo 'menu'
        String mode;
        if (args.length < 1) {
            mode = "menu";
        } else {
            mode = args[0].toLowerCase(); // dry-run | call | menu
        }

        if (mode.equals("menu")) {
            // menu puede recibir un mes como argumento para ejecución no interactiva
            String apikey = null;
            String formato = "xml";
            if (args.length >= 2 && args[1].startsWith("--format") == false && args[1].startsWith("--apikey=") == false) {
                // args[1] podría ser el mes YYYY-MM
                String month = args[1];
                for (int i = 2; i < args.length; i++) {
                    String a = args[i];
                    if (a.startsWith("--apikey=")) apikey = a.substring("--apikey=".length());
                    else if (a.startsWith("--formato=")) formato = a.substring("--formato=".length());
                }
                handleMonthQueryInteractive(month, apikey, formato);
                return;
            }

            // Menú numerado interactivo
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            System.out.println("Modo menú interactivo TMC — escribe '6' o 'exit' para salir");
            outer: while (true) {
                System.out.println();
                System.out.println("1) Consultar mes (YYYY-MM)");
                System.out.println("2) Consultar año (YYYY)");
                System.out.println("3) Consultar periodo (YYYY-MM_YYYY-MM)");
                System.out.println("4) Consultar 'anteriores' (YYYY-MM)");
                System.out.println("5) Consultar 'posteriores' (YYYY-MM)");
                System.out.println("6) Salir");
                System.out.print("Seleccione una opción [1-6]: ");
                String sel = reader.readLine();
                if (sel == null) break;
                sel = sel.trim();
                if (sel.equalsIgnoreCase("exit") || sel.equals("6")) break;
                if (sel.isEmpty()) continue;

                // pedimos apikey y formato opcionales (pueden quedar en blanco para usar por defecto)
                System.out.print("(opcional) --apikey (enter para usar la clave por defecto): ");
                String keyInput = reader.readLine();
                if (keyInput != null && keyInput.startsWith("--apikey=")) apikey = keyInput.substring("--apikey=".length());
                else if (keyInput != null && !keyInput.trim().isEmpty()) apikey = keyInput.trim();

                System.out.print("(opcional) formato [xml|json] (enter para xml): ");
                String fmt = reader.readLine();
                if (fmt != null && !fmt.trim().isEmpty()) formato = fmt.trim();

                switch (sel) {
                    case "1": { // mes
                        System.out.print("Ingrese mes (YYYY-MM) o deje vacío para ingresar año y mes por separado: ");
                        String line = reader.readLine();
                        if (line == null) break outer;
                        line = line.trim();
                        String monthValue = null;
                        if (line.isEmpty()) {
                            // pedir año y mes por separado
                            System.out.print("Ingrese año (YYYY): ");
                            String y = reader.readLine();
                            if (y == null) break outer;
                            y = y.trim();
                            System.out.print("Ingrese mes (MM): ");
                            String m = reader.readLine();
                            if (m == null) break outer;
                            m = m.trim();
                            if (!Validator.isValidYear(y) || !Validator.isValidMonth(m)) {
                                System.err.println("Formato inválido. Año o mes inválido. Use YYYY y MM (ej: 2013 y 02)");
                                continue;
                            }
                            monthValue = y + "-" + m;
                        } else {
                            if (!line.matches("^\\d{4}-\\d{2}$") || !Validator.isValidYear(line.split("-")[0]) || !Validator.isValidMonth(line.split("-")[1])) {
                                System.err.println("Formato inválido. Use YYYY-MM (ej: 2013-02)");
                                continue;
                            }
                            monthValue = line;
                        }
                        fetchAndPrint("mes", monthValue, apikey, formato);
                        break;
                    }
                    case "2": { // anio
                        System.out.print("Ingrese año (YYYY): ");
                        String line = reader.readLine();
                        if (line == null) break outer;
                        line = line.trim();
                        if (!Validator.isValidYear(line)) {
                            System.err.println("Formato inválido. Use YYYY (ej: 2013)");
                            continue;
                        }
                        fetchAndPrint("anio", line, apikey, formato);
                        break;
                    }
                    case "3": { // periodo
                        System.out.print("Ingrese periodo inicio_fin (YYYY-MM_YYYY-MM): ");
                        String line = reader.readLine();
                        if (line == null) break outer;
                        line = line.trim();
                        String[] p = line.split("_");
                        if (p.length != 2 || !p[0].matches("^\\d{4}-\\d{2}$") || !p[1].matches("^\\d{4}-\\d{2}$") || !Validator.isValidYear(p[0].split("-")[0]) || !Validator.isValidMonth(p[0].split("-")[1]) || !Validator.isValidYear(p[1].split("-")[0]) || !Validator.isValidMonth(p[1].split("-")[1])) {
                            System.err.println("Formato inválido. Use YYYY-MM_YYYY-MM (ej: 2010-01_2011-01)");
                            continue;
                        }
                        fetchAndPrint("periodo", line, apikey, formato);
                        break;
                    }
                    case "4": { // anteriores
                        System.out.print("Ingrese mes (YYYY-MM) para 'anteriores': ");
                        String line = reader.readLine();
                        if (line == null) break outer;
                        line = line.trim();
                        if (!line.matches("^\\d{4}-\\d{2}$") || !Validator.isValidYear(line.split("-")[0]) || !Validator.isValidMonth(line.split("-")[1])) {
                            System.err.println("Formato inválido. Use YYYY-MM (ej: 2013-02)");
                            continue;
                        }
                        fetchAndPrint("anteriores", line, apikey, formato);
                        break;
                    }
                    case "5": { // posteriores
                        System.out.print("Ingrese mes (YYYY-MM) para 'posteriores': ");
                        String line = reader.readLine();
                        if (line == null) break outer;
                        line = line.trim();
                        if (!line.matches("^\\d{4}-\\d{2}$") || !Validator.isValidYear(line.split("-")[0]) || !Validator.isValidMonth(line.split("-")[1])) {
                            System.err.println("Formato inválido. Use YYYY-MM (ej: 2013-02)");
                            continue;
                        }
                        fetchAndPrint("posteriores", line, apikey, formato);
                        break;
                    }
                    default:
                        System.err.println("Opción inválida. Ingrese 1..6");
                        break;
                }
            }
            System.out.println("Saliendo del menú.");
            return;
        }

        // Mantener lógica anterior para dry-run y call
        if (args.length < 2) {
            printHelp();
            return;
        }

        String resource = args[1].toLowerCase(); // anio | mes | anteriores | posteriores | periodo
        String value = null;

        // si el tercer argumento existe y no es una flag (--...), lo tratamos como valor posicional
        int idx = 2;
        if (args.length >= 3 && !args[2].startsWith("--")) {
            value = args[2];
            idx = 3;
        }

        String apikey = null;
        String formato = null;
        String callback = null;
        String anioFlag = null;
        String mesFlag = null;

        for (int i = idx; i < args.length; i++) {
            String a = args[i];
            if (a.startsWith("--apikey=")) apikey = a.substring("--apikey=".length());
            else if (a.startsWith("--formato=")) formato = a.substring("--formato=".length());
            else if (a.startsWith("--callback=")) callback = a.substring("--callback=".length());
            else if (a.startsWith("--anio=")) anioFlag = a.substring("--anio=".length());
            else if (a.startsWith("--year=")) anioFlag = a.substring("--year=".length());
            else if (a.startsWith("--mes=")) mesFlag = a.substring("--mes=".length());
            else if (a.startsWith("--month=")) mesFlag = a.substring("--month=".length());
        }

        // Si el recurso es 'mes' y no hay valor posicional pero sí vienen --anio y --mes, construir value
        if (resource.equals("mes") && (value == null || value.isEmpty())) {
            if (anioFlag != null && mesFlag != null) {
                value = anioFlag + "-" + (mesFlag.length() == 1 ? "0" + mesFlag : mesFlag);
            }
        }

        if (apikey == null || apikey.isEmpty()) {
            System.err.println("Aviso: no se proporcionó --apikey; se usará la API key por defecto incluida en el código.");
            // no return; TmcClient tiene DEFAULT_APIKEY
        }

        // Validaciones básicas según recurso
        try {
            switch (resource) {
                case "anio": {
                    if (value == null || !Validator.isValidYear(value)) {
                        System.err.println("Error: para 'anio' debe indicar un año en formato YYYY. Ej: 2013");
                        return;
                    }
                    break;
                }
                case "mes":
                case "anteriores":
                case "posteriores": {
                    if (value == null) {
                        System.err.println("Error: para '" + resource + "' debe indicar fecha en formato YYYY-MM o pasar --anio=YYYY --mes=MM");
                        return;
                    }
                    String[] p = value.split("-");
                    if (p.length != 2 || !Validator.isValidYear(p[0]) || !Validator.isValidMonth(p[1])) {
                        System.err.println("Error: para '" + resource + "' debe indicar fecha en formato YYYY-MM. Ej: 2013-08");
                        return;
                    }
                    break;
                }
                case "periodo": {
                    String[] p = value.split("_");
                    if (p.length != 2) {
                        System.err.println("Error: para 'periodo' indique dos fechas separadas por '_' en formato YYYY-MM_YYYY-MM. Ej: 2010-01_2011-01");
                        return;
                    }
                    String[] a1 = p[0].split("-");
                    String[] a2 = p[1].split("-");
                    if (a1.length != 2 || a2.length != 2 || !Validator.isValidYear(a1[0]) || !Validator.isValidMonth(a1[1]) || !Validator.isValidYear(a2[0]) || !Validator.isValidMonth(a2[1])) {
                        System.err.println("Error: formato inválido para 'periodo'. Use YYYY-MM_YYYY-MM");
                        return;
                    }
                    break;
                }
                default:
                    System.err.println("Error: recurso desconocido. Use anio|mes|anteriores|posteriores|periodo");
                    return;
            }
        } catch (Exception ex) {
            System.err.println("Error durante la validación: " + ex.getMessage());
            return;
        }

        String url = TmcClient.buildUrl(resource, value, apikey, formato, callback);

        if (mode.equals("dry-run")) {
            System.out.println("URL construida:");
            System.out.println(url);
            return;
        }

        if (mode.equals("call")) {
            System.out.println("Llamando a: " + url);
            // Si se solicita XML (o no se indicó formato), obtenemos el cuerpo completo y lo parseamos
            if (formato == null || formato.equalsIgnoreCase("xml")) {
                String xmlBody = TmcClient.fetchUrl(url);
                if (xmlBody == null) {
                    System.err.println("Error: no se pudo obtener la respuesta.");
                } else {
                    parseAndPrintRates(xmlBody);
                }
            } else {
                // Para JSON u otros formatos, mostramos la respuesta cruda (truncada para consola)
                TmcClient.callUrl(url);
            }
            return;
        }

        System.err.println("Modo desconocido: use 'dry-run' o 'call' o 'menu'");
        printHelp();
    }

    // Nuevo: helper para evitar duplicación en el menú
    private static void fetchAndPrint(String resource, String value, String apikey, String formato) {
        String url = TmcClient.buildUrl(resource, value, apikey, formato, null);
        System.out.println("Consultando: " + url);
        String xml = TmcClient.fetchUrl(url);
        if (xml == null) {
            System.err.println("Error: no se pudo obtener la respuesta.");
            return;
        }
        parseAndPrintRates(xml);
    }

    private static void handleMonthQueryInteractive(String month, String apikey, String formato) {
        if (month == null || !month.matches("^\\d{4}-\\d{2}$")) {
            System.err.println("Formato inválido. Use YYYY-MM (ej: 2013-02)");
            return;
        }
        if (!Validator.isValidYear(month.split("-")[0]) || !Validator.isValidMonth(month.split("-")[1])) {
            System.err.println("Mes o año inválido. Use YYYY-MM");
            return;
        }
        String url = TmcClient.buildUrl("mes", month, apikey, formato, null);
        System.out.println("Consultando: " + url);
        String xml = TmcClient.fetchUrl(url);
        if (xml == null) {
            System.err.println("Error: no se pudo obtener la respuesta.");
            return;
        }
        parseAndPrintRates(xml);
    }

    private static void parseAndPrintRates(String xml) {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            Document doc = dbf.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));

            // buscar nodos TMC
            NodeList tmcs = doc.getElementsByTagNameNS("http://api.sbif.cl", "TMC");
            if (tmcs == null || tmcs.getLength() == 0) {
                System.out.println("No se encontraron TMC en la respuesta.");
                return;
            }

            System.out.println("Tasas por tramo:");
            for (int i = 0; i < tmcs.getLength(); i++) {
                Node node = tmcs.item(i);
                if (node.getNodeType() != Node.ELEMENT_NODE) continue;
                Element e = (Element) node;
                String titulo = getTextContent(e, "Titulo");
                String subtitulo = getTextContent(e, "SubTitulo");
                String fecha = getTextContent(e, "Fecha");
                String hasta = getTextContent(e, "Hasta");
                String valor = getTextContent(e, "Valor");
                String tipo = getTextContent(e, "Tipo");

                System.out.println("------------------------------");
                System.out.println("Título   : " + (titulo == null ? "(vacío)" : titulo));
                System.out.println("Subtítulo: " + (subtitulo == null ? "(vacío)" : subtitulo));
                System.out.println("Fecha    : " + fecha + (hasta != null && !hasta.isEmpty() ? "  (hasta: " + hasta + ")" : ""));
                System.out.println("Valor    : " + valor);
                System.out.println("Tipo     : " + tipo);
            }
            System.out.println("------------------------------");
        } catch (Exception ex) {
            System.err.println("Error parseando XML: " + ex.getMessage());
        }
    }

    private static String getTextContent(Element parent, String tagLocalName) {
        // intenta con namespace
        NodeList nl = parent.getElementsByTagNameNS("http://api.sbif.cl", tagLocalName);
        if (nl != null && nl.getLength() > 0) return nl.item(0).getTextContent();
        // fallback sin namespace
        nl = parent.getElementsByTagName(tagLocalName);
        if (nl != null && nl.getLength() > 0) return nl.item(0).getTextContent();
        return null;
    }

    private static void printHelp() {
        System.out.println("Cliente simple para la API TMC (Tasa de Interés Máxima Convencional) - uso en español");
        System.out.println("Uso: java -cp out Main <modo> <recurso> <valor> [--apikey=TU_APIKEY] [--formato=json|xml] [--callback=func]");
        System.out.println("  modos: dry-run  => solo imprime la URL construida");
        System.out.println("         call     => realiza la petición HTTP GET y muestra la respuesta");
        System.out.println("         menu     => modo interactivo para consultar meses. Ej: java -cp out Main menu");
        System.out.println("                 También se puede usar: java -cp out Main menu 2013-01 --formato=xml");
        System.out.println("  recurso: anio | mes | anteriores | posteriores | periodo");
        System.out.println("  flags útiles para 'mes': --anio=YYYY  --mes=MM    (alternativa al valor posicional YYYY-MM)");
        System.out.println("  API key: si no pasa --apikey, se usará la variable de entorno TMC_APIKEY si está definida; de lo contrario se usará la clave por defecto incluida en el código.");
        System.out.println("Ejemplo dry-run (año): java -cp out Main dry-run anio 2013 --formato=xml");
        System.out.println("Ejemplo dry-run (mes posicional): java -cp out Main dry-run mes 2013-02 --formato=xml");
        System.out.println("Ejemplo dry-run (mes con flags): java -cp out Main dry-run mes --anio=2013 --mes=2 --formato=xml");
    }
}