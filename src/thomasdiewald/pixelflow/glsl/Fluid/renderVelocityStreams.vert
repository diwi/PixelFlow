/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */


#version 150

precision mediump float;
precision mediump int;

out vec2  velocity;    // used for rgb-color in the fragrment shader
out float line_domain; // used for alpha channel in the fragmentshader

uniform vec2  wh;
uniform vec2  spacing;
uniform int   display_mode;
uniform ivec2 num_lines;
uniform float velocity_scale;
uniform sampler2D tex_velocity;


void main(){

  // get line index / vertex index
  int line_id = gl_VertexID / 2;
  int vtx_id  = gl_VertexID & 1; //  either 0 (line-start) or 1 (line-end)
  
  // get position (xy)
  int row = line_id / num_lines.x;
  int col = line_id - num_lines.x * row;
  
  // compute origin (line-start)
  vec2 offset = spacing * 0.5;
  vec2 origin = offset + vec2(col, row) * spacing;
  
  // get velocity from texture at origin location
  velocity = texture(tex_velocity, origin / wh).xy;
  
  // scale velocity
  vec2 dir = velocity * velocity_scale;
  
  float len = length(dir + 0.00001);
  dir = dir / sqrt(len*0.08);
  
  // for fragmentshader ... coloring
  velocity = dir*0.2;
  
  // compute current vertex position (based on vtx_id)
  vec2 vtx_pos = vec2(0);
  
  // lines, in velocity direction
  if(display_mode == 0)
  {
    vtx_pos = origin + dir * vtx_id;
    line_domain = 1.0 - float(vtx_id);
  }

  // lines, normal to velocity direction
  if(display_mode == 1)
  {
    dir *= 0.2;
    vec2 dir_n = vec2(dir.y, -dir.x);
    vtx_pos = origin + dir - dir_n + dir_n * vtx_id * 2;
    line_domain = 1.0;
  }
  
  // finish vertex coordinate
  vec2 vtx_pos_n = (vtx_pos + 0.5) / wh; // [0, 1]
  gl_Position = vec4(vtx_pos_n * 2.0 - 1.0, 0, 1); // ndc: [-1, +1]
  
}
