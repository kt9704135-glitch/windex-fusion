package dev.windex.fusion.gui;

import dev.windex.fusion.Module;
import dev.windex.fusion.WindexFusion;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerEntity;

/** Compact original PvP target card for known, nearby player entities. */
public final class PvpHud {
    private PvpHud() {}

    public static void render(DrawContext ctx,WindexFusion mod) {
        MinecraftClient mc=MinecraftClient.getInstance();
        if (mc.player==null || mc.world==null || mc.options.hudHidden
                || mc.currentScreen!=null || !mod.config.on(Module.TARGET_HUD)) return;
        PlayerEntity target=null;
        double nearest=15*15;
        for (PlayerEntity player:mc.world.getPlayers()) {
            if(player==mc.player || player.isSpectator())continue;
            double d=mc.player.squaredDistanceTo(player);
            if(d<nearest){nearest=d;target=player;}
        }
        if(target==null)return;
        int x=Math.max(5,mc.getWindow().getScaledWidth()-180),y=18;
        int accent=0xFF000000|mod.config.accentRgb;
        ctx.fill(x-2,y-2,x+172,y+64,0xAA4CBBEE);
        ctx.fill(x,y,x+170,y+62,0xEE0A182A);
        ctx.fill(x,y,x+170,y+2,accent);
        ctx.drawTextWithShadow(mc.textRenderer,"TARGET  /  WINDEX RECODE",x+10,y+8,accent);
        ctx.drawTextWithShadow(mc.textRenderer,target.getName().getString(),x+10,y+24,0xFFFFFFFF);
        float health=Math.max(0,target.getHealth());
        float maxHealth=Math.max(1,target.getMaxHealth());
        ctx.drawTextWithShadow(mc.textRenderer,String.format(java.util.Locale.ROOT,"%.1f HP  |  %.1fm",health,Math.sqrt(nearest)),x+10,y+39,0xFFD4E8F5);
        ctx.fill(x+10,y+53,x+160,y+57,0xFF183043);
        ctx.fill(x+10,y+53,x+10+(int)(150*Math.min(1,health/maxHealth)),y+57,accent);
    }
}
