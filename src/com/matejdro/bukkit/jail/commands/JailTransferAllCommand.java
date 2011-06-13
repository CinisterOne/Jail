package com.matejdro.bukkit.jail.commands;

import org.bukkit.command.CommandSender;

import com.matejdro.bukkit.jail.Jail;
import com.matejdro.bukkit.jail.PrisonerManager;
import com.matejdro.bukkit.jail.Settings;
import com.matejdro.bukkit.jail.Util;

public class JailTransferAllCommand extends BaseCommand {
	
	public JailTransferAllCommand()
	{
		needPlayer = false;
		permission = "jail.command.jailtransferall";
	}


	public Boolean run(CommandSender sender, String[] args) {		
		if (args.length < 1)
		{
			Util.Message("Usage: /jailtransferall [Old Jail Name] (New Jail Name)", sender);
			return true;
		}
		if (!Jail.zones.containsKey(args[0].toLowerCase()))
		{
			Util.Message("There is no such jail!", sender);
			return true;
		}
		if (args.length > 1 && !Jail.zones.containsKey(args[1].toLowerCase()))
		{
			Util.Message("Target jail does not exist!", sender);
			return true;
		}
		if (args.length > 1 && args[1].equals(Settings.NearestJailCode))
			PrisonerManager.PrepareTransferAll(Jail.zones.get(args[0].toLowerCase()), args[1].toLowerCase());
		else
			PrisonerManager.PrepareTransferAll(Jail.zones.get(args[0].toLowerCase()));
		Util.Message("Transfer command sent!", sender);
		return true;
	}

}
