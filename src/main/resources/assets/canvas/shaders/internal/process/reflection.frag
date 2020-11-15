#include canvas:shaders/internal/process/header.glsl
#include frex:shaders/lib/color.glsl
#include frex:shaders/lib/sample.glsl
#include frex:shaders/lib/math.glsl

/******************************************************
  canvas:shaders/internal/process/reflection.frag
******************************************************/
uniform sampler2D _cvu_base;
uniform sampler2D _cvu_extras;
uniform vec3 _camera_view;
uniform float cvu_intensity;

varying vec2 _cvv_texcoord;

const float sampleUp = 0.001;
const float bias = 0.04;
const float maxDist = 0.1;
const float sampleUpFade = 0.001;
const float maxSideFadeCheck = 0.01;
const float addSideFade = 0.2;

float getReflectivity(vec2 coords){
	return texture2DLod(_cvu_extras, coords, 0).g;
}

vec4 calcReflection(){
	// distance determination loop
	float dist = 0;
	vec2 current = _cvv_texcoord;
	while(getReflectivity(current) > 0){
		current.y += sampleUp;
		if (current.y > 1.0 || dist > maxDist){
			return vec4(0.0);
		}
		dist += sampleUp;
	}

	float upCoord = _cvv_texcoord.y + dist * 2 + bias + dist;// * wobble;
	float groundCoord = _cvv_texcoord.y + dist;

	// side fade determination loop
	float curSideFadeCheck = 0;
	float sideFade = 0;
	//float xWobble = (0.5 - wobble) * (_cvv_texcoord.x - 0.5) * 0.2;
	while(curSideFadeCheck < maxSideFadeCheck){
		sideFade += (getReflectivity(vec2(_cvv_texcoord.x, groundCoord + curSideFadeCheck)) > 0) ? addSideFade : 0;
		curSideFadeCheck += sampleUpFade;
	}

	// discard reflection of reflective fragment to reduce artifact
	bool sampledIsReflective = getReflectivity(vec2(_cvv_texcoord.x, upCoord)) > 0;

	if(upCoord > 1.0 || dist > maxDist || sampledIsReflective){
		return vec4(0.0);
	} else {
		return smoothstep(maxDist, 0.0, dist)
		* smoothstep(1.0, 0.95, upCoord)
		* smoothstep(1, 0, sideFade * smoothstep(0.0, maxDist, dist))
		* texture2D(_cvu_base, vec2(_cvv_texcoord.x /*+ xWobble*/, upCoord));
	}
}

void main() {
	float reflectivity = getReflectivity(_cvv_texcoord);
	vec4 base = texture2D(_cvu_base, _cvv_texcoord);

	if (reflectivity > 0){
		gl_FragData[0] = base + calcReflection() * cvu_intensity;
	} else {
		gl_FragData[0] = base;
	}
}
