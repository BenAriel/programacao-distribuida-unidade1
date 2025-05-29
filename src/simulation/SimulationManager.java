package simulation;

import utils.FileLogger;
import data.Drone;
import servers.Database;
import servers.DataCenterServer;
import servers.LoadBalancerDroneToServer;
import servers.MulticastPublisher;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SimulationManager {
    public static void main(String[] args) {
        FileLogger.log("SimulationManager", "🚀 Iniciando simulação do Sistema de Coleta de Dados Climáticos...");

        int loadBalancerPort = 8000;
        int server1Port = 9001;
        int server2Port = 9002;

        // Multicast (para usuários - Modo 1)
        // configuração do MulticastPublisher do slide dele
        String multicastIP = "225.7.8.9";
        int multicastPort = 55554;

        long simulationTimeMinutes = 1;
        Database centralizedDatabase = new Database();
        MulticastPublisher publisher = null;
        try {
            publisher = new MulticastPublisher(multicastIP, multicastPort);
        } catch (SocketException e) {
            FileLogger.log("SimulationManager", "🚨 Falha ao criar MulticastPublisher: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        List<String> dataCenterServerAddresses = new ArrayList<>();
        dataCenterServerAddresses.add("localhost:" + server1Port);
        dataCenterServerAddresses.add("localhost:" + server2Port);

        ExecutorService executor = Executors.newCachedThreadPool();
        DataCenterServer server1 = new DataCenterServer("S1", server1Port, centralizedDatabase, publisher);
        DataCenterServer server2 = new DataCenterServer("S2", server2Port, centralizedDatabase, publisher);
        executor.submit(server1);
        executor.submit(server2);

        // Inicialização e Submissão do Load Balancer
        LoadBalancerDroneToServer loadBalancer = new LoadBalancerDroneToServer(loadBalancerPort,
                dataCenterServerAddresses);
        executor.submit(loadBalancer);

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

        droneNorte.parar();
        droneSul.parar();
        droneLeste.parar();
        droneOeste.parar();
        FileLogger.log("SimulationManager", "Drones sinalizados para parar.");

        loadBalancer.parar();
        FileLogger.log("SimulationManager", "Load Balancer sinalizado para parar.");

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

        FileLogger.log("SimulationManager", "\n📊 Simulação finalizada. Verifique os logs no console.");
        FileLogger.log("SimulationManager",
                "Dados finais no banco de dados (último registro): " + centralizedDatabase.getLastData());

        FileLogger.close();

    }
}