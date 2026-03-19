import java.io.*;
import java.net.*;

/**
 * Cliente para probar Apache y Nginx.
 */
public class GestionCliente {
    public static void main(String[] args) {
        String host = "localhost";
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        
        System.out.println("Conectando al servidor en puerto " + port + "...");
        
        try (Socket socket = new Socket(host, port);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            
            out.println("Petición de recursos");
            String response = in.readLine();
            System.out.println("Respuesta: " + response);
            
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}
