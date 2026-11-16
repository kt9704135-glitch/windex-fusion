package dev.windex.fusion.mixin;

import dev.windex.fusion.WindexFusion;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.LightUpdateS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerMixin {
    @Inject(method = "onLightUpdate", at = @At("TAIL"))
    private void windex$lightSignal(LightUpdateS2CPacket packet, CallbackInfo ci) {
        if (WindexFusion.INSTANCE != null)
            WindexFusion.INSTANCE.onLightUpdate(packet.getChunkX(), packet.getChunkZ());
    }
}
