package tfc.smallerunits.client.render;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import tfc.smallerunits.client.tracking.SUCapableChunk;
import tfc.smallerunits.client.tracking.SUCapableWorld;
import tfc.smallerunits.data.capability.ISUCapability;
import tfc.smallerunits.data.capability.SUCapabilityManager;

public class SURenderManager {
	public static void drawChunk(LevelChunk chunk, Level world, ChunkRenderDispatcher.RenderChunk renderChunk, RenderType type) {
		SUCapableChunk suCapable = ((SUCapableChunk) chunk);
		SUVBOEmitter vboEmitter = ((SUCapableWorld) world).getVBOEmitter();
		ISUCapability capability = SUCapabilityManager.getCapability(chunk);
		SUChunkRender render = suCapable.getChunkRender();
		for (BlockPos pos : suCapable.dirty())
			render.addBuffers(pos, vboEmitter.genBuffers(chunk, suCapable, capability, pos));
		suCapable.reset();
		render.draw(renderChunk, type);
	}
}
