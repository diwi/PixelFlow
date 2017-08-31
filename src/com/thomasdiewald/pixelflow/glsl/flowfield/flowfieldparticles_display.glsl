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

uniform float     point_size;
uniform ivec2     wh_position;
uniform sampler2D tex_position;
uniform vec4      col_A = vec4(1, 1, 1, 1.0);
uniform vec4      col_B = vec4(0, 0, 0, 0.0);

#if SHADER_VERT

out vec2 velocity;

void main(){

  // get point index / vertex index
  int point_id = gl_VertexID;

  // get position (xy)
  int row = point_id / wh_position.x;
  int col = point_id - wh_position.x * row;
  
  // get particle position, velocity
  vec4 position = texelFetch(tex_position, ivec2(col, row), 0);
  vec2 pos = position.xy;
  vec2 vel = position.zw;
  
  velocity = vel;

  gl_Position  = vec4(pos * 2 - 1, 0, 1); // ndc: [-1, +1]
  gl_PointSize = point_size;
}

#endif // #if SHADER_VERT



#if SHADER_FRAG

out vec4 glFragColor;

in vec2 velocity;

void main(){

  vec2 pc = abs(gl_PointCoord * 2.0 - 1.0); // abs[-1, 1]
  
  // 1) round + falloff with radius
  float falloff = 1.0 - clamp(length(pc), 0.0, 1.0);
  // falloff = pow(falloff, 1);
  
  glFragColor = mix(col_A, col_B, 1.0 - falloff);
  
}

#endif // #if SHADER_FRAG





