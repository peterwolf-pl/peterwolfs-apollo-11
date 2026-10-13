package pl.peterwolf.apollo11.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import pl.peterwolf.apollo11.Apollo11;
import pl.peterwolf.apollo11.network.FlightStartPayload;
import pl.peterwolf.apollo11.world.MoonDimension;

public final class FlightWindowHud {
    private static final int FLIGHT_DURATION_TICKS = 100;
    private static final Identifier HUD_ID = Identifier.fromNamespaceAndPath(Apollo11.MOD_ID, "flight_window");
    private static final Identifier EARTH_SPRITE = Identifier.fromNamespaceAndPath(Apollo11.MOD_ID, "earth_globe");
    private static final Identifier COCKPIT_SPRITE = Identifier.fromNamespaceAndPath(Apollo11.MOD_ID, "cockpit_panel");
    private static final int STAR_COUNT = 42;
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
        int width = client.getWindow().getGuiScaledWidth();
        int height = client.getWindow().getGuiScaledHeight();
        int centerX = width / 2;
        int centerY = height / 2;
        float progress = Math.min(1.0F, elapsedTicks / (float) FLIGHT_DURATION_TICKS);
        float panelScale = Math.max(width / 1024.0F, height / 576.0F);
        int panelWidth = Math.round(1024 * panelScale);
        int panelHeight = Math.round(576 * panelScale);
        int panelLeft = centerX - panelWidth / 2;
        int panelTop = centerY - panelHeight / 2;
        float portholeRadius = 99 * panelScale;

        graphics.fill(0, 0, width, height, 0xFF03080D);
        drawStars(graphics, centerX, centerY, portholeRadius);
        drawEarth(graphics, centerX, centerY, portholeRadius, progress);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, COCKPIT_SPRITE,
                panelLeft, panelTop, panelWidth, panelHeight);
        drawFlightReadout(graphics, client, panelLeft, panelTop, panelScale, progress);
    }

    private static void drawStars(GuiGraphicsExtractor graphics, int centerX, int centerY, float portholeRadius) {
        for (int index = 0; index < STAR_COUNT; index++) {
            double angle = index * 2.399963229728653 + elapsedTicks * 0.0015;
            double distance = portholeRadius * 0.88 * Math.sqrt(((index * 37) % STAR_COUNT) / (double) STAR_COUNT);
            int x = centerX + (int) Math.round(Math.cos(angle) * distance);
            int y = centerY + (int) Math.round(Math.sin(angle) * distance);
            int size = index % 7 == 0 ? 2 : 1;
            int color = index % 4 == 0 ? 0xFFD3EEFA : 0xFF8199A5;
            graphics.fill(x, y, x + size, y + size, color);
        }
    }

    private static void drawEarth(GuiGraphicsExtractor graphics, int centerX, int centerY,
                                  float portholeRadius, float progress) {
        int globeSize = Math.max(16, (int) (portholeRadius * 1.72F * (1.0F - progress) + 16));
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, EARTH_SPRITE,
                centerX - globeSize / 2, centerY - globeSize / 2, globeSize, globeSize);
    }

    private static void drawFlightReadout(GuiGraphicsExtractor graphics, Minecraft client, int panelLeft,
                                          int panelTop, float panelScale, float progress) {
        var pose = graphics.pose();
        pose.pushMatrix();
        pose.translate(panelLeft, panelTop);
        pose.scale(panelScale, panelScale);

        graphics.fill(756, 51, 892, 112, 0xFF07100A);
        graphics.text(client.font, "PROG   VERB   NOUN", 761, 54, 0xFF83A878);
        graphics.text(client.font, "00     --     --", 761, 66, 0xFFA9E594);
        graphics.text(client.font, "COAST   T+" + String.format("%02d", Math.min(5, elapsedTicks / 20)),
                761, 80, 0xFFB5EBA0);
        int earthRange = Math.round(384_400 * progress);
        graphics.text(client.font, String.format("RNG %06d KM", earthRange), 761, 94, 0xFFB5EBA0);
        graphics.fill(761, 106, 887, 109, 0xFF283B2B);
        graphics.fill(761, 106, 761 + (int) (126 * progress), 109, 0xFF9BD785);

        pose.popMatrix();
    }
}
