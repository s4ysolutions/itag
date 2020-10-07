package s4y.itag.ble;

import solutions.s4y.rasat.Observable;

interface BLEPeripheralObservablesInterface {
    class ConnectedEvent {
    }

    class ConnectionFailedEvent {
        final int status;

        ConnectionFailedEvent(int status) {
            this.status = status;
        }
    }

    class DisconnectedEvent {
        final int status;

        DisconnectedEvent(int status) {
            this.status = status;
        }
    }

    class DiscoveredServicesEvent {
        final BLEService[] services;
        final int status;

        DiscoveredServicesEvent(BLEService[] services, int status) {
            this.services = services;
            this.status = status;
        }
    }

    class CharacteristicEvent {
        final BLECharacteristic characteristic;
        final int status;

        CharacteristicEvent(BLECharacteristic characteristic, int status) {
            this.characteristic = characteristic;
            this.status = status;
        }
    }

    class RSSIEvent {
        public final int rssi;
        public final int status;

        RSSIEvent(int rssi, int status) {
            this.rssi = rssi;
            this.status = status;
        }
    }

    Observable<ConnectedEvent> observableConnected();
    Observable<ConnectionFailedEvent> observableConnectionFailed();
    Observable<DisconnectedEvent> observableDisconnected();
    Observable<DiscoveredServicesEvent> observableDiscoveredServices();
    Observable<CharacteristicEvent> observableWrite();
    Observable<BLECharacteristic> observableNotification();
    Observable<RSSIEvent> observableRSSI();
}
