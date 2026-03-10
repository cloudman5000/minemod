package com.example.examplemod.terminal.domain;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public record TerminalId(String value) {
    public static TerminalId of(ResourceKey<Level> dimension, BlockPos blockPos) {
        String id = dimension.location() + "|" + blockPos.getX() + "," + blockPos.getY() + "," + blockPos.getZ();
        return new TerminalId(id);
    }
}
