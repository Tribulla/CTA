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

        if (side != Direction.DOWN) return LazyOptional.empty();

        BlockPos computerPos = pos.relative(side);
        WirelessLinkSavedData data = WirelessLinkSavedData.get(serverLevel);
        if (!data.hasLinks(computerPos)) return LazyOptional.empty();

        return LazyOptional.of(() -> new WirelessConnectorPeripheral(computerPos, serverLevel));
    }
}
