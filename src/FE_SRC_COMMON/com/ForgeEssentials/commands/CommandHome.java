package com.ForgeEssentials.commands;

import java.util.List;

import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

import com.ForgeEssentials.api.permissions.PermissionsAPI;
import com.ForgeEssentials.api.permissions.query.PermQueryPlayer;
import com.ForgeEssentials.core.PlayerInfo;
import com.ForgeEssentials.core.commands.ForgeEssentialsCommandBase;
import com.ForgeEssentials.util.Localization;
import com.ForgeEssentials.util.OutputHandler;
import com.ForgeEssentials.util.TeleportCenter;
import com.ForgeEssentials.util.AreaSelector.WarpPoint;

public class CommandHome extends ForgeEssentialsCommandBase
{

	@Override
	public String getCommandName()
	{
		return "home";
	}

	@Override
	public void processCommandPlayer(EntityPlayer sender, String[] args)
	{
		if (args.length == 0)
		{
			// homes aren't saving...
			WarpPoint home = PlayerInfo.getPlayerInfo(sender.username).home;
			if (home == null)
			{
				OutputHandler.chatError(sender, Localization.get("message.error.nohome") + getSyntaxPlayer(sender));
			}
			else
			{
				EntityPlayerMP player = (EntityPlayerMP) sender;
				PlayerInfo playerInfo = PlayerInfo.getPlayerInfo(player.username);
				playerInfo.back = new WarpPoint(player);
				TeleportCenter.addToTpQue(home, player);
			}
		}
		else if (PermissionsAPI.checkPermAllowed(new PermQueryPlayer(sender, getCommandPerm() + ".set")))
		{
			if (args.length >= 1 && (args[0].equals("here") || args[0].equals("set")))
			{
				WarpPoint p = new WarpPoint(sender);
				PlayerInfo.getPlayerInfo(sender.username).home = p;
				sender.sendChatToPlayer(Localization.format("command.home.confirm", p.x, p.y, p.z));
			}
		}
	}

	@Override
	public void processCommandConsole(ICommandSender sender, String[] args)
	{
	}

	@Override
	public String getCommandPerm()
	{
		return "ForgeEssentials.BasicCommands." + getCommandName();
	}

	@Override
	public List addTabCompletionOptions(ICommandSender sender, String[] args)
	{
		if (args.length == 1)
			return getListOfStringsMatchingLastWord(args, "here");
		else
			return null;
	}

	@Override
	public boolean canConsoleUseCommand()
	{
		return false;
	}
}
