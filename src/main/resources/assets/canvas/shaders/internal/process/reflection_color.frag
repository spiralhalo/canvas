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
uniform ivec2 _cvu_size;

varying vec2 _cvv_texcoord;

vec2 uvSpace(vec3 viewPos){
	vec4 clipPos = cvu_projection * vec4(viewPos, 1.0);
	clipPos.xyz /= clipPos.w;
	return clipPos.xy * 0.5 + 0.5;
}

vec3 viewSpace(vec2 uv) {
	vec2 clipPos = 2.0 * uv - 1.0;
	vec4 viewPos = cvu_inv_projection * vec4( clipPos.x, clipPos.y, 2.0 * texture2DLod(_cvu_depth, uv, 0).r - 1.0, 1.0);
	return viewPos.xyz / viewPos.w;
}

vec3 normal(vec2 uv){
	return 2.0 * texture2DLod(_cvu_normal, uv, 0).xyz - 1.0;
}

// Adds randomness to reflection ray for rough surfaces
// Would look better if world space coordinates are used for seed
const float wildness = 0.5;
vec3 hash(vec2 uv){
    vec3 a  = fract(uv.xyx * vec3(25.9, 25.9, 25.9));
    a      += dot(a, a.yxz + 19.19);
    vec3 b  = fract((a.xxy + a.yxx)*a.zyx);
    return (2.0 * b - 1.0) * wildness;
}

const float minStepL = 0.1;
const float stepL = 0.4;
const int maxStep = 50;
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
            // Discard ray coming towards the camera
            if(curPos.z > 0){
	            return vec4(0.0, 0.0, 0.0, 1.0);
            }
		    texPos = viewSpace(finalUV);
			return max(0.0, dot(-normalize(march), normalize(normal(finalUV))))
				* smoothstep(0.5, 0.45, abs(finalUV.x - 0.5))
				* smoothstep(0.5, 0.45, abs(finalUV.y - 0.5))
				* texture2D(_cvu_base, finalUV);
		}
		curStep ++;
	}
    // Sky reflection
    if(texture2DLod(_cvu_extras, curUV, 0).a == 0.0 && curPos.z < 0){
        return smoothstep(0.6, 0.5, abs(curUV.x - 0.5))
            * smoothstep(0.6, 0.5, abs(curUV.y - 0.5))
            * texture2D(_cvu_base, curUV);
    }
	return vec4(0.0, 0.0, 0.0, 1.0);
}

void main() {
	float gloss = 1 - texture2DLod(_cvu_extras, _cvv_texcoord, 0).b;

	// Sky has extras.a = 0.0, prevent rendering reflection on sky fragments
	if (gloss > 0 && texture2DLod(_cvu_extras, _cvv_texcoord, 0).a > 0.0){
		gl_FragData[0] = rayMarch(gloss);
	} else {
		gl_FragData[0] = vec4(0.0, 0.0, 0.0, 1.0);
	}
}