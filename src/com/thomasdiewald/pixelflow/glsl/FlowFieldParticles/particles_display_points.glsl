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
uniform vec2      wh_velocity;
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
  vec2 vel = (particle.xy - particle.zw) * wh_velocity;

  pressure = texture(tex_collision, pos).r + length(vel) * 2.0;

  gl_Position  = vec4(pos * 2.0 - 1.0, 0, 1); // ndc: [-1, +1]
  gl_PointSize = point_size;
}

#endif // #if SHADER_VERT



#if SHADER_FRAG

out vec4 out_frag;
in float pressure;

void main(){
  float falloff = 1.0 - texture(tex_sprite, gl_PointCoord).a;
  out_frag  = mix(col_A, col_B, falloff);
  out_frag *= 1.0 + pressure * shader_collision_mult;
  out_frag  = clamp(out_frag, 0.0, 1.0);
}

#endif // #if SHADER_FRAG




