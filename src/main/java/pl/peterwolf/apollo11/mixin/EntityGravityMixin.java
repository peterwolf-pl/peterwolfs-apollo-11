package pl.peterwolf.apollo11.mixin;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pl.peterwolf.apollo11.world.MoonDimension;

@Mixin(Entity.class)
public abstract class EntityGravityMixin {
    @Inject(method = "getGravity", at = @At("RETURN"), cancellable = true)
    private void apollo11$scaleGravityOnMoon(CallbackInfoReturnable<Double> callback) {
        Entity entity = (Entity) (Object) this;
        if (entity.level().dimension().equals(MoonDimension.KEY)) {
            callback.setReturnValue(callback.getReturnValue() / 6.0);
        }
    }
}
