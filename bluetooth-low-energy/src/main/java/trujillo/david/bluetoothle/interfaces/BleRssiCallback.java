package trujillo.david.bluetoothle.interfaces;

public abstract class BleRssiCallback extends BleCallback {
    public abstract void onSuccess(int rssi);
}