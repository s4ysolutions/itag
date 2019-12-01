package s4y.itag.ble;

public class BLEFindMeControlFactoryDefault implements BLEFindMeControlFactoryInterface {
    @Override
    public BLEFindMeControlInterface findMeControll(BLEFindMeInterface findMe) {
        return (BLEFindMeControlInterface)findMe;
    }
}
