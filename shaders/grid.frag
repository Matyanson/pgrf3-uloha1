#version 330

in vec2 vertPos;
in vec2 texCoord;
in vec4 shadowCoord;

in vec3 lightVector;
in vec3 normalVector;

uniform vec3 uBaseColor;
uniform sampler2D shadowMap;
uniform int uUseShadowMap;

out vec4 outColor;

vec3 ambientColor = vec3(0.8, 0.8, 0.8);
vec3 diffuseColor = vec3(1, 1, 1);

void main()
{
    //vec3 baseColor = vec3(0.8, 0.8, 0.8);
    vec3 baseColor = uBaseColor;

    vec3 ld = normalize(lightVector);
    vec3 nd = normalize(normalVector);
    float NDotL = max(dot(nd, ld), 0.0);
    vec3 totalDiffuse = NDotL * diffuseColor;

    //outColor = vec4((ambientColor + totalDiffuse) * baseColor.rgb, 1);
    //outColor = vec4(baseColor, 1);
    //outColor = vec4(nd, 1);
    //outColor = vec4(vertPos, 0, 1);

    float visibility = 1;
    float bias = 0.005;
    if(uUseShadowMap == 1 && texture(shadowMap, shadowCoord.xy).z < shadowCoord.z - bias) {
        visibility = 0.5;
    }

    outColor = visibility * vec4(baseColor, 1);
}
