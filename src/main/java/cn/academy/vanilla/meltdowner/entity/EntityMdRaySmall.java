/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.vanilla.meltdowner.entity;

import cn.academy.core.client.render.ray.RendererRayComposite;
import cn.academy.core.entity.EntityRayBase;
import cn.academy.vanilla.meltdowner.client.render.MdParticleFactory;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.annoreg.mc.RegEntity;
import cn.lambdalib.particle.Particle;
import cn.lambdalib.util.generic.MathUtils;
import cn.lambdalib.util.generic.RandUtils;
import cn.lambdalib.util.generic.VecUtils;
import cn.lambdalib.util.helper.Motion3D;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.world.World;

/**
 * @author WeAthFolD
 */
@Registrant
@SideOnly(Side.CLIENT)
@RegEntity(clientOnly = true)
@RegEntity.HasRender
public class EntityMdRaySmall extends EntityRayBase {
    
    @RegEntity.Render
    public static SmallMdRayRender renderer;

    public EntityMdRaySmall(World world) {
        super(world);
        this.blendInTime = 200;
        this.blendOutTime = 400;
        this.life = 14;
        this.length = 15.0;
    }
    
    @Override
    protected void onFirstUpdate() {
        super.onFirstUpdate();
        worldObj.playSound(posX, posY, posZ, "academy:md.ray_small", 0.5f, 1.0f, false);
    }
    
    @Override
    public void onUpdate() {
        super.onUpdate();
        Particle p = MdParticleFactory.INSTANCE.next(worldObj,
            new Motion3D(this, true).move(RandUtils.ranged(0, 10)).getPosVec(),
            VecUtils.vec(RandUtils.ranged(-.015, .015), RandUtils.ranged(-.015, .015), RandUtils.ranged(-.015, .015)));
        worldObj.spawnEntityInWorld(p);
    }
    
    @Override
    public double getWidth() {
        long dt = getDeltaTime();
        int blendTime = 500;

        if(dt > this.life * 50 - blendTime) {
            double timeFactor = MathUtils.clampd(0, 1, (double) (dt - (this.life * 50 - blendTime)) / blendTime);
            return 1 - timeFactor;
        }
        
        return 1.0;
    }
    
    public static class SmallMdRayRender extends RendererRayComposite {

        public SmallMdRayRender() {
            super("mdray_small");
            this.cylinderIn.width = 0.03;
            this.cylinderIn.color.setColor4i(216, 248, 216, 230);
            
            this.cylinderOut.width = 0.045;
            this.cylinderOut.color.setColor4i(106, 242, 106, 50);
            
            this.glow.width = 0.3;
            this.glow.color.a = 0.5;
        }
        
    }

}
