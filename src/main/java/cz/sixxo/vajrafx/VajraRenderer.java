package cz.sixxo.vajrafx;

import java.nio.FloatBuffer;

import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraftforge.client.IItemRenderer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import cpw.mods.fml.common.FMLLog;

/**
 * Draws the Vajra in hand. Forge has already applied the normal held-item transforms; this adds the
 * mining pose and recoil, swaps the texture, and records where the tip lands on screen so the HUD
 * pass can draw the beam / orb from exactly that point.
 */
public class VajraRenderer implements IItemRenderer {

    private final FloatBuffer itemMatrix = BufferUtils.createFloatBuffer(16);
    private final FloatBuffer handProjection = BufferUtils.createFloatBuffer(16);
    private boolean logged;

    @Override
    public boolean handleRenderType(ItemStack item, ItemRenderType type) {
        return type == ItemRenderType.EQUIPPED || type == ItemRenderType.EQUIPPED_FIRST_PERSON;
    }

    @Override
    public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
        return false;
    }

    @Override
    public void renderItem(ItemRenderType type, ItemStack stack, Object... data) {
        if (type != ItemRenderType.EQUIPPED_FIRST_PERSON) {
            drawItem(stack.getIconIndex());
            return;
        }
        VajraState s = VajraState.INSTANCE;
        IIcon icon = s.currentIcon();
        if (icon == null) icon = stack.getIconIndex();

        GL11.glPushMatrix();
        float a = s.poseAmount();
        float r = s.recoilAmount();
        GL11.glTranslatef(0.5F, 0.5F, 0F);
        GL11.glTranslatef(
            VajraConfig.aimOffsetX * a + s.recoilDist * r,
            VajraConfig.aimOffsetY * a - s.recoilDist * r,
            VajraConfig.aimOffsetZ * a);
        GL11.glRotatef(VajraConfig.aimRotY * a, 0F, 1F, 0F);
        GL11.glRotatef(VajraConfig.aimRotZ * a + s.recoilAng * r, 0F, 0F, 1F);
        GL11.glTranslatef(-0.5F, -0.5F, 0F);

        recordTip(s);
        drawItem(icon);
        GL11.glPopMatrix();
    }

    /** Remembers the tip's screen position (renderItemIn2D maps the texture's top-left to local (1, 1)). */
    private void recordTip(VajraState s) {
        itemMatrix.clear();
        handProjection.clear();
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, itemMatrix);
        GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, handProjection);
        float[] eye = FxMath.transform(itemMatrix, 1F - VajraConfig.tipU, 1F - VajraConfig.tipV, -0.03125F);
        float[] ndc = FxMath.project(handProjection, eye);
        if (ndc == null) return;
        s.tipNdcX = ndc[0];
        s.tipNdcY = ndc[1];
        s.tipFrame = s.frame;
        if (!logged) {
            logged = true;
            FMLLog.info("[VajraFX] Tip found on screen at %.2f, %.2f (screen centre = 0, 0)", ndc[0], ndc[1]);
        }
    }

    private static void drawItem(IIcon icon) {
        if (icon == null) return;
        ItemRenderer.renderItemIn2D(
            Tessellator.instance,
            icon.getMaxU(),
            icon.getMinV(),
            icon.getMinU(),
            icon.getMaxV(),
            icon.getIconWidth(),
            icon.getIconHeight(),
            0.0625F);
    }
}
