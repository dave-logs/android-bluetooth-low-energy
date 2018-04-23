package trujillo.david.bluetoothle.interfaces;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;

import trujillo.david.bluetoothle.exceptions.BleException;
import trujillo.david.bluetoothle.models.ScanResult;

public abstract class BleGattCallback extends BluetoothGattCallback {

    public abstract void onNotFoundDevice();

    public abstract void onFoundDevice(ScanResult scanResult);

    public abstract void onConnectSuccess(BluetoothGatt gatt, int status);

    public abstract void onConnectFailure(BleException exception);

}