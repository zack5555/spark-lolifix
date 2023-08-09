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

package me.lucko.spark.forge;

import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongSet;

import me.lucko.spark.common.platform.world.AbstractChunkInfo;
import me.lucko.spark.common.platform.world.CountMap;
import me.lucko.spark.common.platform.world.WorldInfoProvider;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ClassInheritanceMultiMap;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.gen.ChunkProviderServer;

import java.util.*;

public abstract class ForgeWorldInfoProvider implements WorldInfoProvider {

    protected List<ForgeChunkInfo> getChunksFromCache(ChunkProviderServer provider) {
        LongSet loadedChunks = provider.loadedChunks.keySet();
        List<ForgeChunkInfo> list = new ArrayList<>(loadedChunks.size());

        for (LongIterator iterator = loadedChunks.iterator(); iterator.hasNext(); ) {
            long chunk = iterator.nextLong();
            ClassInheritanceMultiMap<Entity>[] sections = provider.loadedChunks.get(chunk).getEntityLists();
            ChunkPos pos = provider.loadedChunks.get(chunk).getPos();

            list.add(new ForgeChunkInfo(pos, sections));
        }

        return list;
    }

    public static final class Server extends ForgeWorldInfoProvider {
        private final MinecraftServer server;

        public Server(MinecraftServer server) {
            this.server = server;
        }

        @Override
        public CountsResult pollCounts() {
            int players = this.server.getCurrentPlayerCount();
            int entities = 0;
            int chunks = 0;

            for(WorldServer level : this.server.worlds) {
                entities += level.loadedEntityList.size();
                chunks += level.getChunkProvider().getLoadedChunkCount();
            }

            return new CountsResult(players, entities, -1, chunks);
        }

        @Override
        public ChunksResult<ForgeChunkInfo> pollChunks() {
            ChunksResult<ForgeChunkInfo> data = new ChunksResult<>();

            for(WorldServer level : this.server.worlds) {
                List<ForgeChunkInfo> list = getChunksFromCache(level.getChunkProvider());
                data.put(level.provider.getDimensionType().getName(), list);
            }

            return data;
        }
    }

    public static final class Client extends ForgeWorldInfoProvider {
        private final Minecraft client;

        public Client(Minecraft client) {
            this.client = client;
        }

        @Override
        public CountsResult pollCounts() {
            WorldClient level = this.client.world;
            if (level == null) {
                return null;
            }

            int entities = level.loadedEntityList.size();
            //int chunks = level.getChunkProvider().getLoadedChunkCount();
            int chunks = 0;

            return new CountsResult(-1, entities, -1, chunks);
        }

        @Override
        public ChunksResult<ForgeChunkInfo> pollChunks() {
            ChunksResult<ForgeChunkInfo> data = new ChunksResult<>();
/*
            ClientLevel level = this.client.level;
            if(level == null) return null;

            List<ForgeChunkInfo> list = getChunksFromCache(cache);
            data.put(level.dimension().location().getPath(), list);

 */
            return data;
        }
    }

    static final class ForgeChunkInfo extends AbstractChunkInfo<Entity> {
        private final CountMap<Entity> entityCounts;

        ForgeChunkInfo(ChunkPos chunkPos, ClassInheritanceMultiMap<Entity>[] entities) {
            super(chunkPos.x, chunkPos.z);

            this.entityCounts = new CountMap.Simple<>(new HashMap<>());
            for(ClassInheritanceMultiMap<Entity> map : entities) {
                map.forEach(this.entityCounts::increment);
            }
        }

        @Override
        public CountMap<Entity> getEntityCounts() {
            return this.entityCounts;
        }

        @Override
        public String entityTypeName(Entity type) {
            return nonNullName(EntityList.getKey(type), type);
        }

        private static String nonNullName(ResourceLocation res, Entity ent) {
            return (res != null) ? res.toString() : ent.getClass().getName();
        }
    }
}