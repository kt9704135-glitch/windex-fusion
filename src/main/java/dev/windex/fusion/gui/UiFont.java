package dev.windex.fusion.gui;

import net.minecraft.text.Text;

/** Safe fallback until the optional Noto resource is verified in-game. */
public final class UiFont {
    private UiFont() {}
    public static Text text(String value) {
        return Text.literal(value);
    }
}
