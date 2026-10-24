package com.arrl.radiocraft.gametest;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.be_networks.network_objects.RadioNetworkObject;
import com.arrl.radiocraft.common.blockentities.AprsTerminalBlockEntity;
import com.arrl.radiocraft.common.blockentities.RadioInterfaceBlockEntity;
import com.arrl.radiocraft.common.blockentities.StationBus;
import com.arrl.radiocraft.common.blockentities.TransferInterfaceBlockEntity;
import com.arrl.radiocraft.common.blockentities.radio.RadioBlockEntity;
import com.arrl.radiocraft.common.blocks.AprsTerminalBlock;
import com.arrl.radiocraft.common.blocks.RadioLinks;
import com.arrl.radiocraft.common.blocks.AprsTerminalBlock.Display;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import com.arrl.radiocraft.common.radio.data.TncPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/**
 * The station blocks: radio interfaces (TNCs) on VHF base stations, the APRS terminal (H19) and the transfer interface
 * joined to them on the station bus. Beacons and messages go out as packets, decode only on frequency, are digipeated
 * by an interface between two radios, and reach terminals through any chain of bus blocks. The stations have no
 * antennas and talk through their bare connectors (8 blocks), so each test stays in its plot; each test has its own
 * frequencies too. RTTY comes back with the Teletype.
 */
