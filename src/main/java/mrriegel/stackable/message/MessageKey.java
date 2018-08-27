package mrriegel.stackable.message;

import io.netty.buffer.ByteBuf;
import mrriegel.stackable.Stackable;
import mrriegel.stackable.block.BlockPile;
import mrriegel.stackable.tile.TileIngotPile;
import mrriegel.stackable.tile.TileStackable;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class MessageKey implements IMessage, IMessageHandler<MessageKey, IMessage> {
	byte key;
	BlockPos pos;

	public MessageKey() {
	}

	public MessageKey(byte key, BlockPos pos) {
		super();
		this.key = key;
		this.pos = pos;
	}

	@Override
	public IMessage onMessage(MessageKey message, MessageContext ctx) {
		FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(() -> {
			EntityPlayer player = ctx.getServerHandler().player;
			if (message.key == 0) {
				ItemStack main = player.getHeldItemMainhand();
				Block block = TileIngotPile.validItem1(main) ? Stackable.ingots : !main.isEmpty() ? Stackable.any : null;
				if (block != null) {
					BlockPos newPos = message.pos.up();
					if (player.world.isAirBlock(newPos) && player.world.setBlockState(newPos, block.getDefaultState(), 2)) {
						TileStackable t = (TileStackable) player.world.getTileEntity(newPos);
						t.isMaster = true;
						player.world.notifyNeighborsOfStateChange(newPos, block, true);
						block.onBlockActivated(player.world, newPos, block.getDefaultState(), player, EnumHand.MAIN_HAND, EnumFacing.UP, 0, 0, 0);
					}
				}
			} else if (message.key == 1) {
				TileEntity t = player.world.getTileEntity(message.pos);
				if (t instanceof TileStackable) {
					TileStackable tile = ((TileStackable) t).getMaster();
					tile.inv.cycle(!player.isSneaking());
				}
			} else if (message.key == 2) {
				boolean pressed = message.pos.getX() == 1;
				if (pressed)
					BlockPile.ctrlMap.add(player.getUniqueID());
				else
					BlockPile.ctrlMap.remove(player.getUniqueID());
			}
		});
		return null;
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		key = buf.readByte();
		pos = BlockPos.fromLong(buf.readLong());
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeByte(key);
		buf.writeLong(pos.toLong());
	}

}
