import java.io.*;
import java.net.*;

public class ClienteTickets {
    public static void main(String[] args) throws Exception {
        System.out.println("Conectando para pedir ticket...");
        Socket socket = new Socket("localhost", 12346);
        
        PrintWriter salida = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        
        salida.println("Hola, necesito un ticket");
        String respuesta = entrada.readLine();
        
        System.out.println("Servidor responde: " + respuesta);
        socket.close();
    }
}