package ferro2000.immersivetech.common.blocks.metal.tileentities;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.Lists;

import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces.IAdvancedCollisionBounds;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces.IAdvancedSelectionBounds;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces.IGuiTile;
import blusunrize.immersiveengineering.common.blocks.metal.TileEntityMultiblockMetal;
import blusunrize.immersiveengineering.common.util.Utils;

import ferro2000.immersivetech.ImmersiveTech;
import ferro2000.immersivetech.api.ITLib;
import ferro2000.immersivetech.api.crafting.DistillerRecipe;
import ferro2000.immersivetech.common.blocks.metal.multiblocks.MultiblockDistiller;

import ferro2000.immersivetech.common.util.ITSound;
import ferro2000.immersivetech.common.util.ITSounds;
import ferro2000.immersivetech.common.util.network.MessageTileSync;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.IFluidTank;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.oredict.OreDictionary;

public class TileEntityDistiller extends TileEntityMultiblockMetal<TileEntityDistiller, DistillerRecipe> implements IGuiTile, IAdvancedSelectionBounds, IAdvancedCollisionBounds {
	public TileEntityDistiller() {
		super(MultiblockDistiller.instance, new int[] { 3, 3, 3 }, 16000, true);
	}

	public FluidTank[] tanks = new FluidTank[] {
		new FluidTank(24000),
		new FluidTank(24000)
	};

	private ITSound runningSound;

	public NonNullList<ItemStack> inventory = NonNullList.withSize(4, ItemStack.EMPTY);

	@Override
	public void readCustomNBT(NBTTagCompound nbt, boolean descPacket) {
		super.readCustomNBT(nbt, descPacket);
		tanks[0].readFromNBT(nbt.getCompoundTag("tank0"));
		tanks[1].readFromNBT(nbt.getCompoundTag("tank1"));
		running = nbt.getBoolean("running");
		if(!descPacket) inventory = Utils.readInventory(nbt.getTagList("inventory", 10), 4);
	}

	@Override
	public void writeCustomNBT(NBTTagCompound nbt, boolean descPacket) {
		super.writeCustomNBT(nbt, descPacket);
		nbt.setTag("tank0", tanks[0].writeToNBT(new NBTTagCompound()));
		nbt.setTag("tank1", tanks[1].writeToNBT(new NBTTagCompound()));
		nbt.setBoolean("running", running);
		if(!descPacket)
		nbt.setTag("inventory", Utils.writeInventory(inventory));
	}

	private boolean running;
	private float soundVolume;
	private boolean previousRenderState;

	public void handleSounds() {
		if (runningSound == null) runningSound = new ITSound(this, ITSounds.distiller, SoundCategory.BLOCKS, true, 1, 1, getPos());
		if (running) {
			if (soundVolume < 1) soundVolume += 0.01f;
		} else if (soundVolume > 0) soundVolume -= 0.01f;
		if (soundVolume == 0) runningSound.stopSound();
		else {
			BlockPos center = getPos();
			EntityPlayerSP player = Minecraft.getMinecraft().player;
			float attenuation = Math.max((float) player.getDistanceSq(center.getX(), center.getY(), center.getZ()) / 8, 1);
			runningSound.updateVolume(soundVolume / attenuation);
			runningSound.playSound();
		}
	}

	@Override
	public void receiveMessageFromServer(NBTTagCompound message) {
		running = message.getBoolean("running");
	}

	public void notifyNearbyClients() {
		NBTTagCompound tag = new NBTTagCompound();
		tag.setBoolean("running", running);
		BlockPos center = getPos();
		ImmersiveTech.packetHandler.sendToAllTracking(new MessageTileSync(this, tag), new NetworkRegistry.TargetPoint(world.provider.getDimension(), center.getX(), center.getY(), center.getZ(), 0));
	}

