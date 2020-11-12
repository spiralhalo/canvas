#include frex:shaders/api/fragment.glsl
#include frex:shaders/lib/math.glsl

/******************************************************
  canvas:shaders/material/reflective_top.frag
******************************************************/

void frx_startFragment(inout frx_FragmentData fragData) {
    fragData.reflectivity = frx_smootherstep(0.9, 1.0, fragData.vertexNormal.y);
}
