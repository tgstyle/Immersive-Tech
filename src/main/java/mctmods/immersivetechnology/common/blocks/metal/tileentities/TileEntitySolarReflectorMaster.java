package mctmods.immersivetechnology.common.blocks.metal.tileentities;

import blusunrize.immersiveengineering.common.util.Utils;

import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;

public class TileEntitySolarReflectorMaster extends TileEntitySolarReflectorSlave {

    public boolean sun = false;

    @Override
    public void update() {
        if(!world.isRemote && formed) {
            if(canSeeSun() && world.isDaytime()) {
                this.sun = true;
            } else {
                this.sun = false;
            }
        }
    }

    @Override
    public boolean isDummy() {
        return false;
    }

    @Override
    public TileEntitySolarReflectorMaster master() {
        master = this;
        return this;
    }

    public boolean getSunState() {
        return this.sun;
    }

    private boolean canSeeSun() {
        BlockPos pos = this.getPos();
        int hh = 256 - pos.getY();
        for(int h = 2; h < hh; h++) {
            pos = this.getPos().add(0, h, 0);
            if(!Utils.isBlockAt(world, pos, Blocks.AIR, 0)) return false;
        }
        return true;
    }

}