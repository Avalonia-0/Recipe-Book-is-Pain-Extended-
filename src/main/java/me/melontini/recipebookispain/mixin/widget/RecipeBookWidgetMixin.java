package me.melontini.recipebookispain.mixin.widget;

import me.melontini.recipebookispain.RecipeBookIsPain;
import me.melontini.recipebookispain.RecipeBookIsPainExtendedConfig;
import me.melontini.recipebookispain.access.RecipeGroupButtonPlacement;
import me.melontini.recipebookispain.access.RecipeGroupButtonPlacementAccess;
import me.melontini.recipebookispain.access.RecipeBookScrollAccess;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.recipebook.CraftingRecipeBookWidget;
import net.minecraft.client.gui.screen.recipebook.RecipeBookWidget;
import net.minecraft.client.gui.screen.recipebook.RecipeGroupButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.recipebook.RecipeBookType;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.screen.AbstractRecipeScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(RecipeBookWidget.class)
public class RecipeBookWidgetMixin implements RecipeBookScrollAccess {
    @Unique private static final Identifier RBIP_PAGE_BUTTONS = Identifier.of("recipe-book-is-pain", "textures/gui/recipe_book_buttons.png");
    @Unique private static final int RBIP_FALLBACK_GROUPS_PER_PAGE = 5;
    @Unique private static final int RBIP_PAGE_BUTTON_WIDTH = 14;
    @Unique private static final int RBIP_PAGE_BUTTON_HEIGHT = 13;
    @Unique private static final int RBIP_BOOK_WIDTH = 147;
    @Unique private static final int RBIP_BOOK_HEIGHT = 166;
    @Unique private static final int RBIP_TAB_WIDTH = 35;
    @Unique private static final int RBIP_TAB_HEIGHT = 27;
    @Unique private static final int RBIP_ROTATED_TAB_WIDTH = 27;
    @Unique private static final int RBIP_ROTATED_TAB_HEIGHT = 35;
    @Unique private static final int RBIP_LEFT_TOTAL_SLOTS = 6;
    @Unique private static final int RBIP_BOTTOM_SLOTS = 5;
    @Unique private static final int RBIP_TOP_SLOTS = 5;
    @Unique private static final int RBIP_EXTENDED_SLOT_STEP = 27;
    @Unique private static final int RBIP_HORIZONTAL_SCROLL_OUTWARD_PADDING = 20;

    @Shadow @Final @Mutable private List<RecipeBookWidget.Tab> tabs;
    @Shadow @Final private List<RecipeGroupButtonWidget> tabButtons;
    @Shadow protected MinecraftClient client;
    @Shadow private int parentWidth;
    @Shadow private int parentHeight;
    @Shadow private int leftOffset;
    @Shadow private RecipeGroupButtonWidget currentTab;

    @Unique private RecipeGroupButtonWidget rbip$pinnedTab;
    @Unique private List<RecipeGroupButtonWidget> rbip$pageableTabs = List.of();
    @Unique private int rbip$page;
    @Unique private int rbip$pageCount;
    @Unique private int rbip$pageControlX;
    @Unique private int rbip$pageControlY;

    @Inject(at = @At("TAIL"), method = "<init>")
    private void rbip$addCreativeTabs(AbstractRecipeScreenHandler handler, List<RecipeBookWidget.Tab> tabs, CallbackInfo ci) {
        if ((Object) this instanceof CraftingRecipeBookWidget) {
            this.tabs = RecipeBookIsPain.withCreativeTabs(tabs);
        }
    }

    @Inject(at = @At("TAIL"), method = "refreshTabButtons")
    private void rbip$paginateTabButtons(boolean filteringCraftable, CallbackInfo ci) {
        List<RecipeGroupButtonWidget> pageableTabs = new ArrayList<>();
        RecipeGroupButtonWidget pinnedTab = null;

        for (RecipeGroupButtonWidget widget : this.tabButtons) {
            if (!widget.visible) continue;

            if (pinnedTab == null && widget.getCategory() instanceof RecipeBookType) {
                pinnedTab = widget;
            } else {
                pageableTabs.add(widget);
            }
        }

        this.rbip$pinnedTab = pinnedTab;
        this.rbip$pageableTabs = pageableTabs;
        this.rbip$applyPagination(true);
    }

    @Inject(at = @At("HEAD"), method = "render")
    private void rbip$reloadExtendedConfig(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (RecipeBookIsPainExtendedConfig.reloadIfChanged()) {
            this.rbip$applyPagination(true);
        }
    }

