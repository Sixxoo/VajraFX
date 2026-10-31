package cz.sixxo.vajrafx;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

/** Tunable values, written to config/vajrafx.cfg so the look can be adjusted without recompiling. */
public final class VajraConfig {

    // Where the tip is on the Vajra texture (0..1 from the left / from the top)
    public static float tipU = 0.02F;
    public static float tipV = 0.02F;

    // How far in front of the camera the beam starts (blocks). Screen position comes from the tip.
    public static float tipDepth = 0.6F;

    // Used only if the hand renderer does not run: tip position on screen (0..1 of width / height)
    public static float fallbackTipX = 0.66F;
    public static float fallbackTipY = 0.52F;

    // One status line in chat after ~2 s of mining
    public static boolean debugChat = true;

    // Pose while mining (item-local space, after vanilla held-item transforms)
    public static float aimOffsetX = -0.10F;
    public static float aimOffsetY = 0.12F;
    public static float aimOffsetZ = 0.00F;
    public static float aimRotZ = 12.0F;
    public static float aimRotY = 0.0F;

    // Small kick when a block breaks with the beam
    public static float recoilDistance = 0.05F;
    public static float recoilAngle = 4.0F;

    // Beam (left click)
    public static boolean beamEnabled = true;
    public static float beamThickness = 2.5F;
    public static float beamOpacity = 1.0F;
    public static boolean beamHelix = true;

    // Pulse wave (right click on a block)
    public static boolean pulseEnabled = true;
    public static int pulseTicks = 6;
    public static float pulseRadius = 0.3F;
    public static float pulseOpacity = 0.2F;
    public static float pulseRecoilDistance = 0.08F;
    public static float pulseRecoilAngle = 6.0F;

    private VajraConfig() {}

    public static void load(File file) {
        Configuration c = new Configuration(file);
        String t = "tip", p = "pose", b = "beam", o = "orb";
        tipU = c.getFloat("tipU", t, tipU, 0, 1, "Tip position on the texture, 0 = left edge, 1 = right edge.");
        tipV = c.getFloat("tipV", t, tipV, 0, 1, "Tip position on the texture, 0 = top edge, 1 = bottom edge.");
        tipDepth = c.getFloat(
            "tipDepth",
            t,
            tipDepth,
            0.1F,
            3F,
            "How far in front of the camera the beam starts (blocks); smaller = thinner start.");
        fallbackTipX = c.getFloat(
            "fallbackTipX",
            t,
            fallbackTipX,
            0,
            1,
            "Only if the hand renderer does not run: tip on screen, 0 = left, 1 = right.");
        fallbackTipY = c.getFloat(
            "fallbackTipY",
            t,
            fallbackTipY,
            0,
            1,
            "Only if the hand renderer does not run: tip on screen, 0 = top, 1 = bottom.");
        debugChat = c.getBoolean(
            "debugChat",
            "debug",
            debugChat,
            "Print one status line in chat after about 2 seconds of mining.");

        aimOffsetX = c.getFloat("aimOffsetX", p, aimOffsetX, -2, 2, "Held pose while mining: X offset.");
        aimOffsetY = c.getFloat("aimOffsetY", p, aimOffsetY, -2, 2, "Held pose while mining: Y offset.");
        aimOffsetZ = c.getFloat("aimOffsetZ", p, aimOffsetZ, -2, 2, "Held pose while mining: Z offset.");
        aimRotZ = c.getFloat("aimRotZ", p, aimRotZ, -180, 180, "Held pose while mining: tilt (degrees).");
        aimRotY = c.getFloat("aimRotY", p, aimRotY, -180, 180, "Held pose while mining: turn (degrees).");
        recoilDistance = c.getFloat("recoilDistance", p, recoilDistance, 0, 1, "Kick distance when a block breaks.");
        recoilAngle = c.getFloat("recoilAngle", p, recoilAngle, 0, 45, "Kick angle when a block breaks (degrees).");

        beamEnabled = c.getBoolean("beamEnabled", b, beamEnabled, "Draw the energy beam while left-click mining.");
        beamThickness = c.getFloat(
            "beamThickness",
            b,
            beamThickness,
            0.3F,
            20F,
            "Beam core thickness at the tip, in screen pixels at a 240 px tall GUI.");
        beamHelix = c.getBoolean("beamHelix", b, beamHelix, "Two energy strands spiralling around the beam.");
        beamOpacity = c.getFloat(
            "beamOpacity",
            b,
            beamOpacity,
            0.05F,
            1F,
            "How solid the beam looks: lower = more see-through, 1 = full brightness.");

        String w = "pulse";
        pulseEnabled = c.getBoolean(
            "pulseEnabled",
            w,
            pulseEnabled,
            "Pulse wave when a right click reaches a block (fires the same frame you click).");
        pulseTicks = c
            .getInt("pulseTicks", w, pulseTicks, 1, 40, "Time for the wave to reach the block (1 tick = 0.05 s).");
        pulseRadius = c
            .getFloat("pulseRadius", w, pulseRadius, 0.1F, 5F, "Ring radius when it reaches the block (blocks).");
        pulseOpacity = c.getFloat(
            "pulseOpacity",
            w,
            pulseOpacity,
            0.05F,
            1F,
            "How solid the wave looks: lower = more see-through, 1 = full brightness.");
        pulseRecoilDistance = c
            .getFloat("pulseRecoilDistance", w, pulseRecoilDistance, 0, 1, "Kick distance when firing.");
        pulseRecoilAngle = c
            .getFloat("pulseRecoilAngle", w, pulseRecoilAngle, 0, 45, "Kick angle when firing (degrees).");

        // keys from older versions that no longer exist
        if (c.hasCategory("animation")) c.removeCategory(c.getCategory("animation"));
        if (c.hasCategory(o)) c.removeCategory(c.getCategory(o)); // the orb was replaced by the pulse wave
        for (String old : new String[] { "beamWidth", "beamSound", "beamSoundVolume", "beamCooldownSound", "beamSparks",
            "breakSparks" }) {
            c.getCategory(b)
                .remove(old); // sound settings left over from the test build with sound
        }
        if (c.hasChanged()) c.save();
    }
}
