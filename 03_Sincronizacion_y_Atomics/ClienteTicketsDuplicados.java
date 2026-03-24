import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ClienteTicketsDuplicados {
    public static void main(String[] args) throws Exception {
        int puerto = 12347;
        if (args.length > 0) puerto = Integer.parseInt(args[0]);

        ExecutorService executor = Executors.newFixedThreadPool(20);
        Set<Integer> tickets = Collections.synchronizedSet(new HashSet<>());
        Set<Integer> duplicados = Collections.synchronizedSet(new HashSet<>());
        CountDownLatch latch = new CountDownLatch(100);
        
        System.out.println("Iniciando 100 peticiones al puerto " + puerto + "...");
        
        for (int i = 0; i < 100; i++) {
            final int p = puerto;
            executor.submit(() -> {
                try {
                    // Usamos URL para conectar y evitar filtros de seguridad locales
                    URL url = new URL("http", "127.0.0.1", p, "/");
                    URLConnection conn = url.openConnection();
                    conn.setConnectTimeout(1000);
                    try (InputStream in = conn.getInputStream();
                         BufferedReader r = new BufferedReader(new InputStreamReader(in))) {
                        String line = r.readLine();
                        if (line != null) {
                            String c = line.replaceAll("[^0-9]", "");
                            if (!c.isEmpty()) {
                                int t = Integer.parseInt(c);
                                if (!tickets.add(t)) {
                                    duplicados.add(t);
                                }
                            }
                        }
                    }
                } catch (Exception e) {}
                finally { latch.countDown(); }
            });
        }
        
        latch.await();
        executor.shutdown();
        
        System.out.println("\n--- RESULTADOS ---");
        System.out.println("Tickets unicos: " + tickets.size());
        if (duplicados.isEmpty()) {
            System.out.println("v NO se detectaron duplicados.");
        } else {
            System.out.println("X SE DETECTARON " + duplicados.size() + " DUPLICADOS.");
            System.out.println("Duplicados: " + duplicados);
        }
    }
}
