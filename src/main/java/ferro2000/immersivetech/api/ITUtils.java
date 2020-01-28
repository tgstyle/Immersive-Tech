package ferro2000.immersivetech.api;

import ferro2000.immersivetech.api.client.MechanicalEnergyAnimation;
import ferro2000.immersivetech.common.blocks.ITBlockInterface.IMechanicalEnergy;
import ferro2000.immersivetech.common.blocks.metal.tileentities.TileEntityAlternator;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import net.minecraftforge.fluids.IFluidTank;

import java.util.ArrayList;

public class ITUtils {
	public static IFluidTank[] emptyIFluidTankList = new IFluidTank[0];

	public static BlockPos LocalOffsetToWorldBlockPos(BlockPos origin, int x, int y, int z, EnumFacing facing) {
		return LocalOffsetToWorldBlockPos(origin, x, y, z, facing, EnumFacing.UP);
	}

	public static BlockPos LocalOffsetToWorldBlockPos(BlockPos origin, int x, int y, int z, EnumFacing facing, EnumFacing up) {
		if(facing.getAxis() == up.getAxis()) throw new IllegalArgumentException("'facing' and 'up' must be perpendicular to each other!");
		switch (up) {
			case UP:
				switch (facing) {
					case SOUTH: return origin.add(-x, y, z);
					case NORTH: return origin.add(x, y, -z);
					case EAST: return origin.add(z, y, x);
					case WEST: return origin.add(-z, y, -x);
				default:
					break;
				} break;
			case DOWN:
				switch (facing) {
					case SOUTH: return origin.add(x, -y, z);
					case NORTH: return origin.add(-x, -y, -z);
					case EAST: return origin.add(z, -y, -x);
					case WEST: return origin.add(-z, -y, x);
				default:
					break;
				} break;
			case NORTH:
				switch (facing) {
					case UP: return origin.add(-x, z, -y);
					case DOWN: return origin.add(x, -z, -y);
					case EAST: return origin.add(z, x, -y);
					case WEST: return origin.add(-z, -x, -y);
				default:
					break;
				} break;
			case SOUTH:
				switch (facing) {
					case UP: return origin.add(x, z, y);
					case DOWN: return origin.add(-x, -z, y);
					case EAST: return origin.add(z, -x, y);
					case WEST: return origin.add(-z, x, y);
				default:
					break;
				} break;
			case EAST:
				switch (facing) {
					case UP: return origin.add(y, z, -x);
					case DOWN: return origin.add(y, -z, x);
					case SOUTH: return origin.add(y, x, z);
					case NORTH: return origin.add(y, -x, -z);
				default:
					break;
				} break;
			case WEST:
				switch (facing) {
					case UP: return origin.add(-y, z, x);
					case DOWN: return origin.add(-y, -z, -x);
					case SOUTH: return origin.add(-y, -x, z);
					case NORTH: return origin.add(-y, x, -z);
				default:
					break;
				} break;
		}
		throw new IllegalArgumentException("This part of the code should never be reached! Has EnumFacing changed ? ");
	}

	public static <T> T First(ArrayList <T> list, Object o) {
		for(T item: list) {
			if(item.equals(o)) return item;
		}
		return null;
	}

	public static double[] smartBoundingBox(double A, double B, double C, double D, double minY, double maxY, EnumFacing fl, EnumFacing fw) {
		double[] boundingArray = new double[6];

		boundingArray[0] = fl == EnumFacing.WEST ? A : fl == EnumFacing.EAST ? B : fw == EnumFacing.EAST ? C : D;
		boundingArray[1] = minY;
		boundingArray[2] = fl == EnumFacing.NORTH ? A : fl == EnumFacing.SOUTH ? B : fw == EnumFacing.SOUTH ? C : D;
		boundingArray[3] = fl == EnumFacing.EAST ? 1-A : fl == EnumFacing.WEST ? 1-B : fw == EnumFacing.EAST ? 1-D : 1-C;
		boundingArray[4] = maxY;
		boundingArray[5] = fl == EnumFacing.SOUTH ? 1-A : fl == EnumFacing.NORTH ? 1-B : fw == EnumFacing.SOUTH ? 1-D : 1-C;

		return boundingArray;
	}

	public static boolean checkMechanicalEnergyTransmitter(World world, BlockPos startPos) {
		TileEntity tile = world.getTileEntity(startPos);
		if(tile instanceof IMechanicalEnergy) {
			if(((IMechanicalEnergy) tile).isMechanicalEnergyReceiver()) {
				EnumFacing inputFacing = ((IMechanicalEnergy) tile).getMechanicalEnergyInputFacing();
				BlockPos pos = startPos.offset(inputFacing, ((IMechanicalEnergy) tile).inputToCenterDistance() + 1);
				TileEntity tileTransmitter = world.getTileEntity(pos);

				if(tileTransmitter instanceof IMechanicalEnergy && ((IMechanicalEnergy) tileTransmitter).isMechanicalEnergyTransmitter() && (((IMechanicalEnergy) tileTransmitter).getMechanicalEnergyOutputFacing() == inputFacing.getOpposite())) {
					return true;
				}
			}
		}
		return false;
	}

