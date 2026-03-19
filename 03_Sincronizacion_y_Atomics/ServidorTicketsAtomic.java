import java.io.*;
import java.net.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Servidor que usa AtomicInteger para evitar Race Conditions de forma eficiente.
 */
public class ServidorTicketsAtomic {
    private static AtomicInteger contador = new AtomicInteger(1);
    
    public static void main(String[] args) throws Exception {
        ServerSocket servidor = new ServerSocket(12349);
        System.out.println("✓ Servidor SEGURO (Atomic) en puerto 12349");
        
        while (true) {
            Socket conexion = servidor.accept();
            new Thread(() -> {
                try (BufferedReader entrada = new BufferedReader(new InputStreamReader(conexion.getInputStream()));
                     PrintWriter salida = new PrintWriter(conexion.getOutputStream(), true)) {
                    
                    entrada.readLine();
                    int ticket = contador.getAndIncrement();
                    salida.println(ticket);
                    conexion.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }
}
