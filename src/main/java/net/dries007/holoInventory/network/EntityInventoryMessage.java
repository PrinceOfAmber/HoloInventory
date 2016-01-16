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

public class EntityInventoryMessage implements IMessage
{
    NBTTagCompound data;

    public EntityInventoryMessage()
    {

    }

    public EntityInventoryMessage(NBTTagCompound inventoryData)
    {
        data = inventoryData;
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

    public static class Handler implements IMessageHandler<EntityInventoryMessage, IMessage>
    {
        @Override
        public IMessage onMessage(EntityInventoryMessage message, MessageContext ctx)
        {
            if (ctx.side.isClient())
            {
                NBTTagList list = message.data.getTagList("list", 10);
                ItemStack[] itemStacks = new ItemStack[list.tagCount()];
                for (int i = 0; i < list.tagCount(); i++)
                {
                    itemStacks[i] = ItemStack.loadItemStackFromNBT(list.getCompoundTagAt(i));
                }
                if (message.data.hasKey("class")) Renderer.entityMap.put(message.data.getInteger("id"), new NamedData<ItemStack[]>(message.data.getString("name"), message.data.getString("class"), itemStacks));
                else Renderer.entityMap.put(message.data.getInteger("id"), new NamedData<ItemStack[]>(message.data.getString("name"), itemStacks));
            }

            return null;
        }
    }
}
