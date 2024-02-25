package com.arrl.radiocraft.common.radio;

public class SWRHelper {

    public static double getEfficiencyMultiplier(double swr) {
        return 1.0D - ((swr-1) / (swr+1)) * ((swr-1) / (swr+1));
    }

}
