import java.io.*;
import java.net.*;
import java.util.Scanner;

public class ClienteChat {
    public static void main(String[] args) throws IOException {
        Socket socket = new Socket("localhost", 12345);
        PrintWriter salida = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        Scanner teclado = new Scanner(System.in);

        System.out.println("Conectado. Escribe tu mensaje (o 'salir' para desconectar):");

        while (true) {
            System.out.print("> ");
            String texto = teclado.nextLine();
            salida.println(texto);

            if (texto.equalsIgnoreCase("salir")) {
                break;
            }

            String respuesta = entrada.readLine();
            System.out.println(respuesta);
        }

        socket.close();
        teclado.close();
        System.out.println("Cliente cerrado.");
    }
}