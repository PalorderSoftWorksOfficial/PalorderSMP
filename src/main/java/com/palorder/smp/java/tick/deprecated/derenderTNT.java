package com.palorder.smp.java.tick.deprecated;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Iterator;
import java.util.Set;

import static com.palorder.smp.java.Implementations.spawnTNTNuke.nukeSpawnedEntities;
import static com.palorder.smp.java.Implementations.spawnTNTNuke.pausedChunks;

public class derenderTNT {
    /**
     * @deprecated This method is unsafe in the main thread, separate it from the main thread or just don't use it, DO NOT USE!
     */
    @Deprecated(since = "UNSAFE", forRemoval = true)
    @SubscribeEvent
    public static void onWorldTick(TickEvent.LevelTickEvent event) {
        if (event.phase == TickEvent.Phase.START) return;
        if (!(event.level instanceof ServerLevel world)) return;

        Set<ChunkPos> frozen = pausedChunks.get(world);
        if (frozen == null || frozen.isEmpty()) return;

        Set<Entity> entities = nukeSpawnedEntities.get(world);
        if (entities == null || entities.isEmpty()) return;

        Iterator<Entity> iterator = entities.iterator();
        int processedPerTick = 100;
        int count = 0;

        while (iterator.hasNext() && count < processedPerTick) {
            Entity e = iterator.next();
            ChunkPos chunkPos = new ChunkPos(e.blockPosition());
            if (frozen.contains(chunkPos)) {
                e.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
                iterator.remove();
                count++;
            }
        }

        if (entities.isEmpty()) nukeSpawnedEntities.remove(world);
        if (frozen.isEmpty()) pausedChunks.remove(world);
    }
}
