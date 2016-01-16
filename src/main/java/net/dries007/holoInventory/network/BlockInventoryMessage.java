package net.dries007.holoInventory.network;

import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import net.dries007.holoInventory.client.Renderer;
import net.dries007.holoInventory.util.NamedData;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

public class BlockInventoryMessage implements IMessage
{
    NBTTagCompound data;

    public BlockInventoryMessage(NBTTagCompound inventoryData)
    {
        data = inventoryData;
    }

    public BlockInventoryMessage()
    {

    }

    @Override
    public void fromBytes(ByteBuf buf)
    {
        data = ByteBufUtils.readTag(buf);
    }

    @Override
    public void toBytes(ByteBuf buf)
    {
        ByteBufUtils.writeTag(buf, data);
    }

    public static class Handler implements IMessageHandler<BlockInventoryMessage, IMessage>
    {
        @Override
        public IMessage onMessage(BlockInventoryMessage message, MessageContext ctx)
        {
            if (message == null || message.data == null) return null; // hun?
            if (ctx.side.isClient())
            {
                NBTTagList list = message.data.getTagList("list", 10);
                ItemStack[] itemStacks = new ItemStack[list.tagCount()];
                for (int i = 0; i < list.tagCount(); i++)
                {
                    NBTTagCompound tag = list.getCompoundTagAt(i);
                    itemStacks[i] = ItemStack.loadItemStackFromNBT(tag);
                    itemStacks[i].stackSize = tag.getInteger("Count");
                }
                if (message.data.hasKey("class")) Renderer.tileMap.put(message.data.getInteger("id"), new NamedData<ItemStack[]>(message.data.getString("name"), message.data.getString("class"), itemStacks));
                else Renderer.tileMap.put(message.data.getInteger("id"), new NamedData<ItemStack[]>(message.data.getString("name"), itemStacks));
            }

            return null;
        }
    }
}
