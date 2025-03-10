package tfc.smallerunits.utils.asm;

import net.minecraftforge.coremod.api.ASMAPI;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.*;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class MixinConnector implements IMixinConfigPlugin {
	private static final ArrayList<String> classLookup = new ArrayList<>();
	private static final HashMap<String, ArrayList<String>> incompatibilityMap = new HashMap<>();
	
	static {
		classLookup.add("tfc.smallerunits.mixin.compat.ChiselAndBitMeshMixin");
		classLookup.add("tfc.smallerunits.mixin.compat.sodium.SodiumLevelRendererMixin");
		classLookup.add("tfc.smallerunits.mixin.compat.sodium.RenderSectionManagerMixin");
		
		{
			ArrayList<String> incompat = new ArrayList<>();
			incompat.add("me.jellysquid.mods.sodium.mixin.features.chunk_rendering.MixinWorldRenderer");
			incompatibilityMap.put("tfc.smallerunits.mixin.LevelRendererMixin", incompat);
		}
	}
	
	@Override
	public void onLoad(String mixinPackage) {
	}
	
	@Override
	public String getRefMapperConfig() {
		return null;
	}
	
	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
		if (classLookup.contains(mixinClassName)) {
			ClassLoader loader = MixinConnector.class.getClassLoader();
			// tests if the classloader contains a .class file for the target
			InputStream stream = loader.getResourceAsStream(targetClassName.replace('.', '/') + ".class");
			if (stream != null) {
				try {
					stream.close();
					return true;
				} catch (Throwable ignored) {
					return true;
				}
			}
			return false;
		}
		if (incompatibilityMap.containsKey(mixinClassName)) {
			ClassLoader loader = MixinConnector.class.getClassLoader();
			// tests if the classloader contains a .class file for the target
			for (String name : incompatibilityMap.get(mixinClassName)) {
				InputStream stream = loader.getResourceAsStream(name.replace('.', '/') + ".class");
				if (stream == null) {
					return true;
				} else {
					try {
						stream.close();
						return false;
					} catch (Throwable ignored) {
						return false;
					}
				}
			}
			return false;
		}
		return true;
	}
	
	@Override
	public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
	}
	
	@Override
	public List<String> getMixins() {
		return null;
	}
	
	@Override
	public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
		if (
				mixinClassName.equals("tfc.smallerunits.mixin.LevelRendererMixin") ||
						mixinClassName.equals("tfc.smallerunits.mixin.core.PacketUtilsMixin") ||
						mixinClassName.equals("tfc.smallerunits.mixin.data.regions.ChunkMapMixin")
		) {
			try {
				FileOutputStream outputStream = new FileOutputStream(targetClass.name.substring(targetClass.name.lastIndexOf("/") + 1) + "-pre.class");
				ClassWriter writer = new ClassWriter(0);
				targetClass.accept(writer);
				outputStream.write(writer.toByteArray());
				outputStream.flush();
				outputStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if (mixinClassName.equals("tfc.smallerunits.mixin.LevelRendererMixin")) {
			String target = ASMAPI.mapMethod("m_172993_"); // renderChunkLayer
			String desc = "(Lnet/minecraft/client/renderer/RenderType;Lcom/mojang/blaze3d/vertex/PoseStack;DDDLcom/mojang/math/Matrix4f;)V"; // TODO: I'd like to not assume Mojmap
			
			String refOwner = "net/minecraft/client/renderer/chunk/ChunkRenderDispatcher$RenderChunk";
			String ref = ASMAPI.mapMethod("m_112835_"); // getCompiledChunk
			String refDesc = "()Lnet/minecraft/client/renderer/chunk/ChunkRenderDispatcher$CompiledChunk;";
			for (MethodNode method : targetClass.methods) {
				if (method.name.equals(target) && method.desc.equals(desc)) {
//					AbstractInsnNode targetNode = null;
					// TODO: try to find a way to figure out a more specific target
					ArrayList<AbstractInsnNode> targetNodes = new ArrayList<>();
					for (AbstractInsnNode instruction : method.instructions) {
						if (instruction instanceof MethodInsnNode methodNode) {
							if (methodNode.owner.equals(refOwner) && methodNode.name.equals(ref) && methodNode.desc.equals(refDesc)) {
								targetNodes.add(methodNode);
//								targetNode = methodNode;
//								break;
							}
						}
					}
//					if (targetNode != null) {
					for (AbstractInsnNode targetNode : targetNodes) {
						InsnList list = new InsnList();
						list.add(ASMAPI.buildMethodCall("tfc/smallerunits/utils/IHateTheDistCleaner", "updateRenderChunk", "(Lnet/minecraft/client/renderer/chunk/ChunkRenderDispatcher$RenderChunk;)Lnet/minecraft/client/renderer/chunk/ChunkRenderDispatcher$RenderChunk;", ASMAPI.MethodType.STATIC));
						method.instructions.insertBefore(targetNode, list);
					}
//					}
				}
			}
		}
	}
	
	@Override
	public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
		if (
				mixinClassName.equals("tfc.smallerunits.mixin.LevelRendererMixin") ||
						mixinClassName.equals("tfc.smallerunits.mixin.core.PacketUtilsMixin") ||
						mixinClassName.equals("tfc.smallerunits.mixin.data.regions.ChunkMapMixin")
		) {
			try {
				FileOutputStream outputStream = new FileOutputStream(targetClass.name.substring(targetClass.name.lastIndexOf("/") + 1) + "-post.class");
				ClassWriter writer = new ClassWriter(0);
				targetClass.accept(writer);
				outputStream.write(writer.toByteArray());
				outputStream.flush();
				outputStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
