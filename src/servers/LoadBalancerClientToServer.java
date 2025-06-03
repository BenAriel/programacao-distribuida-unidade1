package servers;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
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
    private final List<InetSocketAddress> serverSockets;
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
            BufferedReader input;

            while (true) {
                try {
                    socket = this.serverSocket.accept();
                    input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    String message = input.readLine();
                    String[] messageDT = message.split(":");

                    if (messageDT.length > 1) {
                        message = messageDT[0];
                    }

                    switch (message) {
                        case "server-connect":
                            serverSockets.add(new InetSocketAddress(messageDT[1], Integer.valueOf(messageDT[2])));

                            FileLogger.log("Servidor de ip: " + socket.getInetAddress() + " e Porta: " + socket.getPort() + " conectado");
                            break;

                        case "get-data":
                            FileLogger.log("Cliente solicitou dados: " + socket.getLocalAddress());

                            this.forwardToServer(socket, message);
                        default:
                            break;
                    }
                } catch (IOException e) {
                    FileLogger.log("Erro ao salvar");

                    e.printStackTrace();
                }
            }
        };
    }

    /**
     * Algoritmo do round-robin
     * @return
     */
    private InetSocketAddress selectNextServer() {
        if (serverSockets.isEmpty()) {
            throw new IllegalStateException("Nenhum servidor de destino configurado para o Load Balancer.");
        }

        InetSocketAddress server = serverSockets.get(nextServerIndex);
        nextServerIndex = (nextServerIndex + 1) % serverSockets.size();

        return server;
    }

    /**
     * Cria uma conexão com um servidor
     * @param clientSocket
     * @throws IOException
     */
    private void forwardToServer(Socket clientSocket, String clientMessage) throws IOException {
        InetSocketAddress serverAddress = selectNextServer();
        if (serverSocket == null) {
            clientSocket.close();

            System.out.println("No servers available. Connection closed.");
            return;
        }

        FileLogger.log(serverAddress.getHostName() + ":" + serverAddress.getPort());

        Socket serverSocket = new Socket(serverAddress.getHostName(), serverAddress.getPort());

        OutputStream out = serverSocket.getOutputStream();
        PrintWriter writer = new PrintWriter(out, true);
        writer.println(clientMessage);

        serverSocket.close();
    }
}
