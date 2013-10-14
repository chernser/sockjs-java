package sockjs;

public interface ConnectionListener {

    void onOpen();

    void onClose();

    void onMessage();
}
