package net.kyrptonaught.LEMBackend.mixin;

import net.minecraft.network.Connection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Connection.class)
public class ConnectionMixin {

    @Redirect(method = "getLoggableAddress", at = @At(value = "INVOKE", target = "Ljava/lang/Object;toString()Ljava/lang/String;"))
    private static String hideIP(Object instance) {
        return "<IP>" + instance + "</IP>";
    }
}