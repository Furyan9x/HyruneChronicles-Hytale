package dev.hytalemodding.hyrune.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hytalemodding.hyrune.itemization.ItemInstanceMetadata;
import dev.hytalemodding.hyrune.itemization.ItemizationEligibilityService;
import dev.hytalemodding.hyrune.repair.ItemRarity;
import dev.hytalemodding.hyrune.repair.RepairMaterialCost;
import dev.hytalemodding.hyrune.repair.SalvagePolicy;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.StringJoiner;

/**
 * Milestone 7 first-pass salvage loop for held itemized items.
 */
public class SalvageCommand extends AbstractPlayerCommand {
    private static final int DEFAULT_BENCH_TIER = 1;

    public SalvageCommand() {
        super("salvage", "Salvage the held item for crafting materials.");
    }

    @Override
    protected void execute(@Nonnull CommandContext ctx,
                           @Nonnull Store<EntityStore> store,
                           @Nonnull Ref<EntityStore> ref,
                           @Nonnull PlayerRef playerRef,
                           @Nonnull World world) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            ctx.sendMessage(Message.raw("Player is unavailable."));
            return;
        }

        Inventory inventory = player.getInventory();
        ItemContainer combined = ItemSinkCommandSupport.combinedContainer(player);
        if (inventory == null || combined == null) {
            ctx.sendMessage(Message.raw("Inventory is unavailable."));
            return;
        }

        ItemStack held = inventory.getItemInHand();
        if (held == null || held.isEmpty() || held.getItemId() == null) {
            ctx.sendMessage(Message.raw("Hold an item to salvage."));
            return;
        }
        if (!ItemizationEligibilityService.isEligible(held)) {
            ctx.sendMessage(Message.raw("Held item is not salvage-eligible."));
            return;
        }
        ItemInstanceMetadata metadata = held.getFromMetadataOrNull(ItemInstanceMetadata.KEYED_CODEC);
        if (metadata == null) {
            ctx.sendMessage(Message.raw("Held item has no itemization metadata."));
            return;
        }

        ItemRarity rarity = metadata.getRarity();
        List<RepairMaterialCost> returns = SalvagePolicy.estimateSalvageReturns(held, rarity, DEFAULT_BENCH_TIER);
        if (returns.isEmpty()) {
            ctx.sendMessage(Message.raw("No salvage output available for this item."));
            return;
        }

        if (!ItemSinkCommandSupport.consumeOneFromHand(inventory, held)) {
            ctx.sendMessage(Message.raw("Failed to consume held item."));
            return;
        }

        for (RepairMaterialCost out : returns) {
            ItemSinkCommandSupport.payoutOrDrop(ref, store, combined, new ItemStack(out.getItemId(), out.getQuantity()));
        }

        ctx.sendMessage(Message.raw("[Salvage] Consumed 1x " + held.getItemId()
            + " (" + rarity.name() + ") -> " + summarize(returns)));
    }

    private static String summarize(List<RepairMaterialCost> outputs) {
        StringJoiner joiner = new StringJoiner(", ");
        for (RepairMaterialCost output : outputs) {
            joiner.add(output.getQuantity() + "x " + output.getItemId());
        }
        return joiner.toString();
    }
}

