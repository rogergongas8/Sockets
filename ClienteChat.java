import java.io.*;
import java.net.*;
import java.util.Scanner;

public class ClienteChat {
    public static void main(String[] args) throws IOException {
        Socket socket = null;
        while (socket == null) {
            try {
                socket = new Socket("localhost", 12345);
                System.out.println("Conectado al chat. Escribe tu mensaje (o 'salir' para desconectar):");
            } catch (IOException e) {
                System.out.println("Servidor no disponible. Intentando reconectar en 2 segundos...");
                try {
                    Thread.sleep(2000); // Esperar 2 segundos antes de reintentar
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        PrintWriter salida = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        Scanner teclado = new Scanner(System.in);

        // Hilo para recibir mensajes de otros clientes
        Thread hiloRecepcion = new Thread(() -> {
            try {
                String mensaje;
                while ((mensaje = entrada.readLine()) != null) {
                    System.out.println(mensaje);
                }
            } catch (IOException e) {
                System.out.println("Conexión cerrada.");
            }
        });
        hiloRecepcion.start();

        // Loop para enviar mensajes
        while (true) {
            System.out.print("> ");
            String texto = teclado.nextLine();
            salida.println(texto);

            if (texto.equalsIgnoreCase("salir")) {
                break;
            }
        }

        socket.close();
        teclado.close();
        System.out.println("Cliente cerrado.");
    }
}