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
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.commands.arguments.blocks.BlockStateArgument;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.io.*;
import java.nio.file.*;
import java.util.stream.Collectors;
import java.util.HashMap;
import java.util.Map;

@Mod("orerespawn")
public class OreRespawn {

    public static final Logger LOGGER = LogManager.getLogger();
    private static final Map<BlockPos, BlockState> pendingRespawns = new HashMap<>();
    private static final Set<BlockPos> playerPlaced = new HashSet<>();
    private static final Set<BlockPos> opRegistered = new HashSet<>();
    private static Path saveFile;

    public OreRespawn() {
        MinecraftForge.EVENT_BUS.register(this);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onConfig);
        saveFile = Paths.get("config", "orerespawn_registered.txt");
        loadOpRegistered();
    }
    private void onConfig(ModConfigEvent event) {}

@SubscribeEvent
    public void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        BlockState state = event.getPlacedBlock();
        if (!isTrackedOre(state)) return;
        BlockPos pos = event.getPos();
        if (opRegistered.contains(pos)) return;
        playerPlaced.add(pos);
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
            Commands.literal("orerespawn")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("place")
                    .then(BlockStateArgument.block(event.getBuildContext())
                        .executes(ctx -> {
                            var src = ctx.getSource();
                            var player = src.getPlayerOrException();
                            var hitResult = player.pick(5.0, 1.0f, false);
                            if (hitResult.getType() != HitResult.Type.BLOCK) {
                                src.sendFailure(Component.literal("Look at a block first."));
                                return 0;
                            }
                            BlockPos targetPos = ((BlockHitResult) hitResult).getBlockPos();
                            BlockState chosenState = BlockStateArgument.getBlock(ctx, "block").getState();
                            if (!isTrackedOre(chosenState)) {
                                src.sendFailure(Component.literal("That block isn't a tracked ore."));
                                return 0;
                            }
                            src.getLevel().setBlock(targetPos, chosenState, 3);
                            opRegistered.add(targetPos);
                            playerPlaced.remove(targetPos);
                            saveOpRegistered();
                            src.sendSuccess(Component.literal(
                                "Placed " + chosenState.getBlock().getName().getString() +
                                " at " + targetPos + " — it will respawn when mined."
                            ), true);
                            return 1;
                        })
                    )
                )
        );
    }

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {

        BlockPos pos = event.getPos();
        Level level = (Level) event.getLevel();
        if (!(level instanceof ServerLevel serverLevel)) return;

        if (playerPlaced.contains(pos)) {
            playerPlaced.remove(pos);
            opRegistered.remove(pos);
            return;
        }

        int delayTicks = getRespawnTicks(state);
        pendingRespawns.put(pos, state);
        serverLevel.getServer().execute(() ->
            scheduleRespawn(serverLevel, pos, state, delayTicks)
        );
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

private boolean isTrackedOre(BlockState state) {
        return state.is(BlockTags.COAL_ORES)
            || state.is(BlockTags.IRON_ORES)
            || state.is(BlockTags.COPPER_ORES)
            || state.is(BlockTags.GOLD_ORES)
            || state.is(BlockTags.LAPIS_ORES)
            || state.is(BlockTags.REDSTONE_ORES)
            || state.is(BlockTags.DIAMOND_ORES)
            || state.is(BlockTags.EMERALD_ORES);
    }

    private void saveOpRegistered() {
        try {
            Files.createDirectories(saveFile.getParent());
            List<String> lines = opRegistered.stream()
                .map(p -> p.getX() + "," + p.getY() + "," + p.getZ())
                .collect(Collectors.toList());
            Files.write(saveFile, lines);
        } catch (IOException e) {
            LOGGER.error("Failed to save op-registered ore positions", e);
        }
    }

    private void loadOpRegistered() {
        if (!Files.exists(saveFile)) return;
        try {
            Files.readAllLines(saveFile).forEach(line -> {
                String[] parts = line.split(",");
                if (parts.length == 3) {
                    opRegistered.add(new BlockPos(
                        Integer.parseInt(parts[0].trim()),
                        Integer.parseInt(parts[1].trim()),
                        Integer.parseInt(parts[2].trim())
                    ));
                }
            });
            LOGGER.info("Loaded {} op-registered ore positions", opRegistered.size());
        } catch (IOException e) {
            LOGGER.error("Failed to load op-registered ore positions", e);
        }
    }

    private int getRespawnTicks(BlockState state) {
