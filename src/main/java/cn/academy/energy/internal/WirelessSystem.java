/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.lambdacraft.cn/
 *
 * This project is open-source, and it is distributed under 
 * the terms of GNU General Public License. You can modify
 * and distribute freely as long as you follow the license.
 * 本项目是一个开源项目，且遵循GNU通用公共授权协议。
 * 在遵照该协议的情况下，您可以自由传播和修改。
 * http://www.gnu.org/licenses/gpl.html
 */
package cn.academy.energy.internal;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import cn.academy.energy.api.event.LinkUserEvent;
import cn.academy.energy.api.event.UnlinkUserEvent;
import cn.academy.energy.api.event.WirelessUserEvent.UserType;
import cn.annoreg.mc.RegEventHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.ServerTickEvent;

/**
 * @author WeathFolD
 */
public class WirelessSystem {
    
    @RegEventHandler
    public static WirelessSystem instance = new WirelessSystem();
    
    Map<World, WiWorldData> table = new HashMap();

    public WirelessSystem() {}
    
    @SubscribeEvent
    public void onServerTick(ServerTickEvent event) {
        for(WiWorldData data : table.values()) {
            data.tick();
        }
    }
    
    @SubscribeEvent
    public void linkUser(LinkUserEvent event) {
        if(event.type == UserType.GENERATOR) {
            getDataFor(event.getWorld()).linkGenerator(
                new Coord(event.tile, BlockType.GENERATOR), 
                new Coord(event.tile, BlockType.NODE));
        } else if(event.type == UserType.RECEIVER) {
            getDataFor(event.getWorld()).linkReceiver(
                    new Coord(event.tile, BlockType.GENERATOR), 
                    new Coord(event.tile, BlockType.NODE));
        }
    }
    
    @SubscribeEvent
    public void unlinkUser(UnlinkUserEvent event) {
        if(event.type == UserType.GENERATOR) {
            getDataFor(event.getWorld()).unlinkGenerator(new Coord(event.tile, BlockType.GENERATOR));
        } else if(event.type == UserType.RECEIVER) {
            getDataFor(event.getWorld()).unlinkGenerator(new Coord(event.tile, BlockType.RECEIVER));
        }
    }
    
    private WiWorldData getDataFor(World world) {
        WiWorldData ret = table.get(world);
        if(ret == null) {
            ret = new WiWorldData(world);
            table.put(world, ret);
        }
        
        return ret;
    }

}