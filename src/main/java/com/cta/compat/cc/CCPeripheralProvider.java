package com.cta.compat.cc;

import com.cta.wireless.WirelessLinkSavedData;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.IPeripheralProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nonnull;

public class CCPeripheralProvider implements IPeripheralProvider {

    @Nonnull
    @Override
    public LazyOptional<IPeripheral> getPeripheral(@Nonnull Level world, @Nonnull BlockPos pos, @Nonnull Direction side) {
        if (!(world instanceof ServerLevel serverLevel)) return LazyOptional.empty();

        WirelessLinkSavedData data = WirelessLinkSavedData.get(serverLevel);
        BlockPos computerPos = pos.relative(side);

        // 1. Check if the computer itself holds a wireless link
        if (data.hasLinks(computerPos)) {
            return LazyOptional.of(() -> new WirelessConnectorPeripheral(computerPos, serverLevel));
        }

        // 2. Allow direct physical wired connections to Scopes
        net.minecraft.world.level.block.entity.BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof com.cta.block.ScopeBlockEntity scopeBE) {
            return LazyOptional.of(() -> new ScopePeripheral(scopeBE)); // <-- Fixed this line!
        }

        // 3. Allow direct physical wired connections to CBC Cannons
        if (com.cta.compat.CBCCompat.isCBCLoaded() && com.cta.compat.CBCCompat.isCannonMount(world, pos)) {
            return LazyOptional.of(() -> new CBCCannonPeripheral(world, pos));
        }

        return LazyOptional.empty();
    }
}
