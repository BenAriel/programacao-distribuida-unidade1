package servers;

import utils.FileLogger;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.ArrayList;
import java.net.InetSocketAddress;

public class LoadBalancerDroneToServer implements Runnable {
    private int loadBalancerPort;
    private List<InetSocketAddress> serverTargets;
    private int currentServerIndex = 0;
    private volatile boolean ativo;

    public LoadBalancerDroneToServer(int lbPort, List<String> serverAddresses) {
        this.loadBalancerPort = lbPort;
        this.serverTargets = new ArrayList<>();
        for (String addr : serverAddresses) {
            String[] parts = addr.split(":");
            this.serverTargets.add(new InetSocketAddress(parts[0], Integer.parseInt(parts[1])));
        }
        this.ativo = true;
    }

    @Override
    public void run() {
        FileLogger.log("LoadBalancerDroneToServer",
                "Load Balancer (Drones->Servidores) iniciado na porta: " + loadBalancerPort);
        try (ServerSocket lbSocket = new ServerSocket(loadBalancerPort)) {
            while (ativo) {
                FileLogger.log("LoadBalancerDroneToServer", "Load Balancer aguardando conexão de drone...");
                try (Socket droneSocket = lbSocket.accept();
                        BufferedReader droneIn = new BufferedReader(
                                new InputStreamReader(droneSocket.getInputStream()))) {

                    FileLogger.log("LoadBalancerDroneToServer",
                            "Load Balancer: Drone conectado: " + droneSocket.getInetAddress().getHostAddress());
                    String dataFromDrone;

                    if ((dataFromDrone = droneIn.readLine()) != null) {
                        // Seleciona o próximo servidor (Round Robin por enqaunto)
                        InetSocketAddress targetServerAddress = selectNextServer();
                        FileLogger.log("LoadBalancerDroneToServer",
                                "Load Balancer: Encaminhando dados do drone para Servidor " + targetServerAddress);

                        forwardData(dataFromDrone, targetServerAddress);
                    }
                } catch (IOException e) {
                    if (ativo) {
                        FileLogger.log("LoadBalancerDroneToServer",
                                "Load Balancer: Erro de comunicação com drone ou servidor de destino: "
                                        + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            if (ativo) {
                FileLogger.log("LoadBalancerDroneToServer", "Load Balancer: Erro ao iniciar o ServerSocket na porta "
                        + loadBalancerPort + ": " + e.getMessage());
            }
        }
        FileLogger.log("LoadBalancerDroneToServer", "Load Balancer (Drones->Servidores) finalizado.");
    }

    private InetSocketAddress selectNextServer() {
        if (serverTargets.isEmpty()) {
            throw new IllegalStateException("Nenhum servidor de destino configurado para o Load Balancer.");
        }
        InetSocketAddress server = serverTargets.get(currentServerIndex);
        currentServerIndex = (currentServerIndex + 1) % serverTargets.size();
        return server;
    }

    private void forwardData(String data, InetSocketAddress targetServerAddress) {
        try (Socket serverConnection = new Socket(targetServerAddress.getAddress(), targetServerAddress.getPort());
                PrintWriter serverOut = new PrintWriter(serverConnection.getOutputStream(), true)) {

            serverOut.println(data); // Envia os dados para o DataCenterServer escolhido
            FileLogger.log("LoadBalancerDroneToServer", "Load Balancer: Dados enviados para " + targetServerAddress);
        } catch (IOException e) {
            FileLogger.log("LoadBalancerDroneToServer",
                    "Load Balancer: Falha ao encaminhar dados para " + targetServerAddress + ": " + e.getMessage());
            // Poderia adicionar lógica de retry ou marcar servidor como
            // indisponívemporariamente
        }
    }

    public void parar() {
        this.ativo = false;
        try {
            new Socket("localhost", this.loadBalancerPort).close();
        } catch (IOException e) {
        }
        FileLogger.log("LoadBalancerDroneToServer", "Load Balancer (Drones->Servidores) sinalizado para parar.");
    }
}