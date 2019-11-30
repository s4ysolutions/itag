package s4y.itag.ble;

class BLEException extends Exception {
    private final int status;

    public BLEException(int status) {
        super();
        this.status = status;
    }

    int getStatus() {
        return status;
    }
}
