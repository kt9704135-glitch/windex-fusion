package dev.windex.fusion.gui;

import dev.windex.fusion.Config;
import dev.windex.fusion.Module;
import dev.windex.fusion.WindexFusion;
import dev.windex.fusion.analysis.Markers;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.List;

public final class FusionScreen extends Screen {
    private int page;
    private int x,y,w,h;
    private String query = "";
    private TextFieldWidget search;

    public FusionScreen() { super(Text.literal("Windex Client Recode")); }

    @Override
    protected void init() {
        w = Math.min(950,width-20);
        h = Math.min(540,height-20);
        x = (width-w)/2;
        y = (height-h)/2;
        int side = Math.min(180, Math.max(110,w/4));
        String[] pages = {"Finders", "Analyzers", "Combat", "Movement", "Visual", "Settings"};
        for (int i=0;i<pages.length;i++) {
            final int selected=i;
            add(button(pages[i],x+13,y+96+i*39,side-26,29,
                    () -> {page=selected;clearAndInit();}));
        }
        search=new TextFieldWidget(textRenderer,x+side+19,y+45,Math.max(85,w-side-93),22,Text.literal("Search modules"));
        search.setMaxLength(64);
        search.setText(query);
        search.setChangedListener(value -> query=value);
        addDrawableChild(search);
        add(button("FIND",x+w-68,y+45,52,22,this::clearAndInit));
        if (page == 0) {
            modules(new Module[]{Module.SPAWNER,Module.PLAYER,Module.STORAGE},y+105);
        } else if (page == 1) {
            modules(new Module[]{Module.SUS,Module.CAVE,Module.TERRAIN,Module.DEEPSLATE},y+105);
        } else if(page == 2) {
            modules(new Module[]{Module.AUTO_TOTEM},y+105);
        } else if(page == 3) {
            modules(new Module[]{Module.AUTO_SPRINT},y+105);
        } else if(page==5) {
            initConfig();
        }
        add(button("SAVE", x+w-93,y+h-30,79,19,
                () -> WindexFusion.INSTANCE.config.save()));
    }

    private ButtonWidget button(String text,int bx,int by,int bw,int bh,Runnable action) {
        return ButtonWidget.builder(Text.literal(text), b -> action.run())
                .dimensions(bx,by,bw,bh).build();
    }

    private ButtonWidget add(ButtonWidget button) {return addDrawableChild(button);}

    private void modules(Module[] modules,int start) {
        Config config = WindexFusion.INSTANCE.config;
        int side = Math.min(180, Math.max(110,w/4));
        int shown=0;
        for (int i=0;i<modules.length;i++) {
            final Module module = modules[i];
            if (!module.title.toLowerCase(java.util.Locale.ROOT).contains(query.toLowerCase(java.util.Locale.ROOT))) continue;
            int rowY=start+(shown++)*67;
            add(button(config.on(module)?"● ON":"○ OFF",x+w-98,rowY+3,75,23,
                    () -> { config.toggle(module); clearAndInit(); }));
        }
    }

    private void initConfig() {
        Config c = WindexFusion.INSTANCE.config;
        int side=Math.min(180, Math.max(110,w/4));
        int b=(w-side-72)/4;
        for (int i=0;i<4;i++) {
            String profile = new String[]{"DONUT","PERFORMANCE","BALANCED","DEEP"}[i];
            add(button(profile,x+side+14+i*(b+9),y+131,b,23,() -> {
                c.preset(profile); clearAndInit();
            }));
        }
        increment("Scan radius",y+185,()-> c.scanRadiusChunks=Math.max(1,c.scanRadiusChunks-1),
                ()-> c.scanRadiusChunks=Math.min(6,c.scanRadiusChunks+1));
        increment("Sus threshold",y+218,()-> c.susMinEvidence=Math.max(2,c.susMinEvidence-1),
                ()-> c.susMinEvidence=Math.min(30,c.susMinEvidence+1));
        increment("Particle samples",y+251,()-> c.spawnerMinEachParticle=Math.max(2,c.spawnerMinEachParticle-1),
                ()-> c.spawnerMinEachParticle=Math.min(15,c.spawnerMinEachParticle+1));
        increment("Light updates",y+284,()-> c.lightUpdatesForAlert=Math.max(3,c.lightUpdatesForAlert-1),
                ()-> c.lightUpdatesForAlert=Math.min(30,c.lightUpdatesForAlert+1));
        add(button(c.showHud?"HUD ON":"HUD OFF",x+side+17,y+332,108,22,
                ()->{c.showHud=!c.showHud;c.save();clearAndInit();}));
        add(button("CHANGE COLOUR",x+side+129,y+332,140,22,()->{
            int[] palette={0x9B6CFF,0x48D4C5,0xFF81B1,0xFFBD66,0x66A9FF};
            int idx=0;
            for(int i=0;i<palette.length;i++) if(palette[i]==c.accentRgb) idx=i;
            c.accentRgb=palette[(idx+1)%palette.length];c.save();clearAndInit();
        }));
        add(button("COORDS "+(c.showCoordinates?"ON":"OFF"),x+side+273,y+332,107,22,
                ()->{c.showCoordinates=!c.showCoordinates;c.save();clearAndInit();}));
        add(button(c.arcticParticles?"SNOW FX ON":"SNOW FX OFF",x+side+384,y+332,140,22,
                ()->{c.arcticParticles=!c.arcticParticles;c.save();clearAndInit();}));
        add(button("CHUNK SQUARE COLOUR",x+side+17,y+370,166,22,()->{
            int[] colors={0x57D1FF,0xFFFFFF,0x8A74FF,0xFF75C8,0x65FFC7,0xFFE37B};
            int next=0;
            for (int i=0;i<colors.length;i++) if (colors[i]==c.analyzerRgb) next=(i+1)%colors.length;
            c.analyzerRgb=colors[next];c.save();clearAndInit();
        }));
    }

