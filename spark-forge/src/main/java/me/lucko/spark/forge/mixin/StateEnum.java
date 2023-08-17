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

import net.minecraftforge.fml.common.event.*;

public enum StateEnum {
    GAME_LOAD("game_load"),
    SERVER_LOAD("server_load"),
    COREMOD("coremod"),
    CONSTRUCTION("construction"),
    PRE_INIT("pre_init"),
    INIT("init"),
    POST_INIT("post_init"),
    COMPLETE("complete"),
    SERVER_ABOUT_TO_START("server_about_to_start"),
    SERVER_STARTING("server_starting"),
    SERVER_STARTED("server_started"),
    MOD_ID_MAPPING("mod_id_mapping"),
    INTER_MOD_COMMS("inter_mod_comms"),
    FINGERPRINT_VIOLATION("fingerprint_violation"),
    MOD_DISABLED("mod_disabled"),
    NONE("none");

    private final String value;

    StateEnum(String string) { this.value = string; }

    public String getConfigValue() { return "loadingProfiler_samplerEnabled_" + this.value; }
    public String getValue() { return this.value; }

    //No J17 pattern matching :(
    public static StateEnum getState(FMLEvent stateEvent) {
        //FMLStateEvent
        if(stateEvent instanceof FMLConstructionEvent) {
            return CONSTRUCTION;
        }
        if(stateEvent instanceof FMLPreInitializationEvent) {
            return PRE_INIT;
        }
        if(stateEvent instanceof FMLInitializationEvent) {
            return INIT;
        }
        if(stateEvent instanceof FMLPostInitializationEvent) {
            return POST_INIT;
        }
        if(stateEvent instanceof FMLLoadCompleteEvent) {
            return COMPLETE;
        }
        if(stateEvent instanceof FMLServerAboutToStartEvent) {
            return SERVER_ABOUT_TO_START;
        }
        if(stateEvent instanceof FMLServerStartingEvent) {
            return SERVER_STARTING;
        }
        if(stateEvent instanceof FMLServerStartedEvent) {
            return SERVER_STARTED;
        }
        //Non-FMLStateEvent
        if(stateEvent instanceof FMLModIdMappingEvent) {
            return MOD_ID_MAPPING;
        }
        if(stateEvent instanceof FMLInterModComms.IMCEvent) {
            return INTER_MOD_COMMS;
        }
        if(stateEvent instanceof FMLFingerprintViolationEvent) {
            return FINGERPRINT_VIOLATION;
        }
        if(stateEvent instanceof FMLModDisabledEvent) {
            return MOD_DISABLED;
        }
        //None
        return NONE;
    }
}