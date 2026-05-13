package me.melontini.recipebookispain.mixin.groups;

import me.melontini.recipebookispain.RecipeBookIsPain;
import net.minecraft.client.recipebook.ClientRecipeBook;
import net.minecraft.client.gui.screen.recipebook.RecipeResultCollection;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.NetworkRecipeId;
import net.minecraft.recipe.RecipeDisplayEntry;
import net.minecraft.recipe.book.RecipeBookGroup;
import net.minecraft.util.context.ContextParameterMap;
import net.minecraft.util.context.ContextType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;

@Mixin(value = ClientRecipeBook.class, priority = 999)
public class ClientRecipeBookMixin {

    @Shadow @Final private Map<NetworkRecipeId, RecipeDisplayEntry> recipes;
    @Shadow private Map<RecipeBookGroup, List<RecipeResultCollection>> resultsByCategory;

    @Unique
    private static final ContextParameterMap RBIP_EMPTY_CONTEXT = new ContextParameterMap.Builder().build(new ContextType.Builder().build());

    @Inject(at = @At("TAIL"), method = "refresh")
    private void rbip$refreshCreativeGroups(CallbackInfo ci) {
        RecipeBookIsPain.ensureInitialized();
        if (RecipeBookIsPain.RECIPE_BOOK_GROUP_TO_ITEM_GROUP.isEmpty()) return;

        Map<RecipeBookGroup, EntryBucket> buckets = new LinkedHashMap<>();
        for (RecipeDisplayEntry entry : this.recipes.values()) {
            RecipeBookGroup group = rbip$getGroupForEntry(entry);
            if (group == null) continue;
            buckets.computeIfAbsent(group, ignored -> new EntryBucket()).add(entry);
        }

        if (buckets.isEmpty()) return;

        Map<RecipeBookGroup, List<RecipeResultCollection>> updatedResults = new HashMap<>(this.resultsByCategory);
        buckets.forEach((group, bucket) -> updatedResults.put(group, bucket.toCollections()));
        this.resultsByCategory = Map.copyOf(updatedResults);
    }

    @Unique
    private static RecipeBookGroup rbip$getGroupForEntry(RecipeDisplayEntry entry) {
        try {
            for (ItemStack stack : entry.getStacks(RBIP_EMPTY_CONTEXT)) {
                RecipeBookGroup group = RecipeBookIsPain.toRecipeBookGroup(stack);
                if (group != null) return group;
            }
        } catch (Exception e) {
            RecipeBookIsPain.LOGGER.debug("[RBIP] Could not resolve output stack for recipe display {}", entry.id(), e);
        }
        return null;
    }

    private static class EntryBucket {
        private final List<List<RecipeDisplayEntry>> entries = new ArrayList<>();
        private final Map<Integer, List<RecipeDisplayEntry>> groupedEntries = new LinkedHashMap<>();

        private void add(RecipeDisplayEntry entry) {
            OptionalInt group = entry.group();
            if (group.isEmpty()) {
                this.entries.add(List.of(entry));
                return;
            }

            List<RecipeDisplayEntry> grouped = this.groupedEntries.get(group.getAsInt());
            if (grouped == null) {
                grouped = new ArrayList<>();
                this.groupedEntries.put(group.getAsInt(), grouped);
                this.entries.add(grouped);
            }
            grouped.add(entry);
        }

        private List<RecipeResultCollection> toCollections() {
            return this.entries.stream()
                    .map(entries -> new RecipeResultCollection(List.copyOf(entries)))
                    .toList();
        }
    }
}
