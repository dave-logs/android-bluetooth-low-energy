package trujillo.david.bluetoothle.interfaces;

import trujillo.david.bluetoothle.models.ScanResult;

public interface ScanCallback {

    void onStartScan();

    void onScanning(ScanResult scanResult);

    void onScanComplete();
}
