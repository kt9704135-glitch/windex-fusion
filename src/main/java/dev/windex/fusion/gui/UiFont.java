package dev.windex.fusion.gui;

import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/** Noto Sans; SIL OFL license is distributed with the project. */
public final class UiFont {
    private static final StyleSpriteSource.Font FONT =
            new StyleSpriteSource.Font(Identifier.of("windexfusion", "noto"));
    private UiFont() {}
    public static Text text(String value) {
        return Text.literal(value).styled(style -> style.withFont(FONT));
    }
}
