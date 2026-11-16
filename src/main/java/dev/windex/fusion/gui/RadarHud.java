package dev.windex.fusion.gui;

import dev.windex.fusion.Config;
import dev.windex.fusion.Module;
import dev.windex.fusion.WindexFusion;
import dev.windex.fusion.analysis.Markers;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.List;

/** Small always-on radar; data is from the loaded client world only. */
public final class RadarHud {
    private RadarHud() {}

    public static void render(DrawContext context, WindexFusion mod) {
        Config config = mod.config;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (config == null || !config.showHud || mc.player == null || mc.world == null
                || mc.currentScreen instanceof FusionScreen || !config.on(Module.DEEPSLATE)) return;

        int left = mc.getWindow().getScaledWidth() - 206;
        if (left < 5) left = 5;
        int top = 9;
        int accent = 0xFF000000 | config.accentRgb;
        List<Markers.Marker> near = mod.markers.nearby(mc.player.getX(),mc.player.getZ(),
                config.renderDistanceBlocks);
        near.removeIf(m -> !enabled(config,m.kind()));

        int rows = Math.min(config.maxHudMarkers,near.size());
        int panelH = 150 + Math.max(1,rows) * 13;
        context.fill(left,top,left+196,top+panelH,0xCE10131F);
        context.fill(left,top,left+196,top+2,accent);
        context.drawTextWithShadow(mc.textRenderer,UiFont.text("WINDEX  /  RECODE"),left+10,top+9,0xFFF2F2FF);
        context.drawTextWithShadow(mc.textRenderer,"Signals "+near.size()+"  |  RSHIFT",left+10,top+22,0xFF94A0BD);

        int rx = left+15, ry = top+38, rw = 166, rh = 99;
        context.fill(rx,ry,rx+rw,ry+rh,0x8823293B);
        context.fill(rx+rw/2,ry+2,rx+rw/2+1,ry+rh-2,0x555D6881);
        context.fill(rx+2,ry+rh/2,rx+rw-2,ry+rh/2+1,0x555D6881);
        context.fill(rx+rw/2-2,ry+rh/2-2,rx+rw/2+3,ry+rh/2+3,0xFFFFFFFF);
        for (Markers.Marker m : near) {
            int dx = m.x() - mc.player.getBlockX();
            int dz = m.z() - mc.player.getBlockZ();
            // Radar north is -Z; no assumed server terrain coordinates.
            int px = rx+rw/2+(int)Math.round(dx*(rw/2.-5)/config.renderDistanceBlocks);
            int py = ry+rh/2+(int)Math.round(dz*(rh/2.-5)/config.renderDistanceBlocks);
            px = Math.max(rx+3, Math.min(rx+rw-3, px));
            py = Math.max(ry+3, Math.min(ry+rh-3, py));
            boolean chunkMarker = m.kind()==Markers.Kind.SUS || m.kind()==Markers.Kind.CAVE
                    || m.kind()==Markers.Kind.TERRAIN;
            if (chunkMarker) {
                int side=Math.max(5,(int)Math.round(16.0*(rw-10)/config.renderDistanceBlocks));
                int square=0xFF000000|config.analyzerRgb;
                context.fill(px-side/2,py-side/2,px+(side+1)/2,py+(side+1)/2,
                        0x66000000|config.analyzerRgb);
                context.fill(px-side/2,py-side/2,px+(side+1)/2,py-side/2+1,square);
                context.fill(px-side/2,py+(side-1)/2,px+(side+1)/2,py+(side+1)/2,square);
            } else {
                context.fill(px-2,py-2,px+3,py+3,0xFF000000|m.kind().rgb);
            }
        }
        int y = top+143;
        if (rows == 0) {
            context.drawTextWithShadow(mc.textRenderer,"No nearby evidence yet",left+10,y,0xFFADB3C9);
        } else {
            for (int i = 0; i < rows; i++) {
                Markers.Marker m = near.get(i);
                int dist = (int)Math.sqrt(m.squaredDistance(mc.player.getX(),mc.player.getZ()));
                String label = m.kind().title+" "+dist+"m  "+m.confidence()+"%";
                if (config.showCoordinates) label += "  "+m.x()+","+m.z();
                // Trim to HUD area; full details are shown in the GUI.
                while (mc.textRenderer.getWidth(label) > 177 && label.length() > 3)
                    label = label.substring(0,label.length()-1);
                context.drawTextWithShadow(mc.textRenderer,label,left+11,y,0xFF000000|m.kind().rgb);
                y += 13;
            }
        }
    }

    private static boolean enabled(Config config, Markers.Kind kind) {
        return switch (kind) {
            case SPAWNER, PARTICLE -> config.on(Module.SPAWNER);
            case PLAYER, ACTIVITY -> config.on(Module.PLAYER);
            case SUS -> config.on(Module.SUS);
            case CAVE -> config.on(Module.CAVE);
            case TERRAIN -> config.on(Module.TERRAIN);
            case STORAGE -> config.on(Module.STORAGE);
        };
    }
}
