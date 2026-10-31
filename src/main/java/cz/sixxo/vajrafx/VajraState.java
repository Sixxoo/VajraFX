package cz.sixxo.vajrafx;

import java.lang.reflect.Field;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IIcon;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.client.IItemRenderer;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.client.event.TextureStitchEvent;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.ReflectionHelper;

/** Tracks what the player is doing with the Vajra and drives every part of the animation. */
public final class VajraState {

    public static final VajraState INSTANCE = new VajraState();

    static final int CYCLE = 8; // frames in the active texture loop (1 per tick)
    static final int IMPACT_TICKS = 5;

    Item vajra;
    final IIcon[] activeIcons = new IIcon[CYCLE];
    final IIcon[] chargeIcons = new IIcon[3];

    // --- left click: beam ---
    boolean attackHeld; // button held right now
    boolean engaged; // animation running (keeps going until the loop finishes)
    int animTick;
    float beam, prevBeam; // beam strength, fades out after release
    float pose, prevPose; // 0 = normal hold, 1 = aiming
    int recoil; // ticks left of a kick
    float recoilDist, recoilAng;

    // --- right click: pulse waves, one per click, fired the same frame ---
    static final class Pulse {

        double ox, oy, oz; // world origin (set on the first frame it is drawn, at the tip)
        boolean hasOrigin;
        final double tx, ty, tz; // world point it travels to
        final boolean block; // ends on a block (impact) or fades out in the air
        final float start; // client time it was fired

        Pulse(double tx, double ty, double tz, boolean block, float start) {
            this.tx = tx;
            this.ty = ty;
            this.tz = tz;
            this.block = block;
            this.start = start;
        }
    }

    final List<Pulse> pulses = new ArrayList<>();
    private boolean useWasDown;
    private float lastPulse = -100F;

    int clientTicks;
    float partialTicks;

    // Where the Vajra tip was drawn this frame (normalised screen coords, -1..1), set by VajraRenderer
    int frame, tipFrame = -1;
    float tipNdcX, tipNdcY;

    // World camera of this frame
    final FloatBuffer worldModelview = BufferUtils.createFloatBuffer(16);
    final FloatBuffer worldProjection = BufferUtils.createFloatBuffer(16);
    double camX, camY, camZ;
    boolean worldMatricesValid;

    private boolean hasTarget;
    private int tx, ty, tz;

    private Field fItemToRender, fEquipped, fPrevEquipped;
    private boolean reflectionFailed;
    private boolean loggedWorld, loggedFx;

    // Keeping our renderer in charge of the Vajra, and a one-off in-game status report
    VajraRenderer renderer;
    private boolean registeredInWorld, rendererOverridden;
    int fxFrame = -1;
    private int diagTicks;
    private boolean diagSent;

    private VajraState() {}

    /** Re-registers our renderer when joining a world, and again if another mod replaced it. */
    private void ensureRenderer() {
        if (renderer == null || vajra == null) return;
        IItemRenderer current = MinecraftForgeClient
            .getItemRenderer(new ItemStack(vajra), IItemRenderer.ItemRenderType.EQUIPPED_FIRST_PERSON);
        if (registeredInWorld && current == renderer) return;
        if (current != null && current != renderer) {
            rendererOverridden = true;
            FMLLog.info(
                "[VajraFX] Another renderer was set for the Vajra (%s), taking it back.",
                current.getClass()
                    .getName());
        }
        MinecraftForgeClient.registerItemRenderer(vajra, renderer);
        registeredInWorld = true;
    }

    /** One chat line after ~2 s of mining so problems can be spotted without digging through logs. */
    private void diagnose(EntityClientPlayerMP player) {
        if (diagSent || !VajraConfig.debugChat) return;
        if (++diagTicks < 40) return;
        diagSent = true;
        boolean tipOk = frame - tipFrame <= 2;
        boolean fxOk = frame - fxFrame <= 2;
        String msg = String.format(
            "[VajraFX] %s hand renderer: %s%s, world effects: %s, world hook: %s, tip: %.2f %.2f (disable this in config: debugChat)",
            Tags.VERSION,
            tipOk ? "OK" : "NOT RUNNING (using fallback tip)",
            rendererOverridden ? " (was overridden)" : "",
            fxOk ? "drawn" : "NOT DRAWN",
            loggedWorld ? "OK" : "no",
            tipNdcX,
            tipNdcY);
        player.addChatMessage(new ChatComponentText(msg));
        FMLLog.info("%s", msg);
    }

    boolean isVajra(ItemStack stack) {
        return vajra != null && stack != null && stack.getItem() == vajra;
    }

    static float lerp(float prev, float cur, float t) {
        return prev + (cur - prev) * t;
    }

    /** Client time in ticks, smooth between ticks. */
    float now() {
        return clientTicks + partialTicks;
    }

