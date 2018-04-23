package trujillo.david.bluetoothle.exceptions;

public class InitiatedException extends BleException {
    public InitiatedException() {
        super(ERROR_CODE_INITIAL, "Initiated Exception Occurred! ");
    }
}
