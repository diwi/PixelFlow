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

uniform ivec2     wh_position;
uniform float     line_uv;
uniform vec4      col_A = vec4(0, 0, 0, 1.0);
uniform vec4      col_B = vec4(0, 0, 0, 0.5);
uniform sampler2D tex_position_A;
uniform sampler2D tex_position_B;

#if SHADER_VERT

void main(){

  // get line index / vertex index
  int line_id = gl_VertexID / 2;
  int vtx_id  = gl_VertexID & 1; //  either 0 (line-start) or 1 (line-end)

  // get position (xy)
  int row = line_id / wh_position.x;
  int col = line_id - wh_position.x * row;
  
  vec2 pos = vec2(0);
  if(vtx_id == 0){
    pos = texelFetch(tex_position_A, ivec2(col, row), 0).xy;
  } else {
    pos = texelFetch(tex_position_B, ivec2(col, row), 0).xy;
  }
  
  gl_Position = vec4(pos * 2.0 - 1.0, 0, 1); // ndc: [-1, +1]
}

#endif



#if SHADER_FRAG

out vec4 glFragColor;

void main(){
  float len = line_uv;
  // glFragColor = vec4(1, sqrt(len)*0.9, len*len, (len) * 0.8 + 0.2);
  
  // glFragColor = vec4(1, len, len*len, (len) * 0.5 + 0.5);
  
  // glFragColor = vec4(1, (len) * 0.5 + 0.5, len, (len) * 0.5 + 0.5);
  
  glFragColor = mix(col_A, col_B, line_uv);
}


#endif