	public static boolean checkMechanicalEnergyReceiver(World world, BlockPos startPos) {
		TileEntity tile = world.getTileEntity(startPos);

		if(tile instanceof IMechanicalEnergy) {
			if(((IMechanicalEnergy) tile).isMechanicalEnergyTransmitter()) {
				EnumFacing outputFacing = ((IMechanicalEnergy) tile).getMechanicalEnergyOutputFacing();
				BlockPos pos = startPos.offset(outputFacing, ((IMechanicalEnergy) tile).outputToCenterDistance() + 1);
				TileEntity tileReceiver = world.getTileEntity(pos);

				if(tileReceiver instanceof IMechanicalEnergy && ((IMechanicalEnergy) tileReceiver).isMechanicalEnergyReceiver() && ((IMechanicalEnergy) tileReceiver).getMechanicalEnergyInputFacing() == outputFacing.getOpposite()) {
					return true;
				}
			}
		}
		return false;
	}

	public static int getMechanicalEnergy(World world, BlockPos startPos) {
		TileEntity tile = world.getTileEntity(startPos);
		EnumFacing inputFacing = ((IMechanicalEnergy) tile).getMechanicalEnergyInputFacing();
		BlockPos pos = startPos.offset(inputFacing, ((IMechanicalEnergy) tile).inputToCenterDistance() + 1);
		TileEntity tileInfo = world.getTileEntity(pos);
		TileEntity tileTransmitter = world.getTileEntity(pos.offset(inputFacing, ((IMechanicalEnergy) tileInfo).outputToCenterDistance()));

		if(tileTransmitter instanceof IMechanicalEnergy) {
			return ((IMechanicalEnergy) tileTransmitter).getEnergy();
		} else {
			return 0;
		}
	}

	public static boolean checkAlternatorStatus(World world, BlockPos startPos) {
		TileEntity tile = world.getTileEntity(startPos);
		EnumFacing outputFacing = ((IMechanicalEnergy) tile).getMechanicalEnergyOutputFacing();
		BlockPos pos = startPos.offset(outputFacing, ((IMechanicalEnergy) tile).outputToCenterDistance() + 1);
		TileEntity tileInfo = world.getTileEntity(pos);
		TileEntity tileReceiver = world.getTileEntity(pos.offset(outputFacing, ((IMechanicalEnergy) tileInfo).inputToCenterDistance()));

		if(tileReceiver instanceof TileEntityAlternator) {
			if(((TileEntityAlternator) tileReceiver).canRunMechanicalEnergy()) {
				return true;
			}
		}
		return false;
	}

	public static boolean setRotationAngle(MechanicalEnergyAnimation animation, float rotationSpeed) {
		float oldMomentum = animation.getAnimationMomentum();
		float rotateTo = (animation.getAnimationRotation() + rotationSpeed) % 360;
		animation.setAnimationRotation(rotateTo);
		animation.setAnimationMomentum(rotationSpeed);
		return (oldMomentum != rotationSpeed);
	}

	public static MechanicalEnergyAnimation getMechanicalEnergyAnimation(World world, BlockPos startPos) {
		TileEntity tile = world.getTileEntity(startPos);
		EnumFacing inputFacing = ((IMechanicalEnergy) tile).getMechanicalEnergyInputFacing();
		BlockPos pos = startPos.offset(inputFacing, ((IMechanicalEnergy) tile).inputToCenterDistance() + 1);
		TileEntity tileInfo = world.getTileEntity(pos);
		TileEntity tileTransmitter = world.getTileEntity(pos.offset(inputFacing, ((IMechanicalEnergy) tileInfo).outputToCenterDistance()));

		if(tileTransmitter instanceof IMechanicalEnergy) {
			return ((IMechanicalEnergy) tileTransmitter).getAnimation();
		} else {
			return new MechanicalEnergyAnimation();
		}
	}

	public static EnumFacing getInputFacing(World world, BlockPos startPos) {
		TileEntity tileTransmitter;
		BlockPos pos;

		for(EnumFacing f: EnumFacing.HORIZONTALS) {
			pos = startPos.offset(f, 1);
			tileTransmitter = world.getTileEntity(pos);

			if(tileTransmitter instanceof IMechanicalEnergy) {
				if(((IMechanicalEnergy) tileTransmitter).isMechanicalEnergyTransmitter() && ((IMechanicalEnergy) tileTransmitter).getMechanicalEnergyOutputFacing() == f.getOpposite()) {
					return f;
				}
			}
		}
		return null;
	}

}