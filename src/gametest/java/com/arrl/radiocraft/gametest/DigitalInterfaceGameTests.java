package com.arrl.radiocraft.gametest;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.be_networks.network_objects.RadioNetworkObject;
import com.arrl.radiocraft.common.blockentities.DigitalInterfaceBlockEntity;
import com.arrl.radiocraft.common.blockentities.radio.RadioBlockEntity;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import com.arrl.radiocraft.common.radio.data.TncPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.component.WrittenBookContent;

import java.util.List;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The digital interface (TNC) and APRS: beacons and messages sent as packets through VHF base stations, decoded only on
 * frequency, digipeated by an interface between two radios, and the admin command. The stations have no antennas and
 * talk through their bare connectors (8 blocks), so each test stays in its plot; each test has its own frequencies too.
 */
@GameTestHolder(Radiocraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class DigitalInterfaceGameTests {

    private static final String BATCH = "digital_interface";

    private static final BlockPos RADIO_A = new BlockPos(3, 2, 6);
    private static final BlockPos RADIO_B = new BlockPos(7, 2, 6);
    private static final BlockPos TNC_A = RADIO_A.north();
    private static final BlockPos TNC_B = RADIO_B.north();

    private static RadioBlockEntity radio(GameTestHelper helper, BlockPos pos) {
        return GameTestUtil.blockEntity(helper, pos, RadioBlockEntity.class);
    }

    private static DigitalInterfaceBlockEntity tnc(GameTestHelper helper, BlockPos pos) {
        return GameTestUtil.blockEntity(helper, pos, DigitalInterfaceBlockEntity.class);
    }

    /** A VHF base station on {@code kHz} with a digital interface north of it set to {@code callsign}. */
    private static void station(GameTestHelper helper, BlockPos radioPos, int kHz, BlockPos tncPos, String callsign) {
        helper.setBlock(radioPos, RadiocraftBlocks.VHF_BASE_STATION.get());
        helper.setBlock(tncPos, RadiocraftBlocks.DIGITAL_INTERFACE.get());
        radio(helper, radioPos).setFrequency(kHz * 1000.0F);
        tnc(helper, tncPos).setCallsign(callsign, "gametest");
    }

    private static void powerOn(GameTestHelper helper, BlockPos pos) {
        RadioNetworkObject networkObject = GameTestUtil.networkObject(helper, pos, RadioNetworkObject.class);
        if(!networkObject.isPowered)
            radio(helper, pos).toggle();
        helper.assertTrue(networkObject.isPowered, "Radio at " + pos.toShortString() + " did not power on");
    }

    private static boolean heardBeaconFrom(GameTestHelper helper, BlockPos tncPos, String callsign) {
        return tnc(helper, tncPos).getBeacons().stream().anyMatch(heard -> heard.packet().from().equals(callsign));
    }

    /** A beacon goes out as a burst and arrives whole at a station on the frequency, with the sender's position. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 200, batch = BATCH)
    public static void beaconReachesStationOnFrequency(GameTestHelper helper) {
        station(helper, RADIO_A, 144_390, TNC_A, "W1AAA");
        station(helper, RADIO_B, 144_390, TNC_B, "W1BBB");
        helper.startSequence()
                .thenExecute(() -> {
                    powerOn(helper, RADIO_A);
                    powerOn(helper, RADIO_B);
                })
                .thenIdle(1) // Bare connectors register on the next tick.
                .thenExecute(() -> {
                    helper.assertTrue(tnc(helper, TNC_A).beacon(), "A did not send its beacon");
                    helper.assertTrue(radio(helper, RADIO_A).isSendingData(), "A's radio is not sending the burst");
                    helper.assertFalse(heardBeaconFrom(helper, TNC_B, "W1AAA"), "The beacon arrived before its burst was over");
                })
                .thenWaitUntil(() -> helper.assertTrue(heardBeaconFrom(helper, TNC_B, "W1AAA"), "B never heard A's beacon"))
                .thenExecute(() -> {
                    TncPacket packet = tnc(helper, TNC_B).getBeacons().getFirst().packet();
                    helper.assertTrue(helper.absolutePos(TNC_A).equals(packet.position()), "The beacon carries " + packet.position() + ", not A's position");
                    helper.assertTrue(packet.hops() == 0, "A direct beacon came through " + packet.hops() + " digipeaters");
                })
                .thenSucceed();
    }

    /** A station tuned a channel away does not decode the packet. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 200, batch = BATCH)
    public static void offFrequencyDoesNotDecode(GameTestHelper helper) {
        station(helper, RADIO_A, 144_410, TNC_A, "W2AAA");
        station(helper, RADIO_B, 144_430, TNC_B, "W2BBB");
        helper.startSequence()
                .thenExecute(() -> {
                    powerOn(helper, RADIO_A);
                    powerOn(helper, RADIO_B);
                })
                .thenIdle(1)
                .thenExecute(() -> tnc(helper, TNC_A).beacon())
                .thenWaitUntil(() -> helper.assertFalse(radio(helper, RADIO_A).isSendingData(), "A is still sending"))
                .thenIdle(2)
                .thenExecute(() -> helper.assertTrue(tnc(helper, TNC_B).getBeacons().isEmpty(), "B decoded a packet 20 kHz off its frequency"))
                .thenSucceed();
    }

    /** A message to B's callsign lands in B's log as unread and lights its comparator; one to someone else does not. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 300, batch = BATCH)
    public static void messageToCallsignIsUnread(GameTestHelper helper) {
        station(helper, RADIO_A, 144_450, TNC_A, "W3AAA");
        station(helper, RADIO_B, 144_450, TNC_B, "W3BBB");
        helper.startSequence()
                .thenExecute(() -> {
                    powerOn(helper, RADIO_A);
                    powerOn(helper, RADIO_B);
                })
                .thenIdle(1)
                .thenExecute(() -> helper.assertTrue(tnc(helper, TNC_A).sendMessage("W3XYZ", "not for you"), "A could not send"))
                .thenWaitUntil(() -> helper.assertFalse(radio(helper, RADIO_A).isSendingData(), "A is still sending"))
                .thenIdle(2)
                .thenExecute(() -> {
                    helper.assertTrue(tnc(helper, TNC_B).getMessages().isEmpty(), "B logged a message for W3XYZ");
                    helper.assertFalse(tnc(helper, TNC_B).hasUnread(), "B shows unread for someone else's message");
                    helper.assertTrue(tnc(helper, TNC_A).getMessages().getFirst().outgoing(), "A did not log what it sent");
                    tnc(helper, TNC_A).sendMessage("w3bbb", "hello there");
                })
                .thenWaitUntil(() -> helper.assertTrue(tnc(helper, TNC_B).hasUnread(), "B never got its message"))
                .thenExecute(() -> {
                    DigitalInterfaceBlockEntity.Logged logged = tnc(helper, TNC_B).getMessages().getFirst();
                    helper.assertTrue(logged.packet().from().equals("W3AAA") && logged.packet().text().equals("hello there") && !logged.outgoing(),
                            "B logged the wrong message: " + logged);
                    helper.assertTrue(tnc(helper, TNC_B).getComparatorOutput() == 15, "Comparator does not show the unread message");
                    tnc(helper, TNC_B).markRead();
                    helper.assertTrue(tnc(helper, TNC_B).getComparatorOutput() == 0, "Comparator still lit after reading");
                })
                .thenSucceed();
    }

    /**
     * An interface between two radios digipeats: A beacons on one frequency, the digipeater's first radio hears it and
     * its second radio puts it back out on another, where C hears it one hop on. A and C cannot hear each other.
     */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 300, batch = BATCH)
    public static void digipeaterRelaysToItsOtherRadio(GameTestHelper helper) {
        BlockPos a = new BlockPos(3, 2, 6); // The bare connectors fade at both ends: 3 blocks is plenty, 8 is nothing.
        BlockPos digiIn = new BlockPos(6, 2, 6);
        BlockPos digi = new BlockPos(7, 2, 6);
        BlockPos digiOut = new BlockPos(8, 2, 6);
        BlockPos c = new BlockPos(11, 2, 6);
        station(helper, a, 144_470, a.north(), "W4AAA");
        helper.setBlock(digiIn, RadiocraftBlocks.VHF_BASE_STATION.get());
        helper.setBlock(digiOut, RadiocraftBlocks.VHF_BASE_STATION.get());
        helper.setBlock(digi, RadiocraftBlocks.DIGITAL_INTERFACE.get());
        radio(helper, digiIn).setFrequency(144_470_000.0F);
        radio(helper, digiOut).setFrequency(144_490_000.0F);
        tnc(helper, digi).setCallsign("W4DIG", "gametest");
        station(helper, c, 144_490, c.north(), "W4CCC");
        helper.startSequence()
                .thenExecute(() -> {
                    for(BlockPos pos : new BlockPos[] { a, digiIn, digiOut, c })
                        powerOn(helper, pos);
                })
                .thenIdle(1)
                .thenExecute(() -> tnc(helper, a.north()).beacon())
                .thenWaitUntil(() -> helper.assertTrue(heardBeaconFrom(helper, digi, "W4AAA"), "The digipeater never heard A"))
                .thenExecute(() -> helper.assertFalse(heardBeaconFrom(helper, c.north(), "W4AAA"), "C heard A before the digipeater repeated it"))
                .thenWaitUntil(() -> helper.assertTrue(heardBeaconFrom(helper, c.north(), "W4AAA"), "C never heard the digipeated beacon"))
                .thenExecute(() -> {
                    TncPacket packet = tnc(helper, c.north()).getBeacons().getFirst().packet();
                    helper.assertTrue(packet.hops() == 1, "The digipeated beacon shows " + packet.hops() + " hops");
                })
                .thenSucceed();
    }

    /** A redstone pulse into the interface sends a beacon. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 200, batch = BATCH)
    public static void redstonePulseBeacons(GameTestHelper helper) {
        station(helper, RADIO_A, 144_510, TNC_A, "W5AAA");
        station(helper, RADIO_B, 144_510, TNC_B, "W5BBB");
        helper.startSequence()
                .thenExecute(() -> {
                    powerOn(helper, RADIO_A);
                    powerOn(helper, RADIO_B);
                })
                .thenIdle(1)
                .thenExecute(() -> helper.setBlock(TNC_A.north(), Blocks.REDSTONE_BLOCK))
                .thenWaitUntil(() -> helper.assertTrue(heardBeaconFrom(helper, TNC_B, "W5AAA"), "B never heard the redstone beacon"))
                .thenSucceed();
    }

    private static void givePaper(GameTestHelper helper, BlockPos tncPos, int sheets) {
        tnc(helper, tncPos).insertSupply(new ItemStack(Items.PAPER, sheets));
    }

    private static boolean doneSending(GameTestHelper helper, BlockPos radioPos) {
        return !radio(helper, radioPos).isSendingData();
    }

    /** RTTY typed at A prints at B, on one line under A's callsign, using a line's worth of B's paper. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 300, batch = BATCH)
    public static void rttyPrintsAtFarStation(GameTestHelper helper) {
        station(helper, RADIO_A, 144_550, TNC_A, "W7AAA");
        station(helper, RADIO_B, 144_550, TNC_B, "W7BBB");
        givePaper(helper, TNC_B, 1);
        helper.startSequence()
                .thenExecute(() -> {
                    powerOn(helper, RADIO_A);
                    powerOn(helper, RADIO_B);
                })
                .thenIdle(1)
                .thenExecute(() -> {
                    helper.assertTrue(tnc(helper, TNC_A).typeRtty("cq cq"), "A could not send RTTY");
                    tnc(helper, TNC_A).typeRtty(" de w7aaa\n");
                })
                .thenWaitUntil(() -> helper.assertTrue(tnc(helper, TNC_B).getRttyLines().contains("W7AAA: CQ CQ DE W7AAA"),
                        "B printed " + tnc(helper, TNC_B).getRttyLines() + " / " + tnc(helper, TNC_B).getRttyOpenLine()))
                .thenExecute(() -> {
                    helper.assertTrue(tnc(helper, TNC_B).getPaperLines() == DigitalInterfaceBlockEntity.LINES_PER_PAPER - 1,
                            "B has " + tnc(helper, TNC_B).getPaperLines() + " lines of paper left");
                    helper.assertTrue(tnc(helper, TNC_A).getRttyLines().isEmpty(), "A printed its local copy with no paper");
                })
                .thenSucceed();
    }

    /** With no paper the far station prints nothing. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 300, batch = BATCH)
    public static void rttyNeedsPaper(GameTestHelper helper) {
        station(helper, RADIO_A, 144_570, TNC_A, "W8AAA");
        station(helper, RADIO_B, 144_570, TNC_B, "W8BBB");
        helper.startSequence()
                .thenExecute(() -> {
                    powerOn(helper, RADIO_A);
                    powerOn(helper, RADIO_B);
                })
                .thenIdle(1)
                .thenExecute(() -> tnc(helper, TNC_A).typeRtty("TEST\n"))
                .thenWaitUntil(() -> helper.assertTrue(doneSending(helper, RADIO_A), "A is still sending"))
                .thenIdle(2)
                .thenExecute(() -> helper.assertTrue(tnc(helper, TNC_B).getRttyLines().isEmpty() && tnc(helper, TNC_B).getRttyOpenLine() == null,
                        "B printed without paper"))
                .thenSucceed();
    }

    /** Just under the copy threshold RTTY still prints, but with characters garbled. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 400, batch = BATCH)
    public static void weakRttyPrintsGarbled(GameTestHelper helper) {
        BlockPos far = new BlockPos(8, 2, 6); // 5 blocks from A: (1 - 5/8)^2 = 0.14 against a copy threshold of 0.15.
        station(helper, RADIO_A, 144_590, TNC_A, "W9AAA");
        station(helper, far, 144_590, far.north(), "W9BBB");
        givePaper(helper, far.north(), 1);
        String sent = "THE QUICK BROWN FOX JUMPS OVER THE LAZY DOG 0123456789";
        helper.startSequence()
                .thenExecute(() -> {
                    powerOn(helper, RADIO_A);
                    powerOn(helper, far);
                })
                .thenIdle(1)
                .thenExecute(() -> tnc(helper, TNC_A).typeRtty(sent + "\n"))
                .thenWaitUntil(() -> helper.assertFalse(tnc(helper, far.north()).getRttyLines().isEmpty(), "Nothing printed at the weak station"))
                .thenExecute(() -> {
                    String printed = tnc(helper, far.north()).getRttyLines().getFirst();
                    helper.assertTrue(printed.length() == ("W9AAA: " + sent).length(), "Garbling changed the length: " + printed);
                    helper.assertFalse(printed.equals("W9AAA: " + sent), "A weak signal printed perfectly");
                })
                .thenSucceed();
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

    /** A book sent to B's callsign prints at B as a copy on B's blank book; A keeps the original. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 300, batch = BATCH)
    public static void bookArrivesAsCopy(GameTestHelper helper) {
        station(helper, RADIO_A, 144_610, TNC_A, "W1FAA");
        station(helper, RADIO_B, 144_610, TNC_B, "W1FBB");
        helper.startSequence()
                .thenExecute(() -> {
                    powerOn(helper, RADIO_A);
                    powerOn(helper, RADIO_B);
                    tnc(helper, TNC_A).getOutbox().setStackInSlot(0, book(0));
                    tnc(helper, TNC_B).insertSupply(new ItemStack(Items.BOOK, 2));
                })
                .thenIdle(1)
                .thenExecute(() -> helper.assertTrue(tnc(helper, TNC_A).sendFile("w1fbb"), "A could not send the book"))
                .thenIdle(2)
                .thenExecute(() -> helper.assertTrue(tnc(helper, TNC_B).isTransferring(), "B did not get the header"))
                .thenWaitUntil(() -> helper.assertFalse(tnc(helper, TNC_B).getReceived().getStackInSlot(0).isEmpty(), "B never printed the book"))
                .thenExecute(() -> {
                    ItemStack copy = tnc(helper, TNC_B).getReceived().getStackInSlot(0);
                    helper.assertTrue(copy.is(Items.WRITTEN_BOOK) && generation(copy) == 1, "B printed " + copy + " generation " + generation(copy));
                    helper.assertTrue(tnc(helper, TNC_B).getSupplies().getStackInSlot(DigitalInterfaceBlockEntity.SLOT_BOOK).getCount() == 1, "B did not use one blank book");
                    helper.assertTrue(generation(tnc(helper, TNC_A).getOutbox().getStackInSlot(0)) == 0, "A lost its original");
                })
                .thenSucceed();
    }

    /** A file for someone else, or with no blank to print on, leaves nothing; a copy of a copy cannot be sent. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 400, batch = BATCH)
    public static void fileNeedsAddresseeAndBlank(GameTestHelper helper) {
        station(helper, RADIO_A, 144_630, TNC_A, "W2FAA");
        station(helper, RADIO_B, 144_630, TNC_B, "W2FBB");
        helper.startSequence()
                .thenExecute(() -> {
                    powerOn(helper, RADIO_A);
                    powerOn(helper, RADIO_B);
                    tnc(helper, TNC_A).getOutbox().setStackInSlot(0, book(2));
                })
                .thenIdle(1)
                .thenExecute(() -> {
                    helper.assertFalse(tnc(helper, TNC_A).sendFile(""), "A sent a copy of a copy");
                    tnc(helper, TNC_A).getOutbox().setStackInSlot(0, book(0));
                    helper.assertTrue(tnc(helper, TNC_A).sendFile("W2XYZ"), "A could not send");
                })
                .thenWaitUntil(() -> helper.assertFalse(tnc(helper, TNC_A).isTransferring(), "A is still sending"))
                .thenExecute(() -> {
                    helper.assertTrue(tnc(helper, TNC_B).getFileLog().isEmpty(), "B took a file for W2XYZ: " + tnc(helper, TNC_B).getFileLog());
                    helper.assertTrue(tnc(helper, TNC_A).sendFile(""), "A could not send to everyone");
                })
                .thenWaitUntil(() -> helper.assertFalse(tnc(helper, TNC_B).getFileLog().isEmpty(), "B logged nothing"))
                .thenExecute(() -> {
                    helper.assertTrue(tnc(helper, TNC_B).getFileLog().getFirst().contains("NO BLANK BOOK"), "B logged " + tnc(helper, TNC_B).getFileLog());
                    helper.assertTrue(tnc(helper, TNC_B).getReceived().getStackInSlot(0).isEmpty(), "B printed without a blank");
                })
                .thenSucceed();
    }

    /** A map goes as SSTV (about 26 seconds) and prints on an empty map: the same map. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 800, batch = BATCH)
    public static void mapGoesBySstv(GameTestHelper helper) {
        station(helper, RADIO_A, 144_650, TNC_A, "W3FAA");
        station(helper, RADIO_B, 144_650, TNC_B, "W3FBB");
        ItemStack map = MapItem.create(helper.getLevel(), 0, 0, (byte)0, false, false);
        helper.startSequence()
                .thenExecute(() -> {
                    powerOn(helper, RADIO_A);
                    powerOn(helper, RADIO_B);
                    tnc(helper, TNC_A).getOutbox().setStackInSlot(0, map.copy());
                    tnc(helper, TNC_B).insertSupply(new ItemStack(Items.MAP));
                })
                .thenIdle(1)
                .thenExecute(() -> helper.assertTrue(tnc(helper, TNC_A).sendFile(""), "A could not send the map"))
                .thenIdle(200)
                .thenExecute(() -> helper.assertTrue(tnc(helper, TNC_B).getReceived().getStackInSlot(0).isEmpty(), "The map arrived far too fast for SSTV"))
                .thenWaitUntil(() -> helper.assertFalse(tnc(helper, TNC_B).getReceived().getStackInSlot(0).isEmpty(), "B never printed the map"))
                .thenExecute(() -> {
                    ItemStack copy = tnc(helper, TNC_B).getReceived().getStackInSlot(0);
                    helper.assertTrue(copy.is(Items.FILLED_MAP) && java.util.Objects.equals(copy.get(DataComponents.MAP_ID), map.get(DataComponents.MAP_ID)),
                            "B printed " + copy);
                })
                .thenSucceed();
    }

    /** {@code /aprs broadcast} reaches an interface whose radio is on the frequency, from any distance. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 100, batch = BATCH)
    public static void broadcastCommandReachesListeningStation(GameTestHelper helper) {
        station(helper, RADIO_A, 144_530, TNC_A, "W6AAA");
        helper.startSequence()
                .thenExecute(() -> powerOn(helper, RADIO_A))
                .thenIdle(1) // A freshly placed interface joins the loaded list on the next tick.
                .thenExecute(() -> {
                    var server = helper.getLevel().getServer();
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), "aprs broadcast 144.530 W6AAA net tonight");
                    DigitalInterfaceBlockEntity.Logged logged = tnc(helper, TNC_A).getMessages().stream().findFirst().orElse(null);
                    helper.assertTrue(logged != null && logged.packet().text().equals("net tonight"), "The broadcast did not arrive");
                    helper.assertTrue(tnc(helper, TNC_A).hasUnread(), "The broadcast is not unread");
                })
                .thenSucceed();
    }
}
