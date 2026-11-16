package dev.windex.fusion;

import dev.windex.fusion.analysis.LightCorrelator;
import dev.windex.fusion.analysis.Markers;
import dev.windex.fusion.analysis.ParticleCorrelator;
import dev.windex.fusion.analysis.WorldAnalyzer;
import dev.windex.fusion.gui.FusionScreen;
import dev.windex.fusion.gui.PvpHud;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.util.hit.EntityHitResult;
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
    private int totemStage;
    private int totemSlot=-1;
    private int totemStageTick;
    private InventoryScreen autoOpenedInventory;

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
        HudRenderCallback.EVENT.register((ctx, delta) -> PvpHud.render(ctx, this));
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
        tickAutoTotem(client);
        if (config.on(Module.TRIGGER_BOT) && client.currentScreen == null
                && client.crosshairTarget instanceof EntityHitResult result
                && result.getEntity() instanceof PlayerEntity target
                && target != client.player && !target.isSpectator()
                && client.player.getAttackCooldownProgress(0f) >= 0.98f
                && client.interactionManager != null) {
            client.interactionManager.attackEntity(client.player,target);
            client.player.swingHand(net.minecraft.util.Hand.MAIN_HAND);
        }
        ++ticks;
        if (ticks % 40 == 0) {
            markers.purge();
            particles.prune(System.currentTimeMillis(), config.spawnerWindowSeconds*1000L);
            lights.prune(System.currentTimeMillis(), config.lightWindowSeconds*1000L);
        }
    }

    /** Visible inventory-based totem refill, never hijacking an already open GUI. */
    private void tickAutoTotem(MinecraftClient client) {
        if (!config.on(Module.AUTO_TOTEM) || client.player == null
                || client.interactionManager == null) {
            endAutoTotem(client);
            return;
        }
        if (totemStage > 0 && client.currentScreen != autoOpenedInventory) {
            // The player switched screens; leave their UI alone.
            totemStage=0;
            autoOpenedInventory=null;
            return;
        }
        if (totemStage == 1) {
            if (++totemStageTick < config.autoTotemDelayTicks) return;
            client.interactionManager.clickSlot(client.player.playerScreenHandler.syncId,
                    totemSlot,40,SlotActionType.SWAP,client.player);
            totemStage=2;totemStageTick=0;
            return;
        }
        if (totemStage == 2) {
            if (++totemStageTick >= 2) endAutoTotem(client);
            return;
        }
        if (client.currentScreen != null || client.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) return;
        for (int i=0;i<36;i++) {
            if (!client.player.getInventory().getStack(i).isOf(Items.TOTEM_OF_UNDYING)) continue;
            totemSlot = i < 9 ? 36 + i : i;
            autoOpenedInventory = new InventoryScreen(client.player);
            client.setScreen(autoOpenedInventory);
            totemStage=1;totemStageTick=0;
            return;
        }
    }

    private void endAutoTotem(MinecraftClient client) {
        if (autoOpenedInventory != null && client.currentScreen == autoOpenedInventory) {
            client.setScreen(null);
        }
        totemStage=0;totemStageTick=0;totemSlot=-1;autoOpenedInventory=null;
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
