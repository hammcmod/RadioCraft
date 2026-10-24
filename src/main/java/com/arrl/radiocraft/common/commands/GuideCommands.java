package com.arrl.radiocraft.common.commands;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.RadiocraftServerConfig;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * The RadioCraft guide book (a Patchouli book, see assets/radiocraft/patchouli_books): given once on a player's first
 * join ({@code guide_on_first_join}), and any time with {@code /radiocraft guide} or {@code /radio guide}. Patchouli is
 * optional, so the book is looked up by name; without Patchouli nothing is given.
 */
@EventBusSubscriber(modid = Radiocraft.MOD_ID)
public class GuideCommands {

    private static final ResourceLocation GUIDE_BOOK_ITEM = ResourceLocation.fromNamespaceAndPath("patchouli", "guide_book");
    private static final ResourceLocation BOOK_COMPONENT = ResourceLocation.fromNamespaceAndPath("patchouli", "book");
    private static final ResourceLocation BOOK = Radiocraft.id("guide");
    /** Kept in the player's persisted data, which survives death. */
    private static final String GIVEN_TAG = "radiocraft_guide_given";

    public static final LiteralArgumentBuilder<CommandSourceStack> BUILDER = root("radiocraft");
    public static final LiteralArgumentBuilder<CommandSourceStack> SHORT_BUILDER = root("radio");

    private static LiteralArgumentBuilder<CommandSourceStack> root(String name) {
        return Commands.literal(name).then(Commands.literal("guide").executes(GuideCommands::give));
    }

    private static int give(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        if(!giveBook(player)) {
            ctx.getSource().sendFailure(Component.translatable(Radiocraft.translationKey("commands", "guide.no_patchouli")));
            return 0;
        }
        ctx.getSource().sendSuccess(() -> Component.translatable(Radiocraft.translationKey("commands", "guide.given"), player.getDisplayName()), false);
        return 1;
    }

    /** The guide book, or empty if Patchouli is not installed. */
    @SuppressWarnings("unchecked")
    public static ItemStack guideBook() {
        Item item = BuiltInRegistries.ITEM.get(GUIDE_BOOK_ITEM);
        DataComponentType<?> component = BuiltInRegistries.DATA_COMPONENT_TYPE.get(BOOK_COMPONENT);
        if(item == Items.AIR || component == null)
            return ItemStack.EMPTY;
        ItemStack stack = new ItemStack(item);
        stack.set((DataComponentType<ResourceLocation>)component, BOOK);
        return stack;
    }

    /** Puts a guide book in the player's inventory (or at their feet if it is full). False without Patchouli. */
    public static boolean giveBook(Player player) {
        ItemStack book = guideBook();
        if(book.isEmpty())
            return false;
        if(!player.getInventory().add(book))
            player.drop(book, false);
        return true;
    }

    @SubscribeEvent
    public static void onPlayerJoined(PlayerEvent.PlayerLoggedInEvent event) {
        if(!(event.getEntity() instanceof ServerPlayer player) || !RadiocraftServerConfig.GUIDE_ON_FIRST_JOIN.get())
            return;
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        if(persisted.getBoolean(GIVEN_TAG))
            return;
        if(giveBook(player)) {
            persisted.putBoolean(GIVEN_TAG, true);
            player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
        }
    }
}
