import java.io.*;
import java.net.*;

/**
 * Servidor que NO usa sincronización para demostrar Race Conditions.
 * Varios hilos acceden al mismo 'contador' sin protección, lo que 
 * provocará que se entreguen tickets duplicados a diferentes hilos.
 */
public class ServidorTicketsSinSincronizar {
    // Contador simple sin Atomic ni volatile ni synchronized
    private static int contador = 1;
    
    public static void main(String[] args) throws Exception {
        ServerSocket servidor = new ServerSocket(12347);
        System.out.println("!!! Servidor INSEGURO (Sin Sincronización) en puerto 12347");
        
        while (true) {
            Socket conexion = servidor.accept();
            new Thread(new ManejadorTickets(conexion)).start();
        }
    }
    
    static class ManejadorTickets implements Runnable {
        private Socket socket;
        public ManejadorTickets(Socket socket) { this.socket = socket; }
        
        @Override
        public void run() {
            try {
                BufferedReader entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter salida = new PrintWriter(socket.getOutputStream(), true);
                
                // Leer petición
                entrada.readLine();
                
                // SECCIÓN CRÍTICA SIN PROTECCIÓN
                // Simulamos un pequeño delay para aumentar la probabilidad de race condition
                int ticketAsignado = contador;
                try { Thread.sleep(10); } catch (InterruptedException e) {}
                contador = ticketAsignado + 1;
                
                salida.println(ticketAsignado);
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
