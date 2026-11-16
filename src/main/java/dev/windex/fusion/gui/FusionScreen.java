package dev.windex.fusion.gui;

import dev.windex.fusion.Config;
import dev.windex.fusion.Module;
import dev.windex.fusion.WindexFusion;
import dev.windex.fusion.analysis.Markers;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public final class FusionScreen extends Screen {
    private int page;
    private int x,y,w,h;
    private String query = "";
    private TextFieldWidget search;
    private final List<Control> controls = new ArrayList<>();

    private record Control(String label,int x,int y,int width,int height,Runnable action) {
        boolean contains(double px,double py) {
            return px>=x && px<x+width && py>=y && py<y+height;
        }
    }

    public FusionScreen() { super(Text.literal("Windex Client Recode")); }

    @Override
    protected void init() {
        controls.clear();
        w = Math.min(950,width-20);
        h = Math.min(540,height-20);
        x = (width-w)/2;
        y = (height-h)/2;
        int side = Math.min(180, Math.max(110,w/4));
        String[] pages = {"Combat", "Movement", "Visual", "Settings"};
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
            modules(new Module[]{Module.AUTO_TOTEM,Module.TRIGGER_BOT},y+105);
        } else if (page == 1) {
            modules(new Module[]{Module.AUTO_SPRINT},y+105);
        } else if(page==2) {
            modules(new Module[]{Module.TARGET_HUD},y+105);
        } else if(page==3) {
            initConfig();
        }
        add(button("SAVE", x+w-93,y+h-30,79,19,
                () -> WindexFusion.INSTANCE.config.save()));
    }

    private Control button(String text,int bx,int by,int bw,int bh,Runnable action) {
        return new Control(text,bx,by,bw,bh,action);
    }

    private Control add(Control control) {controls.add(control);return control;}

    @Override
    public boolean mouseClicked(Click click,boolean doubled) {
        if (click.button()==0) {
            for (Control control : List.copyOf(controls)) {
                if (control.contains(click.x(),click.y())) {
                    control.action().run();
                    return true;
                }
            }
        }
        return super.mouseClicked(click,doubled);
    }

    private void rounded(DrawContext ctx,int ax,int ay,int bx,int by,int color) {
        ctx.fill(ax+4,ay,bx-4,by,color);
        ctx.fill(ax,ay+4,bx,by-4,color);
        ctx.fill(ax+2,ay+2,bx-2,by-2,color);
    }

    private void drawControls(DrawContext ctx,int mouseX,int mouseY,int accent) {
        for (Control control:controls) {
            boolean hover=control.contains(mouseX,mouseY);
            boolean selected=(control.y>=y+96 && control.y<=y+96+3*39
                    && control.x==x+13 && (control.y-(y+96))/39==page);
            int fill=selected?0xCB11465D:(hover?0xD52A435A:0xA20A172A);
            rounded(ctx,control.x,control.y,control.x+control.width,control.y+control.height,fill);
            if(selected)ctx.fill(control.x,control.y+5,control.x+2,control.y+control.height-5,accent);
            if(hover)ctx.fill(control.x+5,control.y+control.height-2,control.x+control.width-5,control.y+control.height-1,accent);
            ctx.drawCenteredTextWithShadow(textRenderer,control.label,
                    control.x+control.width/2,control.y+(control.height-9)/2,
                    selected?0xFFFFFFFF:0xFFDCEAF5);
        }
    }

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
        increment("Auto Totem delay",y+141,()-> c.autoTotemDelayTicks=Math.max(1,c.autoTotemDelayTicks-1),
                ()-> c.autoTotemDelayTicks=Math.min(20,c.autoTotemDelayTicks+1));
        add(button("ACCENT COLOUR",x+side+17,y+211,140,26,()->{
            int[] palette={0x6AD5FF,0xFFFFFF,0x9B6CFF,0x48D4C5,0xFF81B1};
            int idx=0;
            for(int i=0;i<palette.length;i++) if(palette[i]==c.accentRgb) idx=i;
            c.accentRgb=palette[(idx+1)%palette.length];c.save();clearAndInit();
        }));
        add(button(c.arcticParticles?"SNOW FX ON":"SNOW FX OFF",x+side+17,y+257,140,26,
                ()->{c.arcticParticles=!c.arcticParticles;c.save();clearAndInit();}));
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
        ctx.drawTextWithShadow(textRenderer,UiFont.text(new String[]{"Combat", "Movement", "Visual", "Settings"}[page]+" Modules"),x+side+18,y+17,0xFF000000|c.textRgb);
        ctx.drawTextWithShadow(textRenderer,"ARCTIC EDITION  /  1.21.11",x+side+18,y+31,0xFFB8E8FF);
        if (page>=0 && page<=2) {
            Module[] modules = switch(page) {
                case 0 -> new Module[]{Module.AUTO_TOTEM,Module.TRIGGER_BOT};
                case 1 -> new Module[]{Module.AUTO_SPRINT};
                default -> new Module[]{Module.TARGET_HUD};
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
        } else if (page==3) {
            ctx.drawTextWithShadow(textRenderer,"PVP SETTINGS",x+side+19,y+107,accent);
            ctx.drawTextWithShadow(textRenderer,"Auto Totem delay: "+c.autoTotemDelayTicks+" ticks",x+side+23,y+147,0xFFE6F5FF);
            ctx.drawTextWithShadow(textRenderer,"Theme accent",x+side+173,y+220,0xFFE6F5FF);
            ctx.fill(x+side+264,y+214,x+side+290,y+233,accent);
        } else {
            ctx.drawTextWithShadow(textRenderer,"No modules installed in this category yet",x+side+26,y+108,0xFFB5D9E9);
        }
        ctx.fill(x+10,y+h-42,x+w-10,y+h-41,0x444D5B73);
        ctx.drawTextWithShadow(textRenderer,"RSHIFT menu  /  ESC close  |  Clues are not proof",x+18,y+h-27,0xFF97A2BA);
        super.render(ctx,mouseX,mouseY,delta);
        drawControls(ctx,mouseX,mouseY,accent);
    }

    @Override public boolean shouldPause() {return false;}
}
