import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EchoServer {
    private final ServerSocket serverSocket;
    private Set<Socket> clients;
    ExecutorService executorService;

    public EchoServer() throws IOException {
        serverSocket = new ServerSocket(9999);
        clients = new HashSet<>();
        executorService = Executors.newFixedThreadPool(5);
    }

    public void start() {
        try (serverSocket) {
            System.out.println("Server ready to start accepting connections.");
            while (true) {
                Socket client = serverSocket.accept();
                clients.add(client);
                executorService.submit(new ChitChat(client));
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
            return;
        }
    }

    private static class ChitChat implements Runnable {
        private Socket client;

        public ChitChat(Socket client) {
            this.client = client;
        }

        public void run() {
            try (Socket client = this.client;
                    var inputStream = new DataInputStream(client.getInputStream());
                    var outputStream = new DataOutputStream(client.getOutputStream());) {
                System.out.println("Connection Established with client :" + client.getInetAddress());
                while (true) {
                    String message = inputStream.readLine();
                    if (message.equals("quit") || message.equals("exit")) {
                        break;
                    }
                    String outputMessage = "ECHO: " + message + "\n";
                    outputStream.writeUTF(outputMessage);
                }
            } catch (IOException e) {
                System.err.println(e.getMessage());
                return;
            }
            System.out.println("connection closed with client!");
        }
    }

    /**
     * Open different terminal and write netcat localhost 9999 on each.
     * then for each terminal a separate tasks with chit chat.
     * write quit or exit to stop disconnect.
     * 
     * @param s Arguments
     * @throws IOException
     */
    public static void main(String... s) throws IOException {
        EchoServer echoServer = new EchoServer();
        echoServer.start();
    }
}
