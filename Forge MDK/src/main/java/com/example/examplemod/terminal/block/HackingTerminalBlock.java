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
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Interactive world terminal block.
 * Owns only interaction behavior; UI logic lives in separate classes.
 */
public class HackingTerminalBlock extends Block implements EntityBlock {

    public HackingTerminalBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        if (player instanceof ServerPlayer serverPlayer) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof HackingTerminalBlockEntity terminalBlockEntity) {
                terminalBlockEntity.onTerminalAccess(serverPlayer);
            }
            MenuProvider menuProvider = new SimpleMenuProvider(
                    (windowId, inventory, ignored) -> new HackingTerminalMenu(windowId, inventory, pos),
                    Component.translatable("screen.examplemod.hacking_terminal")
            );
            serverPlayer.openMenu(menuProvider, buffer -> buffer.writeBlockPos(pos));
        }

        return InteractionResult.CONSUME;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HackingTerminalBlockEntity(pos, state);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        return (tickerLevel, tickerPos, tickerState, tickerBe) -> {
            if (tickerBe instanceof HackingTerminalBlockEntity terminal) {
                HackingTerminalBlockEntity.serverTick(tickerLevel, tickerPos, tickerState, terminal);
            }
        };
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof HackingTerminalBlockEntity terminalBlockEntity) {
                terminalBlockEntity.onTerminalDestroyed();
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

}
