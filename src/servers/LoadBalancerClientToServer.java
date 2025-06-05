package servers;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;

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
    private final ServerSocket serverSocket;

    public LoadBalancerClientToServer(int port) throws IOException {
        this.port = port;
        serverSockets = Collections.synchronizedList(new ArrayList<>());
        executor = Executors.newCachedThreadPool();
        serverSocket = new ServerSocket(port);
    }

    public void run() {
        FileLogger.log("Load balancer iniciado na porta: " + this.port);

        executor.execute(this.handleRequests());
    }

    private Runnable handleRequests() {
        return () -> {
            Socket socket;
            ObjectInputStream input;
            ObjectOutputStream output;

            while (true) {
                try {
                    socket = this.serverSocket.accept();
                    output = new ObjectOutputStream(socket.getOutputStream());

                    input = new ObjectInputStream(socket.getInputStream());
                    String message = (String) input.readObject();
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

                            this.forwardToServer(socket, message, output);
                        default:
                            break;
                    }
                } catch (IOException | ClassNotFoundException e) {
                    FileLogger.log("Erro ao salvar");

                    e.printStackTrace();
                }
            }
        };
    }

    /**
     * Algoritmo do random
     * @return
     */
    private InetSocketAddress selectNextServer() {
    if (serverSockets.isEmpty()) {
        throw new IllegalStateException("Nenhum servidor de destino configurado para o Load Balancer.");
    }

    int randomIndex = ThreadLocalRandom.current().nextInt(serverSockets.size());
    return serverSockets.get(randomIndex);
}

    /**
     * Cria uma conexão com um servidor
     * @param clientSocket
     * @throws IOException
     */
    private void forwardToServer(Socket clientSocket, String clientMessage, ObjectOutputStream clientOut) throws IOException {
        InetSocketAddress serverAddress = selectNextServer();

        Socket serverSocket = new Socket(serverAddress.getHostName(), serverAddress.getPort());

        ObjectOutputStream serverOut = new ObjectOutputStream(serverSocket.getOutputStream());
        ObjectInputStream serverIn = new ObjectInputStream(serverSocket.getInputStream());

        serverOut.writeObject(clientMessage);
        serverOut.flush();

        try {
            Object response = serverIn.readObject();

            clientOut.writeObject(response);
            clientOut.flush();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        serverSocket.close();
        clientSocket.close();
    }
}
