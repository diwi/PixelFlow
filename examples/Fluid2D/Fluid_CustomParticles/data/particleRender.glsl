/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */


#version 150


#define SHADER_VERT 0
#define SHADER_FRAG 0

// uniforms are shared by all all shaders.
uniform  vec2     wh_viewport;
uniform ivec2     num_particles;
uniform float     point_size;
uniform sampler2D tex_particles;


#if SHADER_VERT

out vec4 particle;

void main(){

  // get point index / vertex index
  int point_id = gl_VertexID;

  // get position (xy)
  int row = point_id / num_particles.x;
  int col = point_id - num_particles.x * row;
  
  // compute texture location [0, 1]
  vec2 posn = (vec2(col, row)+0.5) / vec2(num_particles); 
  
  // get particel data: pos, vel
  particle = texture(tex_particles, posn);
  
  // finish vertex coordinate
  gl_Position  = vec4(particle.xy * 2.0 - 1.0, 0, 1); // ndc: [-1, +1]
  gl_PointSize = point_size;
}

#endif // #if SHADER_VERT




#if SHADER_FRAG

in vec4 particle;
out vec4 glFragColor;


void main(){

  vec2 my_PointCoord = ((particle.xy * wh_viewport) - gl_FragCoord.xy) / point_size + 0.5;

  vec2 pc = abs(my_PointCoord * 2.0 - 1.0); // abs[-1, 1]
  
  // 1) round + falloff with radius
  float falloff = 1.0 - clamp(length(pc), 0.0, 1.0);
  falloff = pow(falloff, 0.3);
  
  // 2) round + evenly shaded 
  // if(length(pc) > 1.0) falloff = 0.0; else falloff = 1.0;

  float len = length(particle.zw) * 0.035;
  glFragColor = vec4(len, 0.5, 1-len, len * 0.5 + 0.5);
  glFragColor.a *= falloff;
}

#endif // #if SHADER_VERT

