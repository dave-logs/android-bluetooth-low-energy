package trujillo.david.bluetoothle;


import android.app.Application;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import trujillo.david.bluetoothle.exceptions.BleException;
import trujillo.david.bluetoothle.interfaces.BleCharacterCallback;
import trujillo.david.bluetoothle.interfaces.BleGattCallback;
import trujillo.david.bluetoothle.interfaces.ConnectCallback;
import trujillo.david.bluetoothle.interfaces.ScanCallback;
import trujillo.david.bluetoothle.interfaces.ServiceCallback;
import trujillo.david.bluetoothle.models.ScanResult;
import trujillo.david.bluetoothle.utils.HexUtil;


public class Bluetooth {

    private static final String UUID_NOTIFY = "0000fff7-0000-1000-8000-00805f9b34fb";
    private static final String UUID_READ = "00002902-0000-1000-8000-00805f9b34fb";
    private static final String UUID_WRITE = "0000fff6-0000-1000-8000-00805f9b34fb";
    private static final String UUID_SERVICE = "0000fff0-0000-1000-8000-00805f9b34fb";

    private BluetoothService bluetoothService;

    private ScanCallback scanResults;
    private ConnectCallback connectCallback;

    private Application application;
    private ReaderManager readerManager;

    public Bluetooth(Application application) {
        this.application = application;

        disconnect();

        if (bluetoothService == null) {
            bindService();
        }
    }

    public void setReaderManager(ReaderManager readerManager) {
        this.readerManager = readerManager;
    }

    public void setBluetoothService(BluetoothService bluetoothService) {
        this.bluetoothService = bluetoothService;
    }

    public void scanDevices() {
        if (bluetoothService != null) {
            bluetoothService.scanDevice();
        }
    }

    public void stopScan() {
        try {
            bluetoothService.cancelScan();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setScanResults(ScanCallback scanResults) {
        this.scanResults = scanResults;
    }

    public void setConnectCallback(ConnectCallback connectCallback) {
        this.connectCallback = connectCallback;
    }

    public ConnectCallback getConnectCallback() {
        return connectCallback;
    }

    public void setTimeOut(long timeOut) {
        if (bluetoothService != null) {
            bluetoothService.setTimeOut(timeOut);
        }
    }

    public BluetoothService getBluetoothService() {
        return bluetoothService;
    }

    public void connect(ScanResult scanResult, Boolean cancel, Boolean autoConnect) {
        if (cancel) {
            stopScan();
        }

        refreshDeviceCache();
        disconnect();
        bluetoothService.connectDevice(scanResult, autoConnect);
    }

    public void disconnect() {
        if (bluetoothService != null) {
            if (bluetoothService.getAdapter() != null) {
                bluetoothService.getAdapter().cancelDiscovery();
            }

            if (bluetoothService.getGatt() != null) {
                bluetoothService.getGatt().abortReliableWrite();
            }

            bluetoothService.closeConnect();
        }
    }

    private ServiceCallback serviceCallback = new ServiceCallback() {
        @Override
        public void onConnected() {
            setConnectCallback(connectCallback);
        }

        @Override
        public void onDisConnected() {
        }
    };

    public void scanAndConnectDFU(String name, Long timeout, BleGattCallback bleGattCallbackByName) {
        bluetoothService.scanAndConnectDeviceDFU(name, timeout, bleGattCallbackByName);
    }

    public void scanAndConnect(String name, ConnectCallback connectCallback) {
        bluetoothService.scanAndConnectDevice(name, false, connectCallback);
    }

    public void scanAndConnectMac(String mac, ConnectCallback connectCallback) {
        bluetoothService.scanAndConnectDeviceMac(mac, connectCallback);
    }

    public void writeData(byte[] value) {
        String hex = "";
        for (byte b : value) {
            hex += HexUtil.decToHexTwo(b);
        }

        BleCharacterCallback bleCharacterCallback = new BleCharacterCallback() {
            @Override
            public void onSuccess(BluetoothGattCharacteristic characteristic) {

            }

            @Override
            public void onFailure(BleException exception) {

            }
        };

        if (!hex.isEmpty()) {
            bluetoothService.write(UUID_SERVICE, UUID_WRITE, hex, bleCharacterCallback);
        }
    }

    private void notifyData() {
        bluetoothService.notify(
                UUID_SERVICE,
                UUID_NOTIFY,
                new BleCharacterCallback() {
                    @Override
                    public void onFailure(final BleException exception) {

                    }

                    @Override
                    public void onSuccess(final BluetoothGattCharacteristic characteristic) {
                        if (readerManager != null) {
                            readerManager.processData(characteristic.getValue());
                        }
                    }
                });
    }

    public void readData() {

    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            bluetoothService = ((BluetoothService.BluetoothBinder) service).getService();
            bluetoothService.setScanCallback(callback);
            if (serviceCallback != null) {
                enableBluetooth();
                serviceCallback.onConnected();
                notifyData();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            bluetoothService = null;
            if (serviceCallback != null) {
                serviceCallback.onDisConnected();
            }
        }
    };

    private BluetoothService.Callback callback = new BluetoothService.Callback() {
        @Override
        public void onStartScan() {
            if (scanResults != null) {
                scanResults.onStartScan();
            }
        }

        @Override
        public void onScanning(ScanResult scanResult) {
            if (scanResults != null && scanResult.getDevice().getName() != null) {
                scanResults.onScanning(scanResult);
            }
        }

        @Override
        public void onScanComplete() {
            if (scanResults != null) {
                scanResults.onScanComplete();
            }
        }

        @Override
        public void onConnecting() {
            if (connectCallback != null) {
                connectCallback.onConnecting();
            }
        }

        @Override
        public void onConnectFail() {
            if (connectCallback != null) {
                connectCallback.onConnectFail();
            }
        }

        @Override
        public void onDisConnected() {
            if (connectCallback != null) {
                connectCallback.onDisConnected();
            }
        }

        @Override
        public void onServicesDiscovered() {
            notifyData();
            if (connectCallback != null) {
                connectCallback.onConnected();
            }
        }
    };

    private void enableBluetooth() {
        bluetoothService.enableBluetooth();
    }

    private void bindService() {
        Intent bindIntent = new Intent(application, BluetoothService.class);
        application.bindService(bindIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void unbindService() {
        try {
            application.unbindService(serviceConnection);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void refreshDeviceCache() {
        if (bluetoothService != null) {
            bluetoothService.refreshDeviceCache();
        }
    }

    public void destroy() {
        if (bluetoothService != null)
            unbindService();
    }

}