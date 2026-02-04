package com.palorder.smp.java.helpers;

import com.palorder.smp.java.fakes.RedstoneWireBlockInterface;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

public class RedstoneWireTurbo {

    private final Block wire;

    private final List<UpdateNode> updateQueue0 = new ArrayList<>();
    private final List<UpdateNode> updateQueue1 = new ArrayList<>();
    private final List<UpdateNode> updateQueue2 = new ArrayList<>();
    private final Map<BlockPos, UpdateNode> nodeCache = new HashMap<>();

    private int currentWalkLayer = 0;

    public RedstoneWireTurbo(Block wire) {
        this.wire = wire;
    }

    /**
     * Called by mixin to update surrounding redstone blocks.
     */
    public BlockState updateSurroundingRedstone(Level world, BlockPos pos, BlockState state, BlockPos source) {
        BlockState newState = ((RedstoneWireBlockInterface) wire).updateLogicPublic(world, pos, state);
        if (newState == state) return state;

        if (currentWalkLayer <= 0 && nodeCache.isEmpty()) {
            if (source != null) {
                UpdateNode src = new UpdateNode();
                src.self = source;
                src.parent = source;
                src.visited = true;
                identifyNode(world, src);
                nodeCache.put(source, src);
            }

            UpdateNode upd = new UpdateNode();
            upd.self = pos;
            upd.parent = (source != null) ? source : pos;
            upd.currentState = newState;
            upd.type = UpdateNode.Type.REDSTONE;
            upd.visited = true;
            nodeCache.put(pos, upd);

            propagateChanges(world, upd, 0);
            breadthFirstWalk(world);
            nodeCache.clear();
            return newState;
        } else {
            return scheduleReentrantNeighborChanged(world, pos, newState, source);
        }
    }

    /** Update neighboring block states for this node */
    public void updateNeighborShapes(Level world, BlockPos pos, BlockState state) {
        for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.values()) {
            BlockPos neighborPos = pos.relative(dir);
            world.updateNeighborsAt(neighborPos, wire);
        }
    }

    private void identifyNode(Level world, UpdateNode node) {
        BlockState state = world.getBlockState(node.self);
        node.type = state.getBlock() == wire ? UpdateNode.Type.REDSTONE : UpdateNode.Type.OTHER;
        node.currentState = state;
    }

    private void propagateChanges(Level world, UpdateNode node, int layer) {
        if (node.neighborNodes == null) findNeighbors(world, node);
        int nextLayer = layer + 1;
        for (UpdateNode neighbor : node.neighborNodes) {
            if (neighbor != null && nextLayer > neighbor.layer) {
                neighbor.layer = nextLayer;
                updateQueue1.add(neighbor);
                neighbor.parent = node.self;
            }
        }
    }

    private void findNeighbors(Level world, UpdateNode node) {
        node.neighborNodes = new UpdateNode[24]; // placeholder
        // Implement actual neighbor detection if needed
    }

    private void breadthFirstWalk(Level world) {
        shiftQueue();
        currentWalkLayer = 1;

        while (!updateQueue0.isEmpty() || !updateQueue1.isEmpty()) {
            for (UpdateNode node : updateQueue0) {
                if (node.type == UpdateNode.Type.REDSTONE) {
                    // Could perform more complex redstone updates here
                } else {
                    world.updateNeighborsAt(node.self, wire);
                }
            }
            shiftQueue();
            currentWalkLayer++;
        }

        currentWalkLayer = 0;
    }

    private void shiftQueue() {
        List<UpdateNode> tmp = updateQueue0;
        updateQueue0.clear();
        updateQueue0.addAll(updateQueue1);
        updateQueue1.clear();
        updateQueue1.addAll(updateQueue2);
        updateQueue2.clear();
    }

    private BlockState scheduleReentrantNeighborChanged(Level world, BlockPos pos, BlockState newState, BlockPos source) {
        UpdateNode node = nodeCache.computeIfAbsent(pos, k -> {
            UpdateNode n = new UpdateNode();
            n.self = k;
            n.parent = (source != null) ? source : k;
            n.visited = true;
            identifyNode(world, n);
            return n;
        });

        node.currentState = newState;
        propagateChanges(world, node, currentWalkLayer);
        return newState;
    }

    /**
     * Node representing a single redstone block in the BFS update
     */
    public static class UpdateNode {
        public BlockState currentState;
        public UpdateNode[] neighborNodes;
        public BlockPos self;
        public BlockPos parent;
        public Type type = Type.UNKNOWN;
        public int layer;
        public boolean visited;

        public enum Type {
            REDSTONE,
            OTHER,
            UNKNOWN
        }
    }

    /** Cardinal directions for neighbors */
    public enum Direction {
        NORTH, EAST, SOUTH, WEST, UP, DOWN;

        public BlockPos relative(BlockPos pos) {
            return switch (this) {
                case NORTH -> pos.north();
                case EAST -> pos.east();
                case SOUTH -> pos.south();
                case WEST -> pos.west();
                case UP -> pos.above();
                case DOWN -> pos.below();
            };
        }
    }
}
