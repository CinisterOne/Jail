package com.matejdro.bukkit.jail;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Level;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;

import com.nijikokun.bukkit.Permissions.Permissions;


public class Util {
	public static void Message(String message, Player player)
	{
		String color = "\u00A7f";
		final int maxLength = 61; //Max length of chat text message
        final String newLine = "[NEWLINE]";
        ArrayList<String> chat = new ArrayList<String>();
        chat.add(0, color);
        String[] words = message.split(" ");
        int lineNumber = 0;
        for (int i = 0; i < words.length; i++) {
                if (chat.get(lineNumber).length() + words[i].length() < maxLength && !words[i].equals(newLine)) {
                        chat.set(lineNumber, chat.get(lineNumber) + " " + words[i]);
                }
                else {
                        lineNumber++;
                        if (!words[i].equals(newLine)) {
                                chat.add(lineNumber, color + words[i]);
                        }
                        else
                                chat.add(lineNumber,color);
                }
        }
        for (int i = 0; i < chat.size(); i++) {
                player.sendMessage(chat.get(i));
        }
	}
	
	public static void Message(String message, CommandSender sender)
	{
		if (sender == null) return;
		if (sender instanceof Player)
		{
			Message(message, (Player) sender);
		}
		else
		{
			sender.sendMessage(message);
		}
	}
	
    public static Boolean permission(Player player, String line, PermissionDefault def)
    {
    	    if(Jail.permissions != null) {
    	    	return (((Permissions) Jail.permissions).getHandler()).has(player, line);
    	    } else {
    	    	return player.hasPermission(new Permission(line, def));
    	    }
    }
    
    public static int getNumberOfOccupiedItemSlots(ItemStack[] items)
    {
    	int size = 0;
    	for (ItemStack i : items)
    	{
    		if (i != null) size++;
    	}
    	return size;
    }
    
    public static Boolean isInteger(String text) {
    	  try {
    	    Integer.parseInt(text);
    	    return true;
    	  } catch (NumberFormatException e) {
    	    return false;
    	  }
    	}
    
    public static void log(String text)
    {
		try {
			FileWriter fstream = new FileWriter(new File("plugins" + File.separator + "Jail","jail.log"), true);
			BufferedWriter out = new BufferedWriter(fstream);
			DateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
			out.append("[" + dateFormat.format(new Date()) + "] " + text);
	    	out.newLine();
	    	  //Close the output stream
	    	out.close();
	    	fstream.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			Jail.log.log(Level.SEVERE, "[Jail]: Unable to write data to log file.");

			e.printStackTrace();
		}
    	 
    }

}
