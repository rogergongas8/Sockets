import java.io.*;
import java.net.*;

/**
 * Simulación de Apache: Crea un hilo por cada conexión recibida.
 */
public class ServidorGestionApache {
    public static void main(String[] args) throws IOException {
        ServerSocket server = new ServerSocket(8082);
        System.out.println("--- SIMULACIÓN APACHE (Multi-thread) ---");
        System.out.println("Escuchando en puerto 8082...");
        
        while (true) {
            Socket socket = server.accept();
            System.out.println("[Apache] Nueva petición recibida. Creando HILO dedicado...");
            
            new Thread(() -> {
                try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                     PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
                    
                    String line = in.readLine();
                    Thread.sleep(1000); // Simulamos trabajo pesado
                    out.println("Apache respondió a '" + line + "' usando el hilo: " + Thread.currentThread().getName());
                    
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }
}
