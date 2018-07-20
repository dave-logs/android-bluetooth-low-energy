package trujillo.david.bluetoothle;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import trujillo.david.bluetoothle.exceptions.BleException;
import trujillo.david.bluetoothle.interfaces.BleCharacterCallback;
import trujillo.david.bluetoothle.interfaces.BleGattCallback;
import trujillo.david.bluetoothle.interfaces.ConnectCallback;
import trujillo.david.bluetoothle.interfaces.ListScanCallback;
import trujillo.david.bluetoothle.models.ScanResult;
import trujillo.david.bluetoothle.utils.HexUtil;


public class BluetoothService extends Service {

    public BluetoothBinder mBinder = new BluetoothBinder();
    private BleManager bleManager;
    private Handler threadHandler = new Handler(Looper.getMainLooper());
    private Callback mCallback = null;

    private static long TIME_OUT = 5000;

    private String name;
    private String mac;
    private BluetoothGatt gatt;
    private BluetoothGattService service;
    private BluetoothGattCharacteristic characteristic;

    @Override
    public void onCreate() {
        bleManager = new BleManager(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        bleManager = null;
        mCallback = null;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        bleManager.closeBluetoothGatt();
        return super.onUnbind(intent);
    }

    public class BluetoothBinder extends Binder {
        public BluetoothService getService() {
            return BluetoothService.this;
        }
    }

    public void setScanCallback(Callback callback) {
        mCallback = callback;
    }

    public void setTimeOut(long timeOut) {
        TIME_OUT = timeOut;
    }

    public interface Callback {

        void onStartScan();

        void onScanning(ScanResult scanResult);

        void onScanComplete();

        void onConnecting();

        void onConnectFail();

        void onDisConnected();

        void onServicesDiscovered();
    }

    public void scanDevice() {
        resetInfo();

        if (mCallback != null) {
            mCallback.onStartScan();
        }

        if (bleManager != null) {
            boolean b = bleManager.scanDevice(new ListScanCallback(TIME_OUT) {

                @Override
                public void onScanning(final ScanResult result) {
                    runOnMainThread(new Runnable() {
                        @Override
                        public void run() {
                            if (mCallback != null) {
                                mCallback.onScanning(result);
                            }
                        }
                    });
                }

                @Override
                public void onScanComplete(final ScanResult[] results) {
                    runOnMainThread(new Runnable() {
                        @Override
                        public void run() {
                            if (mCallback != null) {
                                mCallback.onScanComplete();
                            }
                        }
                    });
                }
            });
            if (!b) {
                if (mCallback != null) {
                    mCallback.onScanComplete();
                }
            }
        }
    }

    public void scanAndConnectDevice(String name, boolean autoConnect, ConnectCallback connectCallback) {
        resetInfo();

        if (mCallback != null) {
            mCallback.onStartScan();
        }

        try {
            boolean b = bleManager.scanNameAndConnect(name, TIME_OUT, autoConnect, bleGattCallbackByName);
        } catch (Exception e) {
            if (bleManager == null) {
                bleManager = new BleManager(this);
                boolean b = bleManager.scanNameAndConnect(name, TIME_OUT, autoConnect, bleGattCallbackByName);
            }
        }
    }

    public void scanAndConnectDeviceDFU(String name, Long timeout, BleGattCallback bleGattCallbackByName) {
        resetInfo();

        if (mCallback != null) {
            mCallback.onStartScan();
        }
        boolean b = bleManager.scanNameAndConnect(name, timeout, true, bleGattCallbackByName);
    }

    public void scanAndConnectDeviceMac(String mac, ConnectCallback connectCallback) {
        resetInfo();
        if (mCallback != null) {
            mCallback.onStartScan();
        }

        boolean b = bleManager.scanMacAndConnect(mac, TIME_OUT, true, bleGattCallbackByName);
    }

    public void cancelScan() {
        if (bleManager != null) {
            bleManager.cancelScan();
        }
    }

    public void connectDevice(final ScanResult scanResult, final Boolean autoConnect) {
        if (mCallback != null) {
            mCallback.onConnecting();
        }

        if (bleManager.isConnectingOrConnected()) {
            bleManager.closeBluetoothGatt();
        }

        bleManager.connectDevice(scanResult, autoConnect, bleGattCallback);
    }

    BleGattCallback bleGattCallback = new BleGattCallback() {
        @Override
        public void onNotFoundDevice() {
            runOnMainThread(new Runnable() {
                @Override
                public void run() {
                    if (mCallback != null) {
                        mCallback.onConnectFail();
                    }
                }
            });
        }

        @Override
        public void onFoundDevice(ScanResult scanResult) {
            BluetoothService.this.name = scanResult.getDevice().getName();
            BluetoothService.this.mac = scanResult.getDevice().getAddress();
        }

        @Override
        public void onConnectSuccess(BluetoothGatt gatt, int status) {
            gatt.discoverServices();
        }

        @Override
        public void onServicesDiscovered(final BluetoothGatt gatt, int status) {
            BluetoothService.this.gatt = gatt;
            runOnMainThread(new Runnable() {
                @Override
                public void run() {
                    if (mCallback != null) {
                        mCallback.onServicesDiscovered();
                    }
                }
            });
        }

        @Override
        public void onConnectFailure(BleException exception) {
            Log.d("FAIL CONNECT", "TRUE");

            runOnMainThread(new Runnable() {
                @Override
                public void run() {
                    if (mCallback != null) {
                        mCallback.onConnectFail();
                        mCallback.onDisConnected();
                    }
                }
            });
        }
    };

    BleGattCallback bleGattCallbackByName = new BleGattCallback() {
        @Override
        public void onNotFoundDevice() {
            runOnMainThread(new Runnable() {
                @Override
                public void run() {
                    if (mCallback != null) {
                        mCallback.onConnectFail();
                    }
                }
            });
        }

        @Override
        public void onFoundDevice(ScanResult scanResult) {
            BluetoothService.this.name = scanResult.getDevice().getName();
            BluetoothService.this.mac = scanResult.getDevice().getAddress();
        }

        @Override
        public void onConnectSuccess(BluetoothGatt gatt, int status) {
            gatt.discoverServices();
        }

        @Override
        public void onServicesDiscovered(final BluetoothGatt gatt, int status) {
            BluetoothService.this.gatt = gatt;
            runOnMainThread(new Runnable() {
                @Override
                public void run() {
                    if (mCallback != null) {
                        mCallback.onServicesDiscovered();
                    }
                }
            });
        }

        @Override
        public void onConnectFailure(BleException exception) {
            runOnMainThread(new Runnable() {
                @Override
                public void run() {
                    if (mCallback != null) {
                        mCallback.onConnectFail();
                        mCallback.onDisConnected();
                    }
                }
            });
        }
    };

    public void read(String uuid_service, String uuid_read, BleCharacterCallback callback) {
        bleManager.readDevice(uuid_service, uuid_read, callback);
    }

    public void write(String uuid_service, String uuid_write, String hex, BleCharacterCallback callback) {
        if (bleManager != null) {
            bleManager.writeDevice(uuid_service, uuid_write, HexUtil.hexStringToBytes(hex), callback);
        }
    }

    public void notify(String uuid_service, String uuid_notify, BleCharacterCallback callback) {
        if (bleManager != null) {
            bleManager.notify(uuid_service, uuid_notify, callback);
        }
    }

    public void indicate(String uuid_service, String uuid_indicate, BleCharacterCallback callback) {
        bleManager.indicate(uuid_service, uuid_indicate, callback);
    }

    public void stopNotify(String uuid_service, String uuid_notify) {
        bleManager.stopNotify(uuid_service, uuid_notify);
    }

    public void stopIndicate(String uuid_service, String uuid_indicate) {
        bleManager.stopIndicate(uuid_service, uuid_indicate);
    }

    public void closeConnect() {
        if (bleManager != null) {
            bleManager.closeBluetoothGatt();
        }
        if (mCallback != null) {
            mCallback.onDisConnected();
        }
    }


    public void enableBluetooth() {
        bleManager.enableBluetooth();
    }

    private void resetInfo() {
        name = null;
        mac = null;
        gatt = null;
        service = null;
        characteristic = null;
    }

    public String getName() {
        return name;
    }

    public String getMac() {
        return mac;
    }

    public BluetoothGatt getGatt() {
        return gatt;
    }

    public BluetoothAdapter getAdapter() {
        return bleManager.getAdapter();
    }

    public void setService(BluetoothGattService service) {
        this.service = service;
    }

    public BluetoothGattService getService() {
        return service;
    }

    public void setCharacteristic(BluetoothGattCharacteristic characteristic) {
        this.characteristic = characteristic;
    }

    public BluetoothGattCharacteristic getCharacteristic() {
        return characteristic;
    }

    public void refreshDeviceCache() {
        try {
            if (bleManager != null) {
                bleManager.refreshDeviceCache();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void runOnMainThread(Runnable runnable) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            runnable.run();
        } else {
            threadHandler.post(runnable);
        }
    }

}
