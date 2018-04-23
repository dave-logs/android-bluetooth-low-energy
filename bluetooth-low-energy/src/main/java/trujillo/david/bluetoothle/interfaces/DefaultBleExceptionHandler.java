package trujillo.david.bluetoothle.interfaces;

import android.content.Context;
import android.util.Log;

import trujillo.david.bluetoothle.exceptions.ConnectException;
import trujillo.david.bluetoothle.exceptions.GattException;
import trujillo.david.bluetoothle.exceptions.InitiatedException;
import trujillo.david.bluetoothle.exceptions.OtherException;
import trujillo.david.bluetoothle.exceptions.TimeoutException;

public class DefaultBleExceptionHandler extends BleExceptionHandler {

    private static final String TAG = "BleExceptionHandler";
    private Context context;

    public DefaultBleExceptionHandler(Context context) {
        this.context = context.getApplicationContext();
    }

    @Override
    protected void onConnectException(ConnectException e) {
        Log.e(TAG, e.getDescription());
    }

    @Override
    protected void onGattException(GattException e) {
        Log.e(TAG, e.getDescription());
    }

    @Override
    protected void onTimeoutException(TimeoutException e) {
        Log.e(TAG, e.getDescription());
    }

    @Override
    protected void onInitiatedException(InitiatedException e) {
        Log.e(TAG, e.getDescription());
    }

    @Override
    protected void onOtherException(OtherException e) {
        Log.e(TAG, e.getDescription());
    }
}
