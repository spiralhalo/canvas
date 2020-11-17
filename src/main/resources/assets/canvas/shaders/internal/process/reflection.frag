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

vec3 normal(vec2 uv){
	return 2.0 * texture2DLod(_cvu_normal, uv, 0).xyz - 1.0;
}

// Adds randomness to reflection ray for rough surfaces
// Would be better to use world space coordinates for seed
vec3 hash(vec2 uv){
    vec3 a = fract(uv.xyx * vec3(25.9, 25.9, 25.9));
    a += dot(a, a.yxz + 19.19);
    return fract((a.xxy + a.yxx)*a.zyx);
}

const float minStepL = 0.1;
const float stepL = 0.2;
const int maxStep = 20;
const int maxBinaryStep = 15;

vec2 binaryTest(inout vec3 march, inout vec3 curPos){
	int curStep = 0;
	vec2 curUV;
	vec3 texPos;
	while(curStep < maxBinaryStep){
		curStep ++;
		curUV = uvSpace(curPos);
		texPos = viewSpace(curUV);
        march *= 0.5;
        if(curPos.z > texPos.z)
            curPos += march;
        else
            curPos -= march;
    }
    return uvSpace(curPos);
}

vec4 rayMarch(float reflectance){
	vec3 curPos = viewSpace(_cvv_texcoord);
	float bias = max(1.0, -curPos.z * 0.4);
	vec3 reflected = reflect(normalize(curPos), normalize(normal(_cvv_texcoord)));
	// return vec4(reflected, 1.0);
	vec3 march = mix(hash(_cvv_texcoord), vec3(0.0), reflectance) + reflected * max(minStepL, -curPos.z) * stepL;
	int curStep = 0;
	vec2 curUV;
	vec3 texPos;
	while(curStep < maxStep){
		curPos += march;
		curUV = uvSpace(curPos);
		texPos = viewSpace(curUV);
		if(texture2DLod(_cvu_extras, curUV, 0).a > 0.0 && curPos.z - texPos.z < 0 && curPos.z - texPos.z > -bias){
			vec2 finalUV = binaryTest(march, curPos);
		    texPos = viewSpace(finalUV);
			return max(0.0, dot(-normalize(march), normalize(normal(finalUV))))
				* smoothstep(0.5, 0.45, abs(finalUV.x - 0.5))
				* smoothstep(0.5, 0.45, abs(finalUV.y - 0.5))
				* texture2D(_cvu_base, finalUV);
		}
		curStep ++;
	}
	return vec4(0);
}

void main() {
	vec4 base 	= texture2D(_cvu_base, _cvv_texcoord);
	float metal = texture2DLod(_cvu_extras, _cvv_texcoord, 0).g;
	float gloss = 1 - texture2DLod(_cvu_extras, _cvv_texcoord, 0).b;
	// Sky has extras.a = 0.0
	if (gloss > 0 && texture2DLod(_cvu_extras, _cvv_texcoord, 0).a > 0.0){
		vec4 reflection = rayMarch(gloss);
		vec4 color = base + base * vec4(vec3(1.0) - base.rgb, 1.0)  * reflection;
		vec4 metalColor = base * base + reflection;
		gl_FragData[0] = mix(color, metalColor, metal);
	} else {
		gl_FragData[0] = base;
	}
}