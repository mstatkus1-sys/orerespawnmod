package com.example.orerespawn;

import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.tags.BlockTags;
import net.minecraft.server.level.ServerLevel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

@Mod("orerespawn")
public class OreRespawn {

    public static final Logger LOGGER = LogManager.getLogger();
    private static final Map<BlockPos, BlockState> pendingRespawns = new HashMap<>();

    public OreRespawn() {
        MinecraftForge.EVENT_BUS.register(this);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onConfig);
    }

     private void onConfig(ModConfigEvent event) {}

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        BlockState state = event.getState();
        if (!state.is(BlockTags.COAL_ORES)
         && !state.is(BlockTags.IRON_ORES)
         && !state.is(BlockTags.COPPER_ORES)
         && !state.is(BlockTags.GOLD_ORES)
         && !state.is(BlockTags.LAPIS_ORES)
         && !state.is(BlockTags.REDSTONE_ORES)
         && !state.is(BlockTags.DIAMOND_ORES)
         && !state.is(BlockTags.EMERALD_ORES)) {
            return;
        }

        BlockPos pos = event.getPos();
        Level level = (Level) event.getLevel();
        if (!(level instanceof ServerLevel serverLevel)) return;

        int delayTicks = getRespawnTicks(state);
        pendingRespawns.put(pos, state);

        serverLevel.getServer().execute(() ->
            scheduleRespawn(serverLevel, pos, state, delayTicks)
        );
    }

    private void scheduleRespawn(ServerLevel level, BlockPos pos, BlockState state, int tickDelay) {
        new Thread(() -> {
            try {
                Thread.sleep(tickDelay * 50L); // ticks to ms
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            level.getServer().execute(() -> {
                if (pendingRespawns.containsKey(pos)) {
                    level.setBlock(pos, state, 3);
                    pendingRespawns.remove(pos);
                    LOGGER.info("Ore respawned at {}", pos);
                }
            });
        }).start();
    }

    private int getRespawnTicks(BlockState state) {
        if (state.is(BlockTags.DIAMOND_ORES) || state.is(BlockTags.EMERALD_ORES)) return 12000; // 10 min
        if (state.is(BlockTags.GOLD_ORES) || state.is(BlockTags.LAPIS_ORES))       return 7200;  // 6 min
        if (state.is(BlockTags.IRON_ORES)  || state.is(BlockTags.COPPER_ORES))      return 4800;  // 4 min
        if (state.is(BlockTags.REDSTONE_ORES))                                       return 6000;  // 5 min
        return 2400; // coal — 2 min
    }
}
