#version 330

in vec2 texCoord;
in vec4 viewCoord;
in vec4 shadowCoord;

in vec3 normalVector;
in vec3 lightVector;
in vec3 viewVector;

uniform vec3 uBaseColor;
uniform float uSpecStrength;
uniform float uShininess;
uniform sampler2D shadowMap;
uniform int uUseShadowMap;
uniform int uColorMode;

out vec4 outColor;

void main()
{
    // Setup directions
    vec3 normalDir = normalize(normalVector);
    vec3 lightDir = normalize(lightVector);
    vec3 viewDir = normalize(viewVector);

    // Base color
    vec3 baseColor = uBaseColor;

    // Light color
    vec3 lightColor = vec3(0.8, 0.8, 0.7);

    // Ambient
    vec3 ambient = vec3(0.15, 0.15, 0.2);

    // Diffuse
    float NdotL = max(dot(normalDir, lightDir), 0.0);
    float diff = NdotL;

    // Specular
    vec3 reflectDir = reflect(-lightDir, normalDir);
    float VdotR = max(dot(viewDir, reflectDir), 0.0);
    float spec = uSpecStrength * pow(VdotR, uShininess);


    float visibility = 1;
    float bias = 0.005;
    if(uUseShadowMap == 1 && texture(shadowMap, shadowCoord.xy).z < shadowCoord.z - bias) {
        visibility = 0.1;
    }

    vec3 light = ambient + visibility * lightColor * (diff + spec);
    vec3 finalColor = baseColor * light;

    // Color Modes:
    if(uColorMode == 0) outColor = vec4(finalColor, 1.0);

    else if(uColorMode == 1) outColor = vec4(viewCoord.xyz, 1.0);

    else if(uColorMode == 2) outColor = vec4(vec3(shadowCoord.z), 1.0);

    else if(uColorMode == 3) outColor = vec4(normalDir, 1.0);

    else if(uColorMode == 4) outColor = vec4(texCoord, 0.0, 1.0);

    else if(uColorMode == 5) outColor = vec4(light, 1.0);

    else if(uColorMode == 6) outColor = vec4(vec3(texture(shadowMap, shadowCoord.xy).z), 1.0);

    else outColor = vec4(finalColor, 1.0);
}
