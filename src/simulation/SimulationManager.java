package simulation;

import utils.FileLogger;
import data.Drone;
import servers.DataCenterServer;
import servers.LoadBalancerClientToServer;
import servers.LoadBalancerDroneToServer;
import servers.MulticastPublisher;
import java.io.IOException;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SimulationManager {
    public static void main(String[] args) {
        FileLogger.log("SimulationManager", "🚀 Iniciando simulação do Sistema de Coleta de Dados Climáticos...");

        int loadBalancerDronesPort = 8000; 
        int server1Port = 9001;
        int server2Port = 9002;

        int loadBalancerClientsPort = 9000;

        String multicastIP = "230.0.0.1";
        int multicastPort = 55554;

        long simulationTimeMinutes = 3;
        MulticastPublisher publisher = null;
        try {
            publisher = new MulticastPublisher(multicastIP, multicastPort);
        } catch (SocketException e) {
            FileLogger.log("SimulationManager", "Falha ao criar MulticastPublisher: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        List<String> dataCenterServerAddressesForDrones = new ArrayList<>();
        dataCenterServerAddressesForDrones.add("localhost:" + server1Port);
        dataCenterServerAddressesForDrones.add("localhost:" + server2Port);

        ExecutorService executor = Executors.newCachedThreadPool();

        LoadBalancerClientToServer lbClientToServer = null;
        try {
            lbClientToServer = new LoadBalancerClientToServer(loadBalancerClientsPort);
            final LoadBalancerClientToServer finalLbClientToServer = lbClientToServer;
            executor.submit(() -> {
                try {
                    finalLbClientToServer.run();
                } catch (Exception e) {
                    FileLogger.log("SimulationManager",
                            "🚨 Erro ao executar LoadBalancerClientToServer: " + e.getMessage());
                    e.printStackTrace();
                }
            });
            FileLogger.log("SimulationManager",
                    "LoadBalancerClientToServer (para clientes) iniciado na porta: " + loadBalancerClientsPort);

        } catch (IOException e) {
            FileLogger.log("SimulationManager", "🚨 Falha ao criar LoadBalancerClientToServer: " + e.getMessage());
            e.printStackTrace();
            executor.shutdownNow();
            return;
        }

        // Inicialização dos DataCenterServers
        DataCenterServer server1 = new DataCenterServer("S1", server1Port, publisher);
        DataCenterServer server2 = new DataCenterServer("S2", server2Port, publisher);
        executor.submit(server1);
        executor.submit(server2);
        if (lbClientToServer != null) {
            try {
                FileLogger.log("SimulationManager", "Tentando conectar S1 ao LoadBalancerClientToServer...");
                server1.connectToLoadBalancer("localhost", loadBalancerClientsPort); // Método existente em
                                                                                     // DataCenterServer
                FileLogger.log("SimulationManager", "S1 conectado ao LoadBalancerClientToServer.");

                FileLogger.log("SimulationManager", "Tentando conectar S2 ao LoadBalancerClientToServer...");
                server2.connectToLoadBalancer("localhost", loadBalancerClientsPort); // Método existente em
                                                                                     // DataCenterServer
                FileLogger.log("SimulationManager", "S2 conectado ao LoadBalancerClientToServer.");

            } catch (IOException e) {
                FileLogger.log("SimulationManager",
                        "🚨 Falha ao conectar DataCenterServers ao LoadBalancerClientToServer: " + e.getMessage());
                e.printStackTrace();
                
            }
        }

        // Inicialização e Submissão do Load Balancer para Drones
        LoadBalancerDroneToServer loadBalancerDrones = new LoadBalancerDroneToServer(loadBalancerDronesPort,
                dataCenterServerAddressesForDrones);
        executor.submit(loadBalancerDrones);

        // Inicialização e Submissão dos Drones
        Drone droneNorte = new Drone("Norte", '-', 2, 5);
        Drone droneSul = new Drone("Sul", ';', 2, 5);
        Drone droneLeste = new Drone("Leste", ',', 2, 5);
        Drone droneOeste = new Drone("Oeste", '#', 2, 5);

        executor.submit(droneNorte);
        executor.submit(droneSul);
        executor.submit(droneLeste);
        executor.submit(droneOeste);

        FileLogger.log("SimulationManager",
                "\n Simulação em execução por " + simulationTimeMinutes + " minuto(s). Aguarde...\n");
        try {
            Thread.sleep(TimeUnit.MINUTES.toMillis(simulationTimeMinutes));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            FileLogger.log("SimulationManager", "Thread principal da simulação interrompida.");
        }

        FileLogger.log("SimulationManager", "\n Encerrando simulação...");

        // Parar Drones
        droneNorte.parar();
        droneSul.parar();
        droneLeste.parar();
        droneOeste.parar();
        FileLogger.log("SimulationManager", "Drones sinalizados para parar.");

        // Parar Load Balancers
        loadBalancerDrones.parar();
        FileLogger.log("SimulationManager", "LoadBalancerDroneToServer sinalizado para parar.");
        server1.parar();
        server2.parar();
        FileLogger.log("SimulationManager", "Servidores do Centro de Dados sinalizados para parar.");

        // Encerrar o MulticastPublisher
        if (publisher != null) {
            publisher.close();
        }
        FileLogger.log("SimulationManager", "MulticastPublisher fechado."); // Desligar o ExecutorService

        executor.shutdown();
        try {
            FileLogger.log("SimulationManager", "Aguardando finalização das threads...");
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                FileLogger.log("SimulationManager", "Threads não finalizaram no tempo, forçando desligamento...");
                executor.shutdownNow();
                if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                    FileLogger.log("SimulationManager", "🚨 ExecutorService não terminou.");
                }
            }
        } catch (InterruptedException ie) {
            FileLogger.log("SimulationManager",
                    " Aguardo da finalização das threads interrompido. Forçando desligamento...");
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        FileLogger.log("SimulationManager", "\n📊 Simulação finalizada. Verifique os logs."); // Mensagem ajustada

        FileLogger.close();
    }
}