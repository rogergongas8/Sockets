import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;

/**
 * Simulación de Nginx: Usa Selector (Event Loop) para manejar múltiples conexiones
 * con un solo hilo (o muy pocos).
 */
public class ServidorGestionNginx {
    public static void main(String[] args) throws IOException {
        Selector selector = Selector.open();
        ServerSocketChannel serverChannel = ServerSocketChannel.open();
        serverChannel.bind(new InetSocketAddress(8081));
        serverChannel.configureBlocking(false);
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);
        
        System.out.println("--- SIMULACIÓN NGINX (Event Loop / Non-blocking) ---");
        System.out.println("Escuchando en puerto 8081...");
        
        while (true) {
            selector.select(); // Bloquea hasta que haya un evento
            Set<SelectionKey> selectedKeys = selector.selectedKeys();
            Iterator<SelectionKey> iter = selectedKeys.iterator();
            
            while (iter.hasNext()) {
                SelectionKey key = iter.next();
                
                if (key.isAcceptable()) {
                    register(selector, serverChannel);
                }
                
                if (key.isReadable()) {
                    answer(key);
                }
                iter.remove();
            }
        }
    }

    private static void register(Selector selector, ServerSocketChannel serverChannel) throws IOException {
        SocketChannel client = serverChannel.accept();
        client.configureBlocking(false);
        client.register(selector, SelectionKey.OP_READ);
        System.out.println("[Nginx] Nueva conexión registrada en el EVENT LOOP.");
    }

    private static void answer(SelectionKey key) throws IOException {
        SocketChannel client = (SocketChannel) key.channel();
        ByteBuffer buffer = ByteBuffer.allocate(256);
        int read = client.read(buffer);
        
        if (read == -1) {
            client.close();
            return;
        }
        
        String result = new String(buffer.array()).trim();
        System.out.println("[Nginx] Evento de LECTURA detectado. Procesando sin crear hilos nuevos...");
        
        String response = "Nginx respondió a '" + result + "' usando el EVENT LOOP en el hilo principal\n";
        client.write(ByteBuffer.wrap(response.getBytes()));
        client.close();
    }
}
