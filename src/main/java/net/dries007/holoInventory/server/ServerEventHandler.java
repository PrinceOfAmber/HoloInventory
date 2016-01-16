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

import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.dries007.holoInventory.HoloInventory;
import net.dries007.holoInventory.network.RemoveInventoryMessage;
import net.dries007.holoInventory.network.RenameMessage;
import net.dries007.holoInventory.util.*;
import net.minecraft.block.Block;
import net.minecraft.block.BlockJukebox;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryLargeChest;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntityEnderChest;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.entity.player.EntityInteractEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ServerEventHandler
{
    public List<String> banUsers = new ArrayList<String>();
    public HashMap<String, String> overrideUsers = new HashMap<String, String>();
    public HashMap<Integer, InventoryData> blockMap = new HashMap<Integer, InventoryData>();

    @SubscribeEvent()
    public void event(PlayerInteractEvent event)
    {
        if (event.action != PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK) return;
        if (banUsers.contains(event.entityPlayer.getDisplayName()))
        {
            banUsers.remove(event.entityPlayer.getDisplayName());
            event.setCanceled(true);

            TileEntity te = event.entity.worldObj.getTileEntity(event.x, event.y, event.z);
            if (Helper.weWant(te))
            {
                HoloInventory.getConfig().bannedTiles.add(te.getClass().getCanonicalName());
                event.entityPlayer.addChatComponentMessage(new ChatComponentText(te.getClass().getCanonicalName() + " will no longer display a hologram."));
            }
            else
            {
                event.entityPlayer.addChatComponentMessage(new ChatComponentText("That is no inventory. Try again."));
            }
            HoloInventory.getConfig().overrideBannedThings();
        }

        if (overrideUsers.containsKey(event.entityPlayer.getDisplayName()))
        {
            if (FMLCommonHandler.instance().getEffectiveSide().isClient()) return;

            String nameOverride = overrideUsers.get(event.entityPlayer.getDisplayName());
            overrideUsers.remove(event.entityPlayer.getDisplayName());
            event.setCanceled(true);

            TileEntity te = event.entity.worldObj.getTileEntity(event.x, event.y, event.z);
            if (Helper.weWant(te))
            {
                String name = null;
                if (te instanceof BlockJukebox.TileEntityJukebox) name = Data.JUKEBOX_NAME;
                else if (te instanceof IInventory) name = ((IInventory) te).getInventoryName();
                else if (te instanceof TileEntityEnderChest) name = event.entityPlayer.getInventoryEnderChest().getInventoryName();

                HoloInventory.getSnw().sendTo(new RenameMessage(name == null ? "" : name, nameOverride), (EntityPlayerMP) event.entityPlayer);
                event.entityPlayer.addChatComponentMessage(new ChatComponentText(te.getClass().getCanonicalName() + " will now be named " + nameOverride));
            }
            else
            {
                event.entityPlayer.addChatComponentMessage(new ChatComponentText("That is no inventory. Try again."));
            }
        }
    }

    @SubscribeEvent()
    public void event(EntityInteractEvent event)
    {
        if (banUsers.contains(event.entityPlayer.getDisplayName()))
        {
            banUsers.remove(event.entityPlayer.getDisplayName());
            event.setCanceled(true);

            if (Helper.weWant(event.target))
            {
                HoloInventory.getConfig().bannedEntities.add(event.target.getClass().getCanonicalName());
                event.entityPlayer.addChatComponentMessage(new ChatComponentText(event.target.getClass().getCanonicalName() + " will no longer display a hologram."));
            }
            else
            {
                event.entityPlayer.addChatComponentMessage(new ChatComponentText("That is no inventory. Try again."));
            }
            HoloInventory.getConfig().overrideBannedThings();
        }
    }

    @SubscribeEvent()
    public void event(TickEvent.PlayerTickEvent event)
    {
        try
        {
            if (event.phase != TickEvent.Phase.END || event.side != Side.SERVER) return;
            EntityPlayerMP player = (EntityPlayerMP) event.player;
            WorldServer world = player.getServerForPlayer();
            if (world == null) return;

            MovingObjectPosition mo = Helper.getPlayerLookingSpot(player);

            if (mo != null)
            {
                switch (mo.typeOfHit)
                {
                    case BLOCK:
                        Coord coord = new Coord(world.provider.dimensionId, mo);
                        int x = (int) coord.x, y = (int) coord.y, z = (int) coord.z;
                        TileEntity te = world.getTileEntity(x, y, z);
                        if (Helper.weWant(te))
                        {
                            checkForChangedType(coord.hashCode(), te);
                            if (HoloInventory.getConfig().bannedTiles.contains(te.getClass().getCanonicalName()))
                            {
                                // BANNED THING
                                cleanup(coord, player);
                            }
                            else if (te instanceof TileEntityChest)
                            {
                                Block block = world.getBlock(x, y, z);
                                TileEntityChest teChest = (TileEntityChest) te;
                                IInventory inventory = teChest;

                                if (world.getBlock(x, y, z + 1) == block) inventory = new InventoryLargeChest("container.chestDouble", teChest, (TileEntityChest) world.getTileEntity(x, y, z + 1));
                                else if (world.getBlock(x - 1, y, z) == block) inventory = new InventoryLargeChest("container.chestDouble", (TileEntityChest) world.getTileEntity(x - 1, y, z), teChest);
                                else if (world.getBlock(x, y, z - 1) == block) inventory = new InventoryLargeChest("container.chestDouble", (TileEntityChest) world.getTileEntity(x, y, z - 1), teChest);
                                else if (world.getBlock(x + 1, y, z) == block) inventory = new InventoryLargeChest("container.chestDouble", teChest, (TileEntityChest) world.getTileEntity(x + 1, y, z));

                                doStuff(coord.hashCode(), player, inventory);
                            }
                            else if (te instanceof IInventory)
                            {
                                doStuff(coord.hashCode(), player, (IInventory) te);
                            }
                            else if (te instanceof TileEntityEnderChest)
                            {
                                doStuff(coord.hashCode(), player, player.getInventoryEnderChest());
                            }
                            else if (te instanceof BlockJukebox.TileEntityJukebox)
                            {
                                BlockJukebox.TileEntityJukebox realTe = ((BlockJukebox.TileEntityJukebox) te);
                                doStuff(coord.hashCode(), player, Data.JUKEBOX_NAME, realTe.func_145856_a());
                            }
                            else
                            {
                                cleanup(coord, player);
                            }
                        }
                        break;
                    case ENTITY:
                        if (Helper.weWant(mo.entityHit))
                        {
                            doStuff(mo.entityHit.getEntityId(), player, (IInventory) mo.entityHit);
                        }
                        break;
                }
            }
        }
        catch (Exception e)
        {
            HoloInventory.getLogger().warn("Some error while sending over inventory, no hologram for you :(");
            HoloInventory.getLogger().warn("Please make an issue on github if this happens.");

            e.printStackTrace();
        }
    }

    private void checkForChangedType(int id, TileEntity te)
    {
        if (blockMap.containsKey(id))
        {
            InventoryData data = blockMap.get(id);
            if (!te.getClass().getCanonicalName().equals(data.getType())) blockMap.remove(id);
        }
    }

    private void cleanup(Coord coord, EntityPlayerMP player)
    {
        if (blockMap.containsKey(coord.hashCode()))
        {
            InventoryData inventoryData = blockMap.get(coord.hashCode());
            inventoryData.playerSet.remove(player);
            if (inventoryData.playerSet.isEmpty()) blockMap.remove(coord.hashCode());
            NBTTagCompound root = new NBTTagCompound();
            root.setByte("type", (byte) 0);
            root.setInteger("id", coord.hashCode());
            HoloInventory.getSnw().sendTo(new RemoveInventoryMessage(root), player);
        }
    }

    private void doStuff(int id, EntityPlayerMP player, String name, ItemStack... itemStacks)
    {
        doStuff(id, player, new FakeInventory(name, itemStacks));
    }

    private void doStuff(int id, EntityPlayerMP player, IInventory inventory)
    {
        InventoryData inventoryData = blockMap.get(id);
        if (inventoryData == null) inventoryData = new InventoryData(inventory, id);
        else inventoryData.update(inventory);
        inventoryData.sendIfOld(player);
        blockMap.put(id, inventoryData);
    }

    public void clear()
    {
        blockMap.clear();
    }
}