    private void increment(String label,int posY,Runnable dec,Runnable inc) {
        Config c = WindexFusion.INSTANCE.config;
        add(button("-",x+w-115,posY,31,22,()->{dec.run();c.save();clearAndInit();}));
        add(button("+",x+w-70,posY,31,22,()->{inc.run();c.save();clearAndInit();}));
    }

    @Override
    public void render(DrawContext ctx,int mouseX,int mouseY,float delta) {
        Config c = WindexFusion.INSTANCE.config;
        int accent=0xFF000000|c.accentRgb;
        int side=Math.min(180, Math.max(110,w/4));
        ctx.fill(0,0,width,height,0xD006101B);
        ctx.fill(x-5,y-5,x+w+5,y+h+5,0x8846C9FF);
        ctx.fill(x-3,y-3,x+w+3,y+h+3,0xEF030B15);
        ctx.fill(x,y,x+w,y+h,0xED000000 | c.panelRgb);
        ctx.fill(x,y,x+w,y+3,accent);
        ctx.fill(x+4,y+5,x+side,y+h-6,0xEE071323);
        ctx.fill(x+side,y+10,x+side+1,y+h-10,0x554B8BB0);
        // Animated Arctic specks; purely cosmetic and client-side.
        if (c.arcticParticles) {
            double time = System.currentTimeMillis() / 1000.0;
            for (int i=0;i<72;i++) {
                int px = x+10+(int)((i*97.31+Math.sin(time*.55+i)*16)%(Math.max(1,w-20)));
                int py = y+7+(int)((i*53.7+time*(9+i%5)*2)%(Math.max(1,h-14)));
                int size=1+i%3;
                ctx.fill(px,py,px+size,py+size,((i%3==0?0xAA:0x55)<<24)|c.particleRgb);
            }
        }
        ctx.fill(x+9,y+49,x+w-9,y+50,0x55687595);
        ctx.drawTextWithShadow(textRenderer,UiFont.text("WINDEX"),x+17,y+19,0xFF000000|c.textRgb);
        ctx.drawTextWithShadow(textRenderer,"RECODE",x+17,y+35,accent);
        ctx.drawTextWithShadow(textRenderer,UiFont.text(new String[]{"Finders", "Analyzers", "Combat", "Movement", "Visual", "Settings"}[page]+" Modules"),x+side+18,y+17,0xFF000000|c.textRgb);
        ctx.drawTextWithShadow(textRenderer,"ARCTIC EDITION  /  1.21.11",x+side+18,y+31,0xFFB8E8FF);
        if (page>=0 && page<=3) {
            Module[] modules = switch(page) {
                case 0 -> new Module[]{Module.SPAWNER,Module.PLAYER,Module.STORAGE};
                case 1 -> new Module[]{Module.SUS,Module.CAVE,Module.TERRAIN,Module.DEEPSLATE};
                case 2 -> new Module[]{Module.AUTO_TOTEM};
                default -> new Module[]{Module.AUTO_SPRINT};
            };
            int start=y+105;
            int shown=0;
            for (int i=0;i<modules.length;i++) {
                Module m=modules[i];
                if (!m.title.toLowerCase(java.util.Locale.ROOT).contains(query.toLowerCase(java.util.Locale.ROOT))) continue;
                int row=start+(shown++)*67;
                ctx.fill(x+side+15,row-7,x+w-14,row+48,0xA01C3349);
                ctx.fill(x+side+15,row-7,x+side+18,row+48,accent);
                ctx.drawTextWithShadow(textRenderer,UiFont.text(m.title),x+side+28,row+1,0xFF000000|c.textRgb);
                ctx.drawTextWithShadow(textRenderer,UiFont.text(m.description),x+side+28,row+21,0xFFB5BDD6);
            }
        } else if (page==5) {
            ctx.drawTextWithShadow(textRenderer,"PROFILES   "+c.preset,x+side+19,y+110,accent);
            ctx.drawTextWithShadow(textRenderer,"Scan radius: "+c.scanRadiusChunks+" chunks",x+side+23,y+191,0xFFE6E6FA);
            ctx.drawTextWithShadow(textRenderer,"Sus evidence threshold: "+c.susMinEvidence,x+side+23,y+224,0xFFE6E6FA);
            ctx.drawTextWithShadow(textRenderer,"Smoke + flame each: "+c.spawnerMinEachParticle,x+side+23,y+257,0xFFE6E6FA);
            ctx.drawTextWithShadow(textRenderer,"Light alert after: "+c.lightUpdatesForAlert,x+side+23,y+290,0xFFE6E6FA);
            ctx.fill(x+side+192,y+372,x+side+218,y+390,0xFF000000|c.analyzerRgb);
        } else {
            ctx.drawTextWithShadow(textRenderer,"No modules installed in this category yet",x+side+26,y+108,0xFFB5D9E9);
        }
        ctx.fill(x+10,y+h-42,x+w-10,y+h-41,0x444D5B73);
        ctx.drawTextWithShadow(textRenderer,"RSHIFT menu  /  ESC close  |  Clues are not proof",x+18,y+h-27,0xFF97A2BA);
        super.render(ctx,mouseX,mouseY,delta);
    }

    @Override public boolean shouldPause() {return false;}
}
