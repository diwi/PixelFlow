/**
 * 
 * PixelFlow | Copyright (C) 2017 Thomas Diewald - www.thomasdiewald.com
 * 
 * https://github.com/diwi/PixelFlow.git
 * 
 * A Processing/Java library for high performance GPU-Computing.
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */


#version 150

#define SHADER_VERT 0
#define SHADER_FRAG 0

uniform float     point_size;
uniform ivec2     wh_position;
uniform sampler2D tex_position;

#if SHADER_VERT

void main(){
  // get point index / vertex index
  int point_id = gl_VertexID;

  // get position (xy)
  int row = point_id / wh_position.x;
  int col = point_id - wh_position.x * row;
  
  // get particle position, velocity
  vec4 particle = texelFetch(tex_position, ivec2(col, row), 0);

  gl_Position  = vec4(particle.xy * 2.0 - 1.0, 0, 1); // ndc: [-1, +1]
  gl_PointSize = point_size;
}

#endif // #if SHADER_VERT


#if SHADER_FRAG

out float out_frag;

void main(){
  out_frag = max(0, 1.0 - length(gl_PointCoord * 2.0 - 1.0));
}

#endif // #if SHADER_FRAG




