package mctmods.immersivetechnology.common.blocks;

import java.util.List;
import javax.annotation.Nullable;

import mctmods.immersivetechnology.common.tileentities.TileEntityITSlab;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ItemBlockITSlabs extends ItemBlockITBase {
	public ItemBlockITSlabs(Block b) {
		super(b);
	}

	@Override
	public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag tooltipFlag) {
		super.addInformation(stack, world, tooltip, tooltipFlag);
	}

	@SuppressWarnings("deprecation")
	@Override
	public EnumActionResult onItemUse(PlayerEntity player, World world, BlockPos pos, Hand hand, Direction side, float hitX, float hitY, float hitZ) {
		ItemStack stack = player.getHeldItem(hand);
		BlockState iblockstate = world.getBlockState(pos);
		Block localBlock = iblockstate.getBlock();
		BlockPos posThere = pos;
		BlockPos posOffset = pos.offset(side);

		if(localBlock == Blocks.SNOW_LAYER && localBlock.isReplaceable(world, pos)) side = Direction.UP;
		else if(!localBlock.isReplaceable(world, pos)) pos = pos.offset(side);

		TileEntityITSlab stackSlab = null;
		if(side.getAxis().isVertical() && this.block.equals(world.getBlockState(posThere).getBlock()) && world.getBlockState(posThere).getBlock().getMetaFromState(world.getBlockState(posThere)) == stack.getItemDamage()) {
			TileEntity te = world.getTileEntity(posThere);
			if(te instanceof TileEntityITSlab && ((TileEntityITSlab)te).slabType + side.ordinal() == 1) stackSlab = ((TileEntityITSlab)te);
		}
		else if(this.block.equals(world.getBlockState(posOffset).getBlock()) && world.getBlockState(posOffset).getBlock().getMetaFromState(world.getBlockState(posOffset)) == stack.getItemDamage()) {
			TileEntity te = world.getTileEntity(posOffset);
			if(te instanceof TileEntityITSlab) {
				int type = ((TileEntityITSlab)te).slabType;
				if((type == 0 && (side == Direction.DOWN || hitY >= .5)) || (type == 1 && (side == Direction.UP || hitY <= .5))) stackSlab = ((TileEntityITSlab)te);
			}
		} else {
			return super.onItemUse(player, world, pos, hand, side, hitX, hitY, hitZ);
		}
		if(stackSlab != null) {
			stackSlab.slabType = 2;
			stackSlab.markContainingBlockForUpdate(null);
			world.playSound(stackSlab.getPos().getX() + .5, stackSlab.getPos().getY() + .5, stackSlab.getPos().getZ() + .5, this.block.getSoundType().getPlaceSound(), SoundCategory.BLOCKS, (this.block.getSoundType().getVolume() + 1.0F) / 2.0F, this.block.getSoundType().getPitch() * 0.8F, false);
			stack.shrink(1);
			return EnumActionResult.SUCCESS;
		}
		else
			return super.onItemUse(player, world, pos, hand, side, hitX, hitY, hitZ);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public boolean canPlaceBlockOnSide(World worldIn, BlockPos pos, Direction side, PlayerEntity player, ItemStack stack) {
		return true;
	}

	@Override
	public boolean placeBlockAt(ItemStack stack, PlayerEntity player, World world, BlockPos pos, Direction side, float hitX, float hitY, float hitZ, BlockState newState) {
		boolean ret = super.placeBlockAt(stack, player, world, pos, side, hitX, hitY, hitZ, newState);
		if(ret) {
			TileEntity tileEntity = world.getTileEntity(pos);
			if(tileEntity instanceof TileEntityITSlab) ((TileEntityITSlab)tileEntity).slabType = (side == Direction.DOWN || (side != Direction.UP && hitY >= .5)) ? 1 : 0;
		}
		return ret;
	}

}