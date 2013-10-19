package sockjs;

public interface ConnectionListener {

    void onOpen(Connection connection);

    void onClose(Connection connection);

    void onMessage(Connection connection, String message);
}
