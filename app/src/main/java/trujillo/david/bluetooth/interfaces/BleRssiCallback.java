package trujillo.david.bluetooth.interfaces;

public abstract class BleRssiCallback extends BleCallback {
    public abstract void onSuccess(int rssi);
}