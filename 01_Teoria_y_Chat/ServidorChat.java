import java.io.*;
import java.net.*;
import java.util.*;

public class ServidorChat {
    private static List<PrintWriter> clientes = Collections.synchronizedList(new ArrayList<>());

    public static void main(String[] args) throws IOException {
        ServerSocket servidor = new ServerSocket(12345);
        System.out.println("Servidor de chat multi-usuario activo en puerto 12345...");

        while (true) {
            Socket cliente = servidor.accept();
            System.out.println("Nuevo cliente conectado.");
            PrintWriter salida = new PrintWriter(cliente.getOutputStream(), true);
            clientes.add(salida);

            // Crear un hilo para manejar este cliente
            new Thread(() -> manejarCliente(cliente, salida)).start();
        }
    }

    private static void manejarCliente(Socket cliente, PrintWriter salida) {
        try {
            BufferedReader entrada = new BufferedReader(new InputStreamReader(cliente.getInputStream()));
            String mensaje;
            while ((mensaje = entrada.readLine()) != null) {
                if (mensaje.equalsIgnoreCase("salir")) {
                    System.out.println("Cliente desconectado.");
                    break;
                }
                System.out.println("Mensaje recibido: " + mensaje);
                // Retransmitir a todos los clientes conectados
                synchronized (clientes) {
                    for (PrintWriter c : clientes) {
                        if (c != salida) {  // No enviar al remitente
                            c.println("Cliente dice: " + mensaje);
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Error con cliente: " + e.getMessage());
        } finally {
            clientes.remove(salida);
            try {
                cliente.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}