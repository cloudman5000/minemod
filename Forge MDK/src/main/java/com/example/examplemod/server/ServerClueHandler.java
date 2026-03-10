package com.example.examplemod.server;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.effect.ModEffects;
import com.example.examplemod.entity.SignerEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = ExampleMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ServerClueHandler {

    private static final Random RANDOM = new Random();

    // Track states per player
    private static final Map<UUID, BlockPos> activeClueSigns = new HashMap<>();
    private static final Map<UUID, Boolean> isReadingSign = new HashMap<>();
    private static final Map<UUID, Integer> whisperTimers = new HashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END)
            return;

        Player player = event.player;
        Level level = player.level();

        // ONLY RUN ON SERVER
        if (level.isClientSide)
            return;

        UUID pId = player.getUUID();
        BlockPos clueSign = activeClueSigns.get(pId);

        // 1. Spawning the Clue Sign (Requires Dread Effect)
        if (player.hasEffect(ModEffects.DREAD.getHolder().get()) && clueSign == null) {
            if (RANDOM.nextInt(com.example.examplemod.config.ExampleModConfig.CLUE_SIGN_CHANCE.get()) == 0) { // Configured
                                                                                                              // rare
                                                                                                              // chance
                spawnClueSignBehindPlayer(player, level, pId);
            }
        }

        // 2. Whisper and Ambush Logic
        if (clueSign != null) {
            double distance = Math.sqrt(player.blockPosition().distSqr(clueSign));

            // If player walks away, remove the sign
            if (distance > 30) {
                if (level.getBlockState(clueSign).is(Blocks.OAK_SIGN)) {
                    level.setBlock(clueSign, Blocks.AIR.defaultBlockState(), 3);
                }
                activeClueSigns.remove(pId);
                isReadingSign.put(pId, false);
                return;
            }

            // If player gets close, start whispering
            if (distance < 5) {
                int timer = whisperTimers.getOrDefault(pId, 0) + 1;
                whisperTimers.put(pId, timer);

                if (timer % 20 == 0) { // Every second
                    level.playSound(null, clueSign, SoundEvents.WARDEN_AGITATED, SoundSource.AMBIENT, 0.5f,
                            0.5f + RANDOM.nextFloat() * 0.5f);
                    level.playSound(null, clueSign, SoundEvents.ENDERMAN_STARE, SoundSource.AMBIENT, 0.2f, 0.1f);
                }

                // Check if looking at the sign
                Vec3 lookVec = player.getLookAngle();
                Vec3 toSign = Vec3.atCenterOf(clueSign).subtract(player.getEyePosition()).normalize();
                double dotProduct = lookVec.dot(toSign);

                boolean reading = isReadingSign.getOrDefault(pId, false);

                if (dotProduct > 0.9) { // Looking directly at it
                    isReadingSign.put(pId, true);
                } else if (reading && dotProduct < 0.2) { // Turned away after reading
                    // TRIGGER THE SIGNER AMBUSH
                    triggerSignerAmbush(player, level, clueSign);

                    // Remove sign
                    level.setBlock(clueSign, Blocks.AIR.defaultBlockState(), 3);
                    activeClueSigns.remove(pId);
                    isReadingSign.put(pId, false);
                }
            } else {
                whisperTimers.put(pId, 0);
                isReadingSign.put(pId, false);
            }
        }
    }

    private static void spawnClueSignBehindPlayer(Player player, Level level, UUID pId) {
        Vec3 lookVec = player.getLookAngle();
        // Calculate position roughly 5 blocks behind the player
        BlockPos targetPos = BlockPos.containing(
                player.getX() - lookVec.x * 5,
                player.getY(),
                player.getZ() - lookVec.z * 5);

        // Find a valid placement (solid ground below, air above)
        for (int y = -3; y <= 3; y++) {
            BlockPos checkPos = targetPos.above(y);
            if (level.getBlockState(checkPos).isAir()
                    && level.getBlockState(checkPos.below()).isSolidRender(level, checkPos.below())) {
                // Place sign
                level.setBlock(checkPos, Blocks.OAK_SIGN.defaultBlockState(), 3);
                BlockEntity be = level.getBlockEntity(checkPos);
                if (be instanceof SignBlockEntity sign) {
                    String[] messages = { "DON'T LOOK", "BEHIND YOU", "HE IS HERE", "WAKE UP" };
                    String msg = messages[RANDOM.nextInt(messages.length)];
                    sign.updateText(s -> s.setMessage(0, Component.literal(msg)), true);
                }
                activeClueSigns.put(pId, checkPos);
                break;
            }
        }
    }

    private static void triggerSignerAmbush(Player player, Level level, BlockPos signPos) {
        // Find position behind player
        Vec3 lookVec = player.getLookAngle();
        BlockPos spawnPos = BlockPos.containing(
                player.getX() - lookVec.x * 2,
                player.getY(),
                player.getZ() - lookVec.z * 2);

        SignerEntity signer = new SignerEntity(ExampleMod.SIGNER.get(), level);
        signer.setPos(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
        signer.setTargetPlayer(player);
        level.addFreshEntity(signer);
    }
}
