package tfc.smallerunits.networking.sync;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.EmptyLevelChunk;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.network.NetworkEvent;
import tfc.smallerunits.UnitSpace;
import tfc.smallerunits.client.access.tracking.SUCapableChunk;
import tfc.smallerunits.data.capability.ISUCapability;
import tfc.smallerunits.data.capability.SUCapabilityManager;
import tfc.smallerunits.data.storage.UnitPallet;
import tfc.smallerunits.networking.Packet;
import tfc.smallerunits.simulation.level.ITickerLevel;

import java.util.ArrayList;

public class SyncPacketS2C extends Packet {
	private static final ArrayList<SyncPacketS2C> deferred = new ArrayList<>();
	UnitPallet pallet;
	BlockPos realPos;
	int upb;
	CompoundTag[] beData;
	boolean natural;
	
	public SyncPacketS2C(UnitSpace space) {
		if (space.getMyLevel() == null) space.tick();
		pallet = space.getPallet();
		BlockEntity[] tiles = space.getTiles();
		realPos = space.pos;
		upb = space.unitsPerBlock;
		ArrayList<CompoundTag> beData = new ArrayList<>();
		for (BlockEntity tile : tiles) {
			if (tile != null) {
				net.minecraft.network.protocol.Packet<?> pkt = tile.getUpdatePacket();
				if (pkt instanceof ClientboundBlockEntityDataPacket) {
					CompoundTag tag = ((ClientboundBlockEntityDataPacket) pkt).getTag();
					if (tag == null) continue;
					CompoundTag tg = new CompoundTag();
					tg.put("data", tag);
					tg.putInt("x", tile.getBlockPos().getX());
					tg.putInt("y", tile.getBlockPos().getY());
					tg.putInt("z", tile.getBlockPos().getZ());
					tg.putString("id", tile.getType().getRegistryName().toString());
					beData.add(tg);
				}
			}
		}
		natural = space.isNatural;
		this.beData = beData.toArray(new CompoundTag[0]);
	}
	
	public SyncPacketS2C(FriendlyByteBuf buf) {
		super(buf);
		pallet = UnitPallet.fromNBT(buf.readNbt());
		realPos = buf.readBlockPos();
		upb = buf.readInt();
		natural = buf.readBoolean();
		int count;
		beData = new CompoundTag[count = buf.readInt()];
		for (int i = 0; i < count; i++) beData[i] = buf.readNbt();
	}
	
	public static void tick(TickEvent.ClientTickEvent event) {
		ArrayList<SyncPacketS2C> toRemove = new ArrayList<>();
		if (Minecraft.getInstance().screen != null) return;
		if (Minecraft.getInstance().player == null) return;
//		if (Minecraft.getInstance().levelRenderer == null) return;
		if (Minecraft.getInstance().levelRenderer.level == null) return;
		for (SyncPacketS2C syncPacket : deferred) {
			ChunkAccess access = Minecraft.getInstance().level.getChunk(syncPacket.realPos);
			if (access instanceof EmptyLevelChunk) continue;
			if (!(access instanceof LevelChunk chunk)) continue;
			ISUCapability cap = SUCapabilityManager.getCapability(chunk);
			UnitSpace space = new UnitSpace(syncPacket.realPos, chunk.getLevel());
			if (space.getMyLevel() == null) return;
			
			// TODO: adjust player position and whatnot
			
			space.unitsPerBlock = syncPacket.upb;
			space.setUpb(space.unitsPerBlock);
			space.isNatural = syncPacket.natural;
			space.loadPallet(syncPacket.pallet);
			if (cap.getUnit(syncPacket.realPos) != null)
				cap.removeUnit(syncPacket.realPos);
			cap.setUnit(syncPacket.realPos, space);
			((SUCapableChunk) chunk).SU$markDirty(syncPacket.realPos);
			toRemove.add(syncPacket);
			
			Level lvl = space.getMyLevel();
			
			ClientLevel clvl = Minecraft.getInstance().level;
			Minecraft.getInstance().level = (ClientLevel) lvl;
			for (CompoundTag data : syncPacket.beData) {
				String id = data.getString("id");
				CompoundTag tag = data.getCompound("data");
				if (!tag.contains("id"))
					tag.putString("id", id);
				BlockPos up = new BlockPos(data.getInt("x"), data.getInt("y"), data.getInt("z"));
				BlockEntity be = BlockEntity.loadStatic(up, lvl.getBlockState(up), tag);
				if (be == null) return;
				lvl.setBlockEntity(be);
				be.load(tag); // yes
				
				// TODO: this is like 90% redundant
				BlockPos rp = ((ITickerLevel) lvl).getRegion().pos.toBlockPos();
				BlockPos pos = be.getBlockPos();
				int xo = (pos.getX() / syncPacket.upb);
				int yo = (pos.getY() / syncPacket.upb);
				int zo = (pos.getZ() / syncPacket.upb);
				BlockPos parentPos = rp.offset(xo, yo, zo);
				ChunkAccess ac = ((ITickerLevel) lvl).getParent().getChunkAt(parentPos);
				ac.setBlockState(parentPos, tfc.smallerunits.Registry.UNIT_SPACE.get().defaultBlockState(), false);
				((SUCapableChunk) ac).addTile(be);
			}
			Minecraft.getInstance().level = clvl;
		}
		deferred.removeAll(toRemove);
//		toRemove.forEach(deferred::remove);
	}
	
	@Override
	public void write(FriendlyByteBuf buf) {
		super.write(buf);
		buf.writeNbt(pallet.toNBT());
		buf.writeBlockPos(realPos);
		buf.writeInt(upb);
		buf.writeBoolean(natural);
		buf.writeInt(beData.length);
		for (CompoundTag beDatum : beData) buf.writeNbt(beDatum);
	}
	
	@Override
	public void handle(NetworkEvent.Context ctx) {
		if (checkClient(ctx)) {
			deferred.add(this);
			ctx.setPacketHandled(true);
		}
	}
}
