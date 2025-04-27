package gui;

import util.Protocol;
import model.Product;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * JavaFX GUI client for the online shop.
 * <p>
 * Presents a login screen, then a product‐management dashboard:
 * listing, purchasing, returning and adding products.
 * All communication happens over plain TCP sockets using {@link Protocol}.
 */
public class ClientGUI extends Application {
	private final Object networkLock = new Object();

    private String host;
    private int port;

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    private final ObservableList<Product> products = FXCollections.observableArrayList();
    private TableView<Product> tableView;

    @Override
    public void start(Stage primaryStage) {
        // --- parse command-line args ---
        List<String> args = getParameters().getRaw();
        host = args.size() > 0 ? args.get(0) : "localhost";
        port = args.size() > 1
            ? Integer.parseInt(args.get(1))
            : 12345;

        primaryStage.setTitle("Online Shop GUI Client");

        // --- Login Scene ---
        Label userLabel = new Label("Username:");
        TextField userField = new TextField();
        Label passLabel = new Label("Password:");
        PasswordField passField = new PasswordField();
        Button loginBtn = new Button("Login");
        Label loginMsg = new Label();
        VBox loginBox = new VBox(10,
            userLabel, userField,
            passLabel, passField,
            loginBtn, loginMsg
        );
        loginBox.setPadding(new Insets(20));
        Scene loginScene = new Scene(loginBox, 300, 250);

        // --- Main Scene ---
        tableView = new TableView<>();
        TableColumn<Product,String> idCol    = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        TableColumn<Product,String> nameCol  = new TableColumn<>("Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        TableColumn<Product,Double> priceCol = new TableColumn<>("Price");
        priceCol.setCellValueFactory(new PropertyValueFactory<>("price"));
        TableColumn<Product,Integer> qtyCol  = new TableColumn<>("Qty");
        qtyCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        tableView.getColumns().addAll(idCol, nameCol, priceCol, qtyCol);
        tableView.setItems(products);

        Button refreshBtn  = new Button("Refresh");
        Button purchaseBtn = new Button("Purchase");
        Button returnBtn   = new Button("Return");
        HBox actionBox = new HBox(10, refreshBtn, purchaseBtn, returnBtn);
        actionBox.setPadding(new Insets(10));

        Label addLabel = new Label("Add New Product:");
        TextField addId    = new TextField(); addId.setPromptText("ID");
        TextField addName  = new TextField(); addName.setPromptText("Name");
        TextField addPrice = new TextField(); addPrice.setPromptText("Price");
        TextField addQty   = new TextField(); addQty.setPromptText("Qty");
        Button addBtn      = new Button("Add");
        HBox addBox = new HBox(5, addId, addName, addPrice, addQty, addBtn);
        addBox.setPadding(new Insets(10));

        Button logoutBtn = new Button("Logout");
        HBox topBox = new HBox(logoutBtn);
        topBox.setPadding(new Insets(10));

        BorderPane mainPane = new BorderPane();
        mainPane.setTop(topBox);
        mainPane.setCenter(tableView);
        mainPane.setBottom(new VBox(actionBox, addLabel, addBox));
        Scene mainScene = new Scene(mainPane, 600, 400);

        // --- Event Handlers ---

        // LOGIN
        loginBtn.setOnAction(e -> {
            String user = userField.getText().trim();
            String pass = passField.getText().trim();
            loginMsg.setText("Connecting…");
            Task<Boolean> loginTask = new Task<>() {
                @Override protected Boolean call() {
                    try {
                        socket = new Socket(host, port);
                        out = new PrintWriter(
                            new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8),
                            true
                        );
                        in = new BufferedReader(
                            new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8)
                        );
                        out.printf("%s %s %s%n", Protocol.CMD_LOGIN, user, pass);
                        String resp = in.readLine();
                        return Protocol.RESP_AUTH_OK.equals(resp);
                    } catch (IOException ex) {
                        return false;
                    }
                }
            };
            loginTask.setOnSucceeded(ev -> {
                if (loginTask.getValue()) {
                    primaryStage.setScene(mainScene);
                    loadProducts();
                } else {
                    loginMsg.setText("Login failed.");
                    try { socket.close(); } catch (IOException ignore) {}
                }
            });
            new Thread(loginTask).start();
        });

        // REFRESH
        refreshBtn.setOnAction(e -> loadProducts());

        // PURCHASE
        purchaseBtn.setOnAction(e -> {
            Product sel = tableView.getSelectionModel().getSelectedItem();
            if (sel == null) {
                showAlert(Alert.AlertType.WARNING, "Please select a product first.");
                return;
            }
            sendCommand(Protocol.CMD_PURCHASE + " " + sel.getId(), resp -> {
                if (Protocol.RESP_PURCHASE_OK.equals(resp)) {
                    showAlert(Alert.AlertType.INFORMATION, "Purchase successful.");
                } else {
                    showAlert(Alert.AlertType.ERROR, "Purchase failed.");
                }
                loadProducts();
            });
        });

