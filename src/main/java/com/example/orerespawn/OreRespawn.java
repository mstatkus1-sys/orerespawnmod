package com.example.orerespawn;

import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.tags.BlockTags;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.commands.arguments.blocks.BlockStateArgument;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.io.*;
import java.nio.file.*;
import java.util.stream.Collectors;

@Mod("orerespawn")
public class OreRespawn {

    public static final Logger LOGGER = LogManager.getLogger();
    private static final Map<BlockPos, BlockState> pendingRespawns = new HashMap<>();
    private static final Set<BlockPos> playerPlaced = new HashSet<>();
    private static final Set<BlockPos> opRegistered = new HashSet<>();
    private static final Map<String, Integer> customTimes = new HashMap<>();
    private static Path saveFile;
    private static Path timesFile;

    public OreRespawn() {
        MinecraftForge.EVENT_BUS.register(this);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onConfig);
        saveFile = Paths.get("config", "orerespawn_registered.txt");
        timesFile = Paths.get("config", "orerespawn_times.txt");
        loadOpRegistered();
        loadCustomTimes();
    }

    private void onConfig(ModConfigEvent event) {}

    // -----------------------------------------------------------------------
    // Track player-placed ores
    // -----------------------------------------------------------------------

    @SubscribeEvent
    public void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        BlockState state = event.getPlacedBlock();
        if (!isTrackedOre(state)) return;
        BlockPos pos = event.getPos();
        if (opRegistered.contains(pos)) return;
        playerPlaced.add(pos);
    }

    // -----------------------------------------------------------------------
    // Commands
    // -----------------------------------------------------------------------

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
            Commands.literal("orerespawn")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("settime")
                    .then(Commands.argument("ore", StringArgumentType.word())
                        .then(Commands.argument("seconds", IntegerArgumentType.integer(1))
                            .executes(ctx -> {
                                String ore = StringArgumentType.getString(ctx, "ore");
                                int seconds = IntegerArgumentType.getInteger(ctx, "seconds");
                                String key = ore.toLowerCase().replace("minecraft:", "");
                                customTimes.put(key, seconds);
                                saveCustomTimes();
                                ctx.getSource().sendSuccess(Component.literal(
                                    "Respawn time for " + key + " set to " + seconds + "s."
                                ), true);
                                return 1;
                            })
                        )
                    )
                )
                .then(Commands.literal("place")
                    .then(Commands.argument("block", BlockStateArgument.block(event.getBuildContext()))
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

        event.getDispatcher().register(
            Commands.literal("orehelp")
                .requires(src -> src.hasPermission(2))
                .executes(ctx -> {
                    var src = ctx.getSource();
                    src.sendSuccess(Component.literal("§6§l--- OreRespawn Commands ---"), false);
                    src.sendSuccess(Component.literal("§e/orerespawn place <ore>"), false);
                    src.sendSuccess(Component.literal("§7  Look at a block and run this to place a respawning ore node."), false);
                    src.sendSuccess(Component.literal("§7  Example: §f/orerespawn place minecraft:diamond_ore"), false);
                    src.sendSuccess(Component.literal("§e/orerespawn settime <ore> <seconds>"), false);
                    src.sendSuccess(Component.literal("§7  Change how long an ore takes to respawn."), false);
                    src.sendSuccess(Component.literal("§7  Example: §f/orerespawn settime diamond_ore 300"), false);
                    src.sendSuccess(Component.literal("§6§l--- Ore Names ---"), false);
                    src.sendSuccess(Component.literal("§7coal_ore, deepslate_coal_ore"), false);
                    src.sendSuccess(Component.literal("§7iron_ore, deepslate_iron_ore"), false);
                    src.sendSuccess(Component.literal("§7copper_ore, deepslate_copper_ore"), false);
                    src.sendSuccess(Component.literal("§7gold_ore, deepslate_gold_ore"), false);
                    src.sendSuccess(Component.literal("§7lapis_ore, deepslate_lapis_ore"), false);
                    src.sendSuccess(Component.literal("§7redstone_ore, deepslate_redstone_ore"), false);
                    src.sendSuccess(Component.literal("§7diamond_ore, deepslate_diamond_ore"), false);
                    src.sendSuccess(Component.literal("§7emerald_ore, deepslate_emerald_ore"), false);
                    src.sendSuccess(Component.literal("§7ancient_debris, nether_gold_ore, nether_quartz_ore"), false);
                    return 1;
                })
        );
    }

    // -----------------------------------------------------------------------
    // Block break — core respawn logic
    // -----------------------------------------------------------------------

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        BlockState state = event.getState();
        if (!isTrackedOre(state)) return;

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
    }

    private void scheduleRespawn(ServerLevel level, BlockPos pos, BlockState state, int tickDelay) {
        new Thread(() -> {
            try {
                Thread.sleep(tickDelay * 50L);
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

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

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

    private int getRespawnTicks(BlockState state) {
        String name = net.minecraft.core.registries.BuiltInRegistries.BLOCK
            .getKey(state.getBlock()).getPath();
        if (customTimes.containsKey(name)) return customTimes.get(name) * 20;
        if (state.is(BlockTags.DIAMOND_ORES) || state.is(BlockTags.EMERALD_ORES)) return 12000;
        if (state.is(BlockTags.GOLD_ORES)    || state.is(BlockTags.LAPIS_ORES))   return 7200;
        if (state.is(BlockTags.IRON_ORES)    || state.is(BlockTags.COPPER_ORES))  return 4800;
        if (state.is(BlockTags.REDSTONE_ORES))                                     return 6000;
        return 2400;
    }

    // -----------------------------------------------------------------------
    // Persistence
    // -----------------------------------------------------------------------

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

    private void saveCustomTimes() {
        try {
            Files.createDirectories(timesFile.getParent());
            List<String> lines = customTimes.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.toList());
            Files.write(timesFile, lines);
        } catch (IOException e) {
            LOGGER.error("Failed to save custom respawn times", e);
        }
    }

    private void loadCustomTimes() {
        if (!Files.exists(timesFile)) return;
        try {
            Files.readAllLines(timesFile).forEach(line -> {
                String[] parts = line.split("=");
                if (parts.length == 2) {
                    customTimes.put(parts[0].trim(), Integer.parseInt(parts[1].trim()));
                }
            });
            LOGGER.info("Loaded {} custom respawn times", customTimes.size());
        } catch (IOException e) {
            LOGGER.error("Failed to load custom respawn times", e);
        }
    }
}
