/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */


#version 150


uniform ivec2     num_lines;
uniform sampler2D tex_vertices_V0;
uniform sampler2D tex_vertices_V1;

void main(){

  // get line index / vertex index
  int line_id = gl_VertexID / 2;
  int vtx_id  = gl_VertexID & 1; //  either 0 (line-start) or 1 (line-end)

  // get position (xy)
  int row = line_id / num_lines.x;
  int col = line_id - num_lines.x * row;
  
  // texture position
  vec2 posn = (vec2(col, row) + 0.5) / vec2(num_lines);
  
  // get line vertex
  vec2 vtx_pos = vec2(0);
  if(vtx_id == 0){
    vtx_pos = texture(tex_vertices_V0, posn).xy;
  } else {
    vtx_pos = texture(tex_vertices_V1, posn).xy;
  }
  
  // finish vertex coordinate
  gl_Position = vec4(vtx_pos * 2.0 - 1.0, 0, 1); // ndc: [-1, +1]
  
}
