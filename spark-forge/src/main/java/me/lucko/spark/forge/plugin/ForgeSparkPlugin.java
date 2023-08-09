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

import me.lucko.spark.common.SparkPlatform;
import me.lucko.spark.common.SparkPlugin;
import me.lucko.spark.common.command.sender.CommandSender;
import me.lucko.spark.common.sampler.ThreadDumper;
import me.lucko.spark.common.sampler.source.ClassSourceLookup;
import me.lucko.spark.common.sampler.source.SourceMetadata;
import me.lucko.spark.common.util.SparkThreadFactory;
import me.lucko.spark.forge.ForgeClassSourceLookup;
import me.lucko.spark.forge.ForgeCommandSender;
import me.lucko.spark.forge.ForgeSparkMod;

import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.StringUtils;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Level;
import java.util.stream.Collectors;

public abstract class ForgeSparkPlugin implements SparkPlugin {

    private final ForgeSparkMod mod;
    private final Logger logger;
    protected final ScheduledExecutorService scheduler;
    protected SparkPlatform platform;

    /*
    protected final ThreadDumper.GameThread threadDumper = new ThreadDumper.GameThread();

    @Override
    public ThreadDumper getDefaultThreadDumper() {
        return this.threadDumper.get();
    }
    */

    protected ForgeSparkPlugin(ForgeSparkMod mod) {
        this.mod = mod;
        this.logger = LogManager.getLogger("spark");
        this.scheduler = Executors.newScheduledThreadPool(4, new SparkThreadFactory());
    }

    public void enable() {
        this.platform = new SparkPlatform(this);
        this.platform.enable();
    }

    public void disable() {
        this.platform.disable();
        this.scheduler.shutdown();
    }

    public abstract boolean hasPermission(ICommandSender sender, String permission);

    @Override
    public String getVersion() {
        return this.mod.getVersion();
    }

    @Override
    public Path getPluginDirectory() {
        return this.mod.getConfigDirectory();
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
    public ClassSourceLookup createClassSourceLookup() {
        return new ForgeClassSourceLookup();
    }

    @Override
    public Collection<SourceMetadata> getKnownSources() {
        return SourceMetadata.gather(
                Loader.instance().getActiveModList(),
                ModContainer::getModId,
                ModContainer::getVersion,
                mod -> mod.getMetadata().getAuthorList()
        );
    }

    protected List<String> generateSuggestions(ForgeCommandSender sender, String[] args) {
        return this.platform.tabCompleteCommand(sender, args);
    }

    /*
    protected static <T> void registerCommands(CommandDispatcher<T> dispatcher, Command<T> executor, SuggestionProvider<T> suggestor, String... aliases) {
        if (aliases.length == 0) {
            return;
        }

        String mainName = aliases[0];
        LiteralArgumentBuilder<T> command = LiteralArgumentBuilder.<T>literal(mainName)
                .executes(executor)
                .then(RequiredArgumentBuilder.<T, String>argument("args", StringArgumentType.greedyString())
                        .suggests(suggestor)
                        .executes(executor)
                );

        LiteralCommandNode<T> node = dispatcher.register(command);
        for (int i = 1; i < aliases.length; i++) {
            dispatcher.register(LiteralArgumentBuilder.<T>literal(aliases[i]).redirect(node));
        }
    }
    */

    protected static String[] processArgs(String[] args, boolean tabComplete) {//, String... aliases) {
        String[] split = Arrays.stream(args).filter(c -> tabComplete || (!c.trim().isEmpty())).toArray(String[]::new);
        //if(split.length == 0 || !Arrays.asList(aliases).contains(split[0])) {
        //    System.out.println("returning null");
        //    return null;
        //}

        //return Arrays.copyOfRange(split, 1, split.length);
        return split;
    }
}
