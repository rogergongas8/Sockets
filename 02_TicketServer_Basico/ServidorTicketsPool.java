import java.io.*;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class ServidorTicketsPool {
    private static AtomicInteger ticket = new AtomicInteger(1);
    
    public static void main(String[] args) throws Exception {
        // Crear un pool de 10 hilos reutilizables
        ExecutorService pool = Executors.newFixedThreadPool(10);
        
        ServerSocket servidor = new ServerSocket(12346);
        System.out.println("Servidor de tickets con pool de hilos activo en puerto 12346...");
        System.out.println("Pool: 10 hilos máximo");

        try {
            while (true) {
                Socket conexion = servidor.accept();
                System.out.println("Alguien se conectó desde: " + conexion.getInetAddress());
                
                // Enviar la tarea al pool en lugar de crear un nuevo Thread
                pool.execute(new ManejadorTickets(conexion));
            }
        } finally {
            servidor.close();
            pool.shutdown();
        }
    }
    
    // Clase interna para manejar cada cliente
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
                System.out.println("[" + Thread.currentThread().getName() + "] Cliente solicita: " + solicitud);
                
                // Entregar ticket de forma thread-safe
                int numeroTicket = ticket.getAndIncrement();
                salida.println("Tu ticket es el #" + numeroTicket);
                
                System.out.println("[" + Thread.currentThread().getName() + "] Ticket #" + numeroTicket + 
                    " entregado a: " + socket.getInetAddress());
                
                socket.close();
            } catch (IOException e) {
                System.out.println("Error con cliente: " + e.getMessage());
            }
        }
    }
}
