#include canvas:shaders/internal/process/header.glsl
#include frex:shaders/lib/color.glsl
#include frex:shaders/lib/sample.glsl
#include frex:shaders/lib/math.glsl

/******************************************************
  canvas:shaders/internal/process/reflection.frag
******************************************************/
uniform sampler2D _cvu_base;
uniform sampler2D _cvu_extras;
uniform sampler2D _cvu_normal;
uniform sampler2D _cvu_depth;
uniform vec2 _cvu_distance;
uniform float cvu_intensity;
uniform mat4 cvu_projection;
uniform mat4 cvu_inv_projection;

varying vec2 _cvv_texcoord;

float shininess(vec2 coords){
	return texture2DLod(_cvu_extras, coords, 0).g;
}

vec3 normal(vec2 coords){
	return 2 * (texture2DLod(_cvu_normal, coords, 0).xyz - 0.5);
}

float depth(vec2 uv){
	return texture2DLod(_cvu_depth, uv, 0).r;
}

vec2 uvSpace(vec3 viewPos){
	vec4 clipPos = cvu_projection * vec4(viewPos, 1.0);
	clipPos.xyz /= clipPos.w;
	return vec2(clipPos.x, clipPos.y) * 0.5 + 0.5;
}

vec3 viewSpace(vec2 uv) {
	vec2 clipPos = (uv - 0.5) * 2;
	vec4 viewPos = cvu_inv_projection * vec4( clipPos.x, clipPos.y, 2.0 * depth(uv) - 1.0, 1.0);
	return viewPos.xyz / viewPos.w;
}

const float step = 0.1;
const float maxStep = 5000;

vec4 rayMarch(){
	vec3 curPos = viewSpace(_cvv_texcoord);
	vec3 march = reflect(normalize(-curPos), normalize(normal(_cvv_texcoord))) * step;
	float curStep = 0;
	while(curStep < maxStep){
		curPos += march;
		vec2 curUV = uvSpace(curPos);
		if(curPos.z > depth(curUV)){
			return smoothstep(0.0, 0.05, curUV.x)
				* smoothstep(1.0, 0.95, curUV.x)
				* smoothstep(0.0, 0.05, curUV.y)
				* smoothstep(1.0, 0.95, curUV.y)
				* texture2D(_cvu_base, curUV);
		}
		curStep += step;
	}
	return vec4(0);
}

void main() {
	vec4 base = texture2D(_cvu_base, _cvv_texcoord);
	float shininess = shininess(_cvv_texcoord);
	if (shininess > 0){
		gl_FragData[0] = base + base * rayMarch() * shininess;
	} else {
		gl_FragData[0] = base;
	}
}
