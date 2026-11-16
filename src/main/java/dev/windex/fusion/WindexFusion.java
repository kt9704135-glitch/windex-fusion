package dev.windex.fusion;

import dev.windex.fusion.analysis.LightCorrelator;
import dev.windex.fusion.analysis.Markers;
import dev.windex.fusion.analysis.ParticleCorrelator;
import dev.windex.fusion.analysis.WorldAnalyzer;
import dev.windex.fusion.gui.FusionScreen;
import dev.windex.fusion.gui.RadarHud;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class WindexFusion implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("WindexFusion");
    public static WindexFusion INSTANCE;

    public Config config;
    public final Markers markers = new Markers();
    private final ParticleCorrelator particles = new ParticleCorrelator();
    private final LightCorrelator lights = new LightCorrelator();
    private final WorldAnalyzer analyzer = new WorldAnalyzer(markers);
    private ClientWorld lastWorld;
    private long worldEnteredAt;
    private int ticks;

    @Override
    public void onInitializeClient() {
        INSTANCE = this;
        config = Config.load();
        KeyBinding openGui = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.windexfusion.gui", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT,
                KeyBinding.Category.MISC));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openGui.wasPressed())
                client.setScreen(client.currentScreen instanceof FusionScreen ? null : new FusionScreen());
            tick(client);
        });
        HudRenderCallback.EVENT.register((ctx, delta) -> RadarHud.render(ctx, this));
        LOGGER.info("Windex Fusion 1.21.11: {} profile active", config.preset);
    }

    private void tick(MinecraftClient client) {
        if (lastWorld != client.world) {
            markers.clear(); particles.clear(); lights.clear(); analyzer.clear();
            lastWorld = client.world;
            worldEnteredAt = System.currentTimeMillis();
        }
        if (client.world == null || client.player == null) return;
        if (config.on(Module.AUTO_SPRINT) && client.options.forwardKey.isPressed()
                && !client.player.isSneaking() && !client.player.isSprinting()) {
            client.player.setSprinting(true);
        }
        if (config.on(Module.AUTO_TOTEM) && ticks % config.autoTotemDelayTicks == 0
                && client.interactionManager != null && client.currentScreen == null
                && !client.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) {
            for (int i=0;i<36;i++) {
                if (!client.player.getInventory().getStack(i).isOf(Items.TOTEM_OF_UNDYING)) continue;
                int slot = i < 9 ? 36 + i : i;
                client.interactionManager.clickSlot(client.player.playerScreenHandler.syncId,
                        slot,40,SlotActionType.SWAP,client.player);
                break;
            }
        }
        if (config.on(Module.PLAYER)) {
            for (PlayerEntity player : client.world.getPlayers()) {
                if (player == client.player || player.isSpectator()) continue;
                BlockPos p = player.getBlockPos();
                markers.upsert(Markers.Kind.PLAYER, player.getUuidAsString(),
                        p.getX(),p.getY(),p.getZ(),100,
                        "Visible player entity: "+player.getName().getString(), 5000);
            }
        }
        if (++ticks % 2 == 0) analyzer.tick(client, config);
        if (ticks % 40 == 0) {
            markers.purge();
            particles.prune(System.currentTimeMillis(), config.spawnerWindowSeconds*1000L);
            lights.prune(System.currentTimeMillis(), config.lightWindowSeconds*1000L);
        }
    }

    /** Called only when the client particle engine actually receives/spawns a particle. */
    public void onParticle(ParticleEffect effect, double x, double y, double z) {
        if (!config.on(Module.SPAWNER)) return;
        boolean smoke = effect.getType() == ParticleTypes.SMOKE;
        boolean flame = effect.getType() == ParticleTypes.FLAME;
        if (!smoke && !flame) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return;
        BlockPos p = BlockPos.ofFloored(x,y,z);
        if (!client.world.isChunkLoaded(p.getX() >> 4, p.getZ() >> 4)) return;
        Block at = client.world.getBlockState(p).getBlock();
        // Vanilla torches, furnaces, fires and campfires should not be confused with spawners.
        if (at == Blocks.TORCH || at == Blocks.WALL_TORCH || at == Blocks.FIRE
                || at == Blocks.SOUL_FIRE || at == Blocks.CAMPFIRE
                || at == Blocks.SOUL_CAMPFIRE || at == Blocks.FURNACE
                || at == Blocks.BLAST_FURNACE || at == Blocks.SMOKER) return;
        ParticleCorrelator.Evidence hit = particles.note(smoke, x,y,z,
                System.currentTimeMillis(), config.spawnerMinEachParticle,
                config.spawnerWindowSeconds * 1000L);
        if (hit != null) {
            String key = p.toShortString();
            markers.upsert(Markers.Kind.PARTICLE, key, p.getX(),p.getY(),p.getZ(),65,
                    "Smoke "+hit.smoke()+" / flame "+hit.flame()+"; not verified", 90_000);
        }
    }

    /** Light changes are a weak activity hint; they do not reveal or verify a player. */
    public void onLightUpdate(int chunkX, int chunkZ) {
        if (config == null || !config.on(Module.PLAYER)) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) return;
        // Ignore initial lighting traffic after joining/changing dimensions.
        long now = System.currentTimeMillis();
        if (now - worldEnteredAt < 15_000) return;
        if (Math.abs((client.player.getBlockX() >> 4)-chunkX) > 8
                || Math.abs((client.player.getBlockZ() >> 4)-chunkZ) > 8) return;
        int count = lights.note(chunkX,chunkZ,now,config.lightWindowSeconds);
        if (count >= config.lightUpdatesForAlert) {
            int x = (chunkX << 4)+8, z = (chunkZ << 4)+8;
            int y = client.world.getTopY(net.minecraft.world.Heightmap.Type.WORLD_SURFACE,x,z);
            markers.upsert(Markers.Kind.ACTIVITY, chunkX+":"+chunkZ,x,y,z,25,
                    "Repeated light updates ("+count+"); may be unrelated to players", 45_000);
        }
    }
}
