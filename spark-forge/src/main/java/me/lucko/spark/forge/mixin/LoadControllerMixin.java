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
import net.minecraftforge.fml.common.LoadController;
import net.minecraftforge.fml.common.event.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin for profiling game loading based partially on https://github.com/LoliKingdom/LoliASM/blob/master/src/main/java/zone/rong/loliasm/common/internal/mixins/LoadControllerMixin.java
 */
@Mixin(LoadController.class)
public abstract class LoadControllerMixin {

    @Inject(
            method = "propogateStateMessage",
            at = @At("HEAD"),
            remap = false
    )
    public void spark_loadController_propogateStateMessage_head(FMLEvent stateEvent, CallbackInfo ci) {
        if(ForgeSparkMod.getActiveLoadingPlugin() != null) {
            StateEnum state = StateEnum.getState(stateEvent);
            if(!state.equals(StateEnum.NONE)) {
                //Stop coremod profiler manually
                if(state.equals(StateEnum.CONSTRUCTION) && ForgeSparkMod.getActiveLoadingPlugin().getPlatform().getConfiguration().getBoolean(StateEnum.COREMOD.getConfigValue(), false)) {
                    ForgeLoadingSamplerModule.stopForState(ForgeSparkMod.getActiveLoadingPlugin(), ForgeSparkMod.getActiveLoadingPlugin().getPlatform(), StateEnum.COREMOD);
                }
                //Stop game_load profiler manually
                if(state.equals(StateEnum.COMPLETE) && ForgeSparkMod.getActiveLoadingPlugin().getPlatform().getConfiguration().getBoolean(StateEnum.GAME_LOAD.getConfigValue(), false)) {
                    ForgeLoadingSamplerModule.stopForState(ForgeSparkMod.getActiveLoadingPlugin(), ForgeSparkMod.getActiveLoadingPlugin().getPlatform(), StateEnum.GAME_LOAD);
                }
                //Start server_load profiler manually
                if(state.equals(StateEnum.SERVER_ABOUT_TO_START) && ForgeSparkMod.getActiveLoadingPlugin().getPlatform().getConfiguration().getOrSaveBoolean(StateEnum.SERVER_LOAD.getConfigValue(), false)) {
                    ForgeLoadingSamplerModule.startForState(ForgeSparkMod.getActiveLoadingPlugin(), ForgeSparkMod.getActiveLoadingPlugin().getPlatform(), StateEnum.SERVER_LOAD);
                }
                //Begin profiler if enabled
                if(ForgeSparkMod.getActiveLoadingPlugin().getPlatform().getConfiguration().getOrSaveBoolean(state.getConfigValue(), false)) {
                    ForgeLoadingSamplerModule.startForState(ForgeSparkMod.getActiveLoadingPlugin(), ForgeSparkMod.getActiveLoadingPlugin().getPlatform(), state);
                }
            }
        }
    }

    @Inject(
            method = "propogateStateMessage",
            at = @At("RETURN"),
            remap = false
    )
    public void spark_loadController_propogateStateMessage_return(FMLEvent stateEvent, CallbackInfo ci) {
        if(ForgeSparkMod.getActiveLoadingPlugin() != null) {
            StateEnum state = StateEnum.getState(stateEvent);
            if(!state.equals(StateEnum.NONE)) {
                //Stop profiler if enabled
                if(ForgeSparkMod.getActiveLoadingPlugin().getPlatform().getConfiguration().getBoolean(state.getConfigValue(), false)) {
                    ForgeLoadingSamplerModule.stopForState(ForgeSparkMod.getActiveLoadingPlugin(), ForgeSparkMod.getActiveLoadingPlugin().getPlatform(), state);
                }
                //End loader plugin on client when load complete, or server when it is started
                if(state.equals(StateEnum.SERVER_STARTED)) {
                    //Stop server_load profiler if enabled
                    if(ForgeSparkMod.getActiveLoadingPlugin().getPlatform().getConfiguration().getBoolean(StateEnum.SERVER_LOAD.getConfigValue(), false)) {
                        ForgeLoadingSamplerModule.stopForState(ForgeSparkMod.getActiveLoadingPlugin(), ForgeSparkMod.getActiveLoadingPlugin().getPlatform(), StateEnum.SERVER_LOAD);
                    }
                    ForgeSparkMod.endLoadingPlugin();
                }
            }
        }
    }
}