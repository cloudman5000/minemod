package com.example.examplemod.terminal.menu;

import com.example.examplemod.ExampleMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;

/**
 * Minimal menu used only to synchronize opening the terminal screen.
 */
public class HackingTerminalMenu extends AbstractContainerMenu {
    private final ContainerLevelAccess access;
    private final BlockPos terminalPos;

    public HackingTerminalMenu(int containerId, Inventory playerInventory, FriendlyByteBuf data) {
        this(containerId, playerInventory, data.readBlockPos());
    }

    public HackingTerminalMenu(int containerId, Inventory playerInventory, BlockPos terminalPos) {
        super(ExampleMod.HACKING_TERMINAL_MENU.get(), containerId);
        this.terminalPos = terminalPos;
        this.access = ContainerLevelAccess.create(playerInventory.player.level(), terminalPos);
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ExampleMod.HACKING_TERMINAL_BLOCK.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    public BlockPos terminalPos() {
        return terminalPos;
    }
}
