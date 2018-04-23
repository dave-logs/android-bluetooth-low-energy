package trujillo.david.bluetooth.interfaces;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;

import trujillo.david.bluetooth.exceptions.BleException;
import trujillo.david.bluetooth.models.ScanResult;

public abstract class BleGattCallback extends BluetoothGattCallback {

    public abstract void onNotFoundDevice();

    public abstract void onFoundDevice(ScanResult scanResult);

    public abstract void onConnectSuccess(BluetoothGatt gatt, int status);

    public abstract void onConnectFailure(BleException exception);

}