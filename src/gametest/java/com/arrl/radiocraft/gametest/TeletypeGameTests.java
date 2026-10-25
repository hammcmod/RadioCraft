package com.arrl.radiocraft.gametest;

import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import com.arrl.radiocraft.common.be_networks.network_objects.RadioNetworkObject;
import com.arrl.radiocraft.common.blockentities.radio.RadioBlockEntity;
import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.blockentities.TeletypeBlockEntity;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The Teletype Machine (was the solar weather station). It hears the solar weather through a radio (maintainer,
 * 2026-10-23): a radio of any kind next to it with a real antenna on its coax; alone, or with only a dummy load, it
 * reports no signal and its comparator output is 0. And RTTY straight through the radios it touches, printed on paper.
 */
@GameTestHolder(Radiocraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class TeletypeGameTests {

    private static final String NO_RADIO = "message.radiocraft.solar_weather_station.no_radio";

    private static boolean reportsNoSignal(TeletypeBlockEntity station) {
        return station.getReport().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents contents
                && contents.getKey().equals(NO_RADIO);
    }

    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 40)
    public static void aloneItHearsNothing(GameTestHelper helper) {
        BlockPos pos = new BlockPos(4, 2, 4);
        helper.setBlock(pos, RadiocraftBlocks.TELETYPE.get());
        helper.runAfterDelay(20, () -> {
            TeletypeBlockEntity station = GameTestUtil.blockEntity(helper, pos, TeletypeBlockEntity.class);
            helper.assertFalse(station.hasRadio(), "A station on its own thinks it has a radio");
            helper.assertTrue(station.getConditionLevel() == 0, "A station on its own drives a comparator: " + station.getConditionLevel());
            helper.assertTrue(reportsNoSignal(station), "A station on its own reports " + station.getReport().getString());
            helper.succeed();
        });
    }

    /** A dummy load radiates nothing, so a radio with only a dummy load does not count. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 60)
    public static void dummyLoadDoesNotCount(GameTestHelper helper) {
        BlockPos pos = new BlockPos(4, 2, 4), radio = pos.west();
        helper.setBlock(pos, RadiocraftBlocks.TELETYPE.get());
        helper.setBlock(radio, RadiocraftBlocks.VHF_BASE_STATION.get());
        helper.setBlock(radio.north(), RadiocraftBlocks.DUMMY_LOAD.get());
        helper.runAfterDelay(40, () -> {
            TeletypeBlockEntity station = GameTestUtil.blockEntity(helper, pos, TeletypeBlockEntity.class);
            helper.assertFalse(station.hasRadio(), "A radio on a dummy load counts as a receiving station");
            helper.assertTrue(reportsNoSignal(station), "With a dummy load the station reports " + station.getReport().getString());
            helper.succeed();
        });
    }

    /** Any radio with a real antenna next to the station brings the report (and the comparator) to life. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 100)
    public static void radioWithAntennaReports(GameTestHelper helper) {
        BlockPos pos = new BlockPos(4, 2, 4);
        helper.setBlock(pos, RadiocraftBlocks.TELETYPE.get());
        GameTestUtil.radioWithJPole(helper, pos.west());
        helper.succeedWhen(() -> {
            TeletypeBlockEntity station = GameTestUtil.blockEntity(helper, pos, TeletypeBlockEntity.class);
            helper.assertTrue(station.hasRadio(), "The station does not see the radio and its J-pole");
            helper.assertFalse(reportsNoSignal(station), "The station still reports no signal");
            helper.assertTrue(station.getCurrentEvent() == null || station.getConditionLevel() > 0, "The station has a radio but its comparator reads 0");
        });
    }

    // -------------------- RTTY --------------------

    private static final BlockPos RADIO_A = new BlockPos(3, 2, 6);
    private static final BlockPos RADIO_B = new BlockPos(7, 2, 6);

    private static RadioBlockEntity radio(GameTestHelper helper, BlockPos pos) {
        return GameTestUtil.blockEntity(helper, pos, RadioBlockEntity.class);
    }

    private static TeletypeBlockEntity teletype(GameTestHelper helper, BlockPos pos) {
        return GameTestUtil.blockEntity(helper, pos, TeletypeBlockEntity.class);
    }

    /** A VHF base station on {@code kHz} with a teletype north of it set to {@code callsign}. */
    private static void station(GameTestHelper helper, BlockPos radioPos, int kHz, String callsign) {
        helper.setBlock(radioPos, RadiocraftBlocks.VHF_BASE_STATION.get());
        helper.setBlock(radioPos.north(), RadiocraftBlocks.TELETYPE.get());
        radio(helper, radioPos).setFrequency(kHz * 1000.0F);
        teletype(helper, radioPos.north()).setCallsign(callsign, "gametest");
    }

    private static void powerOn(GameTestHelper helper, BlockPos pos) {
        if(!GameTestUtil.networkObject(helper, pos, RadioNetworkObject.class).isPowered)
            radio(helper, pos).toggle();
    }

    private static ItemStack paper() {
        return new ItemStack(Items.PAPER);
    }

    /** RTTY typed at A prints at B, on one line under A's callsign, using a line's worth of B's paper. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 300)
    public static void rttyPrintsAtFarStation(GameTestHelper helper) {
        station(helper, RADIO_A, 144_550, "W7AAA");
        station(helper, RADIO_B, 144_550, "W7BBB");
        teletype(helper, RADIO_B.north()).insertPaper(paper());
        helper.startSequence()
                .thenExecute(() -> {
                    powerOn(helper, RADIO_A);
                    powerOn(helper, RADIO_B);
                })
                .thenIdle(1)
                .thenExecute(() -> {
                    helper.assertTrue(teletype(helper, RADIO_A.north()).typeRtty("cq cq"), "A could not send RTTY");
                    teletype(helper, RADIO_A.north()).typeRtty(" de w7aaa\n");
                })
                .thenWaitUntil(() -> helper.assertTrue(teletype(helper, RADIO_B.north()).getRttyLines().contains("CQ CQ DE W7AAA"),
                        "B printed " + teletype(helper, RADIO_B.north()).getRttyLines() + " / " + teletype(helper, RADIO_B.north()).getRttyOpenLine()))
                .thenExecute(() -> {
                    helper.assertTrue(teletype(helper, RADIO_B.north()).getPaperLines() == TeletypeBlockEntity.LINES_PER_PAPER - 1,
                            "B has " + teletype(helper, RADIO_B.north()).getPaperLines() + " lines of paper left");
                    helper.assertTrue(teletype(helper, RADIO_A.north()).getRttyLines().isEmpty(), "A printed its local copy with no paper");
                })
                .thenSucceed();
    }

    /** With no paper the far station prints nothing. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 300)
    public static void rttyNeedsPaper(GameTestHelper helper) {
        station(helper, RADIO_A, 144_570, "W8AAA");
        station(helper, RADIO_B, 144_570, "W8BBB");
        helper.startSequence()
                .thenExecute(() -> {
                    powerOn(helper, RADIO_A);
                    powerOn(helper, RADIO_B);
                })
                .thenIdle(1)
                .thenExecute(() -> teletype(helper, RADIO_A.north()).typeRtty("TEST\n"))
                .thenWaitUntil(() -> helper.assertTrue(teletype(helper, RADIO_A.north()).getTypedAhead() == 0 && !radio(helper, RADIO_A).isSendingData(), "A is still sending"))
                .thenIdle(2)
                .thenExecute(() -> helper.assertTrue(teletype(helper, RADIO_B.north()).getRttyLines().isEmpty() && teletype(helper, RADIO_B.north()).getRttyOpenLine() == null,
                        "B printed without paper"))
                .thenSucceed();
    }

    /** Just under the copy threshold RTTY still prints, but with characters garbled. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 400)
    public static void weakRttyPrintsGarbled(GameTestHelper helper) {
        BlockPos far = new BlockPos(8, 2, 6); // 5 blocks from A: (1 - 5/8)^2 = 0.14 against a copy threshold of 0.15.
        station(helper, RADIO_A, 144_590, "W9AAA");
        station(helper, far, 144_590, "W9BBB");
        teletype(helper, far.north()).insertPaper(paper());
        String sent = "THE QUICK BROWN FOX JUMPS OVER"; // One line of the paper.
        helper.startSequence()
                .thenExecute(() -> {
                    powerOn(helper, RADIO_A);
                    powerOn(helper, far);
                })
                .thenIdle(1)
                .thenExecute(() -> teletype(helper, RADIO_A.north()).typeRtty(sent + "\n"))
                .thenWaitUntil(() -> helper.assertFalse(teletype(helper, far.north()).getRttyLines().isEmpty(), "Nothing printed at the weak station"))
                .thenExecute(() -> {
                    String printed = teletype(helper, far.north()).getRttyLines().getFirst();
                    helper.assertTrue(printed.length() == sent.length(), "Garbling changed the length: " + printed);
                    helper.assertFalse(printed.equals(sent), "A weak signal printed perfectly");
                })
                .thenSucceed();
    }

    /** SOLAR WX prints the report on the roll, a line per entry, as WX. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 40)
    public static void solarWxPrintsOnThePaper(GameTestHelper helper) {
        BlockPos pos = new BlockPos(4, 2, 4);
        helper.setBlock(pos, RadiocraftBlocks.TELETYPE.get());
        TeletypeBlockEntity teletype = teletype(helper, pos);
        teletype.insertPaper(paper());
        teletype.printReport();
        helper.assertTrue(teletype.getRttyLines().size() >= teletype.getReportLines().size(), "Printed " + teletype.getRttyLines()); // Long lines wrap.
        helper.succeed();
    }

    /** A teletype with no callsign still sends: on RTTY the operator gives it in what they type. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 300)
    public static void rttyNeedsNoCallsign(GameTestHelper helper) {
        station(helper, RADIO_A, 144_670, "");
        station(helper, RADIO_B, 144_670, "W2BBB");
        teletype(helper, RADIO_B.north()).insertPaper(paper());
        helper.startSequence()
                .thenExecute(() -> {
                    powerOn(helper, RADIO_A);
                    powerOn(helper, RADIO_B);
                })
                .thenIdle(1)
                .thenExecute(() -> helper.assertTrue(teletype(helper, RADIO_A.north()).typeRtty("DE K1ABC\n"), "A teletype without a callsign could not send"))
                .thenWaitUntil(() -> helper.assertTrue(teletype(helper, RADIO_B.north()).getRttyLines().contains("DE K1ABC"), "B printed " + teletype(helper, RADIO_B.north()).getRttyLines()))
                .thenSucceed();
    }

    /** Typing goes out about six characters a second (a keystroke sound each), however fast it is typed. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 400)
    public static void rttyGoesOutAtTheMachinesSpeed(GameTestHelper helper) {
        station(helper, RADIO_A, 144_610, "W5AAA");
        station(helper, RADIO_B, 144_610, "W5BBB");
        teletype(helper, RADIO_B.north()).insertPaper(paper());
        int[] printedAfterASecond = {-1};
        helper.startSequence()
                .thenExecute(() -> {
                    powerOn(helper, RADIO_A);
                    powerOn(helper, RADIO_B);
                })
                .thenIdle(1)
                .thenExecute(() -> helper.assertTrue(teletype(helper, RADIO_A.north()).typeRtty("ABCDEFGHIJKLMNOP"), "A could not type"))
                .thenWaitUntil(() -> helper.assertTrue(teletype(helper, RADIO_B.north()).getRttyOpenLine() != null, "B has not started printing"))
                .thenIdle(10)
                .thenExecute(() -> { // Past the delay and the lead-in: count from here for a second.
                    String open = teletype(helper, RADIO_B.north()).getRttyOpenLine();
                    printedAfterASecond[0] = open == null ? 0 : open.length();
                })
                .thenIdle(20)
                .thenExecute(() -> {
                    String open = teletype(helper, RADIO_B.north()).getRttyOpenLine();
                    int inASecond = (open == null ? 0 : open.length()) - printedAfterASecond[0];
                    helper.assertTrue(inASecond >= 5 && inASecond <= 7, "B printed " + inASecond + " characters in a second");
                })
                .thenWaitUntil(() -> helper.assertTrue("ABCDEFGHIJKLMNOP".equals(teletype(helper, RADIO_B.north()).getRttyOpenLine()),
                        "B printed " + teletype(helper, RADIO_B.north()).getRttyOpenLine()))
                .thenSucceed();
    }

    /** A teletype being typed on prints nothing it hears (half duplex). */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 300)
    public static void noReceivingWhileTyping(GameTestHelper helper) {
        station(helper, RADIO_A, 144_630, "W4AAA");
        station(helper, RADIO_B, 144_630, "W4BBB");
        teletype(helper, RADIO_B.north()).insertPaper(paper());
        helper.startSequence()
                .thenExecute(() -> {
                    powerOn(helper, RADIO_A);
                    powerOn(helper, RADIO_B);
                })
                .thenIdle(1)
                .thenExecute(() -> {
                    teletype(helper, RADIO_B.north()).typeRtty("BBBBBBBBBBBBBBBBBBBBBBBB");
                    teletype(helper, RADIO_A.north()).typeRtty("HELLO\n");
                })
                .thenWaitUntil(() -> helper.assertTrue(teletype(helper, RADIO_A.north()).getTypedAhead() == 0 && !radio(helper, RADIO_A).isSendingData(), "A is still sending"))
                .thenIdle(4)
                .thenExecute(() -> {
                    TeletypeBlockEntity b = teletype(helper, RADIO_B.north());
                    boolean heard = b.getRttyLines().stream().anyMatch(line -> line.contains("HELLO")) || (b.getRttyOpenLine() != null && b.getRttyOpenLine().contains("HELLO"));
                    helper.assertFalse(heard, "B printed A while it was sending: " + b.getRttyLines());
                })
                .thenSucceed();
    }

    /** Two stations sending at once on one frequency: the one in the middle cannot copy either cleanly. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 300)
    public static void twoSendingAtOnceGarble(GameTestHelper helper) {
        BlockPos radioC = new BlockPos(11, 2, 6); // As far from B as A is.
        station(helper, RADIO_A, 144_650, "W3AAA");
        station(helper, RADIO_B, 144_650, "W3BBB");
        station(helper, radioC, 144_650, "W3CCC");
        teletype(helper, RADIO_B.north()).insertPaper(paper());
        String sent = "AAAAAAAAAAAAAAAAAAAA";
        helper.startSequence()
                .thenExecute(() -> {
                    powerOn(helper, RADIO_A);
                    powerOn(helper, RADIO_B);
                    powerOn(helper, radioC);
                })
                .thenIdle(1)
                .thenExecute(() -> {
                    teletype(helper, RADIO_A.north()).typeRtty(sent + "\n");
                    teletype(helper, radioC.north()).typeRtty("CCCCCCCCCCCCCCCCCCCC\n");
                })
                .thenWaitUntil(() -> helper.assertTrue(teletype(helper, RADIO_A.north()).getTypedAhead() == 0 && teletype(helper, radioC.north()).getTypedAhead() == 0
                        && !radio(helper, RADIO_A).isSendingData() && !radio(helper, radioC).isSendingData(), "Still sending"))
                .thenIdle(4)
                .thenExecute(() -> {
                    TeletypeBlockEntity b = teletype(helper, RADIO_B.north());
                    StringBuilder fromA = new StringBuilder();
                    for(String line : b.getRttyLines())
                        fromA.append(line);
                    helper.assertFalse(fromA.toString().contains(sent), "B copied A cleanly over C: " + b.getRttyLines());
                })
                .thenSucceed();
    }
}
