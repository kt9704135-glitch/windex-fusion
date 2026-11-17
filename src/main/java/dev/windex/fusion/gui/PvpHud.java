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
        if (mc.player==null || mc.world==null || mc.options.hudHidden || mc.currentScreen!=null) return;
        int accent=0xFF000000|mod.config.accentRgb;
        if (mod.config.on(Module.CROSSHAIR)) drawCrosshair(ctx,mc,accent);
        if (mod.config.on(Module.KEYSTROKES)) drawKeystrokes(ctx,mc,accent);
        if (!mod.config.on(Module.TARGET_HUD)) return;
        PlayerEntity target=null;
        double nearest=15*15;
        for (PlayerEntity player:mc.world.getPlayers()) {
            if(player==mc.player || player.isSpectator())continue;
            double d=mc.player.squaredDistanceTo(player);
            if(d<nearest){nearest=d;target=player;}
        }
        if(target==null)return;
        int x=Math.max(5,mc.getWindow().getScaledWidth()-180),y=18;
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

    private static void drawCrosshair(DrawContext ctx,MinecraftClient mc,int accent) {
        int midX=mc.getWindow().getScaledWidth()/2;
        int midY=mc.getWindow().getScaledHeight()/2;
        ctx.fill(midX-1,midY-8,midX+1,midY-3,accent);
        ctx.fill(midX-1,midY+4,midX+1,midY+9,accent);
        ctx.fill(midX-8,midY-1,midX-3,midY+1,accent);
        ctx.fill(midX+4,midY-1,midX+9,midY+1,accent);
    }

    private static void drawKeystrokes(DrawContext ctx,MinecraftClient mc,int accent) {
        int x=15,y=Math.max(18,mc.getWindow().getScaledHeight()-120);
        key(ctx,mc,"W",x+30,y,mc.options.forwardKey.isPressed(),accent);
        key(ctx,mc,"A",x,y+29,mc.options.leftKey.isPressed(),accent);
        key(ctx,mc,"S",x+30,y+29,mc.options.backKey.isPressed(),accent);
        key(ctx,mc,"D",x+60,y+29,mc.options.rightKey.isPressed(),accent);
        key(ctx,mc,"LMB",x,y+58,mc.options.attackKey.isPressed(),accent);
        key(ctx,mc,"RMB",x+60,y+58,mc.options.useKey.isPressed(),accent);
    }

    private static void key(DrawContext ctx,MinecraftClient mc,String label,int x,int y,boolean down,int accent) {
        int box=down?accent:0xD414283A;
        int width=label.length()>1?58:28;
        ctx.fill(x,y,x+width,y+27,box);
        ctx.fill(x+1,y+1,x+width-1,y+26,down?0xFF1C5773:0xE40A1B2C);
        ctx.drawCenteredTextWithShadow(mc.textRenderer,label,x+width/2,y+9,down?0xFFFFFFFF:0xFFB3C9DA);
    }
}
