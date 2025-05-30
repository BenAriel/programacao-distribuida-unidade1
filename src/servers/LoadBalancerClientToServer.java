package servers;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import utils.FileLogger;

/**
 * Codigo inicial do LoadBalancer de cliente
 */

public class LoadBalancerClientToServer {
    private final int port;
    private final List<Socket> serverSockets;
    /**
     * O executor vai servir para lidar com os Runnable sem que a gente tenha que definir um limite
     */
    private final ExecutorService executor;
    private int nextServerIndex;
    private final ServerSocket serverSocket;

    public LoadBalancerClientToServer(int port) throws IOException {
        this.port = port;
        serverSockets = Collections.synchronizedList(new ArrayList<>());
        executor = Executors.newCachedThreadPool();
        nextServerIndex = 0;
        serverSocket = new ServerSocket(port);
    }

    public void run() {
        FileLogger.log("Load balancer iniciado na porta: " + this.port);

        executor.execute(this.handleRequests());
    }

    private Runnable handleRequests() {
        return () -> {
            Socket socket;
            DataInputStream inputStream;

            while (true) {
                try {
                    socket = this.serverSocket.accept();
                    inputStream = new DataInputStream(socket.getInputStream());

                    String message = inputStream.readUTF();

                    switch (message) {
                        case "server-connect":
                            serverSockets.add(socket);

                            FileLogger.log("Servidor de ip: " + socket.getInetAddress() + " e Porta: " + socket.getPort() + " conectado");
                            break;

                        case "get-data":
                            FileLogger.log("Cliente solicitou dados: " + socket.getLocalAddress());
                    
                        default:
                            break;
                    }

                } catch (IOException e) {
                    FileLogger.log("Erro ao salvar");
                }
            }
        };
    }

    /**
     * Algoritmo do round-robin
     * @return
     */
    private Socket selectNextServer() {
        if (serverSockets.isEmpty()) {
            throw new IllegalStateException("Nenhum servidor de destino configurado para o Load Balancer.");
        }

        Socket server = serverSockets.get(nextServerIndex);

        nextServerIndex = (nextServerIndex + 1) % serverSockets.size();

        return server;
    }
}
