import java.io.*;
import java.net.*;

/**
 * Servidor que usa 'synchronized' para evitar Race Conditions.
 */
public class ServidorTicketsSynchronized {
    private static int contador = 1;
    
    // Método sincronizado para garantizar exclusión mutua
    private synchronized static int getNextTicket() {
        return contador++;
    }
    
    public static void main(String[] args) throws Exception {
        ServerSocket servidor = new ServerSocket(12348);
        System.out.println("✓ Servidor SEGURO (Synchronized) en puerto 12348");
        
        while (true) {
            Socket conexion = servidor.accept();
            new Thread(() -> {
                try (BufferedReader entrada = new BufferedReader(new InputStreamReader(conexion.getInputStream()));
                     PrintWriter salida = new PrintWriter(conexion.getOutputStream(), true)) {
                    
                    entrada.readLine();
                    int ticket = getNextTicket();
                    salida.println(ticket);
                    conexion.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }
}
