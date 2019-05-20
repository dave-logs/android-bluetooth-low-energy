/*
Copyright 2018 David Trujillo

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

Author: David Trujillo
Description: BT Module for IOT
Web: https://github.com/david-trujillo/android-bluetooth-low-energy

gradlew install
gradlew bintrayUpload
 */

package trujillo.david.bluetoothle;


import android.app.Application;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;

import java.util.ArrayList;
import java.util.List;

import trujillo.david.bluetoothle.exceptions.BleException;
import trujillo.david.bluetoothle.interfaces.BleCharacterCallback;
import trujillo.david.bluetoothle.interfaces.BleGattCallback;
import trujillo.david.bluetoothle.interfaces.ConnectCallback;
import trujillo.david.bluetoothle.interfaces.ScanCallback;
import trujillo.david.bluetoothle.interfaces.ServiceCallback;
import trujillo.david.bluetoothle.models.ScanResult;
import trujillo.david.bluetoothle.utils.HexUtil;


public class Bluetooth {

    private String uuidNotify = "0000fff7-0000-1000-8000-00805f9b34fb";
    private String uuidRead = "00002902-0000-1000-8000-00805f9b34fb";
    private String uuidWrite = "0000fff6-0000-1000-8000-00805f9b34fb";
    private String uuidService = "0000fff0-0000-1000-8000-00805f9b34fb";

    public int delayConnection = 250;

    private BluetoothService bluetoothService;

    private ScanCallback scanResults;
    private ConnectCallback connectCallback;
    private ServiceCallback externalServiceCallback;

    private Application application;
    private ReaderManager readerManager;

    public Bluetooth(Application application, Boolean disconnect, String service, String notify, String read, String write) {
        this.uuidNotify = notify;
        this.uuidRead = read;
        this.uuidService = service;
        this.uuidWrite = write;
        this.application = application;

        if (disconnect) {
            disconnect();
        }

        if (bluetoothService == null) {
            bindService();
        }
    }

    public Bluetooth(Application application, Boolean disconnect) {
        this.application = application;

        if (disconnect) {
            disconnect();
        }

        if (bluetoothService == null) {
            bindService();
        }
    }

    public void setReaderManager(ReaderManager readerManager) {
        this.readerManager = readerManager;
    }

    public void setDelayConnection(int delayConnection) {
        this.delayConnection = delayConnection;
    }

    public void setBluetoothService(BluetoothService bluetoothService) {
        this.bluetoothService = bluetoothService;
    }

    public void scanDevices() {
        if (bluetoothService != null) {
            bluetoothService.scanDevice();
        }
    }

    public void setServiceCallback(ServiceCallback externalServiceCallback) {
        this.externalServiceCallback = externalServiceCallback;
    }

    public void stopScan() {
        if (bluetoothService != null) {
            bluetoothService.stopScan();
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

    public BluetoothGatt getGatt() {
        return bluetoothService.getGatt();
    }


    public BluetoothManager getBluetoothManager() {
        return (BluetoothManager) this.application.getSystemService(Context.BLUETOOTH_SERVICE);
    }

    public List<BluetoothDevice> getConnectedDevices() {
        BluetoothManager bluetoothManager = getBluetoothManager();
        if (bluetoothService == null || getGatt() == null || bluetoothManager == null) {
            return new ArrayList<>();
        }
        return getBluetoothManager().getConnectedDevices(BluetoothProfile.GATT);
    }

    public Boolean isDeviceConnected(String name) {
        List<BluetoothDevice> devicesConnected = getConnectedDevices();
        for (BluetoothDevice device : devicesConnected) {
            String deviceName = device.getName();
            if (deviceName != null && deviceName.equals(name)) {
                return true;
            }
        }
        return false;
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
            if (externalServiceCallback != null) {
                externalServiceCallback.onConnected();
            }
        }

        @Override
        public void onDisConnected() {
            if (externalServiceCallback != null) {
                externalServiceCallback.onDisConnected();
            }
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
            bluetoothService.write(uuidService, uuidWrite, hex, bleCharacterCallback);
        }
    }

    private void notifyData() {
        bluetoothService.notify(
                uuidService,
                uuidNotify,
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
                if (delayConnection > 0) {
                    Handler handler = new Handler();
                    Runnable runnable = new Runnable() {
                        public void run() {
                            connectCallback.onConnected();
                        }
                    };
                    handler.postDelayed(runnable, delayConnection);
                } else {
                    connectCallback.onConnected();
                }
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