    @Inject(at = @At("TAIL"), method = "render")
    private void rbip$renderPageControls(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (this.rbip$pageCount <= 1) return;

        this.rbip$drawPageControl(context, this.rbip$pageControlX, this.rbip$pageControlY, false, this.rbip$page > 0, mouseX, mouseY);
        this.rbip$drawPageControl(context, this.rbip$pageControlX + 15, this.rbip$pageControlY, true, this.rbip$page < this.rbip$pageCount - 1, mouseX, mouseY);

        if (this.client.currentScreen != null
                && (this.rbip$isInside(mouseX, mouseY, this.rbip$pageControlX, this.rbip$pageControlY, RBIP_PAGE_BUTTON_WIDTH, RBIP_PAGE_BUTTON_HEIGHT)
                || this.rbip$isInside(mouseX, mouseY, this.rbip$pageControlX + 15, this.rbip$pageControlY, RBIP_PAGE_BUTTON_WIDTH, RBIP_PAGE_BUTTON_HEIGHT))) {
            context.drawTooltip(this.client.textRenderer, Text.literal((this.rbip$page + 1) + "/" + this.rbip$pageCount), mouseX, mouseY);
        }
    }

    @Inject(at = @At("HEAD"), method = "mouseClicked", cancellable = true)
    private void rbip$mouseClickedPageControls(Click click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
        if (this.rbip$pageCount <= 1 || click.button() != 0) return;

        int x = (int) click.x();
        int y = (int) click.y();

        if (this.rbip$isInside(x, y, this.rbip$pageControlX, this.rbip$pageControlY, RBIP_PAGE_BUTTON_WIDTH, RBIP_PAGE_BUTTON_HEIGHT)) {
            if (this.rbip$page > 0) {
                this.rbip$page--;
                this.rbip$applyPagination(false);
                ClickableWidget.playClickSound(this.client.getSoundManager());
                cir.setReturnValue(true);
            }
        } else if (this.rbip$isInside(x, y, this.rbip$pageControlX + 15, this.rbip$pageControlY, RBIP_PAGE_BUTTON_WIDTH, RBIP_PAGE_BUTTON_HEIGHT)) {
            if (this.rbip$page < this.rbip$pageCount - 1) {
                this.rbip$page++;
                this.rbip$applyPagination(false);
                ClickableWidget.playClickSound(this.client.getSoundManager());
                cir.setReturnValue(true);
            }
        }
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        return this.rbip$scrollPages(mouseX, mouseY, verticalAmount);
    }

    @Override
    public boolean rbip$scrollPages(double mouseX, double mouseY, double verticalAmount) {
        if (!RecipeBookIsPainExtendedConfig.get().extendedFeatures()
                || this.rbip$pageCount <= 1
                || verticalAmount == 0.0D
                || !this.rbip$isMouseOverAnyVisibleTab(mouseX, mouseY)) {
            return false;
        }

        int nextPage = this.rbip$page + (verticalAmount > 0.0D ? -1 : 1);
        nextPage = Math.max(0, Math.min(nextPage, this.rbip$pageCount - 1));
        if (nextPage != this.rbip$page) {
            this.rbip$page = nextPage;
            this.rbip$applyPagination(false);
        }
        return true;
    }

    @Unique
    private void rbip$applyPagination(boolean followCurrentTab) {
        int slot = 0;
        int groupsPerPage = this.rbip$getGroupsPerPage();

        if (this.rbip$pinnedTab != null) {
            this.rbip$pinnedTab.visible = true;
            this.rbip$placeTab(this.rbip$pinnedTab, slot);
            slot++;
        }

        this.rbip$pageCount = (this.rbip$pageableTabs.size() + groupsPerPage - 1) / groupsPerPage;
        if (this.rbip$pageCount <= 1) {
            this.rbip$page = 0;
            this.rbip$pageControlX = this.rbip$getPageControlX();
            this.rbip$pageControlY = this.rbip$getPageControlY();
            for (RecipeGroupButtonWidget widget : this.rbip$pageableTabs) {
                widget.visible = true;
                this.rbip$placeTab(widget, slot++);
            }
            return;
        }

        if (followCurrentTab && this.currentTab != null) {
            int currentIndex = this.rbip$pageableTabs.indexOf(this.currentTab);
            if (currentIndex >= 0) {
                this.rbip$page = currentIndex / groupsPerPage;
            }
        }

        this.rbip$page = Math.max(0, Math.min(this.rbip$page, this.rbip$pageCount - 1));
        int start = this.rbip$page * groupsPerPage;
        int end = Math.min(start + groupsPerPage, this.rbip$pageableTabs.size());

        for (int i = 0; i < this.rbip$pageableTabs.size(); i++) {
            RecipeGroupButtonWidget widget = this.rbip$pageableTabs.get(i);
            boolean onPage = i >= start && i < end;
            widget.visible = onPage;
            if (onPage) {
                this.rbip$placeTab(widget, slot++);
            } else {
                this.rbip$resetTabPlacement(widget);
            }
        }

        this.rbip$pageControlX = this.rbip$getPageControlX();
        this.rbip$pageControlY = this.rbip$getPageControlY();
    }

