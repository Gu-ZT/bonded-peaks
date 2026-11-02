package dev.dubhe.bonded.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.dubhe.bonded.BondedPeaks;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import net.minecraft.client.renderer.ShaderProgram;
import net.minecraft.client.renderer.ShaderDefines;

@EventBusSubscriber(modid = BondedPeaks.MOD_ID, value = Dist.CLIENT)
public final class TeamIconShader extends RenderType {
    private static final ShaderProgram SHADER = new ShaderProgram(
        ResourceLocation.fromNamespaceAndPath(BondedPeaks.MOD_ID, "core/team_icon"),
        DefaultVertexFormat.POSITION_TEX_COLOR, ShaderDefines.EMPTY);
    static final RenderType ICON = create("bonded_peaks_icons", DefaultVertexFormat.POSITION_TEX_COLOR,
        VertexFormat.Mode.QUADS, 1536, false, false, CompositeState.builder()
            .setShaderState(new ShaderStateShard(SHADER))
            .setTransparencyState(TRANSLUCENT_TRANSPARENCY).setDepthTestState(NO_DEPTH_TEST)
            .setWriteMaskState(COLOR_WRITE).setCullState(NO_CULL).createCompositeState(false));

    private TeamIconShader() {
        super("bonded_peaks_icons", DefaultVertexFormat.POSITION_TEX_COLOR,
            VertexFormat.Mode.QUADS, 1536, false, false, () -> {}, () -> {});
    }

    @SubscribeEvent
    public static void register(RegisterShadersEvent event)  {
        event.registerShader(SHADER);
    }
}
