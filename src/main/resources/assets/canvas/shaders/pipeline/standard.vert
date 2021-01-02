#include canvas:shaders/pipeline/options.glsl
#include canvas:shaders/pipeline/diffuse.glsl

/******************************************************
  canvas:shaders/pipeline/standard.vert
******************************************************/

void frx_startPipelineVertex(inout frx_VertexData data) {

	// WIP: remove - various api tests
	//if (data.vertex.z < frx_viewDistance() * -0.25) {
	//	data.color = vec4(0.0, 0.0, 0.0, 1.0);
	//}

	// world pos consistency test
	//vec3 worldPos = data.vertex.xyz + frx_modelOriginWorldPos();
	//float f = fract(worldPos.x / 32.0);
	//data.color = vec4(f, f, f, 1.0);

	vec4 viewCoord = gl_ModelViewMatrix * data.vertex;
	gl_ClipVertex = viewCoord;
	gl_FogFragCoord = length(viewCoord.xyz);
}

void frx_writePipelineVertex(in frx_VertexData data) {
	gl_Position = data.vertex;

	#if DIFFUSE_SHADING_MODE != DIFFUSE_MODE_NONE
		_cpv_diffuse = _cp_diffuse(data.normal);
	#endif
}
