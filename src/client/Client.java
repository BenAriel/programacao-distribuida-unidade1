package client;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

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

        FileLogger.log(new String(buffer, 0, receiver.getLength()));

        return new String(buffer, 0, receiver.getLength());
    }

    @SuppressWarnings("unchecked")
    public List<ClimateData> requestData() throws IOException, ClassNotFoundException
    {
        Socket socket = new Socket(LOAD_BALANCER_IP, LOAD_BALANCER_PORT);

        ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
        ObjectInputStream input = new ObjectInputStream(socket.getInputStream());

        output.writeObject("get-data");

        Object objectData = input.readObject();
        List<String> data = new ArrayList<>();

        if (objectData instanceof List) {
            data = (ArrayList<String>) objectData;
        }

        Function<String, ClimateData> stringToClimateData = (item) -> {
            item = item.replaceAll("[\\[\\]\\s]", "");

            String[] parts = item.split("//");

            if (parts.length != 4) {
                throw new IllegalArgumentException("Formato inválido: " + item);
            }

            double temperatura = Double.parseDouble(parts[0]);
            double umidade = Double.parseDouble(parts[1]);
            double pressao = Double.parseDouble(parts[2]);
            double radiacao = Double.parseDouble(parts[3]);

            return new ClimateData(temperatura, umidade, pressao, radiacao);
        };

        socket.close();

        return data.stream()
                     .map(stringToClimateData)
                     .collect(Collectors.toList());
    }
}