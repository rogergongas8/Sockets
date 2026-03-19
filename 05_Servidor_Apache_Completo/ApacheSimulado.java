import java.io.*;
import java.net.*;
import java.util.*;
import java.text.SimpleDateFormat;

/**
 * Servidor Apache Simulado Avanzado.
 * Lee configuración, sirve archivos y genera logs.
 */
public class ApacheSimulado {
    private static int port = 80;
    private static String docRoot = "./www";
    private static String logFile = "server.log";
    private static String serverName = "localhost";

    public static void main(String[] args) throws IOException {
        cargarConfiguracion("config.txt");

        // Crear carpeta root si no existe
        File root = new File(docRoot);
        if (!root.exists())
            root.mkdirs();

        ServerSocket server = new ServerSocket(port);
        log("Servidor iniciado en puerto " + port + " (Root: " + docRoot + ")");
        System.out.println("☆ Apache Simulado ejecutándose en http://" + serverName + ":" + port);

        while (true) {
            Socket client = server.accept();
            new Thread(() -> manejarPeticion(client)).start();
        }
    }

    private static void cargarConfiguracion(String path) {
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty() || line.startsWith("#"))
                    continue;
                String[] parts = line.split(" ", 2);
                if (parts.length < 2)
                    continue;

                switch (parts[0]) {
                    case "Listen":
                        port = Integer.parseInt(parts[1]);
                        break;
                    case "DocumentRoot":
                        docRoot = parts[1];
                        break;
                    case "ServerName":
                        serverName = parts[1];
                        break;
                    case "LogFile":
                        logFile = parts[1];
                        break;
                }
            }
        } catch (IOException e) {
            System.out.println("Aviso: No se pudo cargar config.txt, usando valores por defecto.");
        }
    }

    private static void manejarPeticion(Socket client) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                OutputStream out = client.getOutputStream();
                PrintWriter pw = new PrintWriter(out, true)) {

            String requestLine = in.readLine();
            if (requestLine == null)
                return;

            log("Petición: " + requestLine + " de " + client.getInetAddress());

            String[] parts = requestLine.split(" ");
            if (parts.length < 2)
                return;
            String path = parts[1];
            if (path.equals("/"))
                path = "/index.html";

            File file = new File(docRoot, path);
            if (file.exists() && !file.isDirectory()) {
                enviarRespuesta(out, pw, file, "200 OK");
            } else {
                enviarError(pw, "404 Not Found", "El recurso no existe.");
            }

        } catch (IOException e) {
            log("Error manejando petición: " + e.getMessage());
        } finally {
            try {
                client.close();
            } catch (IOException e) {
            }
        }
    }

    private static void enviarRespuesta(OutputStream out, PrintWriter pw, File file, String status) throws IOException {
        pw.println("HTTP/1.1 " + status);
        pw.println("Server: ApacheSimulado/1.0");
        pw.println("Content-Type: text/html");
        pw.println("Content-Length: " + file.length());
        pw.println("Connection: close");
        pw.println(); // Línea en blanco obligatoria
        pw.flush();

        // Enviar contenido del archivo
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file))) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = bis.read(buffer)) > 0) {
                out.write(buffer, 0, count);
            }
        }
        out.flush();
    }

    private static void enviarError(PrintWriter pw, String status, String msg) {
        pw.println("HTTP/1.1 " + status);
        pw.println("Content-Type: text/html");
        pw.println();
        pw.println("<html><body><h1>" + status + "</h1><p>" + msg + "</p></body></html>");
        pw.flush();
    }

    private static synchronized void log(String msg) {
        String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        String entry = "[" + time + "] " + msg;
        System.out.println(entry);
        try (FileWriter fw = new FileWriter(logFile, true);
                PrintWriter pw = new PrintWriter(fw)) {
            pw.println(entry);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
