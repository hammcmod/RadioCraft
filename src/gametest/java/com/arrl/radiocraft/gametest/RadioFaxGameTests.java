package com.arrl.radiocraft.gametest;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.be_networks.network_objects.RadioNetworkObject;
import com.arrl.radiocraft.common.blockentities.RadioFaxBlockEntity;
import com.arrl.radiocraft.common.blockentities.RadioInterfaceBlockEntity;
import com.arrl.radiocraft.common.blockentities.radio.RadioBlockEntity;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import com.arrl.radiocraft.common.radio.Band;
import com.arrl.radiocraft.common.radio.data.FaxRates;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Objects;

/**
 * The radio fax: documents go to a callsign as thirty acknowledged packets and print on the far machine's blanks. The
 * stations have no antennas and talk through their bare connectors (8 blocks), so each test stays in its plot and gets a
 * frequency of its own: two blocks apart the signal is solid (a bare connector's strength counts at both ends) and no
 * packet is lost; four apart, over VHF's noise floor, about a third are.
 */
@GameTestHolder(Radiocraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class RadioFaxGameTests {

    private static final String BATCH = "fax";

    private static final BlockPos RADIO_A = new BlockPos(3, 2, 6);
    private static final BlockPos RADIO_B = RADIO_A.east(2);
    private static final BlockPos FAX_A = RADIO_A.north().west();
    private static final BlockPos FAX_B = RADIO_B.north().east(); // Away from A's gear: touching it would join the two buses.

    private static RadioBlockEntity radio(GameTestHelper helper, BlockPos pos) {
        return GameTestUtil.blockEntity(helper, pos, RadioBlockEntity.class);
    }

    private static RadioFaxBlockEntity fax(GameTestHelper helper, BlockPos pos) {
        return GameTestUtil.blockEntity(helper, pos, RadioFaxBlockEntity.class);
    }

    /** A VHF base station on {@code kHz}, a radio interface north of it set to {@code callsign}, and a fax beside that. */
    private static void station(GameTestHelper helper, BlockPos radioPos, int kHz, String callsign, Direction faxSide) {
        BlockPos interfacePos = radioPos.north();
        helper.setBlock(radioPos, RadiocraftBlocks.VHF_BASE_STATION.get());
        helper.setBlock(interfacePos, RadiocraftBlocks.RADIO_INTERFACE.get());
        helper.setBlock(interfacePos.relative(faxSide), RadiocraftBlocks.RADIO_FAXES.get("oak").get());
        radio(helper, radioPos).setFrequency(kHz * 1000.0F);
        GameTestUtil.blockEntity(helper, interfacePos, RadioInterfaceBlockEntity.class).setCallsign(callsign, "gametest");
    }

    private static void powerOn(GameTestHelper helper, BlockPos pos) {
        RadioNetworkObject networkObject = GameTestUtil.networkObject(helper, pos, RadioNetworkObject.class);
        if(!networkObject.isPowered)
            radio(helper, pos).toggle();
        helper.assertTrue(networkObject.isPowered, "Radio at " + pos.toShortString() + " did not power on");
    }

    private static ItemStack book(int generation) {
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        book.set(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(Filterable.passThrough("Station Log"), "W1AW", generation,
                List.of(Filterable.passThrough(Component.literal("Page one")), Filterable.passThrough(Component.literal("Page two"))), true));
        return book;
    }

    private static int generation(ItemStack stack) {
        WrittenBookContent content = stack.get(DataComponents.WRITTEN_BOOK_CONTENT);
        return content == null ? -1 : content.generation();
    }

    private static ItemStack printed(GameTestHelper helper, BlockPos faxPos) {
        return fax(helper, faxPos).getReceived().getStackInSlot(0);
    }

    private static String state(GameTestHelper helper, BlockPos faxPos) {
        RadioFaxBlockEntity fax = fax(helper, faxPos);
        return fax.getPhase() + " " + fax.getStatus() + " " + fax.getPacketsDone() + "/" + FaxRates.PACKETS + " retries " + fax.getRetries() + " log " + fax.getLog();
    }

    /** A book faxed to B's callsign prints at B as a copy on B's blank book, after all thirty packets; A keeps the original. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 900, batch = BATCH)
    public static void bookArrivesAsCopy(GameTestHelper helper) {
        station(helper, RADIO_A, 144_710, "W1FAA", Direction.WEST);
        station(helper, RADIO_B, 144_710, "W1FBB", Direction.EAST);
        helper.startSequence()
                .thenExecute(() -> {
                    powerOn(helper, RADIO_A);
                    powerOn(helper, RADIO_B);
                    fax(helper, FAX_A).getOutbox().setStackInSlot(0, book(0));
                    fax(helper, FAX_B).insertSupply(new ItemStack(Items.BOOK, 2));
                })
                .thenIdle(1)
                .thenExecute(() -> helper.assertTrue(fax(helper, FAX_A).send("w1fbb") == null, "A could not send the book"))
                .thenWaitUntil(() -> helper.assertTrue(fax(helper, FAX_B).getPhase() == RadioFaxBlockEntity.Phase.RECEIVING, "B did not answer the call: " + state(helper, FAX_B)))
                .thenWaitUntil(() -> helper.assertFalse(printed(helper, FAX_B).isEmpty(), "B never printed the book: " + state(helper, FAX_B) + " / A " + state(helper, FAX_A)))
                .thenWaitUntil(() -> helper.assertFalse(fax(helper, FAX_A).isBusy(), "A never heard the last ack: " + state(helper, FAX_A)))
                .thenExecute(() -> {
                    ItemStack copy = printed(helper, FAX_B);
                    helper.assertTrue(copy.is(Items.WRITTEN_BOOK) && generation(copy) == 1, "B printed " + copy + " generation " + generation(copy));
                    helper.assertTrue(fax(helper, FAX_B).getSupplies().getStackInSlot(RadioFaxBlockEntity.SLOT_BOOK).getCount() == 1, "B did not use one blank book");
                    helper.assertTrue(generation(fax(helper, FAX_A).getOutbox().getStackInSlot(0)) == 0, "A lost its original");
                    helper.assertTrue(fax(helper, FAX_B).getPacketsDone() == FaxRates.PACKETS, "B took " + fax(helper, FAX_B).getPacketsDone() + " packets");
                    helper.assertTrue(fax(helper, FAX_A).getRecent().contains("W1FBB"), "A did not remember W1FBB: " + fax(helper, FAX_A).getRecent());
                    helper.assertTrue(fax(helper, FAX_A).getStatus().startsWith("SENT"), "A shows " + fax(helper, FAX_A).getStatus());
                })
                .thenSucceed();
    }

    /** Nobody answers a callsign that is not there; a station with no blank says so; a copy of a copy cannot be sent. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 700, batch = BATCH)
    public static void faxNeedsAnAnswerAndABlank(GameTestHelper helper) {
        station(helper, RADIO_A, 144_730, "W2FAA", Direction.WEST);
        station(helper, RADIO_B, 144_730, "W2FBB", Direction.EAST);
        helper.startSequence()
                .thenExecute(() -> {
                    powerOn(helper, RADIO_A);
                    powerOn(helper, RADIO_B);
                    fax(helper, FAX_A).getOutbox().setStackInSlot(0, book(2));
                })
                .thenIdle(1)
                .thenExecute(() -> {
                    helper.assertTrue(fax(helper, FAX_A).send("W2FBB") != null, "A sent a copy of a copy");
                    fax(helper, FAX_A).getOutbox().setStackInSlot(0, book(0));
                    helper.assertTrue(fax(helper, FAX_A).send("") != null, "A sent to nobody");
                    helper.assertTrue(fax(helper, FAX_A).send("W2XYZ") == null, "A could not call");
                })
                .thenWaitUntil(() -> helper.assertTrue(fax(helper, FAX_A).getStatus().equals("NO ANSWER"), "A shows " + state(helper, FAX_A)))
                .thenExecute(() -> {
                    helper.assertTrue(fax(helper, FAX_B).getLog().isEmpty(), "B took a call for W2XYZ: " + fax(helper, FAX_B).getLog());
                    helper.assertTrue(fax(helper, FAX_A).send("W2FBB") == null, "A could not call B");
                })
                .thenWaitUntil(() -> helper.assertTrue(fax(helper, FAX_A).getStatus().equals("W2FBB: NO BOOK"), "A shows " + state(helper, FAX_A)))
                .thenExecute(() -> {
                    helper.assertTrue(printed(helper, FAX_B).isEmpty(), "B printed without a blank");
                    helper.assertTrue(fax(helper, FAX_B).getPhase() == RadioFaxBlockEntity.Phase.IDLE, "B is " + state(helper, FAX_B));
                })
                .thenSucceed();
    }

    /** A map takes twenty seconds on VHF and prints on an empty map: the same map. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 900, batch = BATCH)
    public static void mapTakesTwentySecondsOnVhf(GameTestHelper helper) {
        station(helper, RADIO_A, 144_750, "W3FAA", Direction.WEST);
        station(helper, RADIO_B, 144_750, "W3FBB", Direction.EAST);
        ItemStack map = MapItem.create(helper.getLevel(), 0, 0, (byte)0, false, false);
        helper.startSequence()
                .thenExecute(() -> {
                    powerOn(helper, RADIO_A);
                    powerOn(helper, RADIO_B);
                    fax(helper, FAX_A).getOutbox().setStackInSlot(0, map.copy());
                    fax(helper, FAX_B).insertSupply(new ItemStack(Items.MAP));
                })
                .thenIdle(1)
                .thenExecute(() -> helper.assertTrue(fax(helper, FAX_A).send("W3FBB") == null, "A could not send the map"))
                .thenIdle(300)
                .thenExecute(() -> helper.assertTrue(printed(helper, FAX_B).isEmpty(), "The map arrived in under fifteen seconds: " + state(helper, FAX_B)))
                .thenWaitUntil(() -> helper.assertFalse(printed(helper, FAX_B).isEmpty(), "B never printed the map: " + state(helper, FAX_B) + " / A " + state(helper, FAX_A)))
                .thenExecute(() -> {
                    ItemStack copy = printed(helper, FAX_B);
                    helper.assertTrue(copy.is(Items.FILLED_MAP) && Objects.equals(copy.get(DataComponents.MAP_ID), map.get(DataComponents.MAP_ID)), "B printed " + copy);
                    helper.assertTrue(fax(helper, FAX_A).getRetries() == 0, "A had to send packets again over a solid signal: " + state(helper, FAX_A));
                })
                .thenSucceed();
    }

    /** An enchanted book is used up when sent: A's slot empties and B prints the same book. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 1400, batch = BATCH)
    public static void enchantedBookIsUsedUp(GameTestHelper helper) {
        station(helper, RADIO_A, 144_770, "W4FAA", Direction.WEST);
        station(helper, RADIO_B, 144_770, "W4FBB", Direction.EAST);
        ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
        book.enchant(helper.getLevel().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(Enchantments.UNBREAKING), 2);
        helper.startSequence()
                .thenExecute(() -> {
                    powerOn(helper, RADIO_A);
                    powerOn(helper, RADIO_B);
                    fax(helper, FAX_A).getOutbox().setStackInSlot(0, book.copy());
                    fax(helper, FAX_B).insertSupply(new ItemStack(Items.BOOK));
                })
                .thenIdle(1)
                .thenExecute(() -> helper.assertTrue(fax(helper, FAX_A).send("W4FBB") == null, "A could not send the enchanted book"))
                .thenWaitUntil(() -> helper.assertFalse(printed(helper, FAX_B).isEmpty(), "B never printed the book: " + state(helper, FAX_B) + " / A " + state(helper, FAX_A)))
                .thenWaitUntil(() -> helper.assertFalse(fax(helper, FAX_A).isBusy(), "A never heard the last ack: " + state(helper, FAX_A)))
                .thenExecute(() -> {
                    ItemStack copy = printed(helper, FAX_B);
                    helper.assertTrue(copy.is(Items.ENCHANTED_BOOK) && EnchantmentHelper.getEnchantmentsForCrafting(copy).equals(EnchantmentHelper.getEnchantmentsForCrafting(book)),
                            "B printed " + copy);
                    helper.assertTrue(fax(helper, FAX_A).getOutbox().getStackInSlot(0).isEmpty(), "A kept the enchanted book");
                })
                .thenSucceed();
    }

    /** A fax touching the radio, with no radio interface, works under its own callsign. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 900, batch = BATCH)
    public static void worksStraightOffTheRadio(GameTestHelper helper) {
        BlockPos faxA = RADIO_A.west();
        helper.setBlock(RADIO_A, RadiocraftBlocks.VHF_BASE_STATION.get());
        helper.setBlock(faxA, RadiocraftBlocks.RADIO_FAXES.get("spruce").get());
        radio(helper, RADIO_A).setFrequency(144_790_000.0F);
        station(helper, RADIO_B, 144_790, "W5FBB", Direction.EAST);
        helper.startSequence()
                .thenExecute(() -> {
                    powerOn(helper, RADIO_A);
                    powerOn(helper, RADIO_B);
                    fax(helper, faxA).setCallsign("W5FAA");
                    fax(helper, faxA).getOutbox().setStackInSlot(0, new ItemStack(Items.PAPER));
                    fax(helper, FAX_B).insertSupply(new ItemStack(Items.PAPER));
                })
                .thenIdle(1)
                .thenExecute(() -> {
                    helper.assertTrue(fax(helper, faxA).myCallsign().equals("W5FAA"), "A's fax is " + fax(helper, faxA).myCallsign());
                    helper.assertTrue(fax(helper, faxA).send("W5FBB") == null, "A could not send the paper");
                })
                .thenWaitUntil(() -> helper.assertFalse(printed(helper, FAX_B).isEmpty(), "B never printed the paper: " + state(helper, FAX_B) + " / A " + state(helper, faxA)))
                .thenExecute(() -> helper.assertTrue(fax(helper, FAX_B).getLog().getFirst().equals("RX W5FAA: PAPER"), "B logged " + fax(helper, FAX_B).getLog()))
                .thenSucceed();
    }

    private static final String WEAK_BATCH = "fax_weak";

    /**
     * Packet loss is random: at the default rate a whole fax went through without a loss once (2026-10-28). Its own
     * batch raises the rate a fifth (at half again the fax ran out of time, doubled it gave up), so a loss is all but certain, without slowing the other fax tests.
     */
    @net.minecraft.gametest.framework.BeforeBatch(batch = WEAK_BATCH)
    public static void lossyFax(net.minecraft.server.level.ServerLevel level) {
        com.arrl.radiocraft.RadiocraftServerConfig.FAX_PACKET_LOSS.set(Math.min(1.0D, com.arrl.radiocraft.RadiocraftServerConfig.FAX_PACKET_LOSS.getDefault() * 1.2D));
    }

    @net.minecraft.gametest.framework.AfterBatch(batch = WEAK_BATCH)
    public static void normalFax(net.minecraft.server.level.ServerLevel level) {
        com.arrl.radiocraft.RadiocraftServerConfig.FAX_PACKET_LOSS.set(com.arrl.radiocraft.RadiocraftServerConfig.FAX_PACKET_LOSS.getDefault());
    }

    /** Four blocks apart the signal is weak: packets are lost and sent again, and the fax still gets through. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 6000, batch = WEAK_BATCH)
    public static void weakSignalSendsPacketsAgain(GameTestHelper helper) {
        BlockPos radioB = RADIO_A.east(4);
        station(helper, RADIO_A, 144_810, "W6FAA", Direction.WEST);
        station(helper, radioB, 144_810, "W6FBB", Direction.EAST);
        BlockPos faxB = radioB.north().east();
        helper.startSequence()
                .thenExecute(() -> {
                    powerOn(helper, RADIO_A);
                    powerOn(helper, radioB);
                    fax(helper, FAX_A).getOutbox().setStackInSlot(0, new ItemStack(Items.PAPER));
                    fax(helper, faxB).insertSupply(new ItemStack(Items.PAPER));
                })
                .thenIdle(1)
                .thenExecute(() -> helper.assertTrue(fax(helper, FAX_A).send("W6FBB") == null, "A could not send"))
                .thenWaitUntil(() -> helper.assertFalse(printed(helper, faxB).isEmpty(), "B never printed: " + state(helper, faxB) + " / A " + state(helper, FAX_A)))
                .thenExecute(() -> helper.assertTrue(fax(helper, FAX_A).getRetries() > 0, "No packet was lost over a weak signal: " + state(helper, FAX_A)))
                .thenSucceed();
    }

    /** The rates: a map is 20 seconds on VHF, twice as slow on 10m, eight times on 80m, and ten times as fast on a link. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 20, batch = BATCH)
    public static void ratesFollowTheBand(GameTestHelper helper) {
        ItemStack map = new ItemStack(Items.FILLED_MAP);
        helper.assertTrue(FaxRates.totalTicks(map, Band.getBand("2m")) == 400, "VHF map: " + FaxRates.totalTicks(map, Band.getBand("2m")));
        helper.assertTrue(FaxRates.totalTicks(map, Band.getBand("10m")) == 800, "10m map: " + FaxRates.totalTicks(map, Band.getBand("10m")));
        helper.assertTrue(FaxRates.totalTicks(map, Band.getBand("80m")) == 3200, "80m map: " + FaxRates.totalTicks(map, Band.getBand("80m")));
        helper.assertTrue(FaxRates.totalTicks(book(0), Band.getBand("2m")) == 400, "VHF two-page book: " + FaxRates.totalTicks(book(0), Band.getBand("2m")));
        helper.assertTrue(FaxRates.totalTicks(new ItemStack(Items.ENCHANTED_BOOK), Band.getBand("2m")) == 800, "VHF enchanted book");
        helper.assertTrue(FaxRates.linkPacketTicks(map) * FaxRates.PACKETS <= 60, "Link map: " + FaxRates.linkPacketTicks(map) * FaxRates.PACKETS + " ticks");
        helper.assertTrue(FaxRates.lossChance(1.0D, 0.15D) == 0.0D && FaxRates.lossChance(0.25D, 0.15D) > 0.1D, "Loss " + FaxRates.lossChance(0.25D, 0.15D));
        helper.succeed();
    }
}
