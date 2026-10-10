package pl.peterwolf.apollo11.client;

import net.fabricmc.api.ClientModInitializer;
import pl.peterwolf.apollo11.client.FlightWindowHud;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Apollo11Client implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("peterwolfs_apollo11");

    @Override
    public void onInitializeClient() {
        FlightWindowHud.initialize();
        LOGGER.info("Apollo 11 client assets are ready.");
    }
}
