package tfc.smallerunits.client.render;

import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ChunkBufferBuilderPack;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.model.ModelDataManager;
import net.minecraftforge.client.model.data.EmptyModelData;
import net.minecraftforge.client.model.data.IModelData;
import tfc.smallerunits.UnitSpace;
import tfc.smallerunits.client.access.tracking.SUCapableChunk;
import tfc.smallerunits.client.render.storage.BufferStorage;
import tfc.smallerunits.client.render.util.RenderWorld;
import tfc.smallerunits.client.render.util.TranslatingVertexBuilder;
import tfc.smallerunits.data.capability.ISUCapability;
import tfc.smallerunits.utils.PositionalInfo;
import tfc.smallerunits.utils.math.Math1D;
import tfc.smallerunits.utils.storage.DefaultedMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

public class SUVBOEmitter {
	private static final ArrayList<BufferStorage> vbosFree = new ArrayList<>();
	private static final Object lock = new Object();
	private static final DefaultedMap<RenderType, BufferBuilder> buffers = new DefaultedMap<RenderType, BufferBuilder>().setDefaultVal(() -> new BufferBuilder(16));
	private static final ChunkBufferBuilderPack bufferBuilderPack = new ChunkBufferBuilderPack();
	
	private final HashMap<BlockPos, BufferStorage> used = new HashMap<>();
	private final HashMap<BlockPos, BufferStorage> free = new HashMap<>();

//	private static final ReusableThread[] threads = new ReusableThread[16];
	
	static {
//		for (int i = 0; i < threads.length; i++) {
//			threads[i] = new ReusableThread(() -> {
//			});
//		}
	}
	
	public BufferStorage genBuffers(LevelChunk chunk, SUCapableChunk suCapableChunk, ISUCapability capability, BlockPos pos) {
		UnitSpace space = capability.getUnit(pos);
		
		BufferStorage storage = getAndMark(pos);
		storage.deactivate();
		
		UnitSpace unit = capability.getUnit(pos);
		if (unit == null) {
			free.put(pos, getBuffers(pos));
			return null;
		}
		
		Player player = Minecraft.getInstance().player;
		PositionalInfo info = new PositionalInfo(player);
		info.scalePlayerReach(player, space.unitsPerBlock);
		info.adjust(player, space);

//		AABB scaledBB;
//		player.setBoundingBox(scaledBB = HitboxScaling.getOffsetAndScaledBox(info.box, info.pos, space.unitsPerBlock));
//		player.eyeHeight = (float) (info.eyeHeight * (1d / space.unitsPerBlock));
//		player.setPosRaw(scaledBB.getCenter().x, scaledBB.minY, scaledBB.getCenter().z);
//		if (player.level instanceof ClientLevel) {
//			((LocalPlayer) player).clientLevel = (ClientLevel) space.getMyLevel();
//			Minecraft.getInstance().level = ((LocalPlayer) player).clientLevel;
//		}
		
		Minecraft.getInstance().getProfiler().push("get_blocks");
		BlockState[] states = unit.getBlocks();
		Minecraft.getInstance().getProfiler().pop();
		BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
		PoseStack stack = new PoseStack();
		stack.translate(
				pos.getX() - chunk.getPos().getMinBlockX(),
//				pos.getY() < 0 ? ((16 - pos.getY() % 16) - 16) : (pos.getY() % 16),
//				Math1D.chunkMod(pos.getY(), 16),
				pos.getY() & 15,
				pos.getZ() - chunk.getPos().getMinBlockZ()
		);
		float scl = 1f / space.unitsPerBlock;
		stack.scale(scl, scl, scl);
		DefaultedMap<RenderType, BufferBuilder> buffers = new DefaultedMap<>();
		buffers.setDefaultVal((type) -> {
//			BufferBuilder builder = new ThreadedVertexBuilder(0, bufferBuilderPack.builder(type));
//			BufferBuilder builder = new ThreadedVertexBuilder(0, SUVBOEmitter.buffers.get(type));
//			BufferBuilder builder = SUVBOEmitter.buffers.get(type);
			BufferBuilder builder = bufferBuilderPack.builder(type);
			if (!builder.building()) builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);
			return builder;
		});
		int upb = space.unitsPerBlock;
		Minecraft.getInstance().getProfiler().push("draw_loop");
		
		for (int i = 0; i < RenderType.chunkBufferLayers().size(); i++) {
			RenderType chunkBufferLayer = RenderType.chunkBufferLayers().get(i);
			ForgeHooksClient.setRenderType(chunkBufferLayer);
//			ReusableThread td = threads[i % 16];
//			while (td.isInUse()) {
//			}
//			td.setAction(() -> {
			handleLayer(chunkBufferLayer, buffers, space.getRenderWorld(), stack, upb, space, dispatcher, states);
//			});
//			td.start();
		}
//		for (ReusableThread thread : threads) {
//			while (thread.isInUse()) {
//			}
//		}
		Minecraft.getInstance().getProfiler().popPush("finish");
//		buffers.forEach((type, buf) -> {
//			if (buf instanceof ThreadedVertexBuilder) {
//				((ThreadedVertexBuilder) buf).finish(threads);
//			}
//		});
		ForgeHooksClient.setRenderType(null);
		Minecraft.getInstance().getProfiler().popPush("upload");
		buffers.forEach(storage::upload);
		Minecraft.getInstance().getProfiler().pop();
		
