package net.dries007.holoInventory.network;

import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import net.dries007.holoInventory.HoloInventory;

public class RenameMessage implements IMessage
{
    String name, override;

    public RenameMessage()
    {
    }

    public RenameMessage(String name, String override)
    {
        this.name = name;
        this.override = override;
    }

    @Override
    public void fromBytes(ByteBuf buf)
    {
        name = ByteBufUtils.readUTF8String(buf);
        override = ByteBufUtils.readUTF8String(buf);
    }

    @Override
    public void toBytes(ByteBuf buf)
    {
        ByteBufUtils.writeUTF8String(buf, name);
        ByteBufUtils.writeUTF8String(buf, override);
    }

    public static class Handler implements IMessageHandler<RenameMessage, IMessage>
    {
        @Override
        public IMessage onMessage(RenameMessage message, MessageContext ctx)
        {
            if (ctx.side.isClient())
            {
                HoloInventory.getConfig().nameOverrides.put(message.name, message.override);
                HoloInventory.getConfig().overrideNameThings();
            }
            return null;
        }
    }
}
