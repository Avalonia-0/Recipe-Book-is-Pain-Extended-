package me.melontini.recipebookispain.mixin;

import me.melontini.recipebookispain.access.RecipeBookScrollAccess;
import me.melontini.recipebookispain.mixin.screen.RecipeBookScreenAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.gui.screen.ingame.RecipeBookScreen;
import net.minecraft.client.gui.screen.recipebook.RecipeBookWidget;
import net.minecraft.client.util.Window;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public abstract class MouseMixin {
    @Shadow @Final private MinecraftClient client;

    @Shadow
    private double getScaledX(Window window) {
        throw new AssertionError();
    }

    @Shadow
    private double getScaledY(Window window) {
        throw new AssertionError();
    }

    @Inject(at = @At("HEAD"), method = "onMouseScroll", cancellable = true)
    private void rbip$scrollRecipeBookTabs(long windowHandle, double horizontalAmount, double verticalAmount, CallbackInfo ci) {
        if (windowHandle != this.client.getWindow().getHandle()
                || this.client.getOverlay() != null
                || !(this.client.currentScreen instanceof RecipeBookScreen<?> screen)) {
            return;
        }

        Window window = this.client.getWindow();
        double mouseX = this.getScaledX(window);
        double mouseY = this.getScaledY(window);
        double scaledVerticalAmount = this.rbip$scaleScrollAmount(verticalAmount);

        RecipeBookWidget<?> recipeBook = ((RecipeBookScreenAccessor) screen).rbip$getRecipeBook();
        if (((RecipeBookScrollAccess) recipeBook).rbip$scrollPages(mouseX, mouseY, scaledVerticalAmount)) {
            screen.applyMousePressScrollNarratorDelay();
            ci.cancel();
        }
    }

    private double rbip$scaleScrollAmount(double amount) {
        double scaledAmount = this.client.options.getDiscreteMouseScroll().getValue()
                ? Math.signum(amount)
                : amount;
        return scaledAmount * this.client.options.getMouseWheelSensitivity().getValue();
    }
}
