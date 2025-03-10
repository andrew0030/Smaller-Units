package tfc.smallerunits.simulation.chunk;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.TickingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import tfc.smallerunits.Registry;
import tfc.smallerunits.UnitSpace;
import tfc.smallerunits.UnitSpaceBlock;
import tfc.smallerunits.api.GeneralUtils;
import tfc.smallerunits.client.access.tracking.FastCapabilityHandler;
import tfc.smallerunits.client.access.tracking.SUCapableChunk;
import tfc.smallerunits.data.capability.ISUCapability;
import tfc.smallerunits.data.capability.SUCapabilityManager;
import tfc.smallerunits.networking.hackery.NetworkingHacks;
import tfc.smallerunits.simulation.block.ParentLookup;
import tfc.smallerunits.simulation.level.ITickerLevel;
import tfc.smallerunits.simulation.level.UnitChunkHolder;
import tfc.smallerunits.utils.math.Math1D;

import java.util.ArrayList;
import java.util.List;

public class BasicVerticalChunk extends LevelChunk {
	private final BlockState[] blocks = new BlockState[16 * 16 * 16]; // TODO: I'd like to get rid of this
	public final int yPos;
	// holds the functional chunk and a method which gets the corresponding BasicVerticalChunk from an integer representing which vertical chunk
	// quite basic... weird to word however
	private final VChunkLookup verticalLookup;
	public final ArrayList<BlockPos> updated = new ArrayList<>();
	public final ArrayList<BlockPos> besRemoved = new ArrayList<>();
	public ArrayList<BlockEntity> beChanges = new ArrayList<>();
	ParentLookup lookup;
	private int upb;
	public UnitChunkHolder holder = null;
	
	public final int getIndx(int x, int y, int z) {
		// truthfully, this is x, z, y order, but it really does not matter at all
		return x + (((y * 16) + z) * 16);
	}
	
	public boolean isLoaded() {
		for (BlockState block : blocks) if (block != null) return true;
		return false;
	}
	
	public BlockState getBlockState$(BlockPos pos) {
		// locals would be redundant, this is an internal method
		// this method assumes that pos.y will always be in bounds of the specific BasicVerticalChunk
		BlockState state = blocks[getIndx(pos.getX() & 15, pos.getY(), pos.getZ() & 15)];
		if (state == null) state = Blocks.AIR.defaultBlockState();
		return state;
	}
	
	public BasicVerticalChunk(Level pLevel, ChunkPos pPos, int y, VChunkLookup verticalLookup, ParentLookup lookup, int upb) {
		super(pLevel, pPos);
		this.yPos = y;
		this.verticalLookup = verticalLookup;
		// this actually shouldn't cause any extra memory overhead, it just removes a bunch of null check
		// afaik,
		// sizeof((long) nullptr) == 8
		// sizeof((long) Pointer<BlockState>) == 8
		// in java, you're never really dealing with objects, only pointers
		// however, it will likely slow down world loading a bit
		for (int i = 0; i < blocks.length; i++) blocks[i] = Blocks.AIR.defaultBlockState();
		this.lookup = lookup;
		this.upb = upb;
		setLoaded(true);
		// TODO: use mixin to null out unnecessary fields, maybe
	}
	
	@Override
	public boolean isTicking(BlockPos pPos) {
		// TODO:
		return true;
	}
	
	@Override
	public LevelChunkSection getSection(int p_187657_) {
		if (p_187657_ == yPos) return super.getSection(0);
		return verticalLookup.applyAbs(p_187657_).getSection(p_187657_);
	}
	
	@Override
	public int getSectionsCount() {
		return 3;
	}
	
	@Override
	public int getMinSection() {
		return yPos - 1;
	}
	
	@Override
	public int getMaxSection() {
		return yPos + 1;
	}
	
	@Override
	public int getMinBuildHeight() {
//		return 0;
		if (yPos == 0) return 0;
		return (yPos - 1) * 16;
//		return 0;
//		return -16;
	}
	
	@Override
	public int getMaxBuildHeight() {
//		return level.getMaxBuildHeight();
		return (yPos + 1) * 16;
//		return 16;
//		return 32;
	}
	
	@Override
	public int getSectionIndex(int pY) {
		return this.getSectionIndexFromSectionY(SectionPos.blockToSectionCoord(pY));
	}
	
