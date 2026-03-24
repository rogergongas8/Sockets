import java.io.*;
import java.net.*;
import java.util.*;
import java.nio.file.Files;
import java.util.concurrent.*;

public class DashboardServer {
    private static final int DASHBOARD_PORT = 9000;
    private static final Map<Integer, ProxyConnection> activeSockets = new ConcurrentHashMap<>();
    private static int socketCounter = 1;

    public static void main(String[] args) throws IOException {
        ServerSocket server = new ServerSocket(DASHBOARD_PORT);
        System.out.println("🚀 [DASHBOARD] ACTIVO EN http://localhost:" + DASHBOARD_PORT);
        
        while (true) {
            Socket client = server.accept();
            new Thread(() -> manejarPeticion(client)).start();
        }
    }

    private static void manejarPeticion(Socket client) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
             PrintWriter pw = new PrintWriter(client.getOutputStream(), true)) {
            
            String firstLine = in.readLine();
            if (firstLine == null) return;
            String path = firstLine.split(" ")[1];
            
            if (path.equals("/") || path.equals("/index.html")) {
                enviarArchivo("www/index.html", pw, client.getOutputStream());
            } else if (path.startsWith("/api/launcher")) {
                String cmd = getParam(path, "cmd");
                ejecutarProceso(cmd, pw);
            } else if (path.startsWith("/api/socket/connect")) {
                int port = Integer.parseInt(getParam(path, "port"));
                conectarSocket(port, pw);
            } else if (path.startsWith("/api/socket/send")) {
                int id = Integer.parseInt(getParam(path, "id"));
                String msg = getParam(path, "msg");
                enviarASocket(id, URLDecoder.decode(msg, "UTF-8"), pw);
            } else if (path.startsWith("/api/socket/read")) {
                int id = Integer.parseInt(getParam(path, "id"));
                leerDeSocket(id, pw);
            } else if (path.equals("/api/logs")) {
                enviarLogs(pw);
            } else {
                pw.println("HTTP/1.1 404 Not Found\r\nContent-Type: text/plain\r\n\r\nNot Found");
            }
        } catch (Exception e) { 
            e.printStackTrace();
            try {
                PrintWriter pw = new PrintWriter(client.getOutputStream(), true);
                pw.println("HTTP/1.1 500 Internal Server Error\r\nContent-Type: text/plain\r\n\r\n" + e.getMessage());
            } catch (Exception ex) {}
        } finally {
            try { client.close(); } catch (IOException e) {}
        }
    }

    private static void enviarArchivo(String file, PrintWriter pw, OutputStream out) throws IOException {
        File f = new File(file);
        if (!f.exists()) { pw.println("HTTP/1.1 404 Not Found\r\nContent-Type: text/plain\r\n\r\nNot Found"); return; }
        pw.println("HTTP/1.1 200 OK\r\nContent-Type: text/html; charset=UTF-8\r\nAccess-Control-Allow-Origin: *\r\n\r\n");
        pw.flush();
        Files.copy(f.toPath(), out);
    }

    private static String getParam(String path, String key) {
        if (!path.contains("?")) return "";
        String[] parts = path.split("\\?")[1].split("&");
        for (String p : parts) {
            String[] kv = p.split("=");
            if (kv[0].equals(key)) return kv.length > 1 ? kv[1] : "";
        }
        return "";
    }

    private static void ejecutarProceso(String cmd, PrintWriter pw) {
        String javaCmd = switch(cmd) {
            case "chat" -> "java -cp ../01_Teoria_y_Chat ServidorChat";
            case "t1" -> "java -cp ../02_TicketServer_Basico ServidorTickets";
            case "sync" -> "java -cp ../03_Sincronizacion_y_Atomics ServidorTicketsSinSincronizar";
            case "sync_ok" -> "java -cp ../03_Sincronizacion_y_Atomics ServidorTicketsSynchronized";
            case "atomic" -> "java -cp ../03_Sincronizacion_y_Atomics ServidorTicketsAtomic";
            case "apache" -> "java -cp ../05_Servidor_Apache_Completo ApacheSimulado";
            default -> null;
        };
        if (javaCmd != null) {
            new Thread(() -> {
                try {
                    System.out.println("Lanzando: " + javaCmd);
                    Process p = Runtime.getRuntime().exec(javaCmd);
                    // Los logs de consola van al stdout del dashboard
                    BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
                    String l; while ((l = r.readLine()) != null) System.out.println("["+cmd+"] " + l);
                } catch (Exception e) {}
            }).start();
        }
        pw.println("HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nAccess-Control-Allow-Origin: *\r\n\r\nLanzado");
    }

    private static void conectarSocket(int port, PrintWriter pw) {
        try {
            ProxyConnection conn = new ProxyConnection(port);
            int id = socketCounter++;
            activeSockets.put(id, conn);
            pw.println("HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nAccess-Control-Allow-Origin: *\r\n\r\n" + id);
        } catch (IOException e) {
            pw.println("HTTP/1.1 500 Internal Server Error\r\nContent-Type: text/plain\r\nAccess-Control-Allow-Origin: *\r\n\r\n" + e.getMessage());
        }
    }

    private static void enviarASocket(int id, String msg, PrintWriter pw) {
        ProxyConnection conn = activeSockets.get(id);
        if (conn != null) {
            conn.out.println(msg);
            pw.println("HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nAccess-Control-Allow-Origin: *\r\n\r\nSent");
        } else { pw.println("HTTP/1.1 404 Not Found\r\nContent-Type: text/plain\r\nAccess-Control-Allow-Origin: *\r\n\r\nNot Found"); }
    }

    private static void leerDeSocket(int id, PrintWriter pw) {
        ProxyConnection conn = activeSockets.get(id);
        if (conn != null) {
            String data = conn.readAll();
            pw.println("HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nAccess-Control-Allow-Origin: *\r\n\r\n" + data);
        } else { pw.println("HTTP/1.1 404 Not Found\r\nContent-Type: text/plain\r\n\r\nNot Found"); }
    }

    private static void enviarLogs(PrintWriter pw) {
        // En este V3 los logs irán directo al poll de cada terminal
        pw.println("HTTP/1.1 200 OK\r\n\r\nActive");
    }

    static class ProxyConnection {
        Socket s;
        BufferedReader in;
        PrintWriter out;
        StringBuilder buffer = new StringBuilder();

        ProxyConnection(int port) throws IOException {
            s = new Socket("localhost", port);
            in = new BufferedReader(new InputStreamReader(s.getInputStream()));
            out = new PrintWriter(s.getOutputStream(), true);
            new Thread(() -> {
                try {
                    String line;
                    while ((line = in.readLine()) != null) {
                        synchronized(buffer) { buffer.append(line).append("\n"); }
                    }
                } catch (IOException e) {}
            }).start();
        }

        String readAll() {
            synchronized(buffer) {
                String tmp = buffer.toString();
                buffer.setLength(0);
                return tmp;
            }
        }
    }
}
