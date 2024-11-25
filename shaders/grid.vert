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

vec3 func(float u, float v) {
    // PLANE
    if(uFuncType == 0)
        return vec3(vec2(u, v) * 2 - 1, 0);

    // SPHERE
    // sférické
    if(uFuncType == 1) {
        float zenith = u * PI;
        float azimuth = v * 2 * PI;
        float r = 0.5f;

        // Přepočet na kartézské souřadnice
        float x = r * sin(zenith) * cos(azimuth);
        float y = r * sin(zenith) * sin(azimuth);
        float z = r * cos(zenith);
        return vec3(x, y, z);
    }

    if(uFuncType == 2) {
        // SPIKY BALL
        float radius = 1.0f;

        // modify the radius
        float spikeAmount = 0.4f + 0.2f * cos(uTime);
        float spikeFrequency = 10.f + 9.0f * sin(uTime * 0.3);

        //  UV to spherical
        float theta = u * 2.0 * PI;    // Azimuthal angle (around z-axis)
        float phi = (v - 0.5) * PI;    // Polar angle (from -PI/2 to PI/2)

        // Base spherical coordinates (radius, theta, phi)
        float r = radius + sin(spikeFrequency * phi) * spikeAmount;

        // Parametric equations to convert spherical to Cartesian coordinates
        float x = r * sin(phi) * cos(theta);  // x-coordinate
        float y = r * sin(phi) * sin(theta);  // y-coordinate
        float z = r * cos(phi);               // z-coordinate

        return vec3(x, y, z);
    }

    if(uFuncType == 3) {
        // UFO
        float radius = 1.0f;        // Main radius of the UFO (full radius of the bottom body)
        float topDomeHeight = 0.25f; // Height of the dome (top part), keeping it half of the bottom's radius
        float bottomHeight = 0.5f;   // Height of the squashed body (bottom part)
        float squishFactor = 0.4f;   // Squash factor for the bottom sphere (flattening effect)

        // Convert UV coordinates to spherical coordinates
        float theta = u * 2.0 * PI;  // Azimuthal angle (around z-axis)
        float phi;                   // Polar angle (from z-axis)

        // Define two regions for UV: top hemisphere (v < 0.5) and bottom squashed hemisphere (v >= 0.5)
        float z;                     // z coordinate in world space
        if (v < 0.5) {
            // Top hemisphere (regular spherical shape, with half the bottom radius)
            phi = v * PI;            // Calculate polar angle for the top dome

            // Calculate spherical coordinates for top hemisphere
            z = cos(phi) * topDomeHeight; // z is based on spherical formula
        } else {
            // Bottom hemisphere (squashed sphere)
            phi = (v - 0.5) * PI;     // Polar angle for the squashed bottom

            // Squash the bottom part by reducing the height along the z-axis
            z = cos(phi) * bottomHeight * squishFactor;
        }

        // Calculate x, y coordinates for both parts (same formula for both)
        float r = radius * sin(phi);  // Radial distance on the x-y plane
        float x = r * cos(theta);     // x coordinate
        float y = r * sin(theta);     // y coordinate

        // Return the final position in 3D space
        return vec3(x, y, z);
    }

    return vec3(u, v, 0);
}

vec3 animate(vec3 pos) {
    if(uAnimateType == 0)
        return pos;

    if(uAnimateType == 1) {
        float offset = 1.0 + sin(uTime) * 2.0;

        return pos + vec3(0.0, 0.0, offset);
    }
    if(uAnimateType == 2) {
        float offset = 1.0 + sin(uTime) * 0.2;

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
