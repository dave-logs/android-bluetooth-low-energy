package trujillo.david.bluetoothle.exceptions;

public class OtherException extends BleException {
    public OtherException(String description) {
        super(GATT_CODE_OTHER, description);
    }
}