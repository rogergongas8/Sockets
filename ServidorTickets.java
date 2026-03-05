import java.io.*;
import java.net.*;

public class ServidorTickets {
    public static void main(String[] args) throws Exception {
        ServerSocket servidor = new ServerSocket(12346);
        System.out.println("Servidor de tickets activo en puerto 12346...");
        int ticket = 1;

        while (true) {
            Socket conexion = servidor.accept();
            System.out.println("Alguien se conectó. Esperando su petición...");

            // El servidor espera leer algo. Aquí Telnet lo dejará atascado.
            BufferedReader entrada = new BufferedReader(new InputStreamReader(conexion.getInputStream()));
            entrada.readLine(); 

            PrintWriter salida = new PrintWriter(conexion.getOutputStream(), true);
            salida.println("Tu ticket es el #" + ticket++);
            
            conexion.close();
            System.out.println("Ticket entregado. Servidor libre de nuevo.");
        }
    }
}