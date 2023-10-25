package com.arrl.radiocraft.common.radio;

public class SWRHelper {

    public static double getLossMultiplier(double swr) {
        return (((swr-1) / (swr+1)) * ((swr-1) / (swr+1)));
    }

    public static double getEfficiencyMultiplier(double swr) {
        return 1.0D - swr;
    }

}
