package cz.sixxo.vajrafx;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

/**
 * Pixel-art energy effects drawn in the world pass, in eye space (camera at the origin, looking down -Z).
 * The start point is the Vajra tip, turned into a point just in front of the camera.
 */
final class FxRenderer {

    private static final ResourceLocation CORE = tex("beam_core"), GLOW = tex("beam_glow"), FLARE = tex("flare"),
        RING = tex("ring");

    private static float savedLightX, savedLightY;

    private FxRenderer() {}

    private static ResourceLocation tex(String n) {
        return new ResourceLocation(VajraFX.MODID, "textures/fx/" + n + ".png");
    }

    private static float time(Minecraft mc) {
        return (mc.theWorld != null ? mc.theWorld.getTotalWorldTime() % 100000L : 0) + VajraState.INSTANCE.partialTicks;
    }

    /** Beam width in eye units, from the config's screen-pixel thickness. */
    private static float width() {
        return VajraConfig.beamThickness * 0.014F * VajraConfig.tipDepth / 0.6F;
    }

    // ------------------------------------------------------------------ GL state

    private static void begin() {
        GL11.glPushAttrib(
            GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT
                | GL11.GL_DEPTH_BUFFER_BIT
                | GL11.GL_CURRENT_BIT
                | GL11.GL_LIGHTING_BIT
                | GL11.GL_FOG_BIT);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_FOG);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(false);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE); // additive glow
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        savedLightX = OpenGlHelper.lastBrightnessX;
        savedLightY = OpenGlHelper.lastBrightnessY;
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240F, 240F); // full bright in caves
    }

    private static void end() {
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, savedLightX, savedLightY);
        GL11.glPopMatrix();
        GL11.glDepthMask(true);
        GL11.glPopAttrib();
        GL11.glColor4f(1F, 1F, 1F, 1F);
    }

    private static void bind(Minecraft mc, ResourceLocation r) {
        mc.getTextureManager()
            .bindTexture(r);
    }

    // ------------------------------------------------------------------ beam

    static void beam(Minecraft mc, float[] s, float[] e, float amount, boolean onBlock) {
        float[] d = FxMath.sub(e, s);
        float len = FxMath.len(d);
        if (len < 1e-3F) return;
        float[] mid = FxMath.lerp(s, e, 0.5F);
        float[] side = FxMath.norm(FxMath.cross(d, mid)); // faces the camera
        float[] perp = FxMath.norm(FxMath.cross(d, side));
        float t = time(mc);
        float flick = 0.88F + 0.12F * (float) Math.sin(t * 3.1) + 0.06F * (float) Math.sin(t * 7.7);
        float w = width() * amount * flick;
        float a = amount * VajraConfig.beamOpacity; // brightness / see-through, from the config

        begin();
        try {
            bind(mc, GLOW); // outer glow
            ribbon(s, d, side, len, w * 3.2F, -t * 0.15F, 0.55F * a);
            bind(mc, CORE); // core, energy flowing toward the block
            ribbon(s, d, side, len, w, -t * 0.6F, a);

            if (VajraConfig.beamHelix) {
                GL11.glDisable(GL11.GL_TEXTURE_2D);
                helix(s, d, side, perp, len, w, t, 0F, 0.55F, 0.85F, 1F, 0.85F * a);
                helix(s, d, side, perp, len, w, t, (float) Math.PI, 0.3F, 0.6F, 1F, 0.75F * a);
                GL11.glEnable(GL11.GL_TEXTURE_2D);
            }
            if (onBlock) { // glow where the beam hits
                float pulse = 0.85F + 0.15F * (float) Math.sin(t * 2.3);
                bind(mc, FLARE);
                quad(e, 0.32F * pulse * amount, t * 9F, 1F, 1F, 1F, a);
                bind(mc, RING);
                quad(e, (0.18F + 0.06F * (float) Math.sin(t * 1.3)) * amount, 0, 0.6F, 0.85F, 1F, 0.6F * a);
            }
            bind(mc, FLARE); // flare on the tip
            quad(s, w * 3.5F * (0.85F + 0.2F * (float) Math.sin(t * 2.9)), t * 12F, 1F, 1F, 1F, a);
        } finally {
            end();
        }
    }

    /** Camera-facing strip from s along d, narrow at the tip; texture runs along the length and scrolls. */
    private static void ribbon(float[] s, float[] d, float[] side, float len, float w, float vOff, float a) {
        Tessellator tes = Tessellator.instance;
        tes.startDrawing(GL11.GL_QUAD_STRIP);
        tes.setColorRGBA_F(1F, 1F, 1F, a);
        float tile = Math.max(1e-3F, w * 4F); // texture is twice as long as it is wide
        int segs = 16;
        for (int i = 0; i <= segs; i++) {
            float k = (float) i / segs;
            float taper = Math.min(1F, k * len / (w * 3F));
            float ww = w * (0.35F + 0.65F * taper);
            float x = s[0] + d[0] * k, y = s[1] + d[1] * k, z = s[2] + d[2] * k;
            float v = k * len / tile + vOff;
            tes.addVertexWithUV(x + side[0] * ww, y + side[1] * ww, z + side[2] * ww, 0, v);
            tes.addVertexWithUV(x - side[0] * ww, y - side[1] * ww, z - side[2] * ww, 1, v);
        }
        tes.draw();
    }

    private static void helix(float[] s, float[] d, float[] side, float[] perp, float len, float w, float t,
        float phase, float r, float g, float b, float a) {
        Tessellator tes = Tessellator.instance;
        tes.startDrawing(GL11.GL_QUAD_STRIP);
        tes.setColorRGBA_F(r, g, b, a);
        int segs = Math.max(24, Math.min(200, (int) (len / (w * 1.5F))));
        float radius = w * 2.2F, turn = (float) (2 * Math.PI / (w * 12F)), thick = w * 0.22F;
        for (int i = 0; i <= segs; i++) {
            float k = (float) i / segs;
            float env = Math.min(1F, k * len / (w * 6F)) * Math.min(1F, (1F - k) * len / (w * 6F));
            float ang = k * len * turn - t * 0.9F + phase;
            float c = (float) Math.cos(ang) * radius * env, sn = (float) Math.sin(ang) * radius * env;
            float x = s[0] + d[0] * k + side[0] * c + perp[0] * sn;
            float y = s[1] + d[1] * k + side[1] * c + perp[1] * sn;
            float z = s[2] + d[2] * k + side[2] * c + perp[2] * sn;
            tes.addVertex(x + side[0] * thick, y + side[1] * thick, z + side[2] * thick);
            tes.addVertex(x - side[0] * thick, y - side[1] * thick, z - side[2] * thick);
        }
        tes.draw();
    }

    // ------------------------------------------------------------------ pulse wave (right click)

    /**
     * One pulse wave: pixel rings that leave the tip, travel to the target and widen on the way,
     * facing along the travel direction so you look straight through them. Ends with a flash on a block.
     *
     * @param origin where the wave left the tip (eye space, anchored in the world)
     * @param tip    current tip position, for the muzzle flash
     * @param age    ticks since the click
     */
    static void pulse(Minecraft mc, float[] origin, float[] target, float[] tip, float age, int travel, boolean block) {
        float time = time(mc);
        float near = VajraConfig.tipDepth / 0.6F;
        begin();
        try {
            if (age < 3F) { // muzzle flash, starts the frame you click
                float f = 1F - age / 3F;
                bind(mc, FLARE);
                quad(tip, 0.28F * near * f, time * 15F, 1F, 1F, 1F, f);
            }
            float[] d = FxMath.sub(target, origin);
            float len = FxMath.len(d);
            if (len < 1e-3F) return;
            float[] dir = { d[0] / len, d[1] / len, d[2] / len };
            float[] u = FxMath.cross(dir, new float[] { 0F, 1F, 0F });
            u = FxMath.len(u) < 1e-3F ? new float[] { 1F, 0F, 0F } : FxMath.norm(u);
            float[] v = FxMath.cross(dir, u);

            float o = VajraConfig.pulseOpacity; // see-through: the world stays visible behind the wave
            if (age < travel) {
                float q = age / travel;
                float e = 1F - (1F - q) * (1F - q); // fast out of the tip, slowing down
                float fade = block ? 1F : 1F - q; // fades out in the air, stays bright until it hits a block
                float r0 = 0.05F * near, r1 = VajraConfig.pulseRadius;
                bind(mc, RING);
                for (int k = 2; k >= 0; k--) { // leading ring plus two trailing echoes
                    float qk = e - k * 0.13F;
                    if (qk <= 0F) continue;
                    float[] p = FxMath.lerp(origin, target, qk);
                    float radius = r0 + (r1 - r0) * qk;
                    float a = fade * (1F - k * 0.4F) * o;
                    ring(p, u, v, radius * 1.3F, 0.3F, 0.6F, 1F, a * 0.25F); // faint outer glow
                    ring(p, u, v, radius, 0.75F, 0.92F, 1F, a);
                }
                bind(mc, FLARE); // small faint spark leading the wave, keeps the centre clear
                float[] front = FxMath.lerp(origin, target, e);
                quad(front, (r0 + (r1 - r0) * e) * 0.2F, time * 12F, 0.7F, 0.9F, 1F, 0.25F * fade * o);
            } else if (block) {
                float q = Math.min(1F, (age - travel) / VajraState.IMPACT_TICKS); // flash and shockwave
                float a = (1F - q) * o;
                bind(mc, RING);
                quad(target, 0.3F + 1.4F * q, 0, 0.7F, 0.9F, 1F, a);
                ring(target, u, v, VajraConfig.pulseRadius * (1F + 0.8F * q), 0.6F, 0.85F, 1F, a);
                bind(mc, FLARE);
                quad(target, 0.6F * (1F - q), time * 10F, 1F, 1F, 1F, a);
            }
        } finally {
            end();
        }
    }

    /** A square sprite lying in the plane spanned by u and v (used for rings facing the travel direction). */
    private static void ring(float[] p, float[] u, float[] v, float size, float r, float g, float b, float a) {
        if (size <= 0 || a <= 0) return;
        Tessellator tes = Tessellator.instance;
        tes.startDrawingQuads();
        tes.setColorRGBA_F(r, g, b, a);
        float ux = u[0] * size, uy = u[1] * size, uz = u[2] * size;
        float vx = v[0] * size, vy = v[1] * size, vz = v[2] * size;
        tes.addVertexWithUV(p[0] - ux - vx, p[1] - uy - vy, p[2] - uz - vz, 0, 1);
        tes.addVertexWithUV(p[0] + ux - vx, p[1] + uy - vy, p[2] + uz - vz, 1, 1);
        tes.addVertexWithUV(p[0] + ux + vx, p[1] + uy + vy, p[2] + uz + vz, 1, 0);
        tes.addVertexWithUV(p[0] - ux + vx, p[1] - uy + vy, p[2] - uz + vz, 0, 0);
        tes.draw();
    }

    // ------------------------------------------------------------------ sprites

    private static void quad(float[] p, float size, float angleDeg, float r, float g, float b, float a) {
        if (p == null || size <= 0 || a <= 0) return;
        Tessellator tes = Tessellator.instance;
        tes.startDrawingQuads();
        tes.setColorRGBA_F(r, g, b, a);
        billboard(tes, p, size, angleDeg);
        tes.draw();
    }

    /** Adds one camera-facing square (eye space: x right, y up) to an open quad batch. */
    private static void billboard(Tessellator tes, float[] p, float size, float angleDeg) {
        double ang = Math.toRadians(angleDeg);
        float c = (float) Math.cos(ang) * size, s = (float) Math.sin(ang) * size;
        tes.addVertexWithUV(p[0] - c + s, p[1] - s - c, p[2], 0, 1);
        tes.addVertexWithUV(p[0] + c + s, p[1] + s - c, p[2], 1, 1);
        tes.addVertexWithUV(p[0] + c - s, p[1] + s + c, p[2], 1, 0);
        tes.addVertexWithUV(p[0] - c - s, p[1] - s + c, p[2], 0, 0);
    }

    private static float frac(float v) {
        return v - (float) Math.floor(v);
    }
}
