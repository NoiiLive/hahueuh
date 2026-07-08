package com.example.hahueuh.client;

import com.example.hahueuh.HahUeuh;
import com.example.hahueuh.network.DomainRenderState;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

@EventBusSubscriber(modid = HahUeuh.MODID, value = Dist.CLIENT)
public final class DomainSphereRenderer {
    private DomainSphereRenderer() {}

    private static final float OPACITY = 0.5f;

    private static int shaderProgram = -1;
    private static int screenCopyTex = -1;
    private static int vaoId = -1;

    private static final String VERTEX_SHADER_SRC = """
            #version 150
            out vec2 texCoord;
            void main() {
                vec2 pos[4] = vec2[4](
                    vec2(-1.0, -1.0),
                    vec2( 1.0, -1.0),
                    vec2(-1.0,  1.0),
                    vec2( 1.0,  1.0)
                );
                gl_Position = vec4(pos[gl_VertexID], 0.0, 1.0);
                texCoord = pos[gl_VertexID] * 0.5 + 0.5;
            }
            """;

    private static final String FRAGMENT_SHADER_SRC = """
            #version 150
            in vec2 texCoord;
            out vec4 FragColor;

            uniform sampler2D DiffuseSampler;
            uniform sampler2D DepthSampler;
            uniform mat4 InvProjMat;
            uniform vec3 SphereCenterEye;
            uniform float SphereRadius;
            uniform float Opacity;

            void main() {
                float depth = texture(DepthSampler, texCoord).r;
                vec4 clipPos = vec4(texCoord.x * 2.0 - 1.0, texCoord.y * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
                vec4 viewPos = InvProjMat * clipPos;
                if (abs(viewPos.w) > 0.00001) {
                    viewPos /= viewPos.w;
                }

                float Lmax = length(viewPos.xyz);
                if (depth >= 0.9999) {
                    Lmax = 10000.0;
                }
                vec3 D = normalize(viewPos.xyz);
                vec3 C = SphereCenterEye;
                float R = SphereRadius;

                float b = dot(D, C);
                float c = dot(C, C) - R * R;
                float disc = b * b - c;

                vec3 col = texture(DiffuseSampler, texCoord).rgb;

                // Composite the glowing white spherical shell (front + back hits).
                if (disc >= 0.0) {
                    float sqrtDisc = sqrt(disc);
                    float t1 = b - sqrtDisc;
                    float t2 = b + sqrtDisc;
                    vec3 shellColor = vec3(1.0);

                    if (t1 > 0.0 && t1 < Lmax + 0.5) {
                        vec3 P = D * t1;
                        vec3 N = normalize(P - C);
                        float fresnel = pow(1.0 - abs(dot(N, -D)), 2.2);
                        float contactGlow = smoothstep(0.6, 0.0, abs(Lmax - t1));
                        float shellIntensity = max(fresnel * 1.7, contactGlow * 1.4) + 0.2;
                        col += shellColor * shellIntensity * Opacity;
                    }

                    if (t2 > 0.0 && t2 < Lmax + 0.5) {
                        vec3 P = D * t2;
                        vec3 N = normalize(P - C);
                        float fresnel = pow(1.0 - abs(dot(N, -D)), 2.2);
                        float contactGlow = smoothstep(0.6, 0.0, abs(Lmax - t2));
                        float shellIntensity = max(fresnel * 1.7, contactGlow * 1.4) + 0.2;
                        col += shellColor * shellIntensity * Opacity;
                    }
                }

                FragColor = vec4(col, 1.0);
            }
            """;

    private static void initShaderIfNeeded() {
        if (shaderProgram != -1) return;

        int vsh = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
        GL20.glShaderSource(vsh, VERTEX_SHADER_SRC);
        GL20.glCompileShader(vsh);

        int fsh = GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER);
        GL20.glShaderSource(fsh, FRAGMENT_SHADER_SRC);
        GL20.glCompileShader(fsh);

        shaderProgram = GL20.glCreateProgram();
        GL20.glAttachShader(shaderProgram, vsh);
        GL20.glAttachShader(shaderProgram, fsh);
        GL20.glLinkProgram(shaderProgram);

        GL20.glDeleteShader(vsh);
        GL20.glDeleteShader(fsh);

        screenCopyTex = GL11.glGenTextures();
        vaoId = GL30.glGenVertexArrays();
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;

        float fade = DomainRenderState.advanceAndGetAlpha();
        if (fade <= 0.001f) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        if (DomainRenderState.dimension() == null) return;
        if (!mc.level.dimension().location().equals(DomainRenderState.dimension())) return;

        float radius = (float) DomainRenderState.radius();
        if (radius <= 0.05f) return;

        initShaderIfNeeded();

        Vec3 camera = event.getCamera().getPosition();
        Matrix4f invProj = new Matrix4f(event.getProjectionMatrix()).invert();
        Matrix4f viewMat = new Matrix4f(event.getModelViewMatrix());
        float[] matBuf = new float[16];

        int diffLoc = GL20.glGetUniformLocation(shaderProgram, "DiffuseSampler");
        int depthLoc = GL20.glGetUniformLocation(shaderProgram, "DepthSampler");
        int invProjLoc = GL20.glGetUniformLocation(shaderProgram, "InvProjMat");
        int centerLoc = GL20.glGetUniformLocation(shaderProgram, "SphereCenterEye");
        int radiusLoc = GL20.glGetUniformLocation(shaderProgram, "SphereRadius");
        int opacityLoc = GL20.glGetUniformLocation(shaderProgram, "Opacity");

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, screenCopyTex);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL13.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL13.GL_CLAMP_TO_EDGE);
        GL11.glCopyTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGB, 0, 0,
                mc.getWindow().getWidth(), mc.getWindow().getHeight(), 0);

        GL20.glUseProgram(shaderProgram);

        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, screenCopyTex);
        GL20.glUniform1i(diffLoc, 0);

        GL13.glActiveTexture(GL13.GL_TEXTURE1);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, mc.getMainRenderTarget().getDepthTextureId());
        GL20.glUniform1i(depthLoc, 1);

        invProj.get(matBuf);
        GL20.glUniformMatrix4fv(invProjLoc, false, matBuf);

        Vector4f centerEye = new Vector4f(
                (float) (DomainRenderState.x() - camera.x),
                (float) (DomainRenderState.y() - camera.y),
                (float) (DomainRenderState.z() - camera.z), 1.0f);
        viewMat.transform(centerEye);
        GL20.glUniform3f(centerLoc, centerEye.x, centerEye.y, centerEye.z);
        GL20.glUniform1f(radiusLoc, radius);
        GL20.glUniform1f(opacityLoc, OPACITY * fade);

        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(false);

        GL30.glBindVertexArray(vaoId);
        GL11.glDrawArrays(GL11.GL_TRIANGLE_STRIP, 0, 4);
        GL30.glBindVertexArray(0);

        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(true);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL20.glUseProgram(0);
    }
}
