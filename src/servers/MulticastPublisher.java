package servers;

import utils.FileLogger;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;

public class MulticastPublisher {
    private String multicastGroupIP;
    private int multicastPort;
    private DatagramSocket socket;

    // essa parte de multicast foi basicamente copia do slide
    public MulticastPublisher(String ip, int port) throws SocketException {
        this.multicastGroupIP = ip;
        this.multicastPort = port;
        this.socket = new DatagramSocket();
        FileLogger.log("MulticastPublisher", "MulticastPublisher (Emissor) pronto para enviar para " + ip + ":" + port
                + " a partir da porta " + socket.getLocalPort());
    }

    public void send(String message) {
        try {
            byte[] buffer = message.getBytes(StandardCharsets.UTF_8);
            InetAddress groupAddress = InetAddress.getByName(multicastGroupIP);
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, groupAddress, multicastPort);
            socket.send(packet);
        } catch (IOException e) {
            FileLogger.log("MulticastPublisher", "MulticastPublisher: Erro ao enviar mensagem: " + e.getMessage());
        }
    }

    public void close() {
        if (socket != null && !socket.isClosed()) {
            socket.close();
            FileLogger.log("MulticastPublisher", "MulticastPublisher fechado.");
        }
    }
}