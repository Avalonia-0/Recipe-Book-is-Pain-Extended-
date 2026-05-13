package me.melontini.recipebookispain.mixin.widget;

import me.melontini.recipebookispain.RecipeBookIsPain;
import me.melontini.recipebookispain.access.RecipeGroupButtonPlacement;
import me.melontini.recipebookispain.access.RecipeGroupButtonPlacementAccess;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.screen.ButtonTextures;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.recipebook.ClientRecipeBook;
import net.minecraft.client.gui.screen.recipebook.RecipeBookWidget;
import net.minecraft.client.gui.screen.recipebook.RecipeGroupButtonWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.item.ItemGroup;
import net.minecraft.recipe.book.RecipeBookGroup;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RecipeGroupButtonWidget.class)
public abstract class RecipeGroupButtonMixin extends TexturedButtonWidget implements RecipeGroupButtonPlacementAccess {
    @Unique private static final int RBIP_TAB_WIDTH = 35;
    @Unique private static final int RBIP_TAB_HEIGHT = 27;
    @Unique private static final int RBIP_ROTATED_TAB_WIDTH = 27;
    @Unique private static final int RBIP_ROTATED_TAB_HEIGHT = 35;
    @Unique private static final Identifier RBIP_BOTTOM_TAB = Identifier.of("recipe-book-is-pain", "textures/gui/bottom_tab.png");
    @Unique private static final Identifier RBIP_BOTTOM_TAB_SELECTED = Identifier.of("recipe-book-is-pain", "textures/gui/bottom_tab_selected.png");
    @Unique private static final Identifier RBIP_TOP_TAB = Identifier.of("recipe-book-is-pain", "textures/gui/top_tab.png");
    @Unique private static final Identifier RBIP_TOP_TAB_SELECTED = Identifier.of("recipe-book-is-pain", "textures/gui/top_tab_selected.png");

    @Unique private RecipeGroupButtonPlacement rbip$placement = RecipeGroupButtonPlacement.NORMAL;

    @Shadow @Final private RecipeBookWidget.Tab tab;
    @Shadow private float bounce;
    @Shadow private boolean groupFocused;

    @Shadow public abstract RecipeBookGroup getCategory();

    public RecipeGroupButtonMixin(int x, int y, int width, int height, ButtonTextures textures, ButtonWidget.PressAction pressAction) {
        super(x, y, width, height, textures, pressAction);
    }

    @Override
    public void rbip$setPlacement(RecipeGroupButtonPlacement placement) {
        this.rbip$placement = placement;
    }

    @Override
    public RecipeGroupButtonPlacement rbip$getPlacement() {
        return this.rbip$placement;
    }

    @Inject(at = @At("HEAD"), method = "checkForNewRecipes", cancellable = true)
    private void rbip$skipCreativeTabUnlockBounce(ClientRecipeBook recipeBook, boolean filteringCraftable, CallbackInfo ci) {
        if (RecipeBookIsPain.toItemGroup(this.getCategory()) != null) {
            ci.cancel();
        }
    }

    @Inject(at = @At("HEAD"), method = "drawIcon", cancellable = true)
    private void rbip$drawRotatedButton(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (this.rbip$placement == RecipeGroupButtonPlacement.NORMAL) return;

        boolean pushed = false;
        if (this.bounce > 0.0F) {
            float scale = 1.0F + 0.1F * (float) Math.sin(this.bounce / 15.0F * (float) Math.PI);
            context.getMatrices().pushMatrix();
            context.getMatrices().translate(this.getX() + 8, this.getY() + 12);
            context.getMatrices().scale(1.0F, scale);
            context.getMatrices().translate(-(this.getX() + 8), -(this.getY() + 12));
            pushed = true;
        }

        this.rbip$drawRotatedBackground(context);
        this.rbip$renderIconsAt(context, this.rbip$getRotatedIconX(), this.rbip$getRotatedIconY());

        if (pushed) {
            context.getMatrices().popMatrix();
            this.bounce -= delta;
        }

        ci.cancel();
    }

