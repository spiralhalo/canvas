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

vec2 uvSpace(vec3 viewPos){
	vec4 clipPos = cvu_projection * vec4(viewPos, 1.0);
	clipPos.xyz /= clipPos.w;
	return clipPos.xy * 0.5 + 0.5;
}

vec3 viewSpace(vec2 uv) {
	vec2 clipPos = (uv - 0.5) * 2;
	vec4 viewPos = cvu_inv_projection * vec4( clipPos.x, clipPos.y, 2.0 * texture2DLod(_cvu_depth, uv, 0).r - 1.0, 1.0);
	return viewPos.xyz / viewPos.w;
}

const float stepL = 0.5;
const int maxStep = 60;

vec4 rayMarch(){
	vec3 curPos = viewSpace(_cvv_texcoord);
	float initialZ = curPos.z;
	vec3 reflected = reflect(normalize(curPos), normalize(2 * (texture2DLod(_cvu_normal, _cvv_texcoord, 0).xyz - 0.5)));
	// return vec4(reflected, 1.0);
	vec3 march = reflected * stepL;
	int curStep = 0;
	while(curStep < maxStep){
		curPos += march;
		vec2 curUV = uvSpace(curPos);
		vec3 texPos = viewSpace(curUV);
		if(texPos.z < initialZ && texture2DLod(_cvu_extras, curUV, 0).g == 0.0 && curPos.z < texPos.z){
			return smoothstep(0.5, 0.45, abs(curUV.x - 0.5))
				* smoothstep(0.5, 0.45, abs(curUV.y - 0.5))
				* texture2D(_cvu_base, curUV);
		}
		curStep ++;
	}
	return vec4(0);
}

void main() {
	vec4 base = texture2D(_cvu_base, _cvv_texcoord);
	float shininess = texture2DLod(_cvu_extras, _cvv_texcoord, 0).g;
	if (shininess > 0){
		gl_FragData[0] = base + rayMarch() * shininess;
	} else {
		gl_FragData[0] = base;
	}
}
