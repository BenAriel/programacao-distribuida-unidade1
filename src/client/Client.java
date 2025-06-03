package client;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import data.ClimateData;
import utils.FileLogger;

class Client {
    private List<MulticastSocket> connectedGroups;
    private String LOAD_BALANCER_IP = "localhost";
    private int LOAD_BALANCER_PORT = 9000;

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

    public List<ClimateData> requestData() throws IOException, ClassNotFoundException
    {
        Socket socket = new Socket(LOAD_BALANCER_IP, LOAD_BALANCER_PORT);
        BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        PrintWriter output = new PrintWriter(socket.getOutputStream(), true);

        output.println("get-data");

        String objectData = input.readLine();
        List<ClimateData> data = new ArrayList<>();

        // if (objectData instanceof List) {
        //     data = (List<ClimateData>) input.readObject();
        // }

        socket.close();
        return data;
    }
}