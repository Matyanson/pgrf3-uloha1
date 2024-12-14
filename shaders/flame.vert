#version 330

in float inTimeOffset;
in float inNormalizedIndex;

uniform mat4 uView;
uniform mat4 uProj;
uniform mat4 uWallModel;
uniform float uTime;
uniform float uMinSpeed;
uniform float uMaxSpeed;
uniform float uMinRange;
uniform float uMaxRange;
uniform sampler2D textureNoise;

out vec3 color;

vec2 getPosFromNormIndex(float index, float rowCount) {
    return vec2(mod(index * rowCount, 1.0), index * rowCount);
}

vec3 getXYIntersection(vec3 origin, vec3 end) {
    // Calculate the parameter t at which the line intersects the xy-plane (z=0)
    float t = -origin.z / (end.z - origin.z);

    // Calculate the intersection point
    return origin + t * (end - origin);
}

bool isXYHit(vec3 origin, vec3 end) {
    return  origin.z > 0.0 && end.z < 0.0 || origin.z < 0.0 && end.z > 0.0;
}

vec3 adjustForReflection(vec3 line, vec3 origin) {

    // Transform line into wall local space
    vec3 localOrigin = (inverse(uWallModel) * vec4(origin, 1.0)).xyz;
    vec3 localEnd = (inverse(uWallModel) * vec4(line, 1.0)).xyz;

    // xy plane is hit
    if(!isXYHit(localOrigin, localEnd))
        return line;

    vec3 intersection = getXYIntersection(localOrigin, localEnd);

    // wall is hit
    if(abs(intersection.x) < 1.0 && abs(intersection.y) < 1.0) {
        vec3 passedLine = localEnd - intersection;

        vec3 reflectedDirection = reflect(passedLine, vec3(0.0, 0.0, 1.0));
        vec3 reflectedEnd = reflectedDirection + intersection;
        return vec3(uWallModel * vec4(reflectedEnd, 1.0));
    }

    return line;
}

void main() {
    // setup variables
    float genMax = 100.0; // texture height

    // speed
    float rnd = texture(textureNoise, getPosFromNormIndex(inNormalizedIndex, genMax)).x;
    float speed = mix(uMinSpeed, uMaxSpeed, rnd);

    // distance
    rnd = texture(textureNoise, getPosFromNormIndex(inNormalizedIndex, genMax)).y;
    float dist = mix(uMinRange, uMaxRange, rnd);

    // lifespan
    float lifespan = dist / speed;

    // timing
    float localTime = uTime + inTimeOffset * lifespan;
    float age = mod(localTime, lifespan);
    float t = age / lifespan;
    float generation = localTime / lifespan;
    float genT = mod(generation, genMax) / genMax;

    // direction
    vec2 noiseCoords = vec2(inNormalizedIndex, genT);
    vec3 direction = texture(textureNoise, noiseCoords).xyz;

    vec3 position = direction * dist * t;
    position = adjustForReflection(position, vec3(0.0));
    gl_Position = uProj * uView * vec4(position, 1.0);

    color = vec3(0.7, 0.7 * (1.0 - t), 0.0);
}