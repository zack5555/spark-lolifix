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

package me.lucko.spark.forge;

import me.lucko.spark.common.SparkPlatform;
import me.lucko.spark.common.SparkPlugin;
import me.lucko.spark.common.command.modules.SamplerModule;
import me.lucko.spark.common.command.sender.CommandSender;
import me.lucko.spark.common.sampler.*;
import me.lucko.spark.common.sampler.async.AsyncProfilerAccess;
import me.lucko.spark.common.sampler.async.AsyncSampler;
import me.lucko.spark.common.sampler.async.SampleCollector;
import me.lucko.spark.common.sampler.java.JavaSampler;
import me.lucko.spark.common.sampler.node.MergeMode;
import me.lucko.spark.common.sampler.source.ClassSourceLookup;
import me.lucko.spark.common.util.MethodDisambiguator;
import me.lucko.spark.forge.mixin.StateEnum;
import net.minecraftforge.fml.relauncher.FMLLaunchHandler;
import net.minecraftforge.fml.relauncher.Side;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class ForgeLoadingSamplerModule {

    private static final String OPTION_ENGINE = "loadingProfiler_Option_Engine";
    private static final String OPTION_INTERVAL = "loadingProfiler_Option_Interval";
    private static final String OPTION_IGNORE_SLEEPING = "loadingProfiler_Option_IgnoreSleeping";
    private static final String OPTION_IGNORE_NATIVE = "loadingProfiler_Option_IgnoreNative";
    private static final String OPTION_THREAD_GROUPER = "loadingProfiler_Option_ThreadGrouper";
    private static final String OPTION_THREAD_DUMPER = "loadingProfiler_Option_AllThread";
    private static final String OPTION_ALLOC = "loadingProfiler_Option_MemAlloc";
    private static final String OPTION_ALLOC_INTERVAL = "loadingProfiler_Option_MemAllocInterval";

    //Realistically there shouldn't be any more than 2 running at one time
    private static final Map<StateEnum, Sampler> activeSamplers = new HashMap<>();

    /**
     * Attempts to start a new sampler based on the given FMLState
     *
     * @param plugin active plugin
     * @param platform active platform
     * @param state FMLState of sampler
     */
    public static void startForState(SparkPlugin plugin, SparkPlatform platform, StateEnum state) {
        if(!activeSamplers.containsKey(state)) {
            boolean allowAsync = AsyncProfilerAccess.getInstance(platform).checkSupported(platform);

            boolean profAsync = platform.getConfiguration().getOrSaveString(OPTION_ENGINE, allowAsync ? "async" : "java").equals("async");
            if(profAsync && !allowAsync) {
                platform.getConfiguration().setString(OPTION_ENGINE, "java");
                platform.getConfiguration().save();
                profAsync = false;
                plugin.log(Level.INFO, "Loading Profiler is set to async sampling, but async is not supported, setting to java sampling");
            }
            boolean memAlloc = platform.getConfiguration().getOrSaveBoolean(OPTION_ALLOC, false);
            if(memAlloc && !allowAsync) {
                platform.getConfiguration().setBoolean(OPTION_ALLOC, false);
                platform.getConfiguration().save();
                memAlloc = false;
                plugin.log(Level.INFO, "Loading Profiler is set to memAlloc, but async is not supported, setting to java sampling");
            }
            boolean ignoreSleeping = platform.getConfiguration().getOrSaveBoolean(OPTION_IGNORE_SLEEPING, true);
            boolean ignoreNative = platform.getConfiguration().getOrSaveBoolean(OPTION_IGNORE_NATIVE, false);

            SamplerMode mode = memAlloc ? SamplerMode.ALLOCATION : SamplerMode.EXECUTION;

            ThreadGrouper threadGrouper = ThreadGrouper.parseConfigSetting(platform.getConfiguration().getOrSaveString(OPTION_THREAD_GROUPER, "by-pool"));
            ThreadDumper threadDumper = ThreadDumper.parseConfigSetting(platform.getConfiguration().getOrSaveBoolean(OPTION_THREAD_DUMPER, false) ? "all" : "default");
            if(threadDumper == null) threadDumper = plugin.getDefaultThreadDumper();

            int interval = platform.getConfiguration().getOrSaveInteger(mode == SamplerMode.ALLOCATION ? OPTION_ALLOC_INTERVAL : OPTION_INTERVAL, mode.defaultInterval());
            if(interval <= 0) interval = mode.defaultInterval();
            interval = (int)(mode == SamplerMode.EXECUTION ? interval * 1000d : interval);

            SamplerSettings settings = new SamplerSettings(interval, threadDumper, threadGrouper, -1, false);
            Sampler sampler;
            try {
                if(mode == SamplerMode.ALLOCATION) {
                    sampler = new AsyncSampler(
                            platform,
                            settings,
                            new SampleCollector.Allocation(interval, false));
                }
                else if(profAsync) {
                    sampler = new AsyncSampler(
                            platform,
                            settings,
                            new SampleCollector.Execution(interval)
                    );
                }
                else {
                    sampler = new JavaSampler(
                            platform,
                            settings,
                            ignoreSleeping,
                            ignoreNative
                    );
                }
            }
            catch(Exception e) {
                plugin.log(Level.WARNING, "Loading Profiler has encountered an exception while attempting to start sampling state: " + state.getValue());
                e.printStackTrace();
                return;
            }
            activeSamplers.put(state, sampler);
            plugin.log(Level.INFO, "Loading Profiler has started sampling for loading stage: " + state.getValue());
            sampler.start();

            CompletableFuture<Sampler> future = sampler.getFuture();

            future.whenCompleteAsync((s, throwable) -> {
                if(throwable != null) {
                    plugin.log(Level.WARNING,"Loading Profiler sampler failed unexpectedly for state: " + state.getValue());
                    throwable.printStackTrace();
                }
            });
        }
        else plugin.log(Level.WARNING, "Loading Profiler attempted to start a duplicate sampler for state: " + state.getValue());
    }

    /**
     * Stops the active sampler for the given state and saves to file
     *
     * @param plugin active plugin
     * @param platform active platform
     * @param state FMLState of sampler
     */
    public static void stopForState(SparkPlugin plugin, SparkPlatform platform, StateEnum state) {
        Sampler sampler = activeSamplers.remove(state);
        if(sampler != null) {
            sampler.stop(false);
            plugin.executeAsync(() -> {
                plugin.log(Level.INFO, "Loading Profiler has stopped profiling for loading state: " + state.getValue() + ", saving results, please wait...");
                SamplerModule.handleManualSaveFile(
                        platform,
                        sampler,
                        new Sampler.ExportProps()
                                .creator(new CommandSender.Data("LoadingProfiler", null))
                                .mergeMode(() -> {
                                    MethodDisambiguator methodDisambiguator = new MethodDisambiguator();
                                    return MergeMode.sameMethod(methodDisambiguator);
                                })
                                .classSourceLookup(() -> ClassSourceLookup.create(platform)),
                        "forgeloading_" + state.getValue() + "_" + (FMLLaunchHandler.side() == Side.CLIENT ? "client" : "server") + (sampler.getMode() == SamplerMode.ALLOCATION ? "_memalloc" : "")
                );
            });
        }
    }

    /**
     * Iterates StateEnum values and ensures the samplers are all stopped
     *
     * @param plugin active plugin
     */
    public static void clearHangingSamplers(SparkPlugin plugin) {
        for(StateEnum state : StateEnum.values()) {
            Sampler sample = activeSamplers.remove(state);
            if(sample != null) {
                sample.stop(true);
                plugin.log(Level.WARNING, "Loading Profiler detected hanging sampler for state: " + state.getValue() + ", clearing");
            }
        }
    }
}