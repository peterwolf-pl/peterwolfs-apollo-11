package pl.peterwolf.apollo11.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import pl.peterwolf.apollo11.Apollo11;
import pl.peterwolf.apollo11.network.FlightStartPayload;
import pl.peterwolf.apollo11.world.MoonDimension;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class FlightWindowHud {
    private static final int FLIGHT_DURATION_TICKS = 100;
    private static final Identifier HUD_ID = Identifier.fromNamespaceAndPath(Apollo11.MOD_ID, "flight_window");
    private static final Identifier EARTH_SPRITE = Identifier.fromNamespaceAndPath(Apollo11.MOD_ID, "earth_globe");
    private static int elapsedTicks;
    private static boolean active;

    private FlightWindowHud() {
    }

    public static void initialize() {
        ClientPlayNetworking.registerGlobalReceiver(FlightStartPayload.TYPE,
                (payload, context) -> context.client().execute(FlightWindowHud::start));
        ClientTickEvents.END_CLIENT_TICK.register(FlightWindowHud::tick);
        HudElementRegistry.addLast(HUD_ID, FlightWindowHud::extractRenderState);
    }

    public static boolean isActive() {
        return active;
    }

    private static void start() {
        elapsedTicks = 0;
        active = true;
    }

    private static void tick(Minecraft client) {
        if (!active) {
            return;
        }
        if (client.player == null || client.level == null
                || client.player.level().dimension().equals(MoonDimension.KEY)
                || elapsedTicks >= FLIGHT_DURATION_TICKS + 40) {
            active = false;
            return;
        }
        elapsedTicks++;
    }

    private static void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        if (!active) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        int screenWidth = client.getWindow().getGuiScaledWidth();
        int screenHeight = client.getWindow().getGuiScaledHeight();
        int left = screenWidth / 8;
        int right = screenWidth - left;
        int top = screenHeight / 10;
        int bottom = screenHeight - top;
        int centerX = (left + right) / 2;
        int centerY = (top + bottom) / 2;
        float progress = Math.min(1.0F, elapsedTicks / (float) FLIGHT_DURATION_TICKS);

        graphics.fill(0, 0, screenWidth, screenHeight, 0xFF03080D);
        graphics.fill(left - 5, top - 5, right + 5, bottom + 5, 0xFF52636C);
        graphics.fill(left, top, right, bottom, 0xFF07131A);

        for (int i = 0; i < 32; i++) {
            int starX = left + 8 + (i * 67 % Math.max(1, right - left - 16));
            int starY = top + 38 + (i * 43 % Math.max(1, bottom - top - 76));
            int color = i % 4 == 0 ? 0xFFBCEBFF : 0xFF718D9B;
            graphics.fill(starX, starY, starX + 2, starY + 2, color);
        }

        graphics.fill(left + 8, top + 8, right - 8, top + 28, 0xFF132A35);
        graphics.text(client.font, Component.translatable("hud.peterwolfs_apollo11.flight.title"),
                left + 16, top + 14, 0xFFE1F4FA);

        int globeSize = Math.max(18, (int) (132 * (1.0F - progress) + 16));
        int globeLeft = centerX - globeSize / 2;
        int globeTop = centerY - globeSize / 2;
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, EARTH_SPRITE,
                globeLeft, globeTop, globeSize, globeSize);

        int seconds = Math.min(5, elapsedTicks / 20);
        int earthRange = Math.round(384_400 * progress);
        graphics.text(client.font, Component.translatable("hud.peterwolfs_apollo11.flight.range", earthRange),
                left + 16, bottom - 44, 0xFF8FD5E8);
        graphics.text(client.font,
                Component.translatable("hud.peterwolfs_apollo11.flight.timer", String.format("%02d", seconds)),
                left + 16, bottom - 28, 0xFFDAE9ED);

        int barLeft = left + 16;
        int barRight = right - 16;
        graphics.fill(barLeft, bottom - 12, barRight, bottom - 7, 0xFF233842);
        graphics.fill(barLeft, bottom - 12, barLeft + (int) ((barRight - barLeft) * progress),
                bottom - 7, 0xFF57C6E7);
    }
}