	@SuppressWarnings("unchecked")
	@Override
	public void update() {
		super.update();
		if (isDummy()) return;
		if(world.isRemote) {
			handleSounds();
			return;
		}
		boolean update = false;
		if(energyStorage.getEnergyStored() > 0 && processQueue.size() < this.getProcessQueueMaxLength()) {
			if(tanks[0].getFluidAmount() > 0) {
				DistillerRecipe recipe = DistillerRecipe.findRecipe(tanks[0].getFluid());
				if(recipe != null) {
					MultiblockProcessInMachine<DistillerRecipe> process = new MultiblockProcessInMachine<DistillerRecipe>(recipe).setInputTanks(new int[] {0});
					if(this.addProcessToQueue(process, false)) update = true;
				}
			}
		}
		if(this.tanks[1].getFluidAmount() > 0) {
			ItemStack filledContainer = Utils.fillFluidContainer(tanks[1], inventory.get(2), inventory.get(3), null);
			if(!filledContainer.isEmpty()) {
				if(!inventory.get(3).isEmpty() && OreDictionary.itemMatches(inventory.get(3), filledContainer, true)) inventory.get(3).grow(filledContainer.getCount());
				else if(inventory.get(3).isEmpty()) inventory.set(3, filledContainer.copy());
				inventory.get(2).shrink(1);
				if(inventory.get(2).getCount() <= 0) inventory.set(2, ItemStack.EMPTY);
				update = true;
			}
			EnumFacing fw;
			if(!mirrored) {
				fw = facing.rotateY().getOpposite();
			} else {
				fw = facing.rotateY();
			}
			if(this.tanks[1].getFluidAmount() > 0) {
				FluidStack out = Utils.copyFluidStackWithAmount(this.tanks[1].getFluid(), Math.min(this.tanks[1].getFluidAmount(), 80), false);
				BlockPos outputPos = this.getPos().add(0, -1, 0).offset(fw, 2);
				IFluidHandler output = FluidUtil.getFluidHandler(world, outputPos, facing);
				if(output != null) {
					int accepted = output.fill(out, false);
					if(accepted > 0) {
						int drained = output.fill(Utils.copyFluidStackWithAmount(out, Math.min(out.amount, accepted), false), true);
						this.tanks[1].drain(drained, true);
						update = true;
					}
				}
			}
		}
		ItemStack emptyContainer = Utils.drainFluidContainer(tanks[0], inventory.get(0), inventory.get(1), null);
		if(!emptyContainer.isEmpty() && emptyContainer.getCount() > 0) {
			if(!inventory.get(1).isEmpty() && OreDictionary.itemMatches(inventory.get(1), emptyContainer, true)) inventory.get(1).grow(emptyContainer.getCount());
			else if(inventory.get(1).isEmpty())	inventory.set(1, emptyContainer.copy());
			inventory.get(0).shrink(1);
			if(inventory.get(0).getCount() <= 0) inventory.set(0, ItemStack.EMPTY);
			update = true;
		}
		if(update) {
			this.markDirty();
			this.markContainingBlockForUpdate(null);
		}

		running = shouldRenderAsActive() && !processQueue.isEmpty() && processQueue.get(0).canProcess(this);
		if (previousRenderState != running) notifyNearbyClients();
		previousRenderState = running;
	}

	@Override
	public NonNullList<ItemStack> getInventory() {
		return inventory;
	}

	@Override
	public boolean isStackValid(int slot, ItemStack stack) {
		return true;
	}

	@Override
	public int getSlotLimit(int slot) {
		return 64;
	}

	@Override
	public void doGraphicalUpdates(int slot) {
		this.markDirty();
		this.markContainingBlockForUpdate(null);
	}

	@Override
	public IFluidTank[] getInternalTanks() {
		return tanks;
	}

	@Override
	protected DistillerRecipe readRecipeFromNBT(NBTTagCompound tag) {
		return DistillerRecipe.loadFromNBT(tag);
	}
	
	@Override
	public DistillerRecipe findRecipeForInsertion(ItemStack inserting) {
		return null;
	}

	@Override
	public int[] getEnergyPos() {
		return new int[] {9};
	}

