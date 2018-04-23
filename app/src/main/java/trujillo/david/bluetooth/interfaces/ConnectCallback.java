package trujillo.david.bluetooth.interfaces;

public interface ConnectCallback {

    void onConnecting();

    void onConnectFail();

    void onDisConnected();

    void onConnected();

}