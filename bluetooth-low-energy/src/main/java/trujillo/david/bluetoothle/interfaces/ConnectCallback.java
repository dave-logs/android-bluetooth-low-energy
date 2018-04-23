package trujillo.david.bluetoothle.interfaces;

public interface ConnectCallback {

    void onConnecting();

    void onConnectFail();

    void onDisConnected();

    void onConnected();

}