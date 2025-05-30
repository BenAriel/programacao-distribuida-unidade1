package client;

import java.io.IOException;
import servers.DataCenterServer;
import servers.LoadBalancerClientToServer;

public class ClientTest {
    public static void main(String[] args) throws IOException {
        // testa se o load balancer está aceitando as conexões

        LoadBalancerClientToServer lb = new LoadBalancerClientToServer(9000);

        lb.run();

        DataCenterServer sv = new DataCenterServer(null, 0, null, null);

        sv.connectToLoadBalancer("localhost", 9000);

        DataCenterServer sv2 = new DataCenterServer(null, 0, null, null);

        sv2.connectToLoadBalancer("localhost", 9000);
    }
}
