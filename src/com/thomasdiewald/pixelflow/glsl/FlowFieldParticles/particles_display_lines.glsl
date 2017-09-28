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
uniform float     shader_collision_mult = 1.0;
uniform sampler2D tex_collision;
uniform sampler2D tex_position;
uniform vec4      col_A = vec4(1, 1, 1, 1.0);
uniform vec4      col_B = vec4(0, 0, 0, 0.0);

#if SHADER_VERT

out float pressure;

void main(){

  // get point index / vertex index
  int point_id = gl_VertexID / 2;
  int vtx_id   = gl_VertexID & 1; //  either 0 (line-start) or 1 (line-end)

  // get position (xy)
  int row = point_id / wh_position.x;
  int col = point_id - wh_position.x * row;
  
  // get particle position, velocity
  vec4 particle = texelFetch(tex_position, ivec2(col, row), 0);
  vec2 pos_cur = particle.xy;
  vec2 pos_old = particle.zw;
  vec2 pos = (vtx_id == 1) ? pos_cur : pos_old;

  // should be stripped away by the compiler for SHADER_FRAG_COLLISION == 1
  {
    float vel = length(pos_cur - pos_old) * 2000;
    pressure = texture(tex_collision, pos_cur).r + vel;
  }

  gl_Position  = vec4(pos * 2.0 - 1.0, 0, 1); // ndc: [-1, +1]
}

#endif // #if SHADER_VERT


#if SHADER_FRAG

out vec4 out_frag;
in float pressure;

void main(){
  out_frag = col_A;
  float pf = 1.0 + pressure * shader_collision_mult;
  out_frag.xyzw *= pf;
  out_frag = clamp(out_frag, 0.0, 1.0);
}

#endif // #if SHADER_FRAG