        // RETURN
        returnBtn.setOnAction(e -> {
            Product sel = tableView.getSelectionModel().getSelectedItem();
            if (sel == null) {
                showAlert(Alert.AlertType.WARNING, "Please select a product first.");
                return;
            }
            sendCommand(Protocol.CMD_RETURN + " " + sel.getId(), resp -> {
                if (Protocol.RESP_RETURN_OK.equals(resp)) {
                    showAlert(Alert.AlertType.INFORMATION, "Return successful.");
                } else {
                    showAlert(Alert.AlertType.ERROR, "Return failed.");
                }
                loadProducts();
            });
        });

        // ADD
        addBtn.setOnAction(e -> {
            String id   = addId.getText().trim();
            String nm   = addName.getText().trim();
            String pr   = addPrice.getText().trim();
            String qt   = addQty.getText().trim();
            if (id.isEmpty() || nm.isEmpty() || pr.isEmpty() || qt.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "All fields are required.");
                return;
            }
            sendCommand(Protocol.CMD_ADD + " " + id + ";" + nm + ";" + pr + ";" + qt, resp -> {
                if (Protocol.RESP_ADD_OK.equals(resp)) {
                    showAlert(Alert.AlertType.INFORMATION, "Product added.");
                } else {
                    showAlert(Alert.AlertType.ERROR, "Add failed.");
                }
                addId.clear(); addName.clear(); addPrice.clear(); addQty.clear();
                loadProducts();
            });
        });

        // LOGOUT
        logoutBtn.setOnAction(e -> {
            sendCommand(Protocol.CMD_CLOSE, _resp -> {});
            try { socket.close(); } catch (IOException ignore) {}
            products.clear();
            primaryStage.setScene(loginScene);
        });

        primaryStage.setScene(loginScene);
        primaryStage.show();
    }

    /**
     * Reads the current product list from the server and populates the TableView.
     */
    private void loadProducts() {
        Task<ObservableList<Product>> task = new Task<>() {
            @Override
            protected ObservableList<Product> call() throws Exception {
                synchronized (networkLock) {
                    // 1) send the LIST command
                    System.out.println("→ SENDING: " + Protocol.CMD_LIST);
                    out.println(Protocol.CMD_LIST);

                    // 2) expect LIST_BEGIN
                    String line = in.readLine();
                    System.out.println("← RECEIVED: " + line);
                    if (!Protocol.RESP_LIST_BEGIN.equals(line)) {
                        throw new IOException("Expected LIST_BEGIN but got: " + line);
                    }

                    // 3) read until LIST_END
                    ObservableList<Product> temp = FXCollections.observableArrayList();
                    while (true) {
                        line = in.readLine();
                        System.out.println("← RECEIVED: " + line);
                        if (line == null) {
                            throw new IOException("Stream closed unexpectedly");
                        }
                        if (Protocol.RESP_LIST_END.equals(line)) {
                            break;
                        }
                        if (line.startsWith(Protocol.RESP_PRODUCT + " ")) {
                            String data = line.substring((Protocol.RESP_PRODUCT + " ").length());
                            String[] f = data.split(";");
                            System.out.println("DEBUG parsing product: " + Arrays.toString(f));
                            if (f.length == 4) {
                                String id    = f[0];
                                String name  = f[1];
                                double price = Double.parseDouble(f[2]);
                                int qty      = Integer.parseInt(f[3]);
                                temp.add(new Product(id, name, price, qty));
                            } else {
                                System.err.println("Bad PRODUCT format: " + line);
                            }
                        } else {
                            System.err.println("Unexpected line: " + line);
                        }
                    }

                    return temp;
                }
            }
        };

        // 4) on success, update the ObservableList on the FX thread
        task.setOnSucceeded(evt -> {
            ObservableList<Product> newList = task.getValue();
            System.out.println("→ Updating table with " + newList.size() + " items");
            products.setAll(newList);
        });
        task.setOnFailed(evt -> {
            Throwable ex = task.getException();
            ex.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Failed to load products:\n" + ex.getMessage());
        });

        new Thread(task).start();
    }



    /**
     * Sends a one‐line command, reads the one‐line response, then calls handler on FX thread.
     */
    private void sendCommand(String cmd, Consumer<String> handler) {
    	  Task<String> task = new Task<>() {
    	    @Override protected String call() {
    	      synchronized (networkLock) {
    	        out.println(cmd);
    	        try {
    	          return in.readLine();
    	        } catch (IOException e) {
    	          return null;
    	        }
    	      }
    	    }
    	    @Override protected void succeeded() {
    	      handler.accept(getValue());
    	    }
    	    @Override protected void failed() {
    	      handler.accept(null);
    	    }
    	  };
    	  new Thread(task).start();
    	}


    private void showAlert(Alert.AlertType type, String msg) {
        Platform.runLater(() -> {
            Alert a = new Alert(type, msg, ButtonType.OK);
            a.showAndWait();
        });
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        if (socket != null && !socket.isClosed()) {
            out.println(Protocol.CMD_CLOSE);
            socket.close();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
