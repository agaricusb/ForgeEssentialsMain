package com.ForgeEssentials.permission;

import java.io.File;

import net.minecraftforge.common.MinecraftForge;

import com.ForgeEssentials.api.ForgeEssentialsRegistrar.PermRegister;
import com.ForgeEssentials.api.data.ClassContainer;
import com.ForgeEssentials.api.data.DataStorageManager;
import com.ForgeEssentials.api.modules.FEModule;
import com.ForgeEssentials.api.modules.FEModule.Config;
import com.ForgeEssentials.api.modules.FEModule.Init;
import com.ForgeEssentials.api.modules.FEModule.ModuleDir;
import com.ForgeEssentials.api.modules.FEModule.PreInit;
import com.ForgeEssentials.api.modules.FEModule.ServerInit;
import com.ForgeEssentials.api.modules.FEModule.ServerPostInit;
import com.ForgeEssentials.api.modules.FEModule.ServerStop;
import com.ForgeEssentials.api.modules.event.FEModuleInitEvent;
import com.ForgeEssentials.api.modules.event.FEModulePreInitEvent;
import com.ForgeEssentials.api.modules.event.FEModuleServerInitEvent;
import com.ForgeEssentials.api.modules.event.FEModuleServerPostInitEvent;
import com.ForgeEssentials.api.modules.event.FEModuleServerStopEvent;
import com.ForgeEssentials.api.permissions.IPermRegisterEvent;
import com.ForgeEssentials.api.permissions.PermissionsAPI;
import com.ForgeEssentials.api.permissions.RegGroup;
import com.ForgeEssentials.api.permissions.Zone;
import com.ForgeEssentials.api.permissions.ZoneManager;
import com.ForgeEssentials.core.ForgeEssentials;
import com.ForgeEssentials.data.AbstractDataDriver;
import com.ForgeEssentials.permission.mcoverride.OverrideManager;
import com.ForgeEssentials.util.TeleportCenter;
import com.google.common.collect.HashMultimap;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLServerStartingEvent;

@FEModule(name = "Permissions", parentMod = ForgeEssentials.class, configClass = ConfigPermissions.class)
public class ModulePermissions
{
	// public static ConfigPermissions config;
	public static PermissionsPlayerHandler	ppHandler;
	public static PermissionsBlanketHandler	pbHandler;
	public static SqlHelper				sql;

	@Config
	public static ConfigPermissions		config;

	@ModuleDir
	public static File					permsFolder;

	protected static AbstractDataDriver	data;

	// permission registrations here...
	protected HashMultimap				regPerms;
	private AutoPromote					autoPromote;

	@PreInit
	public void preLoad(FEModulePreInitEvent e)
	{
		ZoneManager.manager = new ZoneHelper();
		PermissionsAPI.manager = new PermissionsHelper();

		MinecraftForge.EVENT_BUS.register(ZoneManager.manager);
		PermRegLoader laoder = new PermRegLoader(e.getCallableMap().getCallable(PermRegister.class));
		regPerms = laoder.loadAllPerms();
		
		DataStorageManager.registerSaveableType(new ClassContainer(AutoPromote.class));
		DataStorageManager.registerSaveableType(new ClassContainer(Zone.class));
	}

	@Init
	public void load(FEModuleInitEvent e)
	{
		// setup SQL
		sql = new SqlHelper(config);
		sql.putRegistrationperms(regPerms);

		ppHandler = new PermissionsPlayerHandler();
		pbHandler = new PermissionsBlanketHandler();
		PermissionsAPI.QUERY_BUS.register(ppHandler);
		PermissionsAPI.QUERY_BUS.register(pbHandler);

		DataStorageManager.registerSaveableType(Zone.class);
		DataStorageManager.registerSaveableType(AutoPromote.class);
	}

	@ServerInit
	public void serverStarting(FEModuleServerInitEvent e)
	{
		// load zones...
		data = DataStorageManager.getReccomendedDriver();
		((ZoneHelper) ZoneManager.manager).loadZones();

		if (config.importBool)
		{
			sql.importPerms(config.importDir);
		}

		// init perms and vMC command overrides
		e.registerServerCommand(new CommandZone());
		e.registerServerCommand(new CommandFEPerm());
		OverrideManager.regOverrides((FMLServerStartingEvent) e.getFMLEvent());
	}

	@ServerPostInit
	public void serverStarted(FEModuleServerPostInitEvent e)
	{
		for (Object obj : DataStorageManager.getReccomendedDriver().loadAllObjects(new ClassContainer(AutoPromote.class)))
		{
			AutoPromote.map.put(((AutoPromote) obj).zone, (AutoPromote) obj);
		}
		autoPromote = new AutoPromote(FMLCommonHandler.instance().getMinecraftServerInstance());
	}

	@PermRegister
	public static void registerPermissions(IPermRegisterEvent event)
	{
		event.registerPermissionLevel("ForgeEssentials.permissions.zone.setparent", RegGroup.ZONE_ADMINS);
		event.registerPermissionLevel("ForgeEssentials.perm", RegGroup.OWNERS);
		event.registerPermissionLevel("ForgeEssentials.perm._ALL_", RegGroup.OWNERS);
		event.registerPermissionLevel("ForgeEssentials.permissions.zone", RegGroup.ZONE_ADMINS);
		event.registerPermissionLevel("ForgeEssentials.permissions.zone._ALL_", RegGroup.ZONE_ADMINS);

		event.registerPermissionLevel("_ALL_", RegGroup.OWNERS);

		event.registerPermissionLevel(TeleportCenter.BYPASS_COOLDOWN, RegGroup.OWNERS);
		event.registerPermissionLevel(TeleportCenter.BYPASS_COOLDOWN, RegGroup.OWNERS);

		event.registerPermissionLevel("ForgeEssentials.BasicCommands.list", RegGroup.GUESTS);
	}

	@ServerStop
	public void serverStopping(FEModuleServerStopEvent e)
	{
		// save all the zones
		ClassContainer con = new ClassContainer(Zone.class);
		for (Zone zone : ZoneManager.getZoneList())
		{
			if (zone == null || zone.isGlobalZone() || zone.isWorldZone())
			{
				continue;
			}
			data.saveObject(con, zone);
		}

		AutoPromote.saveAll();
		autoPromote.interrupt();
	}

}