    @Unique
    private int rbip$getGroupsPerPage() {
        RecipeBookIsPainExtendedConfig config = RecipeBookIsPainExtendedConfig.get();
        if (!config.extendedFeatures()) {
            return RBIP_FALLBACK_GROUPS_PER_PAGE;
        }

        int pinnedCount = this.rbip$pinnedTab == null ? 0 : 1;
        return Math.max(1, config.bottomNumber() - pinnedCount);
    }

    @Unique
    private void rbip$placeTab(RecipeGroupButtonWidget widget, int slot) {
        RecipeBookIsPainExtendedConfig config = RecipeBookIsPainExtendedConfig.get();
        if (!config.extendedFeatures()) {
            this.rbip$placeNormalTab(widget, slot);
            return;
        }

        if (slot < RBIP_LEFT_TOTAL_SLOTS) {
            this.rbip$placeNormalTab(widget, slot);
        } else if (slot < RBIP_LEFT_TOTAL_SLOTS + RBIP_TOP_SLOTS) {
            int topSlot = slot - RBIP_LEFT_TOTAL_SLOTS;
            ((RecipeGroupButtonPlacementAccess) widget).rbip$setPlacement(RecipeGroupButtonPlacement.TOP);
            int x = this.rbip$getTopTabX(topSlot);
            int y = this.rbip$getTopTabY();
            widget.setDimensionsAndPosition(RBIP_ROTATED_TAB_WIDTH, RBIP_ROTATED_TAB_HEIGHT, x, y);
        } else if (slot < RBIP_LEFT_TOTAL_SLOTS + RBIP_TOP_SLOTS + RBIP_BOTTOM_SLOTS) {
            int bottomSlot = slot - RBIP_LEFT_TOTAL_SLOTS - RBIP_TOP_SLOTS;
            ((RecipeGroupButtonPlacementAccess) widget).rbip$setPlacement(RecipeGroupButtonPlacement.BOTTOM);
            int x = this.rbip$getBottomTabX(bottomSlot);
            int y = this.rbip$getBottomTabY();
            widget.setDimensionsAndPosition(RBIP_ROTATED_TAB_WIDTH, RBIP_ROTATED_TAB_HEIGHT, x, y);
        } else {
            this.rbip$placeNormalTab(widget, slot);
        }
    }

    @Unique
    private void rbip$placeNormalTab(RecipeGroupButtonWidget widget, int slot) {
        ((RecipeGroupButtonPlacementAccess) widget).rbip$setPlacement(RecipeGroupButtonPlacement.NORMAL);
        int x = this.rbip$getTabX();
        int y = this.rbip$getTabY() + RBIP_TAB_HEIGHT * slot;
        widget.setDimensionsAndPosition(RBIP_TAB_WIDTH, RBIP_TAB_HEIGHT, x, y);
    }

    @Unique
    private void rbip$resetTabPlacement(RecipeGroupButtonWidget widget) {
        ((RecipeGroupButtonPlacementAccess) widget).rbip$setPlacement(RecipeGroupButtonPlacement.NORMAL);
        widget.setDimensions(RBIP_TAB_WIDTH, RBIP_TAB_HEIGHT);
    }

    @Unique
    private int rbip$getTabX() {
        return this.rbip$getBookX() - 30;
    }

    @Unique
    private int rbip$getTabY() {
        return this.rbip$getBookY() + 3;
    }

    @Unique
    private int rbip$getPageControlX() {
        if (RecipeBookIsPainExtendedConfig.get().extendedFeatures()) {
            return this.rbip$getBookX() - 28;
        }
        return this.rbip$getBookX() + 5;
    }

    @Unique
    private int rbip$getPageControlY() {
        return this.rbip$getBookY() - 12;
    }

    @Unique
    private int rbip$getBookX() {
        return (this.parentWidth - RBIP_BOOK_WIDTH) / 2 - this.leftOffset;
    }

