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
uniform  vec2     wh_viewport_rcp;
uniform sampler2D tex_position;
uniform sampler2D tex_sprite;
uniform vec4      col_A = vec4(1, 1, 1, 1.0);
uniform vec4      col_B = vec4(0, 0, 0, 0.0);

#if SHADER_VERT

out vec2 texcoord;

void main(){

  // get point index / vertex index
  int point_id = gl_InstanceID;
  int vtx_id   = gl_VertexID;

  // vertex coordinate
  int vx = ((vtx_id<<1) & 2) - 1; // [-1, +1]
  int vy = ((vtx_id   ) & 2) - 1; // [-1, +1]
  
  vec2 vtx = vec2(vx, vy);
  
  texcoord = vtx * 0.5 + 0.5; // [0, 1}]
  
  vtx = vtx * point_size * wh_viewport_rcp;

  // get position (xy)
  int row = point_id / wh_position.x;
  int col = point_id - wh_position.x * row;
  
  // get particle position, velocity
  vec4 particle = texelFetch(tex_position, ivec2(col, row), 0);
  vec2 pos = particle.xy * 2.0 - 1.0;
  
  gl_Position  = vec4(vtx + pos, 0, 1); // ndc: [-1, +1]
}

#endif // #if SHADER_VERT


#if SHADER_FRAG_COLLISION

in vec2 texcoord;  
out float out_frag;

void main(){
  out_frag = max(0, 1.0 - length(texcoord * 2.0 - 1.0));
}

#endif // #if SHADER_FRAG



#if SHADER_FRAG_DISPLAY

in vec2 texcoord;  
out vec4 out_frag;

void main(){
  float falloff = texture(tex_sprite, texcoord).a;
  out_frag = mix(col_A, col_B, 1.0 - falloff);
}

#endif // #if SHADER_FRAG




