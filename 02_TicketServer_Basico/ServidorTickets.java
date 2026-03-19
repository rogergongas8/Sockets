import java.io.*;
import java.net.*;

/**
 * SOLUTION 1: Basic Ticket Server (Single Connection)
 * This server handles one client at a time in the main thread.
 * Use 'telnet localhost 12346' to occupy the connection and 
 * see how other clients are blocked.
 */
public class ServidorTickets {
    public static void main(String[] args) throws Exception {
        ServerSocket servidor = new ServerSocket(12346);
        System.out.println("☆ Servidor de TICKETS (Solución 1: SECUENCIAL) activo en puerto 12346");

        try {
            int ticket = 1;
            while (true) {
                Socket conexion = servidor.accept();
                System.out.println("\n[NUEVA CONEXION] de " + conexion.getInetAddress());
                
                // Manejar el cliente en el hilo principal (BLOQUEANTE)
                BufferedReader entrada = new BufferedReader(new InputStreamReader(conexion.getInputStream()));
                PrintWriter salida = new PrintWriter(conexion.getOutputStream(), true);
                
                // Esperar a que el cliente envíe una petición (Telnet lo dejará aquí)
                String peticion = entrada.readLine();
                if (peticion != null) {
                    System.out.println("   Recibido: " + peticion);
                    salida.println("Tu ticket es el #" + ticket++);
                    System.out.println("   Ticket entregado. Servidor libre.");
                }
                
                conexion.close();
            }
        } finally {
            servidor.close();
        }
    }
}