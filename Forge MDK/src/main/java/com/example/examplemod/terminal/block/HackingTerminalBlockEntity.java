package com.example.examplemod.terminal.block;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.terminal.domain.AiAvailabilityState;
import com.example.examplemod.terminal.domain.TerminalActivity;
import com.example.examplemod.terminal.domain.TerminalId;
import com.example.examplemod.terminal.domain.TerminalState;
import com.example.examplemod.terminal.domain.WorkflowState;
import com.example.examplemod.terminal.network.InMemoryTerminalMessageBus;
import com.example.examplemod.terminal.runtime.TerminalRuntimeEngine;
import com.example.examplemod.terminal.runtime.TerminalStateStore;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class HackingTerminalBlockEntity extends BlockEntity implements TerminalStateStore {
    private static final int MAX_HISTORY = 300;
    private static final double MAX_OPERATOR_DISTANCE_SQ = 100.0D * 100.0D;

    private final TerminalRuntimeEngine runtimeEngine = new TerminalRuntimeEngine();
    private final TerminalState state = new TerminalState();

    public HackingTerminalBlockEntity(BlockPos pos, BlockState blockState) {
        super(ExampleMod.HACKING_TERMINAL_BLOCK_ENTITY.get(), pos, blockState);
        if (state.history().isEmpty()) {
            state.history().add("REALITY RESTORATION TERMINAL v0.2");
            state.history().add("type help for command list");
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState blockState, HackingTerminalBlockEntity blockEntity) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        blockEntity.enforceOperatorLifecycle(serverLevel);
        blockEntity.runtimeEngine.tick(blockEntity, serverLevel, blockEntity.terminalId(serverLevel));
        blockEntity.pushSync();
    }

    public void submitCommand(ServerPlayer player, String command) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        runtimeEngine.submitCommand(this, serverLevel, terminalId(serverLevel), player.getUUID(), command);
        pushSync();
    }

    public List<String> historySnapshot() {
        return List.copyOf(state.history());
    }

    public String terminalIdForClient() {
        if (level == null) {
            return "unknown";
        }
        return TerminalId.of(level.dimension(), getBlockPos()).value();
    }

    public AiAvailabilityState aiAvailabilityState() {
        return state.aiAvailabilityState();
    }

    public void onTerminalDestroyed() {
        if (level instanceof ServerLevel serverLevel) {
            InMemoryTerminalMessageBus.get().unregister(serverLevel, terminalId(serverLevel));
        }
        runtimeEngine.terminatePendingActivity(this, "terminal destroyed");
        pushSync();
    }

    @Override
    public TerminalState load() {
        return state;
    }

    @Override
    public void save(TerminalState state) {
        setChanged();
    }

    private void enforceOperatorLifecycle(ServerLevel serverLevel) {
        String activeOperator = state.activeOperatorUuid();
        if (activeOperator == null || activeOperator.isBlank()) {
            return;
        }
        try {
            UUID uuid = UUID.fromString(activeOperator);
            ServerPlayer player = serverLevel.getServer().getPlayerList().getPlayer(uuid);
            if (player == null) {
                runtimeEngine.terminatePendingActivity(this, "operator disconnected");
                return;
            }
            if (player.level() != serverLevel) {
                runtimeEngine.terminatePendingActivity(this, "operator changed dimension");
                return;
            }
            double distanceSq = player.distanceToSqr(getBlockPos().getX() + 0.5, getBlockPos().getY() + 0.5, getBlockPos().getZ() + 0.5);
            if (distanceSq > MAX_OPERATOR_DISTANCE_SQ) {
                runtimeEngine.terminatePendingActivity(this, "operator moved beyond 100 blocks");
            }
        } catch (IllegalArgumentException ignored) {
            runtimeEngine.terminatePendingActivity(this, "invalid operator state");
        }
    }

    private String terminalId(ServerLevel level) {
        return TerminalId.of(level.dimension(), getBlockPos()).value();
    }

    private void pushSync() {
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("activity", state.activity().name());
        tag.putString("aiAvailability", state.aiAvailabilityState().name());
        tag.putString("activeOperator", state.activeOperatorUuid());
        tag.putLong("pendingUntilGameTime", state.pendingUntilGameTime());

        ListTag historyTag = new ListTag();
        for (String line : state.history()) {
            historyTag.add(StringTag.valueOf(line));
        }
        tag.put("history", historyTag);

        ListTag inboxTag = new ListTag();
        for (String line : state.inbox()) {
            inboxTag.add(StringTag.valueOf(line));
        }
        tag.put("inbox", inboxTag);

        if (state.workflowState() != null) {
            tag.put("workflow", state.workflowState().toTag());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        try {
            state.setActivity(TerminalActivity.valueOf(tag.getString("activity")));
        } catch (Exception ignored) {
            state.setActivity(TerminalActivity.IDLE);
        }
        try {
            state.setAiAvailabilityState(AiAvailabilityState.valueOf(tag.getString("aiAvailability")));
        } catch (Exception ignored) {
            state.setAiAvailabilityState(AiAvailabilityState.DISABLED);
        }
        state.setActiveOperatorUuid(tag.getString("activeOperator"));
        state.setPendingUntilGameTime(tag.getLong("pendingUntilGameTime"));

        state.history().clear();
        ListTag historyTag = tag.getList("history", Tag.TAG_STRING);
        for (Tag element : historyTag) {
            state.history().add(element.getAsString());
        }
        while (state.history().size() > MAX_HISTORY) {
            state.history().removeFirst();
        }

        state.inbox().clear();
        ListTag inboxTag = tag.getList("inbox", Tag.TAG_STRING);
        for (Tag element : inboxTag) {
            state.inbox().add(element.getAsString());
        }

        if (tag.contains("workflow", Tag.TAG_COMPOUND)) {
            state.setWorkflowState(WorkflowState.fromTag(tag.getCompound("workflow")));
        } else {
            state.setWorkflowState(null);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        loadAdditional(tag, registries);
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
