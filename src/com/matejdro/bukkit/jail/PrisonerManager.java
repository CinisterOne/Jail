package com.matejdro.bukkit.jail;

import org.bukkit.Material;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class PrisonerManager {
	/**
	 * Parse jail command and prepare user for jailing (if he is online, he will be instantly jailed. Otherwise, he will be jailed first time when he comes online)
	 * @param sender CommandSender that send this command
	 * @param args Arguments for the command. 0 = name, 1 = time, 2 = jailname:cellname, 3 = reason
	 */
	public static void PrepareJail(CommandSender sender, String args[])
	{
		String playername;
		int time = -1;
		String jailname = "";
		if (args.length < 1 || (args.length > 1 && !Util.isInteger(args[1])))
		{
			if (sender != null) Util.Message("Usage: /jail [Name] (Time) (Jail Name:Cell Name) (Reason)", sender);
			return;
		}
		if (Jail.zones.size() < 1)
		{
			if (sender != null) Util.Message("There is no jail available. Build one, before you can jail anyone!", sender);
			return;
		}
		if (Jail.prisoners.containsKey(args[0].toLowerCase()))
		{
			JailPrisoner prisoner = Jail.prisoners.get(args[0].toLowerCase());
			Player player = Jail.instance.getServer().getPlayer(prisoner.getName());
			if (player != null)
			{
				player.teleport(prisoner.getTeleportLocation());
				if (sender != null) Util.Message("Player was teleported back to his jail!", sender);

			}
			else
			{
				if (sender != null) Util.Message("That player is already jailed!", sender);

			}
			return;
		}
		playername = args[0].toLowerCase();
		if (args.length > 1)
			time = Integer.valueOf(args[1]);
		if (args.length > 2)
			jailname = args[2].toLowerCase();
		String reason = "";
		if (args.length > 3)
		{
			for (int i=3;i<args.length;i++)
			{
				reason+= " " + args[i];
			}
			if (reason.length() > 250)
			{
				if (sender != null) Util.Message("Reason is too long!", sender);
				return;
			}
		}
			
		if (jailname.equals(InputOutput.global.getString(Setting.NearestJailCode.getString()))) 
			jailname = "";
		
		String cellname = null;
		if (jailname.contains(":"))
		{
			cellname = jailname.split(":")[1];
			jailname = jailname.split(":")[0];			
		}
			
		String jailer;
		if (sender instanceof Player)
			jailer = ((Player) sender).getName();
		else if (sender == null)
			jailer = "other plugin";
		else
			jailer = "console";
			
		Player player = Jail.instance.getServer().getPlayer(playername);		
		if (player == null)
		{
			JailPrisoner prisoner = new JailPrisoner(playername, time * 6, jailname, cellname, true, "", reason, InputOutput.global.getBoolean(Setting.AutomaticMute.getString(), false),  "" ,jailer);

			if (prisoner.getJail() != null)
			{
				JailCell cell = prisoner.getJail().getRequestedCell(prisoner);
				if (cell != null && (cell.getPlayerName() == null || !cell.getPlayerName().trim().equals("")))
				{
					cell.setPlayerName(prisoner.getName());
					cell.update();
				}
			}
			
			InputOutput.InsertPrisoner(prisoner);
			Jail.prisoners.put(prisoner.getName(), prisoner);
			
			
			Util.Message("Player is offline. He will be automatically jailed when he connnects.", sender);
			
		}
		else
		{
			playername = player.getName().toLowerCase();
			JailPrisoner prisoner = new JailPrisoner(playername, time * 6, jailname, cellname, false, "", reason, InputOutput.global.getBoolean(Setting.AutomaticMute.getString(), false),  "", jailer);
			Jail(prisoner, player);
			Util.Message("Player jailed.", sender);
			
		}
		
		//Log jailing into console
		if (InputOutput.global.getBoolean(Setting.LogJailingIntoConsole.getString(), false))
		{
			String times;
			if (time < 0) times = "forever"; else times = "for " + String.valueOf(time) + "minutes";
			
			Jail.log.info("Player " + playername + " was jailed by " + jailer + " " + times);
		}
	}
	
	/**
	 * Performs jailing of specified JailPrisoner. 
	 * If you just want to jail someone, I recommend using JailAPI.jailPlayer, 
	 * because it supports offline jail and it's easier to do.
	 * @param prisoner JailPrisoner class of the new prisoner. Must be already inserted into database
	 * @param player Player that will be teleported
	 */
	public static void Jail(JailPrisoner prisoner, Player player)
	{
		if (!prisoner.getName().equals(player.getName().toLowerCase())) return;
		prisoner.SetBeingReleased(true);
		JailZone jail = prisoner.getJail();
		if (jail == null)
		{
			jail = JailZoneManager.findNearestJail(player.getLocation());
			prisoner.setJail(jail);
		}
		prisoner.setOfflinePending(false);
		if (prisoner.getReason().isEmpty())
			Util.Message(jail.getSettings().getString(Setting.MessageJail), player);
		else
			Util.Message(jail.getSettings().getString(Setting.MessageJailReason).replace("<Reason>", prisoner.getReason()), player);

		if (jail.getSettings().getBoolean(Setting.DeleteInventoryOnJail)) player.getInventory().clear();
		
		JailCell cell = jail.getRequestedCell(prisoner);
		if (cell == null || (cell.getPlayerName() != null && !cell.getPlayerName().equals("") && !cell.getPlayerName().equals(prisoner.getName()))) 
		{
			cell = null;
			cell = jail.getEmptyCell();
		}
		if (cell != null)
		{
			cell.setPlayerName(player.getName());
			prisoner.setCell(cell);
			prisoner.updateSign();
			if (jail.getSettings().getBoolean(Setting.StoreInventory) && cell.getChest() != null)
			{
				Chest chest = cell.getChest();
				chest.getInventory().clear();
				for (int i = 0;i<40;i++)
				{
					if (chest.getInventory().getSize() <= Util.getNumberOfOccupiedItemSlots(chest.getInventory().getContents())) break;
					if (player.getInventory().getItem(i) == null || player.getInventory().getItem(i).getType() == Material.AIR) continue;
					chest.getInventory().addItem(player.getInventory().getItem(i));
					player.getInventory().clear(i);
				}
								
				if (cell.getSecondChest() != null)
				{
					chest = cell.getSecondChest();
					chest.getInventory().clear();
					for (int i = 0;i<40;i++)
					{
						if (chest.getInventory().getSize() <= Util.getNumberOfOccupiedItemSlots(chest.getInventory().getContents())) break;
						if (player.getInventory().getItem(i) == null || player.getInventory().getItem(i).getType() == Material.AIR) continue;
						chest.getInventory().addItem(player.getInventory().getItem(i));
						player.getInventory().clear(i);
					}

				}
			}
			cell.update();
		}
				
		player.teleport(prisoner.getTeleportLocation());
		if (jail.getSettings().getBoolean(Setting.StoreInventory)) 
		{
			prisoner.storeInventory(player.getInventory());
			for (int i = 0;i<40;i++)
			{
				player.getInventory().clear(i);
			}
			
		}
		
		if (Jail.prisoners.containsKey(prisoner.getName()))
			InputOutput.UpdatePrisoner(prisoner);
		else
			InputOutput.InsertPrisoner(prisoner);
		Jail.prisoners.put(prisoner.getName(), prisoner);
		prisoner.SetBeingReleased(false);
		
		for (Object o : jail.getSettings().getList(Setting.ExecutedCommandsOnJail))
		{
			String s = (String) o;
			CraftServer cs = (CraftServer) Jail.instance.getServer();
			CommandSender coms = new ConsoleCommandSender(Jail.instance.getServer());
			cs.dispatchCommand(coms,prisoner.parseTags(s));
		}
		
	}
	
	/**
	 * Performs releasing of specified JailPrisoner. 
	 * If you just want to release someone, I recommend using prisoner.release, 
	 * because it supports offline release and it's easier to do.
	 * @param prisoner prisoner that will be released
	 * @param player Player that will be teleported
	 */
	public static void UnJail(JailPrisoner prisoner, Player player)
	{
		prisoner.SetBeingReleased(true);
		JailZone jail = prisoner.getJail();	
		Util.Message(jail.getSettings().getString(Setting.MessageUnJail), player);
		player.teleport(jail.getReleaseTeleportLocation());
		prisoner.SetBeingReleased(false);
		
		JailCell cell = prisoner.getCell();
		if (cell != null)
		{
			if (cell.getChest() != null)
			{
				Chest chest = cell.getChest();
				for (int i = 0;i<chest.getInventory().getSize();i++)
				{
					if (chest.getInventory().getItem(i) == null || chest.getInventory().getItem(i).getType() == Material.AIR) continue;
					if (player.getInventory().firstEmpty() == -1)
						player.getWorld().dropItem(player.getLocation(), chest.getInventory().getItem(i));
					else
						player.getInventory().addItem(chest.getInventory().getItem(i));
				}
				chest.getInventory().clear();
				
				if (cell.getSecondChest() != null)
				{
					chest = cell.getSecondChest();
					for (int i = 0;i<chest.getInventory().getSize();i++)
					{
						if (chest.getInventory().getItem(i) == null || chest.getInventory().getItem(i).getType() == Material.AIR) continue;
						if (player.getInventory().firstEmpty() == -1)
							player.getWorld().dropItem(player.getLocation(), chest.getInventory().getItem(i));
						else
							player.getInventory().addItem(chest.getInventory().getItem(i));
					}
					chest.getInventory().clear();

				}
			}
			if (cell.getSign() != null)
			{
				Sign sign = cell.getSign();
				sign.setLine(0, "");
				sign.setLine(1, "");
				sign.setLine(2, "");
				sign.setLine(3, "");
				sign.update();

			}
			cell.setPlayerName("");
			cell.update();
		}
		
		prisoner.restoreInventory(player);
		prisoner.delete();
		
		for (Object o : jail.getSettings().getList(Setting.ExecutedCommandsOnRelease))
		{
			String s = (String) o;
			CraftServer cs = (CraftServer) Jail.instance.getServer();
			CommandSender coms = new ConsoleCommandSender(Jail.instance.getServer());
			cs.dispatchCommand(coms,prisoner.parseTags(s));
		}
	}
	
	/**
	 * Initiate transfer of every prisoner in specified jail zone to another nearest jail zone
	 */
	public static void PrepareTransferAll(JailZone jail)
	{
		PrepareTransferAll(jail, "find nearest");
	}
	
	/**
	 * Initiate transfer of every prisoner in specified jail zone to another jail zone
	 * @param target Name of the destination jail zone
	 */
	public static void PrepareTransferAll(JailZone zone, String target)
	{
		for (JailPrisoner prisoner : zone.getPrisoners())
		{
			prisoner.setTransferDestination(target);
			Player player = Jail.instance.getServer().getPlayer(prisoner.getName());
			if (player == null)
			{
				
				prisoner.setOfflinePending(true);
				InputOutput.UpdatePrisoner(prisoner);
				Jail.prisoners.put(prisoner.getName(), prisoner);
				
			}
			else
			{
				Transfer(prisoner, player);
				
			}
		}
		
	}
	
	/**
	 * Performs transfer of specified JailPrisoner. 
	 * If you just want to transfer someone, I recommend using prisoner.transfer, 
	 * because it supports offline transfer and it's easier to do.
	 * @param prisoner Prisoner that will be transfered
	 * @param player Player that will be teleported
	 */
	public static void Transfer(JailPrisoner prisoner, Player player)
	{
		if (prisoner.getTransferDestination() == "find nearest") prisoner.setTransferDestination(JailZoneManager.findNearestJail(player.getLocation(), prisoner.getJail().getName()).getName());
		
		if (prisoner.getCell() != null)
		{
			Inventory inventory = player.getInventory();
			JailCell cell = prisoner.getCell();
			cell.setPlayerName("");
			if (cell.getSign() != null)
			{
				Sign sign = cell.getSign();
				sign.setLine(0, "");
				sign.setLine(1, "");
				sign.setLine(2, "");
				sign.setLine(3, "");
				sign.update();
			}
			
			if (cell.getChest() != null) 
			{
				for (ItemStack i: cell.getChest().getInventory().getContents())
				{
					if (i == null || i.getType() == Material.AIR) continue;
					inventory.addItem(i);
				}
				cell.getChest().getInventory().clear();
			}
			if (cell.getSecondChest() != null) 
			{
				for (ItemStack i: cell.getSecondChest().getInventory().getContents())
				{
					if (i == null || i.getType() == Material.AIR) continue;
					inventory.addItem(i);
				}
				cell.getSecondChest().getInventory().clear();
			}
			prisoner.setCell(null);
		}
						
		prisoner.SetBeingReleased(true);
		JailZone jail = Jail.zones.get(prisoner.getTransferDestination());
		prisoner.setJail(jail);
		prisoner.setTransferDestination("");
		prisoner.setOfflinePending(false);
		Util.Message(jail.getSettings().getString(Setting.MessageTransfer), player);
		Jail.prisoners.put(prisoner.getName(),prisoner);
		
		JailCell cell = jail.getEmptyCell();
		if (cell != null)
		{
			cell.setPlayerName(player.getName());
			prisoner.setCell(cell);
			prisoner.updateSign();
			if (jail.getSettings().getBoolean(Setting.StoreInventory) && cell.getChest() != null)
			{
				Chest chest = cell.getChest();
				chest.getInventory().clear();
				for (int i = 0;i<40;i++)
				{
					if (chest.getInventory().getSize() <= Util.getNumberOfOccupiedItemSlots(chest.getInventory().getContents())) break;
					if (player.getInventory().getItem(i) == null || player.getInventory().getItem(i).getType() == Material.AIR) continue;
					chest.getInventory().addItem(player.getInventory().getItem(i));
					player.getInventory().clear(i);
				}
								
				if (cell.getSecondChest() != null)
				{
					chest = cell.getSecondChest();
					chest.getInventory().clear();
					for (int i = 0;i<40;i++)
					{
						if (chest.getInventory().getSize() <= Util.getNumberOfOccupiedItemSlots(chest.getInventory().getContents())) break;
						if (player.getInventory().getItem(i) == null || player.getInventory().getItem(i).getType() == Material.AIR) continue;
						chest.getInventory().addItem(player.getInventory().getItem(i));
						player.getInventory().clear(i);
					}

				}
			}
			cell.update();
		}
		
		if (jail.getSettings().getBoolean(Setting.StoreInventory)) 
		{
			prisoner.storeInventory(player.getInventory());
			player.getInventory().clear();
		}
		
		player.teleport(prisoner.getTeleportLocation());
		prisoner.SetBeingReleased(false);
		InputOutput.UpdatePrisoner(prisoner);
	}
}
