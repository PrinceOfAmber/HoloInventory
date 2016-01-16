package net.dries007.holoInventory.network;

import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/**
 * This is a copy paste class :p
 */
public class Message implements IMessage
{
    @Override
    public void fromBytes(ByteBuf buf)
    {

    }

    @Override
    public void toBytes(ByteBuf buf)
    {

    }

    public static class Handler implements IMessageHandler<Message, IMessage>
    {
        @Override
        public IMessage onMessage(Message message, MessageContext ctx)
        {
            return null;
        }
    }
}