    float beamAmount() {
        return lerp(prevBeam, beam, partialTicks);
    }

    float poseAmount() {
        return lerp(prevPose, pose, partialTicks);
    }

    float recoilAmount() {
        if (recoil <= 0) return 0;
        float r = (recoil - partialTicks) / 3F;
        return r < 0 ? 0 : r;
    }

    int pulseTicks() {
        return Math.max(1, VajraConfig.pulseTicks);
    }

    /** Texture the held Vajra should use right now (null = its normal icon). */
    IIcon currentIcon() {
        if (now() - lastPulse < 2F) return chargeIcons[2]; // crystal flashes as a pulse leaves
        if (engaged || beam > 0.5F) return activeIcons[animTick % CYCLE];
        return null;
    }

    // ------------------------------------------------------------------ events

    @SubscribeEvent
    public void onTextureStitch(TextureStitchEvent.Pre event) {
        if (event.map.getTextureType() != 1) return; // item atlas only
        for (int i = 0; i < CYCLE; i++) activeIcons[i] = event.map.registerIcon(VajraFX.MODID + ":vajra_active_" + i);
        for (int i = 0; i < 3; i++) chargeIcons[i] = event.map.registerIcon(VajraFX.MODID + ":vajra_charge_" + i);
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        clientTicks++;
        Minecraft mc = Minecraft.getMinecraft();
        EntityClientPlayerMP player = mc.thePlayer;
        if (player == null || vajra == null) {
            attackHeld = engaged = false;
            beam = prevBeam = pose = prevPose = 0;
            pulses.clear();
            registeredInWorld = false;
            return;
        }
        ensureRenderer();
        boolean holding = isVajra(player.getHeldItem());
        boolean noGui = mc.currentScreen == null;
        MovingObjectPosition hit = mc.objectMouseOver;
        boolean onBlock = hit != null && hit.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK;

        // ---------- left click: beam, runs as long as the button is held, then finishes its loop
        attackHeld = holding && noGui && mc.gameSettings.keyBindAttack.getIsKeyPressed();
        if (attackHeld) {
            engaged = true;
            diagnose(player);
        } else if (engaged && (animTick % CYCLE == 0 || !holding)) engaged = false;
        if (engaged) animTick++;

        prevBeam = beam;
        beam = approach(beam, engaged && holding ? 1F : 0F, 0.5F, 0.25F);
        prevPose = pose;
        pose = approach(pose, engaged && holding ? 1F : 0F, 0.34F, 0.2F);
        if (recoil > 0) recoil--;

        if (attackHeld && onBlock) {
            // The Vajra breaks blocks instantly: the block we aimed at last tick turning into air = a break.
            if (hasTarget && player.worldObj.isAirBlock(tx, ty, tz)
                && !(tx == hit.blockX && ty == hit.blockY && tz == hit.blockZ)) {
                kick(VajraConfig.recoilDistance, VajraConfig.recoilAngle);
            }
            tx = hit.blockX;
            ty = hit.blockY;
            tz = hit.blockZ;
            hasTarget = true;
        } else if (!attackHeld) {
            hasTarget = false;
        }

        // ---------- pulse waves: drop finished ones
        float t = clientTicks;
        for (Iterator<Pulse> it = pulses.iterator(); it.hasNext();) {
            Pulse p = it.next();
            float age = t - p.start;
            if (age > pulseTicks() + IMPACT_TICKS + 1) it.remove();
        }
        if (!holding) pulses.clear();
    }

    private static float approach(float v, float target, float up, float down) {
        if (v < target) return Math.min(target, v + up);
        return Math.max(target, v - down);
    }

    private void kick(float dist, float ang) {
        recoil = 3;
        recoilDist = dist;
        recoilAng = ang;
    }

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        partialTicks = event.renderTickTime;
        frame++;
        worldMatricesValid = false;
        Minecraft mc = Minecraft.getMinecraft();
        EntityClientPlayerMP player = mc.thePlayer;
        if (player == null) return;
        ItemStack held = player.getHeldItem();
        boolean holding = isVajra(held);

        // Right click is checked every frame, so the pulse leaves the same frame you click.
        boolean useDown = holding && mc.currentScreen == null && mc.gameSettings.keyBindUseItem.getIsKeyPressed();
        if (useDown && !useWasDown && VajraConfig.pulseEnabled) firePulse(mc);
        useWasDown = useDown;

