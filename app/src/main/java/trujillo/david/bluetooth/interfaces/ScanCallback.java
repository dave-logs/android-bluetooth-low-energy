package trujillo.david.bluetooth.interfaces;

import trujillo.david.bluetooth.models.ScanResult;

public interface ScanCallback {

    void onStartScan();

    void onScanning(ScanResult scanResult);

    void onScanComplete();
}
