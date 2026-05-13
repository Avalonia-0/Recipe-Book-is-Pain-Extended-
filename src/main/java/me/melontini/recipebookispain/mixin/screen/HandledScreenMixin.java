package me.melontini.recipebookispain.mixin.screen;

import me.melontini.recipebookispain.access.RecipeBookScrollAccess;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.RecipeBookScreen;
import net.minecraft.client.gui.screen.recipebook.RecipeBookWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HandledScreen.class)
public class HandledScreenMixin {
    @Inject(at = @At("HEAD"), method = "mouseScrolled", cancellable = true)
    private void rbip$scrollRecipeBookTabs(double mouseX, double mouseY, double horizontalAmount, double verticalAmount, CallbackInfoReturnable<Boolean> cir) {
        if (!((Object) this instanceof RecipeBookScreen<?>)) return;

        RecipeBookWidget<?> recipeBook = ((RecipeBookScreenAccessor) this).rbip$getRecipeBook();
        if (((RecipeBookScrollAccess) recipeBook).rbip$scrollPages(mouseX, mouseY, verticalAmount)) {
            cir.setReturnValue(true);
        }
    }
}
