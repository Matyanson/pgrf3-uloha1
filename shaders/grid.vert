#version 330

in vec2 inPosition;

uniform mat4 uModel;
uniform mat4 uView;
uniform mat4 uProj;
uniform mat4 uVPLight;
uniform vec3 uLightPos;
uniform vec3 uCameraPos;
uniform float uTime;
uniform int uFuncType;
uniform int uAnimateType;

out vec3 lightVector;
out vec3 normalVector;
out vec3 viewVector;
out vec2 texCoord;
out vec4 viewCoord;
out vec4 shadowCoord;

const float PI = 3.14159;
// const vec3 lightPosition = vec3(0.7, 0.3, 0.5);
const mat4 biasMatrix = mat4(
0.5, 0.0, 0.0, 0.0,
0.0, 0.5, 0.0, 0.0,
0.0, 0.0, 0.5, 0.0,
0.5, 0.5, 0.5, 1.0
);

vec3 func(float x, float y) {
    // PLANE
    if(uFuncType == 0)
        return vec3(vec2(x, y) * 2 - 1, 0);

    // SPHERE
    // sférické
    if(uFuncType == 1) {
        float zenith = x * PI;
        float azimuth = y * 2 * PI;
        float r = 0.5f;

        // Přepočet na kartézské souřadnice
        float _x = r * sin(zenith) * cos(azimuth);
        float _y = r * sin(zenith) * sin(azimuth);
        float _z = r * cos(zenith);
        return vec3(_x, _y, _z);
    }

    return vec3(x, y, 0);
}

vec3 animate(vec3 pos) {
    if(uAnimateType == 0)
        return pos;

    if(uAnimateType == 1) {
        float offset = 1.0 + sin(uTime) * 2.0;

        return pos + vec3(0.0, 0.0, offset);
    }

    return pos;
}

vec3 getNormal(float x, float y) {
    vec3 dx = vec3(func(x + 0.01, y) - func(x - 0.01, y));
    vec3 dy = vec3(func(x, y + 0.01) - func(x, y - 0.01));
    return cross(dx, dy);
}


void main()
{
    texCoord = inPosition;

    vec3 pos = func(inPosition.x, inPosition.y);
    pos = animate(pos);

    vec4 worldPos = vec4(pos, 1.0);
    worldPos = uModel * worldPos;
    viewCoord = uView * worldPos;
    gl_Position = uProj * uView * worldPos;


    normalVector = getNormal(inPosition.x, inPosition.y);
    normalVector = inverse(transpose(mat3(uView * uModel))) * normalVector;
    normalVector = normalVector;

    vec3 lightVectorWorld = uLightPos - worldPos.xyz;
    lightVector = mat3(uView) * lightVectorWorld;

    vec3 viewVectorWorld = uCameraPos - worldPos.xyz;
    viewVector = mat3(uView) * viewVectorWorld;


    mat4 depthBiasMVP = biasMatrix * uVPLight;
    shadowCoord = depthBiasMVP * (uModel * vec4(pos, 1));
}
