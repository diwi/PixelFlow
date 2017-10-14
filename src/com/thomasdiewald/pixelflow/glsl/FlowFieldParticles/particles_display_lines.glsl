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
uniform vec2      wh_viewport;
uniform float     shader_collision_mult = 1.0;
uniform sampler2D tex_collision;
uniform sampler2D tex_position;
uniform vec4      col_A = vec4(1, 1, 1, 1.0);
uniform vec4      col_B = vec4(0, 0, 0, 0.0);

#if SHADER_VERT

out vec4 particle;
out float domain;

void main(){

  // get point index / vertex index
  int point_id = gl_VertexID / 2;
  int vtx_id   = gl_VertexID & 1; //  either 0 (line-start) or 1 (line-end)

  // get position (xy)
  int row = point_id / wh_position.x;
  int col = point_id - wh_position.x * row;
  
  // get particle position, velocity
  vec4 particle = texelFetch(tex_position, ivec2(col, row), 0);
  
  domain = 1.0 - float(vtx_id); // 0 ... 1
  vec2 pos = mix(particle.xy, particle.zw, domain);

  gl_Position  = vec4(pos * 2.0 - 1.0, 0, 1); // ndc: [-1, +1]
}

#endif // #if SHADER_VERT


#if SHADER_FRAG

in vec4 particle;
in float domain;

out vec4 out_frag;

void main(){
  vec2 pos = mix(particle.xy, particle.zw, domain);
  float pressure = texture(tex_collision, pos).r;
  vec2  velocity = (particle.xy - particle.zw) * wh_viewport;
  float mult = pressure + length(velocity) * 2.0;
  mult *= shader_collision_mult;

  out_frag  = col_A;
  out_frag *= 1.0 + mult;
  out_frag  = clamp(out_frag, 0.0, 1.0);
}

#endif // #if SHADER_FRAG




