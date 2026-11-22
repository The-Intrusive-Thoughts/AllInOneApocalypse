package com.flubburr.aioa.mixin;

import com.flubburr.aioa.client.config.AioaInterfaceScale;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(Minecraft.class)
abstract class ClientGuiScaleMixin {
    @ModifyArg(method = "resizeDisplay", at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/platform/Window;calculateScale(IZ)I"), index = 0)
    private int aioa$screenScale(int vanilla) {
        return AioaInterfaceScale.requested((Minecraft) (Object) this, vanilla);
    }
}
