package com.jeremyseq.inhabitants.particles;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import com.mojang.blaze3d.vertex.VertexConsumer;

import org.jetbrains.annotations.NotNull;

public class ImpalerHeadAttackParticle extends TextureSheetParticle {
    private final SpriteSet sprites;
    private final float yawRad;

    protected ImpalerHeadAttackParticle(
        ClientLevel level,
        double x, double y, double z,
        SpriteSet sprites,
        double xSpeed, double ySpeed, double zSpeed
    ) {
        super(level, x, y, z);
        this.sprites = sprites;
        this.yawRad = (float) Math.atan2(zSpeed, xSpeed) + (float) Math.PI / 2;
        
        this.xd = xSpeed;
        this.yd = ySpeed;
        this.zd = zSpeed;
        
        this.hasPhysics = false;
        this.lifetime = 5;
        this.quadSize = 0.5f;
        this.alpha = 1.0f;
        
        this.setSpriteFromAge(sprites);
    }

    @Override
    public @NotNull ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public int getLightColor(float partialTick) {
        return 220 | (220 << 16);
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTick) {
        Vec3 camPos = camera.getPosition();
        float relativeX = (float)(x - camPos.x());
        float relativeY = (float)(y - camPos.y()) + 0.01F;
        float relativeZ = (float)(z - camPos.z());

        float cosYaw = Mth.cos(this.yawRad);
        float sinYaw = Mth.sin(this.yawRad);
        float halfSize = quadSize * 0.5F;
        
        float rotX0 = -halfSize * cosYaw - (-halfSize) * sinYaw;
        float rotZ0 = -halfSize * sinYaw + (-halfSize) * cosYaw;

        float rotX1 = -halfSize * cosYaw - halfSize * sinYaw;
        float rotZ1 = -halfSize * sinYaw + halfSize * cosYaw;

        float rotX2 = halfSize * cosYaw - halfSize * sinYaw;
        float rotZ2 = halfSize * sinYaw + halfSize * cosYaw;

        float rotX3 = halfSize * cosYaw - (-halfSize) * sinYaw;
        float rotZ3 = halfSize * sinYaw + (-halfSize) * cosYaw;

        int light = getLightColor(partialTick);
        float minU = getU0(), minV = getV0();
        float maxU = getU1(), maxV = getV1();
        float particleAlpha = alpha;

        buffer.vertex(relativeX + rotX0, relativeY, relativeZ + rotZ0)
            .uv(minU, minV)
            .color(1, 1, 1, particleAlpha)
            .uv2(light)
            .endVertex();
        buffer.vertex(relativeX + rotX1, relativeY, relativeZ + rotZ1)
            .uv(minU, maxV)
            .color(1, 1, 1, particleAlpha)
            .uv2(light)
            .endVertex();
        buffer.vertex(relativeX + rotX2, relativeY, relativeZ + rotZ2)
            .uv(maxU, maxV)
            .color(1, 1, 1, particleAlpha)
            .uv2(light)
            .endVertex();
        buffer.vertex(relativeX + rotX3, relativeY, relativeZ + rotZ3)
            .uv(maxU, minV)
            .color(1, 1, 1, particleAlpha)
            .uv2(light)
            .endVertex();
    }

    @Override
    public void tick() {
        super.tick();
        this.setSpriteFromAge(sprites);
        
        this.alpha = 1.0F - (age / (float) lifetime);
    }

    public static class Factory implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        public Factory(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(
            @NotNull SimpleParticleType type,
            @NotNull ClientLevel level,
            double x, double y, double z,
            double xSpeed, double ySpeed, double zSpeed
        ) {
            return new ImpalerHeadAttackParticle(
                level, x, y, z, sprites, xSpeed, ySpeed, zSpeed
            );
        }
    }
}
