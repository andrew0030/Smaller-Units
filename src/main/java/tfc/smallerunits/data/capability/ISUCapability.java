package tfc.smallerunits.data.capability;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import tfc.smallerunits.UnitSpace;

public interface ISUCapability {
	void removeUnit(BlockPos pos);
	
	void makeUnit(BlockPos pos);
	
	UnitSpace getOrMakeUnit(BlockPos pos);
	
	CompoundTag serializeNBT();
	
	void deserializeNBT(int index, CompoundTag nbt);
	
	UnitSpace[] getUnits();
	
	void setUnit(BlockPos realPos, UnitSpace space);
	
	UnitSpace getUnit(BlockPos pos);
}
