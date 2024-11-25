#version 330

in vec2 vertPos;
in vec3 viewDir;
in vec2 texCoord;
in vec4 shadowCoord;

in vec3 lightVector;
in vec3 normalVector;

uniform vec3 uBaseColor;
uniform float uSpecStrength;
uniform float uShininess;
uniform sampler2D shadowMap;
uniform int uUseShadowMap;

out vec4 outColor;

void main()
{
    // Setup directions
    vec3 normalDir = normalize(normalVector);
    vec3 lightDir = normalize(lightVector);
    // vec3 viewDir = normalize(uViewPos - vertPos);

    // Base color
    vec3 baseColor = uBaseColor;

    // Ambient
    vec3 ambient = vec3(0.8, 0.8, 0.8);

    // Diffuse
    float NdotL = max(dot(normalDir, lightDir), 0.0);
    float diff = NdotL;

    // Specular
    vec3 reflectDir = reflect(-lightDir, normalDir);
    float VdotR = max(dot(viewDir, reflectDir), 0.0);
    float spec = uSpecStrength * pow(VdotR, uShininess);

    baseColor = baseColor * vec3(1.0, 1.0, 0.8) * (ambient + diff + spec);

    float visibility = 1;
    float bias = 0.005;
    if(uUseShadowMap == 1 && texture(shadowMap, shadowCoord.xy).z < shadowCoord.z - bias) {
        visibility = 0.5;
    }

    outColor = visibility * vec4(baseColor, 1);
}
