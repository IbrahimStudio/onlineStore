# Online Shop System Documentation

## Overview
This application consists of two parts:
- **Server**: maintains a catalog of products and a list of users; listens for TCP socket connections.
- **Client**: console-based UI; connects to the server, authenticates, and sends commands.

Communication is purely text-based over sockets, following the protocol defined in `Protocol.java`.

---

## Components

### Model Classes
- **Product**  
  - Fields: `id`, `name`, `price`, `quantity` (thread-safe via `AtomicInteger`)  
  - Methods: `purchase()`, `restock()`, `toString()` outputs `id;name;price;quantity`.

- **User**  
  - Fields: `username`, `password`  
  - Method: `authenticate(String attempt)` for login validation.

### Protocol
Commands and responses are simple text tokens. See `Protocol.java` for full constants.  
- **Client→Server**  
  - `LOGIN username password`  
  - `LIST`  
  - `PURCHASE id`  
  - `RETURN id`  
  - `ADD id;name;price;quantity`  
  - `CLOSE`

- **Server→Client**  
  - `AUTH_OK` / `AUTH_FAIL`  
  - `LIST_BEGIN` ... `PRODUCT id;name;price;quantity` ... `LIST_END`  
  - `PURCHASE_OK` / `PURCHASE_FAIL`  
  - `RETURN_OK` / `RETURN_FAIL`  
  - `ADD_OK` / `ADD_FAIL`  
  - `BYE`

---

## Server Workflow
1. **Startup**  
   - Loads `users.txt` into a `ConcurrentHashMap<String,User>`.  
   - Loads `products.txt` into `ConcurrentHashMap<String,Product>`.  
   - Begins listening on a configurable port (default 12345).  
2. **Client Handling**  
   - For each incoming socket, a `ClientHandler` runs in an `ExecutorService`.  
   - **Authentication**: expects `LOGIN`; replies `AUTH_OK` or `AUTH_FAIL`.  
   - **Command Loop**: processes `LIST`, `PURCHASE`, `RETURN`, `ADD`, `CLOSE`.  
   - Graceful shutdown via JVM hook.

### Thread-Safety & Logging
- Shared maps use `ConcurrentHashMap`.  
- Stock updates are atomic.  
- Java’s `Logger` records key events and warnings.

---

## Client Workflow
1. **Connect** to server host/port.
2. **Prompt** for username and password; send `LOGIN`.  
3. On `AUTH_OK`, display menu:
   1. List products  
   2. Purchase a product  
   3. Return a product  
   4. Add a new product  
   5. Close  
4. **Read** server replies and display them.  
5. On `CLOSE`, exit.

## GUI Client (JavaFX)

A JavaFX application (`ClientGUI.java`) provides:
- **Login Screen**: username/password prompt
- **Dashboard**:
  - **TableView** of products (ID, name, price, quantity)
  - **Buttons**:
    - Refresh list
    - Purchase selected
    - Return selected
  - **Add-Product Form**: fields for ID, name, price, quantity + Add button
  - **Logout** button

All network I/O is done on background `Task`s to keep the UI responsive.  
Launch with JavaFX modules on the module-path; see “Build & Run” above.


---

## How to Build & Run
1. **Compile**:
   ```bash
   javac -d out src/com/onlineshop/**/*.java