		info.reset(player);
		
		return storage;
	}
	
	private void handleLayer(RenderType chunkBufferLayer, DefaultedMap<RenderType, BufferBuilder> buffers, RenderWorld wld, PoseStack stack, int upb, UnitSpace space, BlockRenderDispatcher dispatcher, BlockState[] states) {
		VertexConsumer consumer = null;
		SectionPos chunkPos = SectionPos.of(space.pos);
		BlockPos chunkOffset = new BlockPos(chunkPos.minBlockX(), chunkPos.minBlockY(), chunkPos.minBlockZ());
		for (int x = 0; x < upb; x++) {
			PoseStack stk = new PoseStack();
			stk.last().pose().load(stack.last().pose());
			stk.last().normal().load(stack.last().normal());
			for (int y = 0; y < upb; y++) {
				for (int z = 0; z < upb; z++) {
					int indx = (((x * upb) + y) * upb) + z;
					BlockState block = states[indx];
					if (block.equals(Blocks.AIR.defaultBlockState())) continue;
					if (!block.getFluidState().isEmpty()) {
						if (ItemBlockRenderTypes.canRenderInLayer(block.getFluidState(), chunkBufferLayer)) {
							if (consumer == null) consumer = buffers.get(chunkBufferLayer);
							BlockPos rPos = new BlockPos(x, y, z);
							TranslatingVertexBuilder builder = new TranslatingVertexBuilder(1f / upb, consumer);
							BlockPos offsetPos = space.getOffsetPos(new BlockPos(x, y, z));
							builder.offset = new Vec3(
									((int) Math1D.getChunkOffset(offsetPos.getX(), 16)) * 16 - chunkOffset.getX() * space.unitsPerBlock,
									((int) Math1D.getChunkOffset(offsetPos.getY(), 16)) * 16 - chunkOffset.getY() * space.unitsPerBlock,
									((int) Math1D.getChunkOffset(offsetPos.getZ(), 16)) * 16 - chunkOffset.getZ() * space.unitsPerBlock
							);
							dispatcher.renderLiquid(
									space.getOffsetPos(rPos),
									wld, builder, block,
									block.getFluidState()
							);
						}
					}
					if (block.getRenderShape() != RenderShape.INVISIBLE) {
						if (ItemBlockRenderTypes.canRenderInLayer(block, chunkBufferLayer)) {
							if (consumer == null) consumer = buffers.get(chunkBufferLayer);
//							if (consumer == null) consumer = Minecraft.getInstance().renderBuffers().bufferSource().getBuffer(chunkBufferLayer);
							stk.pushPose();
							stk.translate(x, y, z);
							BlockPos rPos = new BlockPos(x, y, z);
							IModelData data = ModelDataManager.getModelData(space.getMyLevel(), space.getOffsetPos(rPos));
							if (data == null) data = EmptyModelData.INSTANCE;
							dispatcher.renderBatched(
									block, space.getOffsetPos(rPos),
									wld, stk, consumer,
									true, new Random(space.getOffsetPos(rPos).asLong()),
									data
							);
//							if (SmallerUnits.isIsOFPresent()) {
//								dispatcher.getModelRenderer().tesselateBlock(
//										wld, dispatcher.getBlockModel(block),
//										block, space.getOffsetPos(rPos),
//										stk, consumer, true,
//										new Random(space.getOffsetPos(rPos).asLong()),
//										space.getOffsetPos(rPos).asLong(), OverlayTexture.NO_OVERLAY,
//										data
//								);
//							} else {
//								if (Minecraft.getInstance().options.ambientOcclusion.getId() == AmbientOcclusionStatus.MAX.getId()) {
//									dispatcher.getModelRenderer().tesselateWithAO(
//											wld, dispatcher.getBlockModel(block),
//											block, space.getOffsetPos(rPos),
//											stk, consumer, true,
//											new Random(space.getOffsetPos(rPos).asLong()),
//											space.getOffsetPos(rPos).asLong(), OverlayTexture.NO_OVERLAY,
//											data
//									);
//								} else {
//									dispatcher.getModelRenderer().tesselateWithoutAO(
//											wld, dispatcher.getBlockModel(block),
//											block, space.getOffsetPos(rPos),
//											stk, consumer, true,
//											new Random(space.getOffsetPos(rPos).asLong()),
//											space.getOffsetPos(rPos).asLong(), OverlayTexture.NO_OVERLAY,
//											data
//									);
//								}
//							}
							stk.popPose();
						}
					}
				}
			}
		}
	}
	
	@Deprecated(forRemoval = true)
	private BufferStorage getBuffers(BlockPos pos) {
		if (used.containsKey(pos)) return used.remove(pos);
		else if (free.containsKey(pos)) return free.remove(pos);
		else if (!vbosFree.isEmpty()) return vbosFree.remove(0);
		else return new BufferStorage();
	}
	
	public BufferStorage getAndMark(BlockPos pos) {
		BufferStorage strg = getBuffers(pos);
		used.put(pos, strg);
		return strg;
	}
	
	public void markFree(BlockPos pos) {
		vbosFree.add(getBuffers(pos));
	}
	
	public void free() {
		synchronized (lock) {
			vbosFree.addAll(used.values());
			vbosFree.addAll(free.values());
			used.clear();
			free.clear();
		}
	}
}
