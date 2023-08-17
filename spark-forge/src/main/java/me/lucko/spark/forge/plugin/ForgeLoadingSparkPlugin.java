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

package me.lucko.spark.forge.plugin;

import me.lucko.spark.common.SparkPlatform;
import me.lucko.spark.common.SparkPlugin;
import me.lucko.spark.common.command.sender.CommandSender;
import me.lucko.spark.common.platform.PlatformInfo;
import me.lucko.spark.common.platform.serverconfig.ServerConfigProvider;
import me.lucko.spark.common.sampler.*;
import me.lucko.spark.common.util.SparkThreadFactory;
import me.lucko.spark.forge.ForgePlatformInfo;
import me.lucko.spark.forge.ForgeServerConfigProvider;
import me.lucko.spark.forge.ForgeSparkMod;
import net.minecraftforge.fml.relauncher.FMLLaunchHandler;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Level;
import java.util.stream.Stream;

public class ForgeLoadingSparkPlugin implements SparkPlugin {

    private final boolean isClient = FMLLaunchHandler.side() == Side.CLIENT;
    private final Logger logger;
    protected final ScheduledExecutorService scheduler;
    protected SparkPlatform platform;

    protected ForgeLoadingSparkPlugin() {
        this.logger = LogManager.getLogger("spark");
        this.scheduler = Executors.newScheduledThreadPool(4, new SparkThreadFactory());
    }

    public static ForgeLoadingSparkPlugin register() {
        ForgeLoadingSparkPlugin plugin = new ForgeLoadingSparkPlugin();
        plugin.enable();
        return plugin;
    }

    public void enable() {
        this.platform = new SparkPlatform(this);
        this.platform.enable();
    }

    public void disable() {
        this.platform.disable();
        this.scheduler.shutdown();
    }

    public SparkPlatform getPlatform() {
        return this.platform;
    }

    @Override
    public ThreadDumper getDefaultThreadDumper() {
        return ThreadDumper.ALL;
        //return new ThreadDumper.Specific(Thread.currentThread());
    }

    @Override
    public ServerConfigProvider createServerConfigProvider() {
        return isClient ? null : new ForgeServerConfigProvider();
    }

    @Override
    public String getVersion() {
        return ForgeSparkMod.getVersion();
    }

    @Override
    public Path getPluginDirectory() {
        return ForgeSparkMod.getConfigDirectory();
    }

    @Override
    public String getCommandName() {
        return null;
    }

    @Override
    public Stream<? extends CommandSender> getCommandSenders() {
        return Stream.empty();
    }

    @Override
    public void executeAsync(Runnable task) {
        this.scheduler.execute(task);
    }

    @Override
    public void log(Level level, String msg) {
        if (level == Level.INFO) {
            this.logger.info(msg);
        } else if (level == Level.WARNING) {
            this.logger.warn(msg);
        } else if (level == Level.SEVERE) {
            this.logger.error(msg);
        } else {
            throw new IllegalArgumentException(level.getName());
        }
    }

    @Override
    public PlatformInfo getPlatformInfo() {
        return this.isClient ? new ForgePlatformInfo(PlatformInfo.Type.CLIENT) : new ForgePlatformInfo(PlatformInfo.Type.SERVER);
    }
}