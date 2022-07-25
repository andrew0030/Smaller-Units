package tfc.smallerunits;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.IItemRenderProperties;
import org.jetbrains.annotations.Nullable;
import tfc.smallerunits.data.capability.ISUCapability;
import tfc.smallerunits.data.capability.SUCapabilityManager;

import java.util.List;
import java.util.function.Consumer;

public class UnitSpaceItem extends Item {
	public UnitSpaceItem() {
		super(
				new Properties().tab(Registry.tab)
						.rarity(Rarity.create("su", ChatFormatting.GREEN))
		);
	}
	
	@Override
	public void initializeClient(Consumer<IItemRenderProperties> consumer) {
		consumer.accept(new SUItemRenderProperties());
//		super.initializeClient(consumer);
	}
	
	@Override
	public InteractionResult useOn(UseOnContext pContext) {
		BlockPlaceContext ctx = new BlockPlaceContext(pContext);
		BlockPos pos = ctx.getClickedPos();
		ISUCapability capability = SUCapabilityManager.getCapability(pContext.getLevel().getChunkAt(pos));
		if (capability.getUnit(pos) == null) {
			UnitSpace space = capability.getOrMakeUnit(pos);
			if (ctx.replacingClickedOnBlock() || pContext.getLevel().getBlockState(pos).isAir()) {
				if (pContext.getItemInHand().hasTag()) {
					CompoundTag tag = pContext.getItemInHand().getTag();
					assert tag != null;
					if (tag.contains("upb", Tag.TAG_INT))
						space.setUpb(tag.getInt("upb"));
					else space.setUpb(4);
				}
				pContext.getLevel().setBlockAndUpdate(pos, Registry.UNIT_SPACE.get().defaultBlockState());
				return InteractionResult.SUCCESS;
			}
		}
		return super.useOn(pContext);
	}
	
	@Override
	public void appendHoverText(ItemStack pStack, @Nullable Level pLevel, List<Component> pTooltipComponents, TooltipFlag pIsAdvanced) {
		int upb = 4;
		if (pStack.hasTag()) {
			CompoundTag tag = pStack.getTag();
			if (tag.contains("upb", Tag.TAG_INT)) {
				upb = tag.getInt("upb");
			}
		}
		pTooltipComponents.add(new TranslatableComponent("smallerunits.tooltip.scale", upb));
		super.appendHoverText(pStack, pLevel, pTooltipComponents, pIsAdvanced);
	}
	
	@Override
	public void fillItemCategory(CreativeModeTab pCategory, NonNullList<ItemStack> pItems) {
		if (this.allowdedIn(pCategory)) {
			for (int i = 2; i <= 16; i++) {
				ItemStack stack = new ItemStack(this);
				stack.getOrCreateTag().putInt("upb", i);
				pItems.add(stack);
			}
		}
//		super.fillItemCategory(pCategory, pItems);
	}
}
