#include frex:shaders/api/vertex.glsl
#include frex:shaders/lib/face.glsl

/******************************************************
  canvas:shaders/material/water.vert
******************************************************/

void frx_startVertex(inout frx_VertexData data) {
    frx_var0 = data.vertex;
}

void frx_endVertex(inout frx_VertexData data) {
    // NOOP
}
