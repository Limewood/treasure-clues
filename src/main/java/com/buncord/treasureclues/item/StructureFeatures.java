package com.buncord.treasureclues.item;

public class StructureFeatures {
    public static final String PILLAGER_OUTPOST = "pillager_outpost";
    public static final String MINESHAFT = "mineshaft";
    public static final String WOODLAND_MANSION = "mansion";
    public static final String JUNGLE_TEMPLE = "jungle_pyramid";
    public static final String DESERT_PYRAMID = "desert_pyramid";
    public static final String IGLOO = "igloo";
    public static final String RUINED_PORTAL = "ruined_portal";
    public static final String SHIPWRECK = "shipwreck";
    public static final String SWAMP_HUT = "swamp_hut";
    public static final String STRONGHOLD = "stronghold";
    public static final String OCEAN_MONUMENT = "monument";
    public static final String OCEAN_RUIN = "ocean_ruin";
    public static final String NETHER_BRIDGE = "fortress";
    public static final String END_CITY = "endcity";
    public static final String BURIED_TREASURE = "buried_treasure";
    public static final String VILLAGE = "village";
    public static final String NETHER_FOSSIL = "nether_fossil";
    public static final String BASTION_REMNANT = "bastion_remnant";

    // Skipping the Nether, End and difficult places for now
    // Also skipping mansion, as it is too rare (and Kirby live in one)
    public static final String[] STRUCTURE_FEATURES = new String[] {
            PILLAGER_OUTPOST,
            JUNGLE_TEMPLE,
            DESERT_PYRAMID,
            IGLOO,
            RUINED_PORTAL,
            SHIPWRECK,
            SWAMP_HUT,
            OCEAN_RUIN,
            VILLAGE
    };
}
