package me.melontini.recipebookispain;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import io.wispforest.owo.itemgroup.OwoItemGroup;
import me.melontini.recipebookispain.access.ItemAccess;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.recipebook.RecipeGroupButtonWidget;
import net.minecraft.client.gui.screen.recipebook.RecipeBookWidget;
import net.minecraft.client.recipebook.RecipeBookType;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.book.RecipeBookCategory;
import net.minecraft.recipe.book.RecipeBookGroup;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Environment(EnvType.CLIENT)
public class RecipeBookIsPain {

    public static final Logger LOGGER = LogManager.getLogger("RBIP");
    public static final boolean isOwOLoaded = FabricLoader.getInstance().isModLoaded("owo");

    public static final List<RecipeBookGroup> CRAFTING_SEARCH_LIST = new ArrayList<>();
    public static final List<RecipeBookGroup> CRAFTING_LIST = new ArrayList<>();
    public static final BiMap<RecipeBookGroup, ItemGroup> RECIPE_BOOK_GROUP_TO_ITEM_GROUP = HashBiMap.create();
    private static final List<ItemGroup> MIRRORED_ITEM_GROUPS = new ArrayList<>();
    private static boolean initialized;

    public static synchronized void ensureInitialized() {
        if (initialized) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.world == null) {
            LOGGER.debug("[RBIP] Delaying recipe book init until a client world is available");
            return;
        }

        try {
            ItemGroups.updateDisplayContext(FeatureFlags.FEATURE_MANAGER.getFeatureSet(), false, client.world.getRegistryManager());
        } catch (Exception e) {
            LOGGER.warn("[RBIP] Could not refresh creative item groups before building recipe book tabs", e);
        }

        ItemGroups.getGroups().stream().filter(RecipeBookIsPain::shouldMirror).forEach(itemGroup -> {
            try {
                itemGroup.getSearchTabStacks().stream()
                        .filter(stack -> !stack.isEmpty())
                        .map(ItemStack::getItem)
                        .map(ItemAccess.class::cast)
                        .filter(access -> access.rbip$getPossibleGroup().isEmpty())
                        .forEach(access -> access.rbip$setPossibleGroup(itemGroup));

                RecipeBookGroup recipeBookGroup = new RecipeBookCategory();
                RECIPE_BOOK_GROUP_TO_ITEM_GROUP.put(recipeBookGroup, itemGroup);
                MIRRORED_ITEM_GROUPS.add(itemGroup);
                CRAFTING_LIST.add(recipeBookGroup);
                CRAFTING_SEARCH_LIST.add(recipeBookGroup);
            } catch (Exception e) {
                LOGGER.error("[RBIP] Error while processing {} item group", itemGroup.getDisplayName(), e);
            }
        });

        initialized = true;
        LOGGER.info("[RBIP] recipe book init complete; mirrored {} creative groups", MIRRORED_ITEM_GROUPS.size());
    }

    private static boolean shouldMirror(ItemGroup itemGroup) {
        return itemGroup.getType() != ItemGroup.Type.INVENTORY
                && itemGroup.getType() != ItemGroup.Type.HOTBAR
                && itemGroup.getType() != ItemGroup.Type.SEARCH;
    }

    public static ItemGroup toItemGroup(RecipeBookGroup recipeBookGroup) {
        return RECIPE_BOOK_GROUP_TO_ITEM_GROUP.get(recipeBookGroup);
    }

    public static RecipeBookGroup toRecipeBookGroup(ItemGroup itemGroup) {
        return RECIPE_BOOK_GROUP_TO_ITEM_GROUP.inverse().get(itemGroup);
    }

    public static RecipeBookGroup toRecipeBookGroup(ItemStack stack) {
        ensureInitialized();
        if (stack.isEmpty()) return null;
        return ((ItemAccess) stack.getItem()).rbip$getPossibleGroup()
                .map(RecipeBookIsPain::toRecipeBookGroup)
                .orElse(null);
    }

    public static List<RecipeBookWidget.Tab> withCreativeTabs(List<RecipeBookWidget.Tab> tabs) {
        ensureInitialized();
        List<RecipeBookWidget.Tab> expandedTabs = new ArrayList<>();
        tabs.stream()
                .filter(tab -> tab.category() instanceof RecipeBookType)
                .findFirst()
                .ifPresent(expandedTabs::add);

        for (ItemGroup itemGroup : MIRRORED_ITEM_GROUPS) {
            Optional.ofNullable(toRecipeBookGroup(itemGroup))
                    .map(group -> new RecipeBookWidget.Tab(itemGroup.getIcon(), Optional.empty(), group))
                    .ifPresent(expandedTabs::add);
        }
        return expandedTabs;
    }

    public static boolean rbip$renderOwo(DrawContext context, int i, RecipeGroupButtonWidget widget, ItemGroup group) {
        return rbip$renderOwo(context, widget.getX() + 9 + i, widget.getY() + 5, group);
    }

    public static boolean rbip$renderOwo(DrawContext context, int x, int y, ItemGroup group) {
        if (group instanceof OwoItemGroup owoItemGroup) {
            MinecraftClient client = MinecraftClient.getInstance();
            double e = client.mouse.getX() * client.getWindow().getScaledWidth() / client.getWindow().getWidth();
            double f = client.mouse.getY() * client.getWindow().getScaledHeight() / client.getWindow().getHeight();
            owoItemGroup.icon().render(context, x, y, (int) e, (int) f, client.getRenderTickCounter().getTickProgress(true));
            return true;
        }
        return false;
    }

}
