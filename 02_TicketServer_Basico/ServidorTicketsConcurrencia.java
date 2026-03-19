import java.io.*;
import java.net.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ServidorTicketsConcurrencia {
    private static AtomicInteger ticket = new AtomicInteger(1);
    
    public static void main(String[] args) throws Exception {
        ServerSocket servidor = new ServerSocket(12346);
        System.out.println("Servidor de tickets con concurrencia activo en puerto 12346...");

        while (true) {
            Socket conexion = servidor.accept();
            System.out.println("Alguien se conectó desde: " + conexion.getInetAddress());
            
            // Crear un Thread para manejar cada cliente
            new Thread(new ManejadorTickets(conexion)).start();
        }
    }
    
    // Clase interna para manejar cada cliente en un Thread separado
    static class ManejadorTickets implements Runnable {
        private Socket socket;
        
        public ManejadorTickets(Socket socket) {
            this.socket = socket;
        }
        
        @Override
        public void run() {
            try {
                BufferedReader entrada = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));
                PrintWriter salida = new PrintWriter(
                    socket.getOutputStream(), true);
                
                // Esperar petición del cliente
                String solicitud = entrada.readLine();
                System.out.println("Cliente solicita: " + solicitud);
                
                // Entregar ticket de forma thread-safe
                int numeroTicket = ticket.getAndIncrement();
                salida.println("Tu ticket es el #" + numeroTicket);
                
                System.out.println("Ticket #" + numeroTicket + " entregado a: " + 
                    socket.getInetAddress());
                
                socket.close();
            } catch (IOException e) {
                System.out.println("Error con cliente: " + e.getMessage());
            }
        }
    }
}