	@Override
	public int getSectionIndexFromSectionY(int pSectionIndex) {
//		int ms = this.getMinSection();
//		int v = pSectionIndex + ms;
//		return v;
		
		return level.getSectionIndexFromSectionY(pSectionIndex);
//		return pSectionIndex + yPos;
	}
	
	@Override
	public int getSectionYFromSectionIndex(int pSectionIndex) {
		return super.getSectionYFromSectionIndex(pSectionIndex);
	}
	
	@Nullable
	@Override
	public BlockState setBlockState(BlockPos pos, BlockState pState, boolean pIsMoving) {
		int yO = Math1D.getChunkOffset(pos.getY(), 16);
		if (yO != 0 || pos.getX() < 0 || pos.getZ() < 0 || pos.getX() >= (upb * 32) || pos.getZ() >= (upb * 32)) {
//			if (yPos + yO < 0) {
//				// TODO: fix
//				BasicVerticalChunk chunk = verticalLookup.apply(yPos + yO);
//				if (chunk == null) return Blocks.VOID_AIR.defaultBlockState();
//				if (chunk.holder != null)
//					chunk.holder.setBlockDirty(pos.offset(0, upb * 16, 0));
//				return chunk.setBlockState(new BlockPos(pos.getX(), pos.getY() + upb * 16, pos.getZ()), pState, pIsMoving);
//			}

//			if (yPos + yO < 0)
//				return Blocks.VOID_AIR.defaultBlockState();
			BasicVerticalChunk chunk = verticalLookup.apply(yPos + yO);
			if (chunk.holder != null)
				chunk.holder.setBlockDirty(new BlockPos(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15));
			return chunk.setBlockState$(new BlockPos(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15), pState, pIsMoving);
		}
		if (holder != null)
			holder.setBlockDirty(pos);
		return setBlockState$(pos, pState, pIsMoving);
	}
	
	@Override
	public BlockEntity getBlockEntity(BlockPos pPos, LevelChunk.EntityCreationType pCreationType) {
		return super.getBlockEntity(pPos, pCreationType);
	}
	
	@Override
	public void addAndRegisterBlockEntity(BlockEntity pBlockEntity) {
		super.addAndRegisterBlockEntity(pBlockEntity);
	}
	
	@Override
	public <T extends BlockEntity> TickingBlockEntity createTicker(T pBlockEntity, BlockEntityTicker<T> pTicker) {
		if (yPos == 0)
			return super.createTicker(pBlockEntity, pTicker);
		return verticalLookup.applyAbs(0).createTicker(pBlockEntity, pTicker);
	}
	
