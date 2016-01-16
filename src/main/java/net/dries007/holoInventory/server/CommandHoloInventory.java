/*
 * Copyright (c) 2014. Dries K. Aka Dries007
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package net.dries007.holoInventory.server;

import net.dries007.holoInventory.HoloInventory;
import net.dries007.holoInventory.network.ResetMessage;
import net.dries007.holoInventory.util.InventoryData;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;

import java.util.ArrayList;
import java.util.List;

public class CommandHoloInventory extends CommandBase
{
    @Override
    public String getCommandName()
    {
        return "holoinventory";
    }

    public int getRequiredPermissionLevel()
    {
        return 0;
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender par1ICommandSender)
    {
        return true;
    }

    @Override
    public String getCommandUsage(ICommandSender icommandsender)
    {
        return "Use to <reset> local cache, <reload> the config, <(un)ban> tiles and override names.";
    }

    public List addTabCompletionOptions(ICommandSender sender, String[] args)
    {
        switch (args.length)
        {
            default:
                return null;
            case 1:
                if (isOp(sender)) return getListOfStringsMatchingLastWord(args, "reset", "reload", "overrideName", "ban", "unban");
                else return getListOfStringsMatchingLastWord(args, "reset", "overrideName");
            case 2:
                if (isOp(sender) && args[0].equalsIgnoreCase("unban")) return getListOfStringsMatchingLastWord(args, getAllList());
                else return null;
        }
    }

    private boolean isOp(ICommandSender sender)
    {
        return MinecraftServer.getServer().isSinglePlayer() || !(sender instanceof EntityPlayerMP) || 
        		MinecraftServer.getServer().getConfigurationManager().canSendCommands(((EntityPlayerMP) sender).getGameProfile());
    }

    private List<String> getAllList()
    {
        ArrayList<String> temp = new ArrayList<String>();
        temp.addAll(HoloInventory.getConfig().bannedEntities);
        temp.addAll(HoloInventory.getConfig().bannedTiles);
        return temp;
    }

    private void sendHelp(ICommandSender sender)
    {
        sender.addChatMessage(new ChatComponentText("-= HoloInventory By Dries007 =-"));
        sender.addChatMessage(new ChatComponentText("Use one of the following arguments with this command:"));
        sender.addChatMessage(new ChatComponentText("  * <reset>             -> Reset the clients cache"));
        sender.addChatMessage(new ChatComponentText("  * <reload>            -> Reload (server) config."));
        sender.addChatMessage(new ChatComponentText("  * <overrideName> [new name] -> Override a name"));
        sender.addChatMessage(new ChatComponentText("  * <ban>                -> Ban the next inventory you RIGHTclick"));
        sender.addChatMessage(new ChatComponentText("  * <unban> [a name] -> Unban a an inventory"));
        sender.addChatMessage(new ChatComponentText("Pro tip: Use tab completion!"));
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws WrongUsageException
    {
        if (args.length == 0)
        {
            sendHelp(sender);
        }
        else if (args[0].equalsIgnoreCase("reset"))
        {
            if (!(sender instanceof EntityPlayer)) throw new WrongUsageException("You can't use this as the server...");
            HoloInventory.getSnw().sendTo(new ResetMessage(), (EntityPlayerMP) sender);
            for (InventoryData data : ServerHandler.serverEventHandler.blockMap.values())
            {
                //noinspection SuspiciousMethodCalls
                data.playerSet.remove(sender);
            }
        }
        else if (args[0].equalsIgnoreCase("reload"))
        {
            if (sender instanceof EntityPlayer)
                HoloInventory.getSnw().sendTo(new ResetMessage(), (EntityPlayerMP) sender);
            if (isOp(sender))
                HoloInventory.getConfig().reload();

            sender.addChatMessage(new ChatComponentText("Reload command send."));
        }
        else if (args[0].equalsIgnoreCase("overrideName"))
        {
            if (!(sender instanceof EntityPlayer)) throw new WrongUsageException("You can't use this as the server...");
            if (args.length == 1)
            {
                throw new WrongUsageException("Give a name, can contain spaces.");
            }
            else
            {
                StringBuilder stringBuilder = new StringBuilder();
                for (int i = 1; i < args.length; i++) stringBuilder.append(args[i]).append(' ');
                String name = stringBuilder.toString().trim();

                sender.addChatMessage(new ChatComponentText("RIGHTclick a block or entity to override the hologram name on that type to " + name));
                ServerHandler.serverEventHandler.overrideUsers.put(sender.getName(), name);
            }
        }
        else if (args[0].equalsIgnoreCase("ban"))
        {
            if (isOp(sender))
            {
                ServerHandler.serverEventHandler.banUsers.add(sender.getName());
                sender.addChatMessage(new ChatComponentText("RIGHTclick a block or entity to ban the hologram on that type."));
            }
            else
            {
                sender.addChatMessage(new ChatComponentText("You are not opped. Ban stuff in SSP to block it client side."));
            }
        }
        else if (args[0].equalsIgnoreCase("unban"))
        {
            if (isOp(sender))
            {
                if (args.length == 1)
                {
                    if (getAllList().size() == 0) sender.addChatMessage(new ChatComponentText("You didn't ban any inventories yet..."));
                    else sender.addChatMessage(new ChatComponentText("A list of all banned inventories:\n" + joinNiceString(getAllList().toArray())));
                }
                else if (getAllList().contains(args[1]))
                {
                    HoloInventory.getConfig().bannedEntities.remove(args[1]);
                    HoloInventory.getConfig().bannedTiles.remove(args[1]);
                    HoloInventory.getConfig().overrideBannedThings();
                }
                else
                {
                    sender.addChatMessage(new ChatComponentText("That thing is not on any banlist I know of...").setChatStyle(new ChatStyle().setColor(EnumChatFormatting.RED)));
                }
            }
            else
            {
                sender.addChatMessage(new ChatComponentText("You are not opped. Unban stuff in SSP to unblock it client side."));
                sender.addChatMessage(new ChatComponentText("If you think this ban is stupid, ask a server admin to remove it."));
            }
        }
        else
        {
            sendHelp(sender);
        }
    }  

    @Override
    public int compareTo(ICommand par1Obj)
    {
        return super.compareTo((ICommand) par1Obj);
    }
}