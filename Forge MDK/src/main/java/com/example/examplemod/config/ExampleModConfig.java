package com.example.examplemod.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class ExampleModConfig {

        public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
        public static final ForgeConfigSpec SPEC;

        // Config Values
        public static final ForgeConfigSpec.BooleanValue ENABLE_JUMPSCARES;
        public static final ForgeConfigSpec.IntValue GLITCH_SPAWN_RATE;
        public static final ForgeConfigSpec.IntValue CLUE_SIGN_CHANCE;
        public static final ForgeConfigSpec.BooleanValue ALLOW_FAKE_PERMISSIONS;
        public static final ForgeConfigSpec.BooleanValue ENABLE_TILE_GLITCHES;

        static {
                BUILDER.push("Horror Settings");

                ENABLE_JUMPSCARES = BUILDER
                                .comment("Enable loud jumpscare visual and audio cues.")
                                .define("enableJumpscares", true);

                GLITCH_SPAWN_RATE = BUILDER
                                .comment("Frequency of Glitch attacks. Higher number means it spawns MORE often.")
                                .defineInRange("glitchSpawnRate", 50, 1, 100);

                CLUE_SIGN_CHANCE = BUILDER
                                .comment(
                                                "Chance (1 in X) per tick for a Clue Sign to spawn when afflicted by Dread. Lower means MORE frequent.")
                                .defineInRange("clueSignChance", 800, 100, 10000);

                ALLOW_FAKE_PERMISSIONS = BUILDER
                                .comment("Allow the Fake OS Permission screen to randomly appear.")
                                .define("allowFakePermissions", true);

                ENABLE_TILE_GLITCHES = BUILDER
                                .comment("Allow blocks around the Glitch to become corrupted.")
                                .define("enableTileGlitches", true);

                BUILDER.pop();
                SPEC = BUILDER.build();
        }
}