	// TODO: I'm sure I can shrink this
	public BlockState setBlockState$(BlockPos pPos, BlockState pState, boolean pIsMoving) {
//		LevelChunkSection levelchunksection = getSection(getSectionIndex(pPos.getY()));
		LevelChunkSection levelchunksection = super.getSection(0);
		boolean flag = levelchunksection.hasOnlyAir();
		int j = pPos.getX() & 15;
		int k = pPos.getY();
		int l = pPos.getZ() & 15;
		if (!level.isClientSide) {
			BlockPos parentPos = GeneralUtils.getParentPosPrecise(pPos, this); // this is returning the wrong thing
			LevelChunk ac = ((ITickerLevel) level).getParent().getChunkAt(parentPos);
			if (ac instanceof FastCapabilityHandler capabilityHandler) {
				UnitSpace space = capabilityHandler.getSUCapability().getUnit(parentPos);
				if (space == null) {
					BlockState state = ac.getBlockState(parentPos);
					if (state.isAir()) { // TODO: do this better
						if (!pState.isAir()) {
							ac.setBlockState(parentPos, tfc.smallerunits.Registry.UNIT_SPACE.get().defaultBlockState(), false);
							ac.getLevel().sendBlockUpdated(parentPos, state, Registry.UNIT_SPACE.get().defaultBlockState(), 0);
							space = capabilityHandler.getSUCapability().getOrMakeUnit(parentPos);
							// TODO: debug why space can still be null after this or what
							space.isNatural = true;
//							space.unitsPerBlock = ((ITickerWorld) level).getUPB();
							space.setUpb(((ITickerLevel) level).getUPB());
							space.sendSync(PacketDistributor.TRACKING_CHUNK.with(() -> ac));
						}
					}
				}
				int indx = getIndx(j, k, l);
				ac.setUnsaved(true);
				if (space != null) {
					space.removeState(blocks[indx]);
					space.addState(pState);
					if (space.isEmpty() && space.isNatural) {
						space.clear();
						NetworkingHacks.LevelDescriptor descriptor = NetworkingHacks.unitPos.get();
						NetworkingHacks.unitPos.remove();
						ac.setBlockState(parentPos, Blocks.AIR.defaultBlockState(), false);
						BlockState state = ac.getBlockState(parentPos);
						ac.getLevel().sendBlockUpdated(parentPos, state, Registry.UNIT_SPACE.get().defaultBlockState(), 0);
						capabilityHandler.getSUCapability().removeUnit(pPos);
						if (descriptor != null)
							NetworkingHacks.unitPos.set(descriptor);
					}
				}
			}
		}
		// TODO
		if (flag && pState.isAir()) {
			return null;
		} else {
//			getSection(getSectionIndex(0)).setBlockState(j, k, l, pState);
			super.getSection(0).setBlockState(j, k, l, pState);
			pPos = pPos.above(yPos * 16);
			int indx = getIndx(j, k, l);
//			BlockState blockstate = levelchunksection.setBlockState(j, k, l, pState);
			BlockState blockstate = blocks[indx];
			if (blockstate == pState) {
				return null;
			} else {
				blocks[indx] = pState;
				Block block = pState.getBlock();
				this.heightmaps.get(Heightmap.Types.MOTION_BLOCKING).update(j, k, l, pState);
				this.heightmaps.get(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES).update(j, k, l, pState);
				this.heightmaps.get(Heightmap.Types.OCEAN_FLOOR).update(j, k, l, pState);
				this.heightmaps.get(Heightmap.Types.WORLD_SURFACE).update(j, k, l, pState);
				boolean flag1 = levelchunksection.hasOnlyAir();
				// TODO
				if (flag != flag1) {
//					this.level.getChunkSource().getLightEngine().updateSectionStatus(pPos, !flag1);
					level.getLightEngine().checkBlock(chunkPos.getWorldPosition().offset(pPos).offset(0, yPos * 16, 0));
				}
				
				boolean flag2 = blockstate.hasBlockEntity();
				if (!this.level.isClientSide) {
					blockstate.onRemove(this.level, pPos, pState, pIsMoving);
				} else if ((!blockstate.is(block) || !pState.hasBlockEntity()) && flag2) {
					this.removeBlockEntity(pPos);
				}
				
				if (!blocks[indx].is(block)) {
					return null;
				} else {
					if (!this.level.isClientSide && !this.level.captureBlockSnapshots) {
						pState.onPlace(this.level, pPos, blockstate, pIsMoving);
					}
					
					if (pState.hasBlockEntity()) {
						BlockPos realPos = pPos.offset(chunkPos.getWorldPosition());
						BlockEntity blockentity = this.getBlockEntity(realPos, LevelChunk.EntityCreationType.CHECK);
						if (blockentity == null) {
							blockentity = ((EntityBlock) block).newBlockEntity(realPos, pState);
							if (blockentity != null) {
								this.addAndRegisterBlockEntity(blockentity);
							}
						} else {
							blockentity.setBlockState(pState);
							this.updateBlockEntityTicker(blockentity);
						}
					}
					
					this.unsaved = true;
					updated.add(pPos.below(yPos * 16));
					return blockstate;
				}
			}
		}
	}

//	Block edgeBlock = Registry.UNIT_EDGE.get();
	
	@Override
	public BlockState getBlockState(BlockPos pos) {
		boolean lookupPass = false;
		BlockPos parentPos = GeneralUtils.getParentPos(pos, this);
		BlockState parentState = lookup.getState(parentPos);
		if (parentState.isAir() || parentState.getBlock() instanceof UnitSpaceBlock) lookupPass = true;
		if (lookupPass) {
			int yO = Math1D.getChunkOffset(pos.getY(), 16);
			if (yO != 0 || pos.getX() < 0 || pos.getZ() < 0 || pos.getX() >= (upb * 32) || pos.getZ() >= (upb * 32)) {
				BasicVerticalChunk chunk = verticalLookup.apply(yPos + yO);
				return chunk.getBlockState$(new BlockPos(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15));
			}
			return getBlockState$(pos);
		} else {
//			System.out.println(GeneralUtils.getParentPos(pos, this));
//			return edgeBlock.defaultBlockState();
			return Registry.UNIT_EDGE.get().defaultBlockState();
		}
	}
	
