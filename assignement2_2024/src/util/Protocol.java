package util;

/**
 * Defines the text-based protocol commands and responses exchanged between
 * client and server.
 */
public final class Protocol {
    private Protocol() { /* prevent instantiation */ }

    // Client → Server commands
    public static final String CMD_LOGIN    = "LOGIN";    // LOGIN username password
    public static final String CMD_LIST     = "LIST";     // LIST
    public static final String CMD_PURCHASE = "PURCHASE"; // PURCHASE id
    public static final String CMD_RETURN   = "RETURN";   // RETURN id
    public static final String CMD_ADD      = "ADD";      // ADD id;name;price;quantity
    public static final String CMD_CLOSE    = "CLOSE";    // CLOSE

    // Server → Client responses
    public static final String RESP_AUTH_OK       = "AUTH_OK";
    public static final String RESP_AUTH_FAIL     = "AUTH_FAIL";
    public static final String RESP_LIST_BEGIN    = "LIST_BEGIN";
    public static final String RESP_PRODUCT       = "PRODUCT";       // PRODUCT id;name;price;quantity
    public static final String RESP_LIST_END      = "LIST_END";
    public static final String RESP_PURCHASE_OK   = "PURCHASE_OK";
    public static final String RESP_PURCHASE_FAIL = "PURCHASE_FAIL";
    public static final String RESP_RETURN_OK     = "RETURN_OK";
    public static final String RESP_RETURN_FAIL   = "RETURN_FAIL";
    public static final String RESP_ADD_OK        = "ADD_OK";
    public static final String RESP_ADD_FAIL      = "ADD_FAIL";
    public static final String RESP_CLOSE         = "BYE";
}
