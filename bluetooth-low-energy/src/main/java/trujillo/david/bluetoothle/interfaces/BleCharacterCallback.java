package trujillo.david.bluetoothle.interfaces;

import android.bluetooth.BluetoothGattCharacteristic;

public abstract class BleCharacterCallback extends BleCallback {
    public abstract void onSuccess(BluetoothGattCharacteristic characteristic);
}