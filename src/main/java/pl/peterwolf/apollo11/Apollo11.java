package pl.peterwolf.apollo11;

import net.fabricmc.api.ModInitializer;
import pl.peterwolf.apollo11.launch.LaunchSequence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Apollo11 implements ModInitializer {
    public static final String MOD_ID = "peterwolfs_apollo11";
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ModBlocks.initialize();
        ModItems.initialize();
        LaunchSequence.initialize();
        LOGGER.info("Preparing the Apollo 11 lunar mission.");
    }
}
