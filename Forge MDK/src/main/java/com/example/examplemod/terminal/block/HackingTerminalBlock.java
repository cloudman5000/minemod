package com.example.examplemod.terminal.block;

import com.example.examplemod.terminal.menu.HackingTerminalMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Interactive world terminal block.
 * Owns only interaction behavior; UI logic lives in separate classes.
 */
public class HackingTerminalBlock extends Block {

    public HackingTerminalBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        if (player instanceof ServerPlayer serverPlayer) {
            MenuProvider menuProvider = new SimpleMenuProvider(
                    (windowId, inventory, ignored) -> new HackingTerminalMenu(windowId, inventory, pos),
                    Component.translatable("screen.examplemod.hacking_terminal")
            );
            serverPlayer.openMenu(menuProvider, buffer -> buffer.writeBlockPos(pos));
        }

        return InteractionResult.CONSUME;
    }
}
