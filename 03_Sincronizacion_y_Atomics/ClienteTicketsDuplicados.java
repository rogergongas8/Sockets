import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Cliente diseñado para detectar tickets duplicados.
 * Lanza 100 hilos pidiendo tickets simultáneamente.
 */
public class ClienteTicketsDuplicados {
    public static void main(String[] args) throws Exception {
        int numPeticiones = 100;
        int puerto = 12347; // Por defecto el puerto del servidor sin sincronizar
        
        if (args.length > 0) puerto = Integer.parseInt(args[0]);

        ExecutorService executor = Executors.newFixedThreadPool(20);
        Set<Integer> ticketsRecibidos = Collections.synchronizedSet(new HashSet<>());
        Set<Integer> duplicados = Collections.synchronizedSet(new HashSet<>());
        
        System.out.println("Iniciando 100 peticiones simultáneas al puerto " + puerto + "...");
        
        CountDownLatch latch = new CountDownLatch(numPeticiones);
        
        for (int i = 0; i < numPeticiones; i++) {
            executor.submit(() -> {
                try (Socket socket = new Socket("localhost", puerto);
                     PrintWriter salida = new PrintWriter(socket.getOutputStream(), true);
                     BufferedReader entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                    
                    salida.println("Dame ticket");
                    String resp = entrada.readLine();
                    if (resp != null) {
                        try {
                            int ticket = Integer.parseInt(resp.trim());
                            if (!ticketsRecibidos.add(ticket)) {
                                duplicados.add(ticket);
                                System.out.println("!!! DUPLICADO DETECTADO: Ticket #" + ticket);
                            }
                        } catch (NumberFormatException nfe) {
                            // En caso de que el servidor devuelva texto "Tu ticket es #1"
                            String cleaned = resp.replaceAll("[^0-9]", "");
                            int ticket = Integer.parseInt(cleaned);
                            if (!ticketsRecibidos.add(ticket)) {
                                duplicados.add(ticket);
                                System.out.println("!!! DUPLICADO DETECTADO: Ticket #" + ticket);
                            }
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await();
        executor.shutdown();
        
        System.out.println("\n--- RESULTADOS ---");
        System.out.println("Peticiones totales: " + numPeticiones);
        System.out.println("Tickets únicos: " + ticketsRecibidos.size());
        if (duplicados.isEmpty()) {
            System.out.println("✓ NO se detectaron tickets duplicados.");
        } else {
            System.out.println("X SE DETECTARON " + duplicados.size() + " TICKETS DUPLICADOS.");
            System.out.println("Duplicados: " + duplicados);
        }
    }
}
