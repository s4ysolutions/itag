package s4y.waytoday.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class GRPCChannelProvider {
    private final boolean tls;
    private final String host;
    private final int port;

    private static GRPCChannelProvider sInstance;

    public ManagedChannel channel() {
        ManagedChannelBuilder channelBuilder = ManagedChannelBuilder
                .forAddress(host, port);
        if (!tls)
            channelBuilder.usePlaintext();
        return channelBuilder.build();
    }

    private GRPCChannelProvider() {
        this.host = "tracker.way.today";
        this.port = 9101;
        // tls = (port % 1000) > 100;
        tls = true;
    }

    public static GRPCChannelProvider getInstance() {
        if (sInstance == null) {
            sInstance = new GRPCChannelProvider();
        }
        return sInstance;
    }
}
