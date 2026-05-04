package com.cta.block;

import com.cta.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;

public class ScopeBlock extends BaseEntityBlock {
    
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    
    private static final VoxelShape SHAPE = Block.box(5, 5, 5, 11, 11, 11);
    
    public ScopeBlock() {
        super(Properties.of()
                .mapColor(MapColor.METAL)
                .strength(2.0f, 6.0f)
                .sound(SoundType.METAL)
                .noOcclusion());
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }
    
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }
    
    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite());
    }
    
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
    
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
    
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof ScopeBlockEntity scopeBE)) {
            return InteractionResult.FAIL;
        }
        
        ItemStack heldItem = player.getItemInHand(hand);

        // 1. Handle Wrench interactions first
        if (isCreateWrench(heldItem)) {
            if (level.isClientSide) {
                return InteractionResult.SUCCESS; // Acknowledge wrench use on the client
            }
            return handleWrenchInteraction(state, level, pos, player, scopeBE);
        }
        
        // 2. Prevent the scope from opening if the player is sneaking!
        // This stops the glitching and allows standard Shift-Click behaviors.
        if (player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        
        // 3. Open the Scope Viewer
        if (level.isClientSide) {
            return InteractionResult.SUCCESS; // Acknowledge scope opening on the client
        }
        
        if (player instanceof ServerPlayer serverPlayer) {
            if (scopeBE.isBeingViewed()) {
                return InteractionResult.FAIL;
            }
            scopeBE.startViewing(serverPlayer);
            return InteractionResult.SUCCESS;
        }
        
        return InteractionResult.SUCCESS;
    }
    
    protected InteractionResult handleWrenchInteraction(BlockState state, Level level, BlockPos pos, Player player, ScopeBlockEntity scopeBE) {
        if (player.isShiftKeyDown()) {
            if (scopeBE.getBoundCannonMount() != null) {
                scopeBE.bindToCannonMount(null);
                player.displayClientMessage(Component.translatable("message.cta.scope.unbound"), true);
                return InteractionResult.SUCCESS;
            }
            float newPitch = scopeBE.getScopePitch() + 15;
            if (newPitch > 90) newPitch = -90 + (newPitch - 90);
            if (newPitch < -90) newPitch = 90 + (newPitch + 90);
            scopeBE.setScopeRotation(scopeBE.getScopeYaw(), newPitch);
        } else {
            float newYaw = scopeBE.getScopeYaw() + 15;
            if (newYaw >= 360) newYaw -= 360;
            if (newYaw < 0) newYaw += 360;
            scopeBE.setScopeRotation(newYaw, scopeBE.getScopePitch());
        }
        return InteractionResult.SUCCESS;
    }
    
    protected boolean isCreateWrench(ItemStack stack) {
        if (stack.isEmpty()) return false;
        var registryName = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (registryName == null) return false;
        return registryName.getNamespace().equals("create") && registryName.getPath().equals("wrench");
    }
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ScopeBlockEntity(pos, state);
    }
    
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (!level.isClientSide) {
            return createTickerHelper(type, ModBlockEntities.SCOPE_BE.get(), ScopeBlockEntity::serverTick);
        }
        return null;
    }
    
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ScopeBlockEntity scopeBE) {
                scopeBE.onRemoved();
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}