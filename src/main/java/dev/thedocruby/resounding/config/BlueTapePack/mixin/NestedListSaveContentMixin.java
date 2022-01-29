package dev.thedocruby.resounding.config.BlueTapePack.mixin;

import me.shedaniel.clothconfig2.api.ReferenceProvider;
import me.shedaniel.clothconfig2.gui.entries.AbstractListListEntry;
import me.shedaniel.clothconfig2.gui.entries.NestedListListEntry;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;

@SuppressWarnings("ALL")
@Mixin(NestedListListEntry.class)
public abstract class NestedListSaveContentMixin extends AbstractListListEntry {
    @Shadow @Final private List<ReferenceProvider<?>> referencableEntries;

    public NestedListSaveContentMixin(Text fieldName, List value, boolean defaultExpanded, Supplier tooltipSupplier, Consumer saveConsumer, Supplier defaultValue, Text resetButtonKey, boolean requiresRestart, boolean deleteButtonEnabled, boolean insertInFront, BiFunction createNewCell) {
        super(fieldName, value, defaultExpanded, tooltipSupplier, saveConsumer, defaultValue, resetButtonKey, requiresRestart, deleteButtonEnabled, insertInFront, createNewCell);
    }

    @Override
    public void save(){
        try {
            referencableEntries.forEach((e) -> e.provideReferenceEntry().save());
        } catch (Exception ignored) {}
        super.save();
    }
}
