import java.io.*;
import java.net.*;
import java.util.*;
import java.nio.file.Files;
import java.util.concurrent.*;

public class DashboardServer {
    private static final int DASHBOARD_PORT = 9000;
    private static final Map<String, StringBuilder> processLogs = new ConcurrentHashMap<>();
    private static final Map<Integer, ProxyConnection> activeSockets = new ConcurrentHashMap<>();
    private static final Map<String, Process> startedProcesses = new ConcurrentHashMap<>();
    private static int socketCounter = 1;

    public static void main(String[] args) throws IOException {
        ServerSocket server = new ServerSocket(DASHBOARD_PORT);
        System.out.println("🚀 [DASHBOARD v7.0] ONLINE en http://localhost:" + DASHBOARD_PORT);
        
        while (true) {
            Socket client = server.accept();
            new Thread(() -> manejarPeticion(client)).start();
        }
    }

    private static void manejarPeticion(Socket client) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream(), "UTF-8"));
             PrintWriter pw = new PrintWriter(new OutputStreamWriter(client.getOutputStream(), "UTF-8"), true)) {
            
            String firstLine = in.readLine();
            if (firstLine == null) return;
            String[] requestParts = firstLine.split(" ");
            if (requestParts.length < 2) return;
            String path = requestParts[1];
            
            if (path.equals("/") || path.equals("/index.html")) {
                enviarArchivo("www/index.html", pw, client.getOutputStream());
            } else if (path.startsWith("/api/launcher/stopall")) {
                detenerTodo(pw);
            } else if (path.startsWith("/api/launcher")) {
                String cmd = getParam(path, "cmd");
                ejecutarProceso(cmd, pw);
            } else if (path.startsWith("/api/process/read")) {
                String cmd = getParam(path, "cmd");
                leerLogsProceso(cmd, pw);
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
                // This endpoint is now deprecated in favor of /api/process/read
                pw.println("HTTP/1.1 200 OK\r\n\r\nActive (deprecated)");
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
        String dir = null;
        String javaCmd = switch(cmd) {
            case "chat" -> { dir = "../01_Teoria_y_Chat"; yield "java -Dfile.encoding=UTF-8 -cp . ServidorChat"; }
            case "chat_client" -> { dir = "../01_Teoria_y_Chat"; yield "java -Dfile.encoding=UTF-8 -cp . ClienteChat"; }
            case "t_blocking" -> { dir = "../02_TicketServer_Basico"; yield "java -Dfile.encoding=UTF-8 -cp . ServidorTickets"; }
            case "t_concurrent" -> { dir = "../02_TicketServer_Basico"; yield "java -Dfile.encoding=UTF-8 -cp . ServidorTicketsConcurrencia"; }
            case "t_pool" -> { dir = "../02_TicketServer_Basico"; yield "java -Dfile.encoding=UTF-8 -cp . ServidorTicketsPool"; }
            case "sync" -> { dir = "../03_Sincronizacion_y_Atomics"; yield "java -Dfile.encoding=UTF-8 -cp . ServidorTicketsSinSincronizar"; }
            case "sync_client" -> { dir = "../03_Sincronizacion_y_Atomics"; yield "java -Dfile.encoding=UTF-8 -cp . ClienteTicketsDuplicados"; }
            case "sync_ok" -> { dir = "../03_Sincronizacion_y_Atomics"; yield "java -Dfile.encoding=UTF-8 -cp . ServidorTicketsSynchronized"; }
            case "atomic" -> { dir = "../03_Sincronizacion_y_Atomics"; yield "java -Dfile.encoding=UTF-8 -cp . ServidorTicketsAtomic"; }
            case "apache_vs" -> { dir = "../04_Apache_vs_Nginx"; yield "java -Dfile.encoding=UTF-8 -cp . ServidorGestionApache"; }
            case "nginx" -> { dir = "../04_Apache_vs_Nginx"; yield "java -Dfile.encoding=UTF-8 -cp . ServidorGestionNginx"; }
            case "apache" -> { dir = "../05_Servidor_Apache_Completo"; yield "java -Dfile.encoding=UTF-8 -cp . ApacheSimulado"; }
            default -> null;
        };

        if (javaCmd != null) {
            final String fCmd = javaCmd;
            final String fDir = dir;
            
            // Si ya existe este proceso, lo matamos antes de lanzar otro para evitar "Port already in use"
            Process existing = startedProcesses.get(cmd);
            if (existing != null && existing.isAlive()) {
                existing.destroyForcibly();
                try { Thread.sleep(200); } catch (InterruptedException e) {}
            }

            processLogs.put(cmd, new StringBuilder("🚀 Iniciando [" + cmd + "]...\n"));
            new Thread(() -> {
                try {
                    System.out.println("Lanzando en " + fDir + ": " + fCmd);
                    ProcessBuilder pb = new ProcessBuilder(fCmd.split(" "));
                    if (fDir != null) pb.directory(new File(fDir));
                    pb.redirectErrorStream(true);
                    Process p = pb.start();
                    startedProcesses.put(cmd, p);
                    
                    BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), "UTF-8"));
                    String l;
                    while ((l = r.readLine()) != null) {
                        StringBuilder sb = processLogs.get(cmd);
                        if (sb != null) synchronized(sb) { sb.append(l).append("\n"); }
                    }
                } catch (Exception e) { 
                    e.printStackTrace();
                    StringBuilder sb = processLogs.get(cmd);
                    if (sb != null) synchronized(sb) { sb.append("❌ ERROR: ").append(e.getMessage()).append("\n"); }
                }
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
            pw.print("HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nAccess-Control-Allow-Origin: *\r\n\r\n" + data);
            pw.flush();
        } else { pw.println("HTTP/1.1 404 Not Found\r\nContent-Type: text/plain\r\n\r\nNot Found"); }
    }

    private static void leerLogsProceso(String cmd, PrintWriter pw) {
        StringBuilder sb = processLogs.get(cmd);
        String data = "";
        if (sb != null) {
            synchronized(sb) {
                data = sb.toString();
                sb.setLength(0);
            }
        }
        pw.print("HTTP/1.1 200 OK\r\nContent-Type: text/plain; charset=UTF-8\r\nAccess-Control-Allow-Origin: *\r\n\r\n" + data);
        pw.flush();
    }

    private static void detenerTodo(PrintWriter pw) {
        for (Process p : startedProcesses.values()) { p.destroyForcibly(); }
        startedProcesses.clear();
        processLogs.clear();
        pw.println("HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nAccess-Control-Allow-Origin: *\r\n\r\nStopped");
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
