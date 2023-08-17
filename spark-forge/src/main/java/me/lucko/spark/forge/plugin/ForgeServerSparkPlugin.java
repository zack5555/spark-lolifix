/*
 * This file is part of spark.
 *
 *  Copyright (c) lucko (Luck) <luck@lucko.me>
 *  Copyright (c) contributors
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 * Modified on 8/9/2023 by fonnymunkey under GNU GPLv3 for 1.12.2 backport
 */

package me.lucko.spark.forge.plugin;

import me.lucko.spark.common.monitor.ping.PlayerPingProvider;
import me.lucko.spark.common.platform.PlatformInfo;
import me.lucko.spark.common.platform.serverconfig.ServerConfigProvider;
import me.lucko.spark.common.platform.world.WorldInfoProvider;
import me.lucko.spark.common.sampler.ThreadDumper;
import me.lucko.spark.common.tick.TickHook;
import me.lucko.spark.common.tick.TickReporter;
import me.lucko.spark.forge.ForgeCommandSender;
import me.lucko.spark.forge.ForgePlatformInfo;
import me.lucko.spark.forge.ForgePlayerPingProvider;
import me.lucko.spark.forge.ForgeServerConfigProvider;
import me.lucko.spark.forge.ForgeSparkMod;
import me.lucko.spark.forge.ForgeTickHook;
import me.lucko.spark.forge.ForgeTickReporter;
import me.lucko.spark.forge.ForgeWorldInfoProvider;

import me.lucko.spark.forge.mixin.MinecraftServerAccessorMixin;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.PermissionAPI;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

public class ForgeServerSparkPlugin extends ForgeSparkPlugin implements ICommand {

    public static ForgeServerSparkPlugin register(ForgeSparkMod mod, FMLServerStartingEvent event) {
        ForgeServerSparkPlugin plugin = new ForgeServerSparkPlugin(mod, event.getServer());
        plugin.enable();

        event.registerServerCommand(plugin);
        PermissionAPI.registerNode("spark", DefaultPermissionLevel.OP, "Access to the spark command");

        return plugin;
    }

    private final MinecraftServer server;

    private final ThreadDumper gameThreadDumper;

    @Override
    public ThreadDumper getDefaultThreadDumper() {
        return this.gameThreadDumper;
    }

    private ForgeServerSparkPlugin(ForgeSparkMod mod, MinecraftServer server) {
        super(mod);
        this.server = server;
        this.gameThreadDumper = new ThreadDumper.Specific(((MinecraftServerAccessorMixin)server).getServerThread());
    }

    @Override
    public void enable() {
        super.enable();
    }

    @Override
    public void disable() {
        super.disable();
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) {
        String[] proc = processArgs(args, false);

        this.platform.executeCommand(new ForgeCommandSender(sender, this), proc);
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos pos) {
        String[] proc = processArgs(args, true);

        return generateSuggestions(new ForgeCommandSender(sender, this), proc);
    }

    @Override
    public boolean hasPermission(ICommandSender sender, String permission) {
        if(sender instanceof EntityPlayer) {
            return PermissionAPI.hasPermission((EntityPlayer)sender, permission);
        }
        else {
            return true;
        }
    }

    @Override
    public Stream<ForgeCommandSender> getCommandSenders() {
        return Stream.concat(
            this.server.getPlayerList().getPlayers().stream(),
            Stream.of(this.server)
        ).map(sender -> new ForgeCommandSender(sender, this));
    }

    @Override
    public boolean runBackgroundProfiler() { return true; }

    @Override
    public void executeSync(Runnable task) {
        this.server.addScheduledTask(task);
    }

    @Override
    public TickHook createTickHook() {
        return new ForgeTickHook(TickEvent.Type.SERVER);
    }

    @Override
    public TickReporter createTickReporter() {
        return new ForgeTickReporter(TickEvent.Type.SERVER);
    }

    @Override
    public PlayerPingProvider createPlayerPingProvider() {
        return new ForgePlayerPingProvider(this.server);
    }

    @Override
    public ServerConfigProvider createServerConfigProvider() {
        return new ForgeServerConfigProvider();
    }

    @Override
    public WorldInfoProvider createWorldInfoProvider() {
        return new ForgeWorldInfoProvider.Server(this.server);
    }

    @Override
    public PlatformInfo getPlatformInfo() {
        return new ForgePlatformInfo(PlatformInfo.Type.SERVER);
    }

    @Override
    public String getCommandName() {
        return "spark";
    }

    @Override
    public String getName() {
        return getCommandName();
    }

    @Override
    public String getUsage(ICommandSender iCommandSender) {
        return "/" + getCommandName();
    }

    @Override
    public List<String> getAliases() {
        return Collections.singletonList(getCommandName());
    }

    @Override
    public boolean checkPermission(MinecraftServer minecraftServer, ICommandSender sender) {
        return this.platform.hasPermissionForAnyCommand(new ForgeCommandSender(sender, this));
    }

    @Override
    public boolean isUsernameIndex(String[] strings, int i) {
        return false;
    }

    @Override
    public int compareTo(ICommand o) {
        return getCommandName().compareTo(o.getName());
    }
}