	@Override
	public int[] getRedstonePos() {
		return new int[] {11};
	}

	@Override
	public int[] getOutputSlots() {
		return null ;
	}

	@Override
	public int[] getOutputTanks() {
		return new int[] {1};
	}

	@Override
	public boolean additionalCanProcessCheck(MultiblockProcess<DistillerRecipe> process) {
		return true;
	}

	@Override
	public void doProcessOutput(ItemStack output) {
		BlockPos pos = getPos().add(0, -1, 0).offset(facing.getOpposite(), -2);
		TileEntity inventoryTile = this.world.getTileEntity(pos);
		if(inventoryTile != null) output = Utils.insertStackIntoInventory(inventoryTile, output, facing.getOpposite());
		if(output != null) Utils.dropStackAtPos(world, pos, output, facing);
	}

	@Override
	public void doProcessFluidOutput(FluidStack output) {
	}

	@Override
	public void onProcessFinish(MultiblockProcess<DistillerRecipe> process) {
	}

	@Override
	public int getMaxProcessPerTick() {
		return 1;
	}

	@Override
	public int getProcessQueueMaxLength() {
		return 1;
	}

	@Override
	public float getMinProcessDistance(MultiblockProcess<DistillerRecipe> process) {
		return 0;
	}
	
	@Override
	public boolean isInWorldProcessingMachine() {
		return false;
	}
	
	@Override
	protected IFluidTank[] getAccessibleFluidTanks(EnumFacing side) {
		TileEntityDistiller master = this.master();
		if(master != null) {
			if(pos == 5 && (side == null || side == (mirrored ? facing.rotateYCCW():facing.rotateY()))) {
				return new FluidTank[] {master.tanks[0]};
			} else if(pos == 3 && (side == null || side == (mirrored ? facing.rotateY():facing.rotateYCCW()))) {
				return new FluidTank[] {master.tanks[1]};
			}
		}
		return new FluidTank[0];
	}

	@Override
	protected boolean canFillTankFrom(int iTank, EnumFacing side, FluidStack resource) {
		TileEntityDistiller master = this.master();
		if(master == null) return false;
		if(pos == 5 && (side == null || side == (mirrored ? facing.rotateYCCW():facing.rotateY()))) {
			FluidStack resourceClone = Utils.copyFluidStackWithAmount(resource, 1000, false);
			FluidStack resourceClone2 = Utils.copyFluidStackWithAmount(master.tanks[iTank].getFluid(), 1000, false);
			if(master.tanks[iTank].getFluidAmount() >= master.tanks[iTank].getCapacity()) return false;
			if(master.tanks[iTank].getFluid() == null) {
				DistillerRecipe incompleteRecipes = DistillerRecipe.findRecipe(resourceClone);
				return incompleteRecipes != null;
			} else {
				DistillerRecipe incompleteRecipes1 = DistillerRecipe.findRecipe(resourceClone);
				DistillerRecipe incompleteRecipes2 = DistillerRecipe.findRecipe(resourceClone2);
				return incompleteRecipes1 == incompleteRecipes2;
			}
		}
		return false;
	}

	@Override
	protected boolean canDrainTankFrom(int iTank, EnumFacing side) {
		return (pos == 3 && (side == null || side == (mirrored ? facing.rotateY():facing.rotateYCCW())));
	}

	@Override
	public boolean canOpenGui() {
		return formed;
	}

	@Override
	public int getGuiID() {
		return ITLib.GUIID_Distiller;
	}

	@Override
	public TileEntity getGuiMaster() {
		return master();
	}

	@Override
	public TileEntityDistiller getTileForPos(int targetPos) {
		BlockPos target = getBlockPosForPos(targetPos);
		TileEntity tile = world.getTileEntity(target);
		return tile instanceof TileEntityDistiller ? (TileEntityDistiller) tile : null;
	}
	
