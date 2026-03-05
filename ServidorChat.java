import java.io.*;
import java.net.*;

public class ServidorChat {
    public static void main(String[] args) throws IOException {
        ServerSocket servidor = new ServerSocket(12345);
        System.out.println("Esperando cliente...");
        
        Socket cliente = servidor.accept();
        System.out.println("Cliente conectado.");

        BufferedReader entrada = new BufferedReader(new InputStreamReader(cliente.getInputStream()));
        PrintWriter salida = new PrintWriter(cliente.getOutputStream(), true);

        String mensaje;
        while ((mensaje = entrada.readLine()) != null) {
            if (mensaje.equalsIgnoreCase("salir")) {
                System.out.println("Cierre ordenado solicitado por el cliente.");
                break;
            }
            System.out.println("Cliente dice: " + mensaje);
            salida.println("Servidor confirma recepción de: " + mensaje);
        }

        cliente.close();
        servidor.close();
        System.out.println("Servidor cerrado.");
    }
}