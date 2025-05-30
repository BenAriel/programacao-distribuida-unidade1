package client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.List;

class Client {
    private List<MulticastSocket> connectedGroups;

    public Client() {
        connectedGroups = new ArrayList<>();
    }

    /**
     * Este metodo encapsula a parte de se juntar a um grupo
     * @param inetGroup
     * @param port
     * @return
     * @throws IOException
     */
    public MulticastSocket joinGroup(String inetGroup, int port) throws IOException {
        MulticastSocket socket = new MulticastSocket(port);
        InetAddress multicastIp = InetAddress.getByName(inetGroup);
        InetSocketAddress group = new InetSocketAddress(multicastIp, port);
        NetworkInterface interfaceRede = NetworkInterface.getByName("wlo1");

        socket.joinGroup(group, interfaceRede);

        connectedGroups.add(socket);

        return socket;
    }

    /**
     * Espera até receber uma string do servidor
     * @param socket
     * @return String de dados do servidor
     * @throws IOException
     */
    public String waitMessage(MulticastSocket socket) throws IOException {
        byte[] buffer = new byte[1024];

        DatagramPacket receiver = new DatagramPacket( 
                buffer, 
                buffer.length); 
 
        socket.receive(receiver);

        return new String(buffer, 0, receiver.getLength());
    }
}