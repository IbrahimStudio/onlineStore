package server;

import model.Product;
import model.User;
import util.Protocol;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main server class. Loads products and users from files, listens for client
 * connections, authenticates users, and processes commands in parallel.
 */
public class Server {
    private static final Logger logger = Logger.getLogger(Server.class.getName());

    private final int port;
    private final Map<String, Product> products = new ConcurrentHashMap<>();
    private final Map<String, User> users      = new ConcurrentHashMap<>();
    private final ExecutorService clientPool   = Executors.newCachedThreadPool();
    private ServerSocket serverSocket;

    public Server(int port) {
        this.port = port;
    }

    /** Loads users.txt and products.txt, then starts accepting clients. */
    public void start() {
        loadUsers("C:\\Users\\ibrah\\eclipse-workspace\\assignement2_2024\\src\\sources\\users.txt");
        loadProducts("C:\\Users\\ibrah\\eclipse-workspace\\assignement2_2024\\src\\sources\\products.txt");
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));

        try (ServerSocket sock = new ServerSocket(port)) {
            serverSocket = sock;
            logger.info("Server listening on port " + port);

            while (!sock.isClosed()) {
                try {
                    Socket client = sock.accept();
                    logger.info("Connection from " + client.getRemoteSocketAddress());
                    clientPool.submit(new ClientHandler(client));
                } catch (IOException e) {
                    if (!sock.isClosed()) {
                        logger.log(Level.SEVERE, "Accept failed", e);
                    }
                }
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Could not start server", e);
        } finally {
            shutdown();
        }
    }

    /** Closes the server socket and stops client threads gracefully. */
    private void shutdown() {
        logger.info("Shutting down server");
        try {
            if (serverSocket != null && !serverSocket.isClosed())
                serverSocket.close();
        } catch (IOException e) {
            logger.log(Level.WARNING, "Error closing server socket", e);
        }
        clientPool.shutdown();
        try {
            if (!clientPool.awaitTermination(5, TimeUnit.SECONDS)) {
                clientPool.shutdownNow();
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    /** Reads users.txt (username;password) and populates the users map. */
    private void loadUsers(String filePath) {
        try (BufferedReader r = new BufferedReader(new InputStreamReader(
                new FileInputStream(filePath), StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] p = line.split(";");
                if (p.length != 2) {
                    logger.warning("Bad user entry: " + line);
                    continue;
                }
                users.put(p[0], new User(p[0], p[1]));
            }
            logger.info("Loaded " + users.size() + " users");
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to load users", e);
        }
    }

    /** Reads products.txt (id;name;price;quantity) and populates the products map. */
    private void loadProducts(String filePath) {
        try (BufferedReader r = new BufferedReader(new InputStreamReader(
                new FileInputStream(filePath), StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] p = line.split(";");
                if (p.length != 4) {
                    logger.warning("Bad product entry: " + line);
                    continue;
                }
                try {
                    double price = Double.parseDouble(p[2]);
                    int qty    = Integer.parseInt(p[3]);
                    products.put(p[0], new Product(p[0], p[1], price, qty));
                } catch (NumberFormatException ex) {
                    logger.warning("Number format error in: " + line);
                }
            }
            logger.info("Loaded " + products.size() + " products");
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to load products", e);
        }
    }

    /** Handles a single client session: authentication + command loop. */
    private class ClientHandler implements Runnable {
        private final Socket socket;
        private User currentUser;

        ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            logger.info("Handler started for " + socket.getRemoteSocketAddress());
            try (BufferedReader in = new BufferedReader(new InputStreamReader(
                         socket.getInputStream(), StandardCharsets.UTF_8));
                 PrintWriter out = new PrintWriter(new OutputStreamWriter(
                         socket.getOutputStream(), StandardCharsets.UTF_8), true)) {

                if (!authenticate(in, out)) {
                    socket.close();
                    return;
                }

                String line;
                while ((line = in.readLine()) != null) {
                    String[] parts = line.split(" ", 2);
                    String cmd     = parts[0];
                    String payload = parts.length > 1 ? parts[1] : "";

                    switch (cmd) {
                        case Protocol.CMD_LIST:
                            serveList(out);
                            break;
                        case Protocol.CMD_PURCHASE:
                            servePurchase(out, payload);
                            break;
                        case Protocol.CMD_RETURN:
                            serveReturn(out, payload);
                            break;
                        case Protocol.CMD_ADD:
                            serveAdd(out, payload);
                            break;
                        case Protocol.CMD_CLOSE:
                            out.println(Protocol.RESP_CLOSE);
                            return;
                        default:
                            out.println("UNKNOWN_COMMAND");
                            break;
                    }
                }
            } catch (IOException e) {
                logger.log(Level.WARNING, "I/O error with client", e);
            } finally {
                try { socket.close(); } catch (IOException ignore) {}
            }
        }

        /** Expects “LOGIN user pass”. Replies AUTH_OK or AUTH_FAIL. */
        private boolean authenticate(BufferedReader in, PrintWriter out) throws IOException {
            String line = in.readLine();
            if (line == null) {
                return false;
            }
            String[] tok = line.split(" ");
            if (tok.length != 3 || !Protocol.CMD_LOGIN.equals(tok[0])) {
                out.println(Protocol.RESP_AUTH_FAIL);
                return false;
            }
            User u = users.get(tok[1]);
            if (u != null && u.authenticate(tok[2])) {
                currentUser = u;
                out.println(Protocol.RESP_AUTH_OK);
                return true;
            } else {
                out.println(Protocol.RESP_AUTH_FAIL);
                return false;
            }
        }

        private void serveList(PrintWriter out) {
            out.println(Protocol.RESP_LIST_BEGIN);
            products.values().forEach(p ->
                out.println(Protocol.RESP_PRODUCT + " " + p.toString())
            );
            out.println(Protocol.RESP_LIST_END);
        }

        private void servePurchase(PrintWriter out, String id) {
            Product p = products.get(id);
            if (p != null && p.purchase()) {
                out.println(Protocol.RESP_PURCHASE_OK);
            } else {
                out.println(Protocol.RESP_PURCHASE_FAIL);
            }
        }

        private void serveReturn(PrintWriter out, String id) {
            Product p = products.get(id);
            if (p != null) {
                p.restock();
                out.println(Protocol.RESP_RETURN_OK);
            } else {
                out.println(Protocol.RESP_RETURN_FAIL);
            }
        }

        private void serveAdd(PrintWriter out, String payload) {
            String[] p = payload.split(";");
            if (p.length != 4) {
                out.println(Protocol.RESP_ADD_FAIL);
                return;
            }
            String id   = p[0], name = p[1];
            try {
                double price = Double.parseDouble(p[2]);
                int qty      = Integer.parseInt(p[3]);
                Product prod = new Product(id, name, price, qty);
                if (products.putIfAbsent(id, prod) == null) {
                    out.println(Protocol.RESP_ADD_OK);
                } else {
                    out.println(Protocol.RESP_ADD_FAIL);
                }
            } catch (NumberFormatException ex) {
                out.println(Protocol.RESP_ADD_FAIL);
            }
        }
    }

    /** Starts the server. Usage: java com.onlineshop.Server [port] */
    public static void main(String[] args) {
        int port = 12345;
        if (args.length > 0) {
            try { port = Integer.parseInt(args[0]); }
            catch (NumberFormatException ignored) { /* keep default */ }
        }
        new Server(port).start();
    }
}