    @Inject(at = @At("HEAD"), method = "renderIcons", cancellable = true)
    private void rbip$render(DrawContext context, CallbackInfo ci) {
        ItemGroup group = RecipeBookIsPain.toItemGroup(this.getCategory());
        if (group == null) return;

        int i = this.groupFocused ? -2 : 0;

        if (RecipeBookIsPain.isOwOLoaded) {
            if (RecipeBookIsPain.rbip$renderOwo(context, i, (RecipeGroupButtonWidget) (Object) this, group)) {
                ci.cancel();
            }
        }
    }

    @Unique
    private void rbip$drawRotatedBackground(DrawContext context) {
        int localX = this.groupFocused ? -2 : 0;
        context.getMatrices().pushMatrix();
        if (this.rbip$placement == RecipeGroupButtonPlacement.BOTTOM) {
            context.getMatrices().translate(this.getX(), this.getY() + RBIP_ROTATED_TAB_HEIGHT);
            context.getMatrices().rotate(-(float) Math.PI / 2.0F);
        } else {
            context.getMatrices().translate(this.getX() + RBIP_ROTATED_TAB_WIDTH, this.getY());
            context.getMatrices().rotate((float) Math.PI / 2.0F);
        }
        Identifier texture = this.rbip$getRotatedTexture();
        if (texture.getPath().startsWith("textures/")) {
            context.drawTexture(RenderPipelines.GUI_TEXTURED, texture, localX, 0, 0.0F, 0.0F, RBIP_TAB_WIDTH, RBIP_TAB_HEIGHT, RBIP_TAB_WIDTH, RBIP_TAB_HEIGHT);
        } else {
            context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, texture, localX, 0, RBIP_TAB_WIDTH, RBIP_TAB_HEIGHT);
        }
        context.getMatrices().popMatrix();
    }

    @Unique
    private Identifier rbip$getRotatedTexture() {
        if (this.rbip$placement == RecipeGroupButtonPlacement.TOP) {
            return this.groupFocused ? RBIP_TOP_TAB_SELECTED : RBIP_TOP_TAB;
        }
        if (this.rbip$placement == RecipeGroupButtonPlacement.BOTTOM) {
            return this.groupFocused ? RBIP_BOTTOM_TAB_SELECTED : RBIP_BOTTOM_TAB;
        }
        return this.textures.get(true, this.groupFocused);
    }

    @Unique
    private void rbip$renderIconsAt(DrawContext context, int x, int y) {
        ItemGroup group = RecipeBookIsPain.toItemGroup(this.getCategory());
        if (group != null && RecipeBookIsPain.isOwOLoaded && RecipeBookIsPain.rbip$renderOwo(context, x, y, group)) {
            return;
        }

        if (this.tab.secondaryIcon().isPresent()) {
            context.drawItemWithoutEntity(this.tab.primaryIcon(), this.getX(), y);
            context.drawItemWithoutEntity(this.tab.secondaryIcon().get(), this.getX() + 11, y);
        } else {
            context.drawItemWithoutEntity(this.tab.primaryIcon(), x, y);
        }
    }

    @Unique
    private int rbip$getRotatedIconX() {
        int offset = this.rbip$placement == RecipeGroupButtonPlacement.TOP ? 1 : 0;
        return this.getX() + (RBIP_ROTATED_TAB_WIDTH - 16) / 2 + offset;
    }

    @Unique
    private int rbip$getRotatedIconY() {
        int y = this.getY() + (RBIP_ROTATED_TAB_HEIGHT - 16) / 2;
        if (this.rbip$placement == RecipeGroupButtonPlacement.TOP) {
            return y - 1;
        }
        if (this.rbip$placement == RecipeGroupButtonPlacement.BOTTOM) {
            return y + 1;
        }
        return y;
    }
}