	@Override
	public float[] getBlockBounds() {
		if(pos > 0 && pos < 9 && pos != 5 && pos != 3 && pos != 7) return new float[] {0, 0, 0, 1, .5f, 1};
		if(pos == 11) return new float[] {facing == EnumFacing.WEST ? .5f:0, 0, facing == EnumFacing.NORTH ? .5f:0, facing == EnumFacing.EAST ? .5f:1, 1, facing == EnumFacing.SOUTH ? .5f:1};
		if(pos == 21 || pos == 24) return new float[] {0, 0, 0, 1, .5f, 1};
		return new float[] {0, 0, 0, 1, 1, 1};
	}

	@Override
	public List<AxisAlignedBB> getAdvancedColisionBounds() {
		return getAdvancedSelectionBounds();
	}

	@Override
	public List<AxisAlignedBB> getAdvancedSelectionBounds() {
		EnumFacing fl = facing;
		EnumFacing fw = facing.rotateY();
		if(mirrored) fw = fw.getOpposite();
		if(pos == 2) {
			List<AxisAlignedBB> list = Lists.newArrayList(new AxisAlignedBB(0, 0, 0, 1, .5f, 1).offset(getPos().getX(), getPos().getY(), getPos().getZ()));
			float minX = fl == EnumFacing.WEST ? .625f: fl == EnumFacing.EAST ? .125f: .125f;
			float maxX = fl == EnumFacing.EAST ? .375f: fl == EnumFacing.WEST ? .875f: .25f;
			float minZ = fl == EnumFacing.NORTH ? .625f: fl == EnumFacing.SOUTH ? .125f: .125f;
			float maxZ = fl == EnumFacing.SOUTH ? .375f: fl == EnumFacing.NORTH ? .875f: .25f;
			list.add(new AxisAlignedBB(minX, .5f, minZ, maxX, 1, maxZ).offset(getPos().getX(), getPos().getY(), getPos().getZ()));
			minX = fl == EnumFacing.WEST ? .625f: fl == EnumFacing.EAST ? .125f: .75f;
			maxX = fl == EnumFacing.EAST ? .375f: fl == EnumFacing.WEST ? .875f: .875f;
			minZ = fl == EnumFacing.NORTH ? .625f: fl == EnumFacing.SOUTH ? .125f: .75f;
			maxZ = fl == EnumFacing.SOUTH ? .375f: fl == EnumFacing.NORTH ? .875f: .875f;
			list.add(new AxisAlignedBB(minX, .5f, minZ, maxX, 1, maxZ).offset(getPos().getX(), getPos().getY(), getPos().getZ()));
			return list;
		}
		if(pos == 6 || pos == 8) {
			List<AxisAlignedBB> list = Lists.newArrayList(new AxisAlignedBB(0, 0, 0, 1, .5f, 1).offset(getPos().getX(), getPos().getY(), getPos().getZ()));
			fl = fl.getOpposite();
			if(pos == 8) fw = fw.getOpposite();
			float minX = fl == EnumFacing.WEST ? .6875f: fl == EnumFacing.EAST ? .0625f: fw == EnumFacing.EAST ? .0625f: .6875f;
			float maxX = fl == EnumFacing.EAST ? .3125f: fl == EnumFacing.WEST ? .9375f: fw == EnumFacing.EAST ? .3125f: .9375f;
			float minZ = fl == EnumFacing.NORTH ? .6875f: fl == EnumFacing.SOUTH ? .0625f: fw == EnumFacing.SOUTH ? .0625f: .6875f;
			float maxZ = fl == EnumFacing.SOUTH ? .3125f: fl == EnumFacing.NORTH ? .9375f: fw == EnumFacing.SOUTH ? .3125f: .9375f;
			list.add(new AxisAlignedBB(minX, .5f, minZ, maxX, 1.1875f, maxZ).offset(getPos().getX(), getPos().getY(), getPos().getZ()));
			return list;
		}
		return null;
	}

	@Override
	public boolean isOverrideBox(AxisAlignedBB box, EntityPlayer player, RayTraceResult mop, ArrayList<AxisAlignedBB> list) {
		return false;
	}

}