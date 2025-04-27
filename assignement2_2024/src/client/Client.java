package client;

import util.Protocol;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/**
 * Console client that connects to the online-shop server,
 * authenticates, and sends commands interactively.
 */
public class Client {
    private final String host;
    private final int port;

    public Client(String host, int port) {
        this.host = host;
        this.port = port;
    }

    /** Connects, logs in, then enters the interactive command loop. */
    public void start() {
        try (Socket sock = new Socket(host, port);
             PrintWriter out = new PrintWriter(new OutputStreamWriter(
                     sock.getOutputStream(), StandardCharsets.UTF_8), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(
                     sock.getInputStream(), StandardCharsets.UTF_8));
             Scanner console = new Scanner(System.in)) {

            // --- Authentication ---
            System.out.print("Username: ");
            String user = console.nextLine().trim();
            System.out.print("Password: ");
            String pass = console.nextLine().trim();
            out.printf("%s %s %s%n", Protocol.CMD_LOGIN, user, pass);

            String resp = in.readLine();
            if (!Protocol.RESP_AUTH_OK.equals(resp)) {
                System.out.println("Auth failed. Exiting.");
                return;
            }
            System.out.println("Logged in successfully.");

            // --- Main loop ---
            boolean done = false;
            while (!done) {
                showMenu();
                String choice = console.nextLine().trim();
                switch (choice) {
                    case "1":
                        out.println(Protocol.CMD_LIST);
                        handleList(in);
                        break;
                    case "2":
                        System.out.print("ID to purchase: ");
                        out.println(Protocol.CMD_PURCHASE + " " + console.nextLine().trim());
                        System.out.println("Server: " + in.readLine());
                        break;
                    case "3":
                        System.out.print("ID to return: ");
                        out.println(Protocol.CMD_RETURN + " " + console.nextLine().trim());
                        System.out.println("Server: " + in.readLine());
                        break;
                    case "4":
                        System.out.print("New ID: ");
                        String id   = console.nextLine().trim();
                        System.out.print("Name: ");
                        String nm   = console.nextLine().trim();
                        System.out.print("Price: ");
                        String pr   = console.nextLine().trim();
                        System.out.print("Qty: ");
                        String qt   = console.nextLine().trim();
                        out.println(Protocol.CMD_ADD + " " + id + ";" + nm + ";" + pr + ";" + qt);
                        System.out.println("Server: " + in.readLine());
                        break;
                    case "5":
                        out.println(Protocol.CMD_CLOSE);
                        System.out.println("Server: " + in.readLine());
                        done = true;
                        break;
                    default:
                        System.out.println("Invalid option.");
                }
            }
        } catch (IOException e) {
            System.err.println("Connection error: " + e.getMessage());
        }
    }

    private void showMenu() {
        System.out.println("\n1) List products");
        System.out.println("2) Purchase a product");
        System.out.println("3) Return a product");
        System.out.println("4) Add a new product");
        System.out.println("5) Close");
        System.out.print("Select: ");
    }

    /** Reads until LIST_END, printing each PRODUCT line in human-readable form. */
    private void handleList(BufferedReader in) throws IOException {
        String line = in.readLine();
        if (!Protocol.RESP_LIST_BEGIN.equals(line)) {
            System.out.println("Unexpected reply: " + line);
            return;
        }
        System.out.println("\n--- Product List ---");
        while ((line = in.readLine()) != null && !Protocol.RESP_LIST_END.equals(line)) {
            if (line.startsWith(Protocol.RESP_PRODUCT + " ")) {
                String[] f = line.substring((Protocol.RESP_PRODUCT + " ").length()).split(";");
                if (f.length == 4) {
                    System.out.printf("ID: %s | Name: %s | Price: %s | Qty: %s%n",
                            f[0], f[1], f[2], f[3]);
                }
            }
        }
    }

    /** Entry point: java com.onlineshop.Client [host] [port] */
    public static void main(String[] args) {
        String host = "localhost";
        int port    = 12345;
        if (args.length > 0) host = args[0];
        if (args.length > 1) {
            try { port = Integer.parseInt(args[1]); }
            catch (NumberFormatException ignored) {}
        }
        new Client(host, port).start();
    }
}
