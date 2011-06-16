package com.matejdro.bukkit.jail.listeners;

import java.util.List;

import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerQuitEvent;

import com.matejdro.bukkit.jail.InputOutput;
import com.matejdro.bukkit.jail.Jail;
import com.matejdro.bukkit.jail.JailCellCreation;
import com.matejdro.bukkit.jail.JailPrisoner;
import com.matejdro.bukkit.jail.JailZoneCreation;
import com.matejdro.bukkit.jail.PrisonerManager;
import com.matejdro.bukkit.jail.Settings;
import com.matejdro.bukkit.jail.Util;
import com.matejdro.bukkit.jail.commands.JailSetCommand;

public class JailPlayerListener extends PlayerListener {
	private Jail plugin;

	public JailPlayerListener(Jail instance)
	{
		plugin = instance;
	}		
	
	public void onPlayerInteract(PlayerInteractEvent event) {
		if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getPlayer().getItemInHand().getTypeId() == Settings.SelectionTool)
		{
			if ( JailZoneCreation.players.containsKey(event.getPlayer().getName()))
			{
				JailZoneCreation.select(event.getPlayer(), event.getClickedBlock());
				event.setCancelled(true);
			}
			else if ( JailCellCreation.players.containsKey(event.getPlayer().getName()))
			{
				JailCellCreation.select(event.getPlayer(), event.getClickedBlock());
				event.setCancelled(true);
			}
			else if ( JailSetCommand.players.containsKey(event.getPlayer().getName()))
			{
				JailSetCommand.RightClick(event.getClickedBlock(), event.getPlayer());
				event.setCancelled(true);
			}
			
		}
	}
	
	public void onPlayerInteract(PlayerInteractEntityEvent event) {
			Player player = event.getPlayer();
			if (!InputOutput.jailStickParameters.containsKey(player.getItemInHand().getTypeId())) return;
			if (!Util.permission(player, "jail.usejailstick" + String.valueOf(player.getItemInHand().getTypeId()), player.isOp())) return;
			
			String[] param = InputOutput.jailStickParameters.get(player.getItemInHand().getTypeId());
			
			List<Block> targets = player.getLineOfSight(null, Integer.parseInt(param[1]));
			targets.remove(0);
			Entity ent = event.getRightClicked();
			if (ent == null || !(ent instanceof Player)) return;
			Player target = (Player) ent;
			if (Util.permission(target, "jail.canbestickjailed", true))
			{
				String args[] = new String[4];
				args[0] = target.getName();
				args[1] = param[2];
				args[2] = param[3];
				args[3] = param[4];
				PrisonerManager.PrepareJail((CommandSender) event.getPlayer(), args); 
			}
	}
	
	public void onPlayerChat(PlayerChatEvent event) {
		if ( JailCellCreation.players.containsKey(event.getPlayer().getName()))
		{
			if (JailCellCreation.chatmessage(event.getPlayer(), event.getMessage()));
				event.setCancelled(true);
		}
	}

	
	 public void onPlayerJoin(PlayerJoinEvent event) {
		 if (Jail.prisoners.containsKey(event.getPlayer().getName().toLowerCase()))
		 {
			 JailPrisoner prisoner = Jail.prisoners.get(event.getPlayer().getName().toLowerCase());
			 if (prisoner.offlinePending())
			 {
				 if (prisoner.getTransferDestination().isEmpty())
				 {
					 if (prisoner.getRemainingTime() != 0)
					 {
						 PrisonerManager.Jail(prisoner, event.getPlayer());
					 }
					 else
					 {
						 PrisonerManager.UnJail(prisoner, event.getPlayer());
					 } 
				 }
				 else
				 {
					 PrisonerManager.Transfer(prisoner, event.getPlayer());
				 }
				 
			 }
		 }
	 }
	 
	 public void onPlayerQuit(PlayerQuitEvent event) {
		JailPrisoner prisoner = Jail.prisoners.get(event.getPlayer().getName().toLowerCase());
		if (prisoner == null) return;

		for (Object o : prisoner.getGuards().toArray())
		{
			Wolf w = (Wolf) o;
			prisoner.getGuards().remove(w);
			Jail.guards.remove(w);
			w.remove();
		}
	 }
}
