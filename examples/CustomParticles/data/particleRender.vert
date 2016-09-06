/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */


#version 150


out vec2 velocity;

uniform ivec2     num_particles;
uniform float     point_size;
uniform sampler2D tex_particles;

void main(){

  // get point index / vertex index
  int point_id = gl_VertexID;

  // get position (xy)
  int row = point_id / num_particles.x;
  int col = point_id - num_particles.x * row;
  
  // compute texture location [0, 1]
  vec2 posn = (vec2(col, row)+0.5) / vec2(num_particles); 
  
  // get particel data
  vec4 particel_data = texture(tex_particles, posn);
  
  vec2 particel_pos = particel_data.xy;
  vec2 particel_vel = particel_data.zw;
  
  // get fluid data at current location
  velocity = particel_vel;

  // finish vertex coordinate
  vec2 vtx_pos_n = particel_pos; // [0, 1]

  gl_Position  = vec4(vtx_pos_n * 2.0 - 1.0, 0, 1); // ndc: [-1, +1]
  gl_PointSize = point_size;
}
