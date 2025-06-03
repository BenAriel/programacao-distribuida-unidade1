package client;

import java.io.IOException;
import servers.DataCenterServer;
import servers.Database;
import servers.LoadBalancerClientToServer;

public class ClientTest {
    public static void main(String[] args) throws IOException {
        Database db = new Database();

        LoadBalancerClientToServer lb = new LoadBalancerClientToServer(9000);

        lb.run();

        DataCenterServer sv = new DataCenterServer("localhost", 5432, db, null);

        sv.connectToLoadBalancer("localhost", 9000);

        sv.run();
    }
}
