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
#define SHADER_FRAG_COLLISION 0
#define SHADER_FRAG_DISPLAY 0

uniform float     point_size;
uniform ivec2     wh_position;
uniform float     shader_collision_mult = 1.0;
uniform sampler2D tex_collision;
uniform sampler2D tex_position;
uniform sampler2D tex_sprite;
uniform vec4      col_A = vec4(1, 1, 1, 1.0);
uniform vec4      col_B = vec4(0, 0, 0, 0.0);

#if SHADER_VERT

out float pressure;

void main(){

  // get point index / vertex index
  int point_id = gl_VertexID;

  // get position (xy)
  int row = point_id / wh_position.x;
  int col = point_id - wh_position.x * row;
  
  // get particle position, velocity
  vec4 particle = texelFetch(tex_position, ivec2(col, row), 0);
  vec2 pos = particle.xy;

  // should be stripped away by the compiler for SHADER_FRAG_COLLISION == 1
  {
    float vel = length(pos - particle.zw) * 2000;
    pressure = texture(tex_collision, pos).r + vel;
  }

  gl_Position  = vec4(pos * 2.0 - 1.0, 0, 1); // ndc: [-1, +1]
  gl_PointSize = point_size;
}

#endif // #if SHADER_VERT


#if SHADER_FRAG_COLLISION

out float out_frag;

void main(){
  out_frag = max(0, 1.0 - length(gl_PointCoord * 2.0 - 1.0));
}

#endif // #if SHADER_FRAG



#if SHADER_FRAG_DISPLAY

out vec4 out_frag;
in float pressure;

void main(){
  float falloff = texture(tex_sprite, gl_PointCoord).a;
  out_frag = mix(col_A, col_B, 1.0 - falloff);
  
  float pf = 1.0 + pressure * shader_collision_mult;
  out_frag.xyzw *= pf;
}

#endif // #if SHADER_FRAG