    @Unique
    private int rbip$getBookY() {
        return (this.parentHeight - RBIP_BOOK_HEIGHT) / 2;
    }

    @Unique
    private int rbip$getBottomTabX(int slot) {
        return this.rbip$getHorizontalTabStartX() + slot * RBIP_EXTENDED_SLOT_STEP;
    }

    @Unique
    private int rbip$getBottomTabY() {
        return this.rbip$getBookY() + RBIP_BOOK_HEIGHT - 5;
    }

    @Unique
    private int rbip$getTopTabX(int slot) {
        return this.rbip$getHorizontalTabStartX() + slot * RBIP_EXTENDED_SLOT_STEP;
    }

    @Unique
    private int rbip$getTopTabY() {
        return this.rbip$getBookY() - RBIP_ROTATED_TAB_HEIGHT + 5;
    }

    @Unique
    private int rbip$getHorizontalTabStartX() {
        return this.rbip$getBookX() + (RBIP_BOOK_WIDTH - RBIP_TOP_SLOTS * RBIP_ROTATED_TAB_WIDTH) / 2;
    }

    @Unique
    private void rbip$drawPageControl(DrawContext context, int x, int y, boolean next, boolean active, int mouseX, int mouseY) {
        int u = next ? 14 : 0;
        if (active && this.rbip$isInside(mouseX, mouseY, x, y, RBIP_PAGE_BUTTON_WIDTH, RBIP_PAGE_BUTTON_HEIGHT)) {
            u += 28;
        }

        int v = active ? 0 : 13;
        context.drawTexture(RenderPipelines.GUI_TEXTURED, RBIP_PAGE_BUTTONS, x, y, u, v, RBIP_PAGE_BUTTON_WIDTH, RBIP_PAGE_BUTTON_HEIGHT, 256, 256);
    }

    @Unique
    private boolean rbip$isInside(int x, int y, int left, int top, int width, int height) {
        return x >= left && x < left + width && y >= top && y < top + height;
    }

    @Unique
    private boolean rbip$isMouseOverAnyVisibleTab(double mouseX, double mouseY) {
        RbipScrollArea topArea = null;
        RbipScrollArea bottomArea = null;

        for (RecipeGroupButtonWidget widget : this.tabButtons) {
            if (!widget.visible) continue;

            RecipeGroupButtonPlacement placement = ((RecipeGroupButtonPlacementAccess) widget).rbip$getPlacement();
            if (placement == RecipeGroupButtonPlacement.TOP) {
                topArea = this.rbip$includeScrollArea(topArea, this.rbip$getExpandedHorizontalArea(widget));
            } else if (placement == RecipeGroupButtonPlacement.BOTTOM) {
                bottomArea = this.rbip$includeScrollArea(bottomArea, this.rbip$getExpandedHorizontalArea(widget));
            } else if (this.rbip$isInside(mouseX, mouseY, widget.getX(), widget.getY(), widget.getWidth(), widget.getHeight())) {
                return true;
            }
        }

        if (topArea != null && this.rbip$isInside(
                mouseX, mouseY,
                topArea.left(), topArea.top(),
                topArea.width(), topArea.height())) {
            return true;
        }

        return bottomArea != null && this.rbip$isInside(
                mouseX, mouseY,
                bottomArea.left(), bottomArea.top(),
                bottomArea.width(), bottomArea.height());
    }

    @Unique
    private RbipScrollArea rbip$getExpandedHorizontalArea(RecipeGroupButtonWidget widget) {
        return new RbipScrollArea(
                widget.getX(),
                widget.getY() - RBIP_HORIZONTAL_SCROLL_OUTWARD_PADDING,
                widget.getX() + widget.getWidth(),
                widget.getY() + widget.getHeight() + RBIP_HORIZONTAL_SCROLL_OUTWARD_PADDING);
    }

    @Unique
    private RbipScrollArea rbip$includeScrollArea(RbipScrollArea current, RbipScrollArea area) {
        return current == null
                ? area
                : new RbipScrollArea(
                        Math.min(current.left(), area.left()),
                        Math.min(current.top(), area.top()),
                        Math.max(current.right(), area.right()),
                        Math.max(current.bottom(), area.bottom()));
    }

    @Unique
    private boolean rbip$isInside(double x, double y, int left, int top, int width, int height) {
        return x >= left && x < left + width && y >= top && y < top + height;
    }

    @Unique
    private record RbipScrollArea(int left, int top, int right, int bottom) {
        private int width() {
            return this.right - this.left;
        }

        private int height() {
            return this.bottom - this.top;
        }
    }
}