	@Override
	public FluidState getFluidState(BlockPos pos) {
		return getBlockState(pos).getFluidState();
	}
	
	private void setBlockFast$(BlockPos pos, BlockState state) {
		int j = pos.getX() & 15;
		int k = pos.getY();
		int l = pos.getZ() & 15;
		int indx = getIndx(j, k, l);
		BlockPos parentPos = GeneralUtils.getParentPos(pos, this);
		
		// TODO: this can be optimized, but it's good for now
		ChunkAccess ac = ((ITickerLevel) level).getParent().getChunkAt(parentPos);
		ISUCapability capability = SUCapabilityManager.getCapability(((ITickerLevel) level).getParent(), ac);
		UnitSpace space = capability.getOrMakeUnit(parentPos);
		space.removeState(blocks[indx]);
		space.addState(state);
		
		blocks[indx] = state;
		super.getSection(0).setBlockState(j, k, l, state);
		
		level.getLightEngine().checkBlock(chunkPos.getWorldPosition().offset(pos).offset(0, yPos * 16, 0));

//		if (level.isClientSide) {
//			if (state.hasBlockEntity()) {
//				BlockEntity be = ((EntityBlock) state.getBlock()).newBlockEntity(new BlockPos(j, k, l).offset(this.chunkPos.getWorldPosition().getX(), this.yPos * 16, this.chunkPos.getWorldPosition().getZ()), state);
//				if (be == null) return;
//				setBlockEntity(be);
//
////				ac.setBlockState(parentPos, tfc.smallerunits.Registry.UNIT_SPACE.get().defaultBlockState(), false);
//				((SUCapableChunk) ac).addTile(be);
//			}
//		}
	}
	
	public void setBlockFast(BlockPos pos, BlockState state) {
		int yO = Math1D.getChunkOffset(pos.getY(), 16);
		if (yO != 0) {
			BasicVerticalChunk chunk = verticalLookup.apply(yPos + yO);
			chunk.setBlockFast$(new BlockPos(pos.getX(), pos.getY() & 15, pos.getZ()), state);
//			if (level.isClientSide) {
//				if (state.hasBlockEntity()) {
//					BlockEntity be;
////					setBlockEntity(be = ((EntityBlock) state.getBlock()).newBlockEntity(pos, state));
//					setBlockEntity(be = ((EntityBlock) state.getBlock()).newBlockEntity(pos.offset(this.chunkPos.getWorldPosition().getX(), this.yPos * 16, this.chunkPos.getWorldPosition().getZ()), state));
//
//					BlockPos rp = ((TickerServerWorld) level).region.pos.toBlockPos();
//					int xo = (pos.getX() / ((TickerServerWorld) level).getUnitsPerBlock());
//					int yo = (pos.getY() / ((TickerServerWorld) level).getUnitsPerBlock());
//					int zo = (pos.getZ() / ((TickerServerWorld) level).getUnitsPerBlock());
//					BlockPos parentPos = rp.offset(xo, yo, zo);
//					ChunkAccess ac = ((TickerServerWorld) level).parent.getChunkAt(parentPos);
//					ac.setBlockState(parentPos, tfc.smallerunits.Registry.UNIT_SPACE.get().defaultBlockState(), false);
//					((SUCapableChunk) ac).addTile(be);
//				}
//			}
			return;
		}
		setBlockFast$(new BlockPos(pos), state);
	}
	
	ArrayList<ServerPlayer> oldPlayersTracking = new ArrayList<>();
	ArrayList<ServerPlayer> playersTracking = new ArrayList<>();
	
	public void randomTick() {
		for (int k = 0; k < level.getGameRules().getInt(GameRules.RULE_RANDOMTICKING); ++k) {
			BlockPos blockpos1 = level.getBlockRandomPos(0, 0, 0, 15);
			BlockState blockstate = this.getBlockState(blockpos1);
			BlockPos wp = blockpos1.offset(chunkPos.getWorldPosition()).relative(Direction.UP, yPos * 16 - 1);
			if (blockstate.isRandomlyTicking()) {
				if (!level.isClientSide() && level instanceof ServerLevel) {
					blockstate.randomTick((ServerLevel) level, wp.above(), level.random);
				}
			}
			
			FluidState fluidstate = blockstate.getFluidState();
			if (fluidstate.isRandomlyTicking()) {
				fluidstate.randomTick(level, wp, level.random);
			}
		}
	}
	
