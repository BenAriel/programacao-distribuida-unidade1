package client;

import java.io.IOException;
import java.net.SocketException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import data.Drone;
import servers.DataCenterServer;
import servers.Database;
import servers.LoadBalancerClientToServer;
import servers.LoadBalancerDroneToServer;
import servers.MulticastPublisher;
import utils.FileLogger;

/**
 * Essa classe temporaria serve para testar o servidor, rode essa antes de rodar a client interface
 */
public class ServerConnectionTest {
    public static void main(String[] args) throws IOException {
        long simulationTimeMinutes = 1;
        ExecutorService executor = Executors.newCachedThreadPool();

        MulticastPublisher publisher = null;
        try {
            publisher = new MulticastPublisher("225.7.8.9", 55554);
        } catch (SocketException e) {
            FileLogger.log("SimulationManager", "🚨 Falha ao criar MulticastPublisher: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        Database db = new Database();

        LoadBalancerClientToServer lb = new LoadBalancerClientToServer(9000);

        lb.run();

        DataCenterServer sv = new DataCenterServer("localhost", 5433, db, publisher);

        sv.connectToLoadBalancer("localhost", 9000);

        LoadBalancerDroneToServer loadDrone = new LoadBalancerDroneToServer(9001, List.of("localhost:" + 5433));

        executor.submit(loadDrone);
        executor.submit(sv);

        Drone droneNorte = new Drone("Norte", '-', 2, 5);
        
        executor.submit(droneNorte);

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
    }
}
