package dev.windex.fusion.mixin;

import dev.windex.fusion.WindexFusion;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.particle.ParticleEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ParticleManager.class)
public abstract class ParticleManagerMixin {
    @Inject(method = "addParticle(Lnet/minecraft/particle/ParticleEffect;DDDDDD)Lnet/minecraft/client/particle/Particle;", at = @At("HEAD"))
    private void windex$observe(ParticleEffect parameters, double x, double y, double z,
                                double velocityX, double velocityY, double velocityZ,
                                CallbackInfoReturnable<Particle> cir) {
        if (WindexFusion.INSTANCE != null) WindexFusion.INSTANCE.onParticle(parameters,x,y,z);
    }
}
