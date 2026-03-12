#version 150

uniform sampler2D DiffuseSampler;
uniform vec2 InSize;
uniform vec2 OutSize;
uniform float Time;

uniform float Strength;
uniform float Saturation;
uniform float Contrast;
uniform float Lift;
uniform float Vignette;
uniform float Grain;
uniform vec3 Tint;

in vec2 texCoord0;
out vec4 fragColor;

vec3 acesTonemap(vec3 x) {
    const float a = 2.51;
    const float b = 0.03;
    const float c = 2.43;
    const float d = 0.59;
    const float e = 0.14;
    return clamp((x * (a * x + b)) / (x * (c * x + d) + e), 0.0, 1.0);
}

float hash12(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

void main() {
    vec4 src = texture(DiffuseSampler, texCoord0);
    vec3 c = src.rgb;

    float luma = dot(c, vec3(0.2126, 0.7152, 0.0722));
    vec3 gray = vec3(luma);

    vec3 dst = mix(gray, c, clamp(Saturation, 0.0, 1.0));
    dst = acesTonemap(dst);
    dst = (dst - 0.5) * Contrast + 0.5;
    dst += Lift;
    dst *= Tint;
    dst = clamp(dst, 0.0, 1.0);

    vec2 uv = texCoord0 - 0.5;
    float aspect = OutSize.x / max(1.0, OutSize.y);
    uv.x *= aspect;
    float r = length(uv);
    float vig = 1.0 - smoothstep(0.45, 0.98, r);
    dst *= mix(1.0, vig, clamp(Vignette, 0.0, 1.0));

    float t = Time * 0.03;
    vec2 p = (texCoord0 * OutSize) + vec2(t * 37.0, t * 17.0);
    float n1 = hash12(p);
    float n2 = hash12(p + vec2(13.1, 31.7));
    float g = (n1 + n2) - 1.0;
    dst += g * Grain * (0.55 + 0.45 * luma);
    dst = clamp(dst, 0.0, 1.0);

    vec3 outc = mix(c, dst, clamp(Strength, 0.0, 1.0));
    fragColor = vec4(outc, src.a);
}
