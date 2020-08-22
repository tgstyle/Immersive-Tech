package mctmods.immersivetechnology.common.blocks.metal.multiblocks;

import blusunrize.immersiveengineering.api.Lib;
import blusunrize.immersiveengineering.api.MultiblockHandler;
import blusunrize.immersiveengineering.api.MultiblockHandler.IMultiblock;
import blusunrize.immersiveengineering.api.crafting.IngredientStack;
import blusunrize.immersiveengineering.client.ClientUtils;
import blusunrize.immersiveengineering.common.IEContent;
import blusunrize.immersiveengineering.common.blocks.BlockTypes_MetalsAll;
import blusunrize.immersiveengineering.common.blocks.metal.BlockTypes_MetalDecoration0;
import blusunrize.immersiveengineering.common.blocks.metal.BlockTypes_MetalDecoration1;
import blusunrize.immersiveengineering.common.blocks.metal.BlockTypes_MetalDevice1;
import blusunrize.immersiveengineering.common.util.Utils;
import mctmods.immersivetechnology.common.ITContent;
import mctmods.immersivetechnology.common.blocks.metal.tileentities.TileEntityDistillerSlave;
import mctmods.immersivetechnology.common.blocks.metal.types.BlockType_MetalMultiblock;
import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class MultiblockDistiller implements IMultiblock {
	public static MultiblockDistiller instance = new MultiblockDistiller();

	static ItemStack[][][] structure = new ItemStack[3][3][3];
	static {
		for(int h = 0 ; h < 3 ; h ++) {
			for(int l = 0 ; l < 3 ; l ++) {
				for(int w = 0 ; w < 3 ; w ++) {
					if(h == 0) {
						if(l == 0 && w == 0) {
							structure[h][l][w] = new ItemStack(IEContent.blockMetalDecoration0, 1, BlockTypes_MetalDecoration0.LIGHT_ENGINEERING.getMeta());
						} else if(l == 1 && w > - 1) {
							structure[h][l][w] = new ItemStack(IEContent.blockMetalDevice1, 1, BlockTypes_MetalDevice1.FLUID_PIPE.getMeta());
						} else {
							structure[h][l][w] = new ItemStack(IEContent.blockMetalDecoration1, 1, BlockTypes_MetalDecoration1.STEEL_SCAFFOLDING_0.getMeta());
						}
					} else if(h == 1) {
						if(l == 0 && w == 0) {
							structure[h][l][w] = new ItemStack(IEContent.blockMetalDecoration0, 1, BlockTypes_MetalDecoration0.LIGHT_ENGINEERING.getMeta());
						} else if(l == 0 && w == 2) {
							structure[h][l][w] = new ItemStack(IEContent.blockMetalDecoration0, 1, BlockTypes_MetalDecoration0.RS_ENGINEERING.getMeta());
						} else if(l > 0 && w > - 1) {
							structure[h][l][w] = new ItemStack(IEContent.blockSheetmetal, 1, BlockTypes_MetalsAll.IRON.getMeta());
						}
					} else if(h == 2) {
						if(l > 0 && w > 0) {
							structure[h][l][w] = new ItemStack(IEContent.blockSheetmetal, 1, BlockTypes_MetalsAll.IRON.getMeta());
						} else if(l > 0 && w < 1) {
							structure[h][l][w] = new ItemStack(IEContent.blockMetalDecoration1, 1, BlockTypes_MetalDecoration1.STEEL_SCAFFOLDING_0.getMeta());
						}
					}
				}
			}
		}
	}

	@SideOnly(Side.CLIENT)
	static ItemStack renderStack;

	@Override
	public String getUniqueName() {
		return "IT:Distiller";
	}

	@Override
	public boolean isBlockTrigger(BlockState state) {
		return Utils.compareToOreName(new ItemStack(state.getBlock(), 1, state.getBlock().getMetaFromState(state)), "blockSheetmetalIron");
	}

	@Override
	public boolean createStructure(World world, BlockPos pos, Direction side, PlayerEntity player) {
		side = (side == Direction.UP || side == Direction.DOWN)? Direction.fromAngle(player.rotationYaw) : side.getOpposite();
		BlockState master = ITContent.blockMetalMultiblock.getStateFromMeta(BlockType_MetalMultiblock.DISTILLER.getMeta());
		BlockState slave = ITContent.blockMetalMultiblock.getStateFromMeta(BlockType_MetalMultiblock.DISTILLER_SLAVE.getMeta());
		boolean mirror = false;
		if(!this.structureCheck(world, pos, side, mirror)) {
			mirror = true;
			if(!this.structureCheck(world, pos, side, mirror)) return false;
		}
		if(player != null) {
			ItemStack hammer = player.getHeldItemMainhand().getItem().getToolClasses(player.getHeldItemMainhand()).contains(Lib.TOOL_HAMMER)?player.getHeldItemMainhand(): player.getHeldItemOffhand();
			if(MultiblockHandler.fireMultiblockFormationEventPost(player, this, pos, hammer).isCanceled()) return false;
		}
		for(int h = - 1 ; h <= 1 ; h ++)
			for(int l = - 1 ; l <= 1 ; l ++)
				for(int w = - 1 ; w <= 1 ; w ++) {
					if((h == 0 && w == 0 && l == - 1) || (h == 1 && l < 0)) continue;
					int ww = mirror ? - w : w;
					BlockPos pos2 = pos.offset(side, l).offset(side.rotateY(), ww).add(0, h, 0);
					int[] offset = new int[] {(side == Direction.WEST ? - l : side == Direction.EAST ? l : side == Direction.NORTH ? ww : - ww), h, (side == Direction.NORTH ? - l : side == Direction.SOUTH ? l : side == Direction.EAST ? ww : - ww)};
					world.setBlockState(pos2, (offset[0]==0&&offset[1]==0&&offset[2]==0)? master : slave);
					TileEntity curr = world.getTileEntity(pos2);
					if(curr instanceof TileEntityDistillerSlave) {
						TileEntityDistillerSlave tile = (TileEntityDistillerSlave)curr;
						tile.facing = side;
						tile.formed = true;
						tile.pos = (h + 1) * 9 + (l + 1) * 3 + (w + 1);
						tile.offset = offset;
						tile.mirrored = mirror;
						tile.markDirty();
						world.addBlockEvent(pos2, ITContent.blockMetalMultiblock, 255, 0);
					}
				}
		return true;
	}

	boolean structureCheck(World world, BlockPos startPos, Direction dir, boolean mirror) {
		for(int h = - 1 ; h < 2 ; h ++) {
			for(int l = - 1 ; l < 2 ; l ++) {
				for(int w = - 1 ; w < 2 ; w ++) {
					if((h == 0 && w == 0 && l == - 1) || (h == 1 && l < 0))continue;
					int ww = mirror ? - w : w;
					BlockPos pos = startPos.offset(dir, l).offset(dir.rotateY(), ww).add(0, h, 0);
					if(h == - 1) {
						if(l == - 1 && w == - 1) {
							if(!Utils.isBlockAt(world, pos, IEContent.blockMetalDecoration0, BlockTypes_MetalDecoration0.LIGHT_ENGINEERING.getMeta())) return false;
						} else if(l == 0) {
							if(!Utils.isBlockAt(world, pos, IEContent.blockMetalDevice1, BlockTypes_MetalDevice1.FLUID_PIPE.getMeta())) return false;
						} else {
							if(!Utils.isOreBlockAt(world, pos, "scaffoldingSteel")) return false;
						}
					} else if(h == 0) {
						if(l == - 1 && w == - 1) {
							if(!Utils.isBlockAt(world, pos, IEContent.blockMetalDecoration0, BlockTypes_MetalDecoration0.LIGHT_ENGINEERING.getMeta())) return false;
						} else if(l == - 1 && w == 1) {
							if(!Utils.isBlockAt(world, pos, IEContent.blockMetalDecoration0, BlockTypes_MetalDecoration0.RS_ENGINEERING.getMeta())) return false;
						} else {
							if(!Utils.isOreBlockAt(world, pos, "blockSheetmetalIron")) return false;
						}
					} else if(h == 1) {
						if(l > - 1 && w > - 1) {
							if(!Utils.isOreBlockAt(world, pos, "blockSheetmetalIron")) return false;
						} else if(l > - 1 && w == - 1) {
							if(!Utils.isOreBlockAt(world, pos, "scaffoldingSteel")) return false;
						}
					}
				}
			}
		}
		return true;
	}

	@Override
	public ItemStack[][][] getStructureManual() {
		return structure;
	}

	static final IngredientStack[] materials = new IngredientStack[] {
			new IngredientStack("scaffoldingSteel", 7), 
			new IngredientStack(new ItemStack(IEContent.blockMetalDevice1, 3, BlockTypes_MetalDevice1.FLUID_PIPE.getMeta())), 
			new IngredientStack(new ItemStack(IEContent.blockMetalDecoration0, 1, BlockTypes_MetalDecoration0.RS_ENGINEERING.getMeta())), 
			new IngredientStack(new ItemStack(IEContent.blockMetalDecoration0, 2, BlockTypes_MetalDecoration0.LIGHT_ENGINEERING.getMeta())), 
			new IngredientStack("blockSheetmetalIron", 10)};
	@Override
	public IngredientStack[] getTotalMaterials() {
		return materials;
	}

	@Override
	public boolean overwriteBlockRender(ItemStack stack, int iterator) {
		return false;
	}

	@Override
	public float getManualScale() {
		return 13;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public boolean canRenderFormedStructure() {
		return true;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void renderFormedStructure() {
		if(renderStack == null) renderStack = new ItemStack(ITContent.blockMetalMultiblock, 1, BlockType_MetalMultiblock.DISTILLER.getMeta());
		GlStateManager.translate(1.5, 1.5, 1.5);
		GlStateManager.rotate(- 45, 0, 1, 0);
		GlStateManager.rotate(- 20, 1, 0, 0);
		GlStateManager.scale(4, 4, 4);
		GlStateManager.disableCull();
		ClientUtils.mc().getRenderItem().renderItem(renderStack, ItemCameraTransforms.TransformType.GUI);
		GlStateManager.enableCull();
	}

}