package com.matejdro.bukkit.jail.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionDefault;

import com.matejdro.bukkit.jail.Util;

public abstract class BaseCommand {
//Command system concept by oliverw92
	public Boolean needPlayer;
	public Boolean adminCommand;
	public String permission;
	
	public abstract Boolean run(CommandSender sender, String[] args);
	
	public Boolean execute(CommandSender sender, String[] args)
	{
		if (!(sender instanceof Player) && needPlayer) return false;
		if (sender instanceof Player && !Util.permission((Player) sender, permission, adminCommand ? PermissionDefault.OP : PermissionDefault.TRUE)) return false;
		
		return run(sender, args);
	}

}