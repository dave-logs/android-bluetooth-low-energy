package trujillo.david.bluetoothle;

import android.os.Handler;
import android.os.Looper;

public class ReaderManager {

    private Bluetooth bluetooth;

    public ReaderManager(Bluetooth bluetooth) {
        this.bluetooth = bluetooth;
        bluetooth.setReaderManager(this);
    }

    public void reader(byte[] data) {

    }

    protected void write(byte[] data) {
        this.bluetooth.writeData(data);
    }

    protected void processData(final byte[] data) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                reader(data);
            }
        });
    }

}
