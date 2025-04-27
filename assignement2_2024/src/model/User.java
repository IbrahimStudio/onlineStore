package model;

import java.util.Objects;
import java.util.logging.Logger;

/**
 * Represents a user with a username and password.
 */
public class User {
    private static final Logger logger = Logger.getLogger(User.class.getName());

    private final String username;
    private final String password;

    /**
     * Constructs a new User.
     *
     * @param username unique username (non-null)
     * @param password password (non-null)
     * @throws NullPointerException if username or password is null
     */
    public User(String username, String password) {
        this.username = Objects.requireNonNull(username, "username cannot be null");
        this.password = Objects.requireNonNull(password, "password cannot be null");
    }

    public String getUsername() {
        return username;
    }

    /**
     * Validates a login attempt.
     *
     * @param attempt the password to check
     * @return true if the password matches; false otherwise
     */
    public boolean authenticate(String attempt) {
        boolean ok = this.password.equals(attempt);
        if (!ok) {
            logger.warning("Authentication failed for user: " + username);
        }
        return ok;
    }
}
