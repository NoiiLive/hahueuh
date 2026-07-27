#version 150

uniform sampler2D DiffuseSampler;

in vec2 texCoord;

uniform float Intensity;

out vec4 fragColor;

void main() {
    vec4 color = texture(DiffuseSampler, texCoord);
    // Rec. 601 luminance, mixed in by Intensity (0 = full colour, 1 = full grayscale).
    float luma = dot(color.rgb, vec3(0.299, 0.587, 0.114));
    vec3 gray = vec3(luma);
    fragColor = vec4(mix(color.rgb, gray, Intensity), 1.0);
}