        if (!holding) return;
        if (engaged || now() - lastPulse < 6F) {
            // Our animation replaces the vanilla "hammering" arm swing.
            player.isSwingInProgress = false;
            player.swingProgressInt = 0;
            player.swingProgress = 0;
            player.prevSwingProgress = 0;
        }
        keepEquipped(mc, held);
    }

    /** Fires only when the right click really reaches a block; the wave travels to the exact spot clicked. */
    private void firePulse(Minecraft mc) {
        MovingObjectPosition hit = mc.objectMouseOver;
        if (hit == null || hit.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK || hit.hitVec == null) return;
        pulses.add(new Pulse(hit.hitVec.xCoord, hit.hitVec.yCoord, hit.hitVec.zCoord, true, now()));
        lastPulse = now();
        kick(VajraConfig.pulseRecoilDistance, VajraConfig.pulseRecoilAngle);
    }

    /**
     * Every block broken drains EU, the server re-sends the stack and vanilla plays the
     * lower-and-raise "re-equip" dip. Keep the Vajra steady in hand instead.
     */
    private void keepEquipped(Minecraft mc, ItemStack held) {
        if (reflectionFailed) return;
        try {
            if (fItemToRender == null) {
                fItemToRender = ReflectionHelper.findField(ItemRenderer.class, "itemToRender", "field_78453_b");
                fEquipped = ReflectionHelper.findField(ItemRenderer.class, "equippedProgress", "field_78454_c");
                fPrevEquipped = ReflectionHelper.findField(ItemRenderer.class, "prevEquippedProgress", "field_78451_d");
            }
            ItemRenderer ir = mc.entityRenderer.itemRenderer;
            ItemStack shown = (ItemStack) fItemToRender.get(ir);
            if (shown != held && isVajra(shown)) {
                fItemToRender.set(ir, held);
                fEquipped.setFloat(ir, 1.0F);
                fPrevEquipped.setFloat(ir, 1.0F);
            }
        } catch (Exception e) {
            reflectionFailed = true;
            FMLLog.warning("[VajraFX] Could not stop the re-equip dip: %s", e);
        }
    }

    /** Captures this frame's world camera, then draws the effects in the world pass. */
    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        worldModelview.clear();
        worldProjection.clear();
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, worldModelview);
        GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, worldProjection);
        camX = RenderManager.renderPosX;
        camY = RenderManager.renderPosY;
        camZ = RenderManager.renderPosZ;
        worldMatricesValid = true;
        if (!loggedWorld) {
            loggedWorld = true;
            FMLLog.info("[VajraFX] World camera hook active.");
        }
        drawWorldFx();
    }

    private float[] toEye(double x, double y, double z) {
        return FxMath.transform(worldModelview, (float) (x - camX), (float) (y - camY), (float) (z - camZ));
    }

    /**
     * Beam and pulse waves are drawn in the world pass (the layer GTNH reliably shows them in). The start point
     * is the Vajra tip: its screen position, recorded by the hand renderer, is turned back into a point just
     * in front of the camera, so the effects leave the screen exactly where the tip is drawn.
     */
    private void drawWorldFx() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.gameSettings.thirdPersonView != 0) return;
        if (!isVajra(mc.thePlayer.getHeldItem())) return;
        float b = beamAmount();
        boolean beamOn = VajraConfig.beamEnabled && b > 0.01F;
        if (!beamOn && pulses.isEmpty()) return;

        float nx, ny;
        if (frame - tipFrame <= 2) { // tip recorded by our hand renderer (previous frame)
            nx = tipNdcX;
            ny = tipNdcY;
        } else { // hand renderer did not run: configured screen position
            nx = VajraConfig.fallbackTipX * 2F - 1F;
            ny = 1F - VajraConfig.fallbackTipY * 2F;
        }
        float[] tip = FxMath.unproject(worldProjection, nx, ny, VajraConfig.tipDepth);

        if (beamOn) {
            MovingObjectPosition hit = mc.objectMouseOver;
            boolean onBlock = hit != null && hit.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK
                && hit.hitVec != null;
            float[] end;
            if (onBlock) {
                end = toEye(hit.hitVec.xCoord, hit.hitVec.yCoord, hit.hitVec.zCoord);
            } else {
                float reach = mc.playerController != null ? mc.playerController.getBlockReachDistance() : 5F;
                end = FxMath.unproject(worldProjection, 0F, 0F, reach);
            }
            FxRenderer.beam(mc, tip, end, b, onBlock);
        }

        float t = now();
        for (Pulse p : pulses) {
            if (!p.hasOrigin) { // anchor the wave in the world at the tip, the frame it is fired
                float[] rel = FxMath.eyeToRelative(worldModelview, tip);
                p.ox = camX + rel[0];
                p.oy = camY + rel[1];
                p.oz = camZ + rel[2];
                p.hasOrigin = true;
            }
            float[] origin = toEye(p.ox, p.oy, p.oz);
            float[] target = toEye(p.tx, p.ty, p.tz);
            FxRenderer.pulse(mc, origin, target, tip, t - p.start, pulseTicks(), p.block);
        }

        fxFrame = frame;
        if (!loggedFx) {
            loggedFx = true;
            FMLLog.info("[VajraFX] Drawing effects in the world pass, tip at %.2f, %.2f", nx, ny);
        }
    }
}
