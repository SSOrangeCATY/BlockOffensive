package com.phasetranscrystal.blockoffensive;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public class BOConfig {
    public static class Client{
        public final ForgeConfigSpec.BooleanValue hudEnabled;
        public final ForgeConfigSpec.IntValue hudPosition;
        public final ForgeConfigSpec.IntValue messageShowTime;
        public final ForgeConfigSpec.IntValue maxShowCount;

        private Client(ForgeConfigSpec.Builder builder) {
            builder.push("client");
            {
                hudEnabled = builder.comment("Kill message enabled").define("hudEnabled",true);
                hudPosition = builder.comment("Kill message position").defineInRange("hudPosition",2,1,4);
                messageShowTime = builder.comment("Per message show time").defineInRange("messageShowTime",5,1,60);
                maxShowCount = builder.comment("Max show count").defineInRange("maxShowCount",5,1,10);
            }
            builder.pop();
        }
    }

    public static class Common {
        public final ForgeConfigSpec.BooleanValue showLogin;

        public final ForgeConfigSpec.DoubleValue teammateMuffledStepVolume;
        public final ForgeConfigSpec.DoubleValue teammateStepVolume;
        public final ForgeConfigSpec.DoubleValue enemyMuffledStepVolume;
        public final ForgeConfigSpec.DoubleValue enemyStepVolume;

        public final ForgeConfigSpec.IntValue fuseTime;
        public final ForgeConfigSpec.IntValue explosionRadius;

        private Common(ForgeConfigSpec.Builder builder) {
            builder.push("login");
            {
                showLogin = builder.comment("Show Login Message").define("showLoginMessage", true);
            }
            builder.pop();

            builder.push("step sound");
            {
                teammateMuffledStepVolume = builder.comment("Teammate Muffled Step Volume").defineInRange("teammateMuffledStepVolume", 0.05D, 0, 10);
                teammateStepVolume = builder.comment("Teammate Step Volume").defineInRange("teammateStepVolume", 0.15D, 0, 10);
                enemyMuffledStepVolume = builder.comment("Enemy Muffled Step Volume").defineInRange("enemyMuffledStepVolume", 0.4D, 0, 10);
                enemyStepVolume = builder.comment("Enemy Step Volume").defineInRange("enemyStepVolume", 1.2D, 0, 10);
            }

            builder.pop();

            builder.push("c4");
            {
                fuseTime = builder.comment("Fuse Time").defineInRange("Fuse Time", 800, 1, 3200);
                explosionRadius = builder.comment("Explosion Radius").defineInRange("Explosion Radius", 60, 10, 300);
            }
        }
    }
    public static final Client client;
    public static final ForgeConfigSpec clientSpec;
    public static final Common common;
    public static final ForgeConfigSpec commonSpec;

    static {
        final Pair<Client, ForgeConfigSpec> clientSpecPair = new ForgeConfigSpec.Builder().configure(Client::new);
        client = clientSpecPair.getLeft();
        clientSpec = clientSpecPair.getRight();
        final Pair<Common,ForgeConfigSpec> serverSpecPair = new ForgeConfigSpec.Builder().configure(Common::new);
        common = serverSpecPair.getLeft();
        commonSpec = serverSpecPair.getRight();
    }
}