	public BasicVerticalChunk getSubChunk(int cy) {
		return verticalLookup.apply(cy);
	}
	
	@Override
	public void setBlockEntity(BlockEntity pBlockEntity) {
		BlockPos blockpos = pBlockEntity.getBlockPos();
		if (this.getBlockState(new BlockPos(blockpos.getX() & 15, blockpos.getY() - (yPos * 16), blockpos.getZ() & 15)).hasBlockEntity()) {
			pBlockEntity.setLevel(this.level);
			pBlockEntity.clearRemoved();
			BlockEntity blockentity = this.blockEntities.put(blockpos.immutable(), pBlockEntity);
			if (blockentity != null && blockentity != pBlockEntity) {
				blockentity.setRemoved();
			}
		}
		
		if (!level.isClientSide) return;
		
		ITickerLevel tickerWorld = ((ITickerLevel) level);
		
		BlockPos parentPos = GeneralUtils.getParentPos(pBlockEntity.getBlockPos().offset(0, -(yPos * 16), 0), this);
		ChunkAccess ac;
		ac = tickerWorld.getParent().getChunkAt(parentPos);

//		ISUCapability cap = SUCapabilityManager.getCapability((LevelChunk) ac);
//		UnitSpace space = cap.getUnit(parentPos);
		// TODO: check if a renderer exists, or smth?
		((SUCapableChunk) ac).addTile(pBlockEntity);
	}
	
	@Override
	public void removeBlockEntity(BlockPos pPos) {
		if (!level.isClientSide) {
//			besRemoved.add(pPos);
//			for (BlockEntity beChange : beChanges) {
//				if (beChange == null) continue;
//				if (beChange.getBlockPos().equals(pPos)) {
//					beChange.setRemoved();
//					beChanges.remove(beChange);
//					break;
//				}
//			}
		} else {
			ITickerLevel tickerWorld = ((ITickerLevel) level);
			
			pPos = new BlockPos(pPos.getX() & 15, pPos.getY(), pPos.getZ() & 15);
			BlockPos parentPos = GeneralUtils.getParentPos(pPos, this);
			ChunkAccess ac;
			ac = tickerWorld.getParent().getChunkAt(parentPos);
			pPos = pPos.offset(getPos().getWorldPosition());
			
			ISUCapability cap = SUCapabilityManager.getCapability((LevelChunk) ac);
			UnitSpace space = cap.getUnit(parentPos);
//			if (space == null) {
//				space = cap.getOrMakeUnit(parentPos);
//				space.isNatural = true;
//				space.setUpb(upb);
//				space.sendSync(PacketDistributor.TRACKING_CHUNK.with(() -> (LevelChunk) ac));
//			}
			// TODO: check if a renderer exists, or smth?
			ArrayList<BlockEntity> toRemove = new ArrayList<>();
			synchronized (((SUCapableChunk) ac).getTiles()) {
				for (BlockEntity tile : ((SUCapableChunk) ac).getTiles()) {
					if (tile.getBlockPos().equals(pPos)) {
						toRemove.add(tile);
					}
				}
				((SUCapableChunk) ac).getTiles().removeAll(toRemove);
			}
		}
		super.removeBlockEntity(pPos);
	}
	
	public boolean isTrackedBy(ServerPlayer player) {
		return oldPlayersTracking.contains(player);
	}
	
	public void setTracked(ServerPlayer player) {
		playersTracking.add(player);
		if (oldPlayersTracking.contains(player)) oldPlayersTracking.remove(player);
	}
	
	public void swapTracks() {
		oldPlayersTracking = playersTracking;
		playersTracking = new ArrayList<>();
	}
	
	public List<ServerPlayer> getPlayersTracking() {
		return oldPlayersTracking;
	}
	
	@Override
	// TODO: do this more properly
	public FluidState getFluidState(int pX, int pY, int pZ) {
		return getBlockState(new BlockPos(pX, pY, pZ)).getFluidState();
	}
}