@GameTestHolder(Radiocraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class StationGameTests {

    private static final String BATCH = "station";

    private static final BlockPos RADIO_A = new BlockPos(3, 2, 6);
    private static final BlockPos RADIO_B = new BlockPos(7, 2, 6);
    private static final BlockPos IFACE_A = RADIO_A.north();
    private static final BlockPos IFACE_B = RADIO_B.north();
    private static final BlockPos APRS_A = IFACE_A.north();
    private static final BlockPos APRS_B = IFACE_B.north();
    private static final BlockPos FILES_A = IFACE_A.west();
    private static final BlockPos FILES_B = IFACE_B.west();

    private static RadioBlockEntity radio(GameTestHelper helper, BlockPos pos) {
        return GameTestUtil.blockEntity(helper, pos, RadioBlockEntity.class);
    }

    private static RadioInterfaceBlockEntity radioInterface(GameTestHelper helper, BlockPos pos) {
        return GameTestUtil.blockEntity(helper, pos, RadioInterfaceBlockEntity.class);
    }

    private static AprsTerminalBlockEntity aprs(GameTestHelper helper, BlockPos pos) {
        return GameTestUtil.blockEntity(helper, pos, AprsTerminalBlockEntity.class);
    }

    private static TransferInterfaceBlockEntity files(GameTestHelper helper, BlockPos pos) {
        return GameTestUtil.blockEntity(helper, pos, TransferInterfaceBlockEntity.class);
    }

    /**
     * A VHF base station on {@code kHz} with a radio interface north of it set to {@code callsign}, an APRS terminal
     * north of that and a transfer interface west of it.
     */
    private static void station(GameTestHelper helper, BlockPos radioPos, int kHz, String callsign) {
        BlockPos interfacePos = radioPos.north();
        helper.setBlock(radioPos, RadiocraftBlocks.VHF_BASE_STATION.get());
        helper.setBlock(interfacePos, RadiocraftBlocks.RADIO_INTERFACE.get());
        helper.setBlock(interfacePos.north(), RadiocraftBlocks.APRS_TERMINAL.get());
        helper.setBlock(interfacePos.west(), RadiocraftBlocks.TRANSFER_INTERFACE.get());
        radio(helper, radioPos).setFrequency(kHz * 1000.0F);
        radioInterface(helper, interfacePos).setCallsign(callsign, "gametest");
    }

    private static void powerOn(GameTestHelper helper, BlockPos pos) {
        RadioNetworkObject networkObject = GameTestUtil.networkObject(helper, pos, RadioNetworkObject.class);
        if(!networkObject.isPowered)
            radio(helper, pos).toggle();
        helper.assertTrue(networkObject.isPowered, "Radio at " + pos.toShortString() + " did not power on");
    }

    private static boolean heardBeaconFrom(GameTestHelper helper, BlockPos aprsPos, String callsign) {
        return aprs(helper, aprsPos).getBeacons().stream().anyMatch(heard -> heard.packet().from().equals(callsign));
    }

    private static Display display(GameTestHelper helper, BlockPos aprsPos) {
        return helper.getBlockState(aprsPos).getValue(AprsTerminalBlock.DISPLAY);
    }

    // -------------------- APRS --------------------

    /** A beacon goes out as a burst and arrives whole at a station on the frequency, with the sender's position. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 200, batch = BATCH)
    public static void beaconReachesStationOnFrequency(GameTestHelper helper) {
        station(helper, RADIO_A, 144_390, "W1AAA");
        station(helper, RADIO_B, 144_390, "W1BBB");
        helper.startSequence()
                .thenExecute(() -> {
                    powerOn(helper, RADIO_A);
                    powerOn(helper, RADIO_B);
                })
                .thenIdle(1) // Bare connectors register on the next tick.
                .thenExecute(() -> {
                    helper.assertTrue(aprs(helper, APRS_A).beacon(), "A did not send its beacon");
                    helper.assertTrue(radio(helper, RADIO_A).isSendingData(), "A's radio is not sending the burst");
                    helper.assertFalse(heardBeaconFrom(helper, APRS_B, "W1AAA"), "The beacon arrived before its burst was over");
                })
                .thenWaitUntil(() -> helper.assertTrue(heardBeaconFrom(helper, APRS_B, "W1AAA"), "B never heard A's beacon"))
                .thenExecute(() -> {
                    TncPacket packet = aprs(helper, APRS_B).getBeacons().getFirst().packet();
                    helper.assertTrue(helper.absolutePos(APRS_A).equals(packet.position()), "The beacon carries " + packet.position() + ", not A's position");
                    helper.assertTrue(packet.hops() == 0, "A direct beacon came through " + packet.hops() + " digipeaters");
                })
                .thenSucceed();
    }

    /** A station tuned a channel away does not decode the packet. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 200, batch = BATCH)
    public static void offFrequencyDoesNotDecode(GameTestHelper helper) {
        station(helper, RADIO_A, 144_410, "W2AAA");
        station(helper, RADIO_B, 144_430, "W2BBB");
        helper.startSequence()
                .thenExecute(() -> {
                    powerOn(helper, RADIO_A);
                    powerOn(helper, RADIO_B);
                })
                .thenIdle(1)
                .thenExecute(() -> aprs(helper, APRS_A).beacon())
                .thenWaitUntil(() -> helper.assertFalse(radio(helper, RADIO_A).isSendingData(), "A is still sending"))
                .thenIdle(2)
                .thenExecute(() -> helper.assertTrue(aprs(helper, APRS_B).getBeacons().isEmpty(), "B decoded a packet 20 kHz off its frequency"))
                .thenSucceed();
    }

    /** A message to B's callsign lands in B's log as unread and lights its comparator; one to someone else does not. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 300, batch = BATCH)
    public static void messageToCallsignIsUnread(GameTestHelper helper) {
        station(helper, RADIO_A, 144_450, "W3AAA");
        station(helper, RADIO_B, 144_450, "W3BBB");
        helper.startSequence()
                .thenExecute(() -> {
                    powerOn(helper, RADIO_A);
                    powerOn(helper, RADIO_B);
                })
                .thenIdle(1)
                .thenExecute(() -> helper.assertTrue(aprs(helper, APRS_A).sendMessage("W3XYZ", "not for you"), "A could not send"))
                .thenWaitUntil(() -> helper.assertFalse(radio(helper, RADIO_A).isSendingData(), "A is still sending"))
                .thenIdle(2)
                .thenExecute(() -> {
                    helper.assertTrue(aprs(helper, APRS_B).getMessages().isEmpty(), "B logged a message for W3XYZ");
                    helper.assertFalse(aprs(helper, APRS_B).hasUnread(), "B shows unread for someone else's message");
                    helper.assertTrue(aprs(helper, APRS_A).getMessages().getFirst().outgoing(), "A did not log what it sent");
                    aprs(helper, APRS_A).sendMessage("w3bbb", "hello there");
                })
                .thenWaitUntil(() -> helper.assertTrue(aprs(helper, APRS_B).hasUnread(), "B never got its message"))
                .thenExecute(() -> {
                    AprsTerminalBlockEntity.Logged logged = aprs(helper, APRS_B).getMessages().getFirst();
                    helper.assertTrue(logged.packet().from().equals("W3AAA") && logged.packet().text().equals("hello there") && !logged.outgoing(),
                            "B logged the wrong message: " + logged);
                    helper.assertTrue(aprs(helper, APRS_B).getComparatorOutput() == 15, "Comparator does not show the unread message");
                    aprs(helper, APRS_B).markSeen();
                    helper.assertTrue(aprs(helper, APRS_B).getComparatorOutput() == 0, "Comparator still lit after reading");
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
        station(helper, a, 144_470, "W4AAA");
        helper.setBlock(digiIn, RadiocraftBlocks.VHF_BASE_STATION.get());
        helper.setBlock(digiOut, RadiocraftBlocks.VHF_BASE_STATION.get());
        helper.setBlock(digi, RadiocraftBlocks.RADIO_INTERFACE.get());
        helper.setBlock(digi.north(), RadiocraftBlocks.APRS_TERMINAL.get());
        radio(helper, digiIn).setFrequency(144_470_000.0F);
        radio(helper, digiOut).setFrequency(144_490_000.0F);
        radioInterface(helper, digi).setCallsign("W4DIG", "gametest");
        radioInterface(helper, digi).setDigipeatEnabled(true);
        station(helper, c, 144_490, "W4CCC");
        helper.startSequence()
                .thenExecute(() -> {
                    for(BlockPos pos : new BlockPos[] { a, digiIn, digiOut, c })
                        powerOn(helper, pos);
                })
                .thenIdle(1)
                .thenExecute(() -> aprs(helper, a.north().north()).beacon())
                .thenWaitUntil(() -> helper.assertTrue(heardBeaconFrom(helper, digi.north(), "W4AAA"), "The digipeater never heard A"))
                .thenExecute(() -> helper.assertFalse(heardBeaconFrom(helper, c.north().north(), "W4AAA"), "C heard A before the digipeater repeated it"))
                .thenWaitUntil(() -> helper.assertTrue(heardBeaconFrom(helper, c.north().north(), "W4AAA"), "C never heard the digipeated beacon"))
                .thenExecute(() -> {
                    TncPacket packet = aprs(helper, c.north().north()).getBeacons().getFirst().packet();
                    helper.assertTrue(packet.hops() == 1, "The digipeated beacon shows " + packet.hops() + " hops");
                })
                .thenSucceed();
    }

    /**
     * Classic APRS: one radio, digipeating switched on from the APRS terminal. A and C are out of each other's reach,
     * both within the digipeater's; A's beacon reaches C through it, one hop on, on the same frequency.
     */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 300, batch = BATCH)
    public static void singleRadioDigipeater(GameTestHelper helper) {
        BlockPos a = new BlockPos(3, 2, 6);
        BlockPos digi = new BlockPos(7, 2, 6);
        BlockPos c = new BlockPos(11, 2, 6); // 8 blocks from A: the bare connectors do not reach.
        station(helper, a, 144_670, "W5AAA");
        station(helper, digi, 144_670, "W5DIG");
        station(helper, c, 144_670, "W5CCC");
        net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
        tag.putBoolean("digipeat", true);
        aprs(helper, digi.north().north()).applySettings(tag);
        helper.startSequence()
                .thenExecute(() -> {
                    helper.assertTrue(radioInterface(helper, digi.north()).isDigipeatEnabled(), "The terminal did not switch the interface's digipeating on");
                    for(BlockPos pos : new BlockPos[] { a, digi, c })
                        powerOn(helper, pos);
                })
                .thenIdle(1)
                .thenExecute(() -> aprs(helper, a.north().north()).beacon())
                .thenWaitUntil(() -> helper.assertTrue(heardBeaconFrom(helper, digi.north().north(), "W5AAA"), "The digipeater never heard A"))
                .thenExecute(() -> {
                    helper.assertTrue(radioInterface(helper, digi.north()).isRepeating(), "The RPT lamp is not lit");
                    helper.assertFalse(heardBeaconFrom(helper, c.north().north(), "W5AAA"), "C heard A directly");
                })
                .thenWaitUntil(() -> helper.assertTrue(heardBeaconFrom(helper, c.north().north(), "W5AAA"), "C never heard the digipeated beacon"))
                .thenExecute(() -> helper.assertTrue(aprs(helper, c.north().north()).getBeacons().getFirst().packet().hops() == 1, "Not one hop"))
                .thenSucceed();
    }

    /** No repeat with digipeating off, of a packet already at 8 hops, or of a message for the digipeater itself. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 100, batch = BATCH)
    public static void digipeaterLimits(GameTestHelper helper) {
        station(helper, RADIO_A, 144_690, "W6DIG");
        helper.startSequence()
                .thenExecute(() -> powerOn(helper, RADIO_A))
                .thenIdle(1)
                .thenExecute(() -> {
                    RadioInterfaceBlockEntity digi = radioInterface(helper, IFACE_A);
                    RadioBlockEntity radio = radio(helper, RADIO_A);
                    digi.receive(radio, TncPacket.beacon(1L, "W6XYZ", BlockPos.ZERO, "off", "HOUSE"));
                    helper.assertFalse(digi.isRepeating(), "Repeated with digipeating off");
                    digi.setDigipeatEnabled(true);
                    TncPacket tired = TncPacket.beacon(2L, "W6XYZ", BlockPos.ZERO, "far", "HOUSE");
                    for(int i = 0; i < 8; i++)
                        tired = tired.digipeated();
                    digi.receive(radio, tired);
                    helper.assertFalse(digi.isRepeating(), "Repeated a packet already 8 hops along");
                    digi.receive(radio, TncPacket.message(3L, "W6XYZ", "W6DIG", "for me"));
                    helper.assertFalse(digi.isRepeating(), "Repeated a message for itself");
                    digi.receive(radio, TncPacket.message(4L, "W6XYZ", "W6ABC", "for someone else"));
                    helper.assertTrue(digi.isRepeating(), "Did not repeat a message for someone else");
                })
                .thenSucceed();
    }

    /** A redstone pulse into the APRS terminal sends a beacon. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 200, batch = BATCH)
    public static void redstonePulseBeacons(GameTestHelper helper) {
        station(helper, RADIO_A, 144_510, "W5AAA");
        station(helper, RADIO_B, 144_510, "W5BBB");
        helper.startSequence()
                .thenExecute(() -> {
                    powerOn(helper, RADIO_A);
                    powerOn(helper, RADIO_B);
                })
                .thenIdle(1)
                .thenExecute(() -> helper.setBlock(APRS_A.north(), Blocks.REDSTONE_BLOCK))
                .thenWaitUntil(() -> helper.assertTrue(heardBeaconFrom(helper, APRS_B, "W5AAA"), "B never heard the redstone beacon"))
                .thenSucceed();
    }

    /** {@code /aprs broadcast} reaches an APRS terminal whose radio is on the frequency, from any distance. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 100, batch = BATCH)
    public static void broadcastCommandReachesListeningStation(GameTestHelper helper) {
        station(helper, RADIO_A, 144_530, "W6AAA");
        helper.startSequence()
                .thenExecute(() -> powerOn(helper, RADIO_A))
                .thenIdle(1) // A freshly placed terminal joins the loaded list on the next tick.
                .thenExecute(() -> {
                    var server = helper.getLevel().getServer();
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), "aprs broadcast 144.530 W6AAA net tonight");
                    AprsTerminalBlockEntity.Logged logged = aprs(helper, APRS_A).getMessages().stream().findFirst().orElse(null);
                    helper.assertTrue(logged != null && logged.packet().text().equals("net tonight"), "The broadcast did not arrive");
                    helper.assertTrue(aprs(helper, APRS_A).hasUnread(), "The broadcast is not unread");
                })
                .thenSucceed();
    }

    // -------------------- THE STATION BUS --------------------

    /**
     * Radio, radio interface, transfer interface, APRS terminal in a row: the terminal at the end of the chain sends
     * and hears through the interface two blocks away. Breaking the transfer interface out of the middle cuts it off.
     */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 200, batch = BATCH)
    public static void terminalWorksThroughAChain(GameTestHelper helper) {
        BlockPos files = IFACE_A.north();
        BlockPos end = files.north();
        helper.setBlock(RADIO_A, RadiocraftBlocks.VHF_BASE_STATION.get());
        helper.setBlock(IFACE_A, RadiocraftBlocks.RADIO_INTERFACE.get());
        helper.setBlock(files, RadiocraftBlocks.TRANSFER_INTERFACE.get());
        helper.setBlock(end, RadiocraftBlocks.APRS_TERMINAL.get());
        radio(helper, RADIO_A).setFrequency(144_710_000.0F);
        radioInterface(helper, IFACE_A).setCallsign("W7AAA", "gametest");
        station(helper, RADIO_B, 144_710, "W7BBB");
        helper.startSequence()
                .thenExecute(() -> {
                    powerOn(helper, RADIO_A);
                    powerOn(helper, RADIO_B);
                    StationBus bus = aprs(helper, end).bus();
                    helper.assertTrue(bus.interfaces().size() == 1 && bus.terminals().size() == 2, "The chain's bus is " + bus);
                    helper.assertTrue(aprs(helper, end).getCallsign().equals("W7AAA"), "The terminal does not send under the interface's callsign");
                })
                .thenIdle(1)
                .thenExecute(() -> helper.assertTrue(aprs(helper, APRS_B).beacon(), "B could not beacon"))
                .thenWaitUntil(() -> helper.assertTrue(heardBeaconFrom(helper, end, "W7BBB"), "The end of the chain never heard B"))
                .thenWaitUntil(() -> helper.assertFalse(radio(helper, RADIO_B).isSendingData(), "B is still sending"))
                .thenExecute(() -> helper.assertTrue(aprs(helper, end).beacon(), "The end of the chain could not beacon"))
                .thenWaitUntil(() -> helper.assertTrue(heardBeaconFrom(helper, APRS_B, "W7AAA"), "B never heard the end of the chain"))
                .thenExecute(() -> {
                    helper.setBlock(files, Blocks.AIR);
                    helper.assertFalse(aprs(helper, end).bus().hasInterface(), "Still joined with the middle of the chain gone");
                    helper.assertFalse(aprs(helper, end).beacon(), "Beaconed with no radio interface");
                })
                .thenSucceed();
    }

    /** On its own an APRS terminal has no callsign, cannot send, and its screen stays dark. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 60, batch = BATCH)
    public static void terminalWithoutInterface(GameTestHelper helper) {
        helper.setBlock(APRS_A, RadiocraftBlocks.APRS_TERMINAL.get());
        helper.startSequence()
                .thenIdle(11)
                .thenExecute(() -> {
                    AprsTerminalBlockEntity terminal = aprs(helper, APRS_A);
                    helper.assertTrue(terminal.getCallsign().isEmpty(), "A lone terminal has a callsign");
                    helper.assertFalse(terminal.beacon(), "A lone terminal beaconed");
                    helper.assertFalse(terminal.sendMessage("W1XYZ", "hello"), "A lone terminal sent a message");
                    helper.assertTrue(display(helper, APRS_A) == Display.OFF, "A lone terminal shows " + display(helper, APRS_A));
                })
                .thenSucceed();
    }

    /**
     * A terminal between two radio interfaces, each on its own radio on the same frequency: it sends through both, and
     * a message for it, decoded by both, is logged once.
     */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 300, batch = BATCH)
    public static void twoInterfacesOnOneBus(GameTestHelper helper) {
        BlockPos radio1 = new BlockPos(3, 2, 6);
        BlockPos radio2 = new BlockPos(5, 2, 6);
        BlockPos terminal = new BlockPos(4, 2, 5); // Touching both interfaces.
        BlockPos far = new BlockPos(4, 2, 9); // About 3 blocks from each radio.
        for(BlockPos radio : new BlockPos[] { radio1, radio2 }) {
            helper.setBlock(radio, RadiocraftBlocks.VHF_BASE_STATION.get());
            helper.setBlock(radio.north(), RadiocraftBlocks.RADIO_INTERFACE.get());
            radio(helper, radio).setFrequency(144_730_000.0F);
            radioInterface(helper, radio.north()).setCallsign("W8AAA", "gametest");
        }
        helper.setBlock(terminal, RadiocraftBlocks.APRS_TERMINAL.get());
        helper.setBlock(far, RadiocraftBlocks.VHF_BASE_STATION.get());
        helper.setBlock(far.south(), RadiocraftBlocks.RADIO_INTERFACE.get());
        helper.setBlock(far.south().south(), RadiocraftBlocks.APRS_TERMINAL.get());
        radio(helper, far).setFrequency(144_730_000.0F);
        radioInterface(helper, far.south()).setCallsign("W8BBB", "gametest");
        helper.startSequence()
                .thenExecute(() -> {
                    for(BlockPos pos : new BlockPos[] { radio1, radio2, far })
                        powerOn(helper, pos);
                    helper.assertTrue(aprs(helper, terminal).bus().interfaces().size() == 2, "The terminal is not joined to both interfaces");
                })
                .thenIdle(1)
                .thenExecute(() -> {
                    helper.assertTrue(aprs(helper, terminal).sendMessage("W8BBB", "two ways"), "A could not send");
                    helper.assertTrue(radio(helper, radio1).isSendingData() && radio(helper, radio2).isSendingData(), "A did not send on both radios");
                })
                .thenWaitUntil(() -> helper.assertTrue(aprs(helper, far.south().south()).hasUnread(), "B never got the message"))
                .thenWaitUntil(() -> helper.assertFalse(radio(helper, radio1).isSendingData() || radio(helper, radio2).isSendingData(), "A is still sending"))
                .thenExecute(() -> aprs(helper, far.south().south()).sendMessage("W8AAA", "got it"))
                .thenWaitUntil(() -> helper.assertTrue(aprs(helper, terminal).hasUnread(), "A never got the reply"))
                .thenIdle(5)
                .thenExecute(() -> {
                    long copies = aprs(helper, far.south().south()).getMessages().stream().filter(logged -> !logged.outgoing()).count();
                    helper.assertTrue(copies == 1, "B logged " + copies + " copies");
                    long received = aprs(helper, terminal).getMessages().stream().filter(logged -> !logged.outgoing()).count();
                    helper.assertTrue(received == 1, "A logged the reply " + received + " times");
                })
                .thenSucceed();
    }

    /** The H19's screen: READY with a VHF radio on, CONTACT for a new station, MSG over it, and both cleared on opening. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 100, batch = BATCH)
    public static void screenShowsReadyContactAndMsg(GameTestHelper helper) {
        station(helper, RADIO_A, 144_750, "W9AAA");
        helper.startSequence()
                .thenExecute(() -> helper.assertTrue(display(helper, APRS_A) == Display.OFF, "Lit before the radio is on"))
                .thenExecute(() -> powerOn(helper, RADIO_A))
                .thenWaitUntil(() -> helper.assertTrue(display(helper, APRS_A) == Display.READY, "Not READY with the radio on: " + display(helper, APRS_A)))
                .thenExecute(() -> {
                    AprsTerminalBlockEntity terminal = aprs(helper, APRS_A);
                    terminal.receiveDirect(TncPacket.beacon(11L, "W9NEW", BlockPos.ZERO, "", "HOUSE"));
                    helper.assertTrue(display(helper, APRS_A) == Display.CONTACT, "A new station shows " + display(helper, APRS_A));
                    terminal.receiveDirect(TncPacket.message(12L, "W9NEW", "W9AAA", "hi"));
                    helper.assertTrue(display(helper, APRS_A) == Display.MSG, "A message shows " + display(helper, APRS_A));
                    terminal.markSeen();
                    helper.assertTrue(display(helper, APRS_A) == Display.READY, "Opening it left " + display(helper, APRS_A));
                    terminal.receiveDirect(TncPacket.beacon(13L, "W9NEW", BlockPos.ZERO, "again", "HOUSE"));
                    helper.assertTrue(display(helper, APRS_A) == Display.READY, "A station already heard lit CONTACT");
                })
                .thenSucceed();
    }

    /** With its TX switch off, a radio interface sends nothing for its terminals. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 60, batch = BATCH)
    public static void transmitSwitchOff(GameTestHelper helper) {
        station(helper, RADIO_A, 144_770, "W0AAA");
        helper.startSequence()
                .thenExecute(() -> powerOn(helper, RADIO_A))
                .thenIdle(1)
                .thenExecute(() -> {
                    radioInterface(helper, IFACE_A).setTransmitEnabled(false);
                    helper.assertFalse(aprs(helper, APRS_A).beacon(), "Beaconed with TX off");
                    helper.assertFalse(radio(helper, RADIO_A).isSendingData(), "The radio is sending with TX off");
                    radioInterface(helper, IFACE_A).setTransmitEnabled(true);
                    helper.assertTrue(aprs(helper, APRS_A).beacon(), "Did not beacon with TX back on");
                })
                .thenSucceed();
    }

    /**
     * Floor cables: the radio interface shows one to its radio and its terminals, the terminals to the interface, and
     * nothing points at an empty side.
     */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 20, batch = BATCH)
    public static void floorCablesFollowLinks(GameTestHelper helper) {
        helper.setBlock(IFACE_A, RadiocraftBlocks.RADIO_INTERFACE.get()); // First: setBlock skips placement, so the
        helper.setBlock(RADIO_A, RadiocraftBlocks.VHF_BASE_STATION.get()); // others arrive as neighbour updates.
        helper.setBlock(APRS_A, RadiocraftBlocks.APRS_TERMINAL.get());
        helper.setBlock(FILES_A, RadiocraftBlocks.TRANSFER_INTERFACE.get());
        var iface = helper.getBlockState(IFACE_A);
        helper.assertTrue(iface.getValue(RadioLinks.SOUTH), "No cable from the interface to its radio");
        helper.assertTrue(iface.getValue(RadioLinks.NORTH), "No cable from the interface to the H19");
        helper.assertTrue(iface.getValue(RadioLinks.WEST), "No cable from the interface to the transfer interface");
        helper.assertFalse(iface.getValue(RadioLinks.EAST), "A cable from the interface to nothing");
        helper.assertTrue(helper.getBlockState(APRS_A).getValue(RadioLinks.SOUTH), "No cable from the H19 to the interface");
        helper.assertTrue(helper.getBlockState(FILES_A).getValue(RadioLinks.EAST), "No cable from the transfer interface to the interface");
        helper.setBlock(RADIO_A, Blocks.AIR);
        helper.assertFalse(helper.getBlockState(IFACE_A).getValue(RadioLinks.SOUTH), "The cable stayed after the radio went");
        helper.succeed();
    }

    // -------------------- FILES --------------------

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
        station(helper, RADIO_A, 144_610, "W1FAA");
        station(helper, RADIO_B, 144_610, "W1FBB");
        helper.startSequence()
                .thenExecute(() -> {
                    powerOn(helper, RADIO_A);
                    powerOn(helper, RADIO_B);
                    files(helper, FILES_A).getOutbox().setStackInSlot(0, book(0));
                    files(helper, FILES_B).insertSupply(new ItemStack(Items.BOOK, 2));
                })
                .thenIdle(1)
                .thenExecute(() -> helper.assertTrue(files(helper, FILES_A).sendFile("w1fbb"), "A could not send the book"))
                .thenIdle(2)
                .thenExecute(() -> helper.assertTrue(files(helper, FILES_B).isTransferring(), "B did not get the header"))
                .thenWaitUntil(() -> helper.assertFalse(files(helper, FILES_B).getReceived().getStackInSlot(0).isEmpty(), "B never printed the book"))
                .thenExecute(() -> {
                    ItemStack copy = files(helper, FILES_B).getReceived().getStackInSlot(0);
                    helper.assertTrue(copy.is(Items.WRITTEN_BOOK) && generation(copy) == 1, "B printed " + copy + " generation " + generation(copy));
                    helper.assertTrue(files(helper, FILES_B).getSupplies().getStackInSlot(TransferInterfaceBlockEntity.SLOT_BOOK).getCount() == 1, "B did not use one blank book");
                    helper.assertTrue(generation(files(helper, FILES_A).getOutbox().getStackInSlot(0)) == 0, "A lost its original");
                })
                .thenSucceed();
    }

    /** A file for someone else, or with no blank to print on, leaves nothing; a copy of a copy cannot be sent. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 400, batch = BATCH)
    public static void fileNeedsAddresseeAndBlank(GameTestHelper helper) {
        station(helper, RADIO_A, 144_630, "W2FAA");
        station(helper, RADIO_B, 144_630, "W2FBB");
        helper.startSequence()
                .thenExecute(() -> {
                    powerOn(helper, RADIO_A);
                    powerOn(helper, RADIO_B);
                    files(helper, FILES_A).getOutbox().setStackInSlot(0, book(2));
                })
                .thenIdle(1)
                .thenExecute(() -> {
                    helper.assertFalse(files(helper, FILES_A).sendFile(""), "A sent a copy of a copy");
                    files(helper, FILES_A).getOutbox().setStackInSlot(0, book(0));
                    helper.assertTrue(files(helper, FILES_A).sendFile("W2XYZ"), "A could not send");
                })
                .thenWaitUntil(() -> helper.assertFalse(files(helper, FILES_A).isTransferring(), "A is still sending"))
                .thenExecute(() -> {
                    helper.assertTrue(files(helper, FILES_B).getFileLog().isEmpty(), "B took a file for W2XYZ: " + files(helper, FILES_B).getFileLog());
                    helper.assertTrue(files(helper, FILES_A).sendFile(""), "A could not send to everyone");
                })
                .thenWaitUntil(() -> helper.assertFalse(files(helper, FILES_B).getFileLog().isEmpty(), "B logged nothing"))
                .thenExecute(() -> {
                    helper.assertTrue(files(helper, FILES_B).getFileLog().getFirst().contains("NO BLANK BOOK"), "B logged " + files(helper, FILES_B).getFileLog());
                    helper.assertTrue(files(helper, FILES_B).getReceived().getStackInSlot(0).isEmpty(), "B printed without a blank");
                })
                .thenSucceed();
    }

    /** A map goes as SSTV (about 26 seconds) and prints on an empty map: the same map. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 800, batch = BATCH)
    public static void mapGoesBySstv(GameTestHelper helper) {
        station(helper, RADIO_A, 144_650, "W3FAA");
        station(helper, RADIO_B, 144_650, "W3FBB");
        ItemStack map = MapItem.create(helper.getLevel(), 0, 0, (byte)0, false, false);
        helper.startSequence()
                .thenExecute(() -> {
                    powerOn(helper, RADIO_A);
                    powerOn(helper, RADIO_B);
                    files(helper, FILES_A).getOutbox().setStackInSlot(0, map.copy());
                    files(helper, FILES_B).insertSupply(new ItemStack(Items.MAP));
                })
                .thenIdle(1)
                .thenExecute(() -> helper.assertTrue(files(helper, FILES_A).sendFile(""), "A could not send the map"))
                .thenIdle(200)
                .thenExecute(() -> helper.assertTrue(files(helper, FILES_B).getReceived().getStackInSlot(0).isEmpty(), "The map arrived far too fast for SSTV"))
                .thenWaitUntil(() -> helper.assertFalse(files(helper, FILES_B).getReceived().getStackInSlot(0).isEmpty(), "B never printed the map"))
                .thenExecute(() -> {
                    ItemStack copy = files(helper, FILES_B).getReceived().getStackInSlot(0);
                    helper.assertTrue(copy.is(Items.FILLED_MAP) && java.util.Objects.equals(copy.get(DataComponents.MAP_ID), map.get(DataComponents.MAP_ID)),
                            "B printed " + copy);
                })
                .thenSucceed();
    }
}
