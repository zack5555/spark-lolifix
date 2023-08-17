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

package me.lucko.spark.forge.mixin;

import me.lucko.spark.forge.ForgeLoadingSamplerModule;
import me.lucko.spark.forge.ForgeSparkMod;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.Mixins;

import java.util.Map;

@IFMLLoadingPlugin.Name("Spark")
@IFMLLoadingPlugin.MCVersion("1.12.2")
@IFMLLoadingPlugin.SortingIndex(-10000)
public class SparkMixinPlugin implements IFMLLoadingPlugin {

    public SparkMixinPlugin() {
        MixinBootstrap.init();
        ForgeSparkMod.startLoadingPlugin();
        if(ForgeSparkMod.getActiveLoadingPlugin() != null) {
            //Begin game_load profiler if enabled
            if(ForgeSparkMod.getActiveLoadingPlugin().getPlatform().getConfiguration().getOrSaveBoolean(StateEnum.GAME_LOAD.getConfigValue(), false)) {
                ForgeLoadingSamplerModule.startForState(ForgeSparkMod.getActiveLoadingPlugin(), ForgeSparkMod.getActiveLoadingPlugin().getPlatform(), StateEnum.GAME_LOAD);
            }
            //Begin coremod profiler if enabled
            if(ForgeSparkMod.getActiveLoadingPlugin().getPlatform().getConfiguration().getOrSaveBoolean(StateEnum.COREMOD.getConfigValue(), false)) {
                ForgeLoadingSamplerModule.startForState(ForgeSparkMod.getActiveLoadingPlugin(), ForgeSparkMod.getActiveLoadingPlugin().getPlatform(), StateEnum.COREMOD);
            }
            //Only bother injecting into LoadController if LoadProfiler is enabled
            Mixins.addConfiguration("mixins.spark.main.json");
        }
        Mixins.addConfiguration("mixins.spark.accessors.json");
    }
    @Override
    public String[] getASMTransformerClass()
    {
        return new String[0];
    }

    @Override
    public String getModContainerClass()
    {
        return null;
    }

    @Override
    public String getSetupClass()
    {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data)
    {
    }

    @Override
    public String getAccessTransformerClass()
    {
        return null;
    }
}
