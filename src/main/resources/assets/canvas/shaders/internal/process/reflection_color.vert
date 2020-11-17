#include canvas:shaders/internal/process/header.glsl

/******************************************************
  canvas:shaders/internal/process/reflection.vert
******************************************************/
uniform sampler2D _cvu_base;
uniform sampler2D _cvu_extras;
uniform sampler2D _cvu_normal;
uniform sampler2D _cvu_depth;
uniform mat4 cvu_projection;
uniform mat4 cvu_inv_projection;
uniform ivec2 _cvu_size;

attribute vec2 in_uv;

varying vec2 _cvv_texcoord;

void main() {
	vec4 outPos = gl_ProjectionMatrix * vec4(gl_Vertex.xy * _cvu_size, 0.0, 1.0);
	gl_Position = vec4(outPos.xy, 0.2, 1.0);
	_cvv_texcoord = in_uv;
}
