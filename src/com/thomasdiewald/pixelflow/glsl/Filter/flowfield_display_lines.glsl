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

#define LINE_MODE 0
#define LINE_SHADING 0

#define SHADER_VERT 0
#define SHADER_FRAG 0

uniform ivec2 wh_lines;
uniform  vec2 wh_lines_rcp;
uniform float vel_scale;
uniform vec4  col_A = vec4(0, 0, 0, 1.0);
uniform vec4  col_B = vec4(0, 0, 0, 0.5);
uniform sampler2D tex_velocity;

#if SHADER_VERT

out vec4 col_AB;

void main(){

  // get line index / vertex index
  int line_id = gl_VertexID / 2;
  int vtx_id  = gl_VertexID & 1; //  either 0 (line-start) or 1 (line-end)
  
  // get position (xy)
  int row = line_id / wh_lines.x;
  int col = line_id - wh_lines.x * row;


  // compute origin (line-start)
  vec2 origin = vec2(col, row) + 0.5;
  
  // get velocity from texture at origin location
  vec2 vel = texture(tex_velocity, origin * wh_lines_rcp).xy;
  
  // normalize
  // float len = length(vel);
  // if(len > 0.0001){
    // vel = clamp(len, 0.0, 1.0) * vel / len;
  // } else {
    // len = 0.0;
    // vel = vec2(0.0);
  // }
  

#if (LINE_MODE == 0)
  // lines, in velocity
  vec2 vtx_pos = origin + vel * vtx_id * vel_scale;
  col_AB = mix(col_A, col_B, float(vtx_id));
#endif // (LINE_MODE == 0)

#if (LINE_MODE == 1)
  // lines, normal to velocity
  vec2 vel_n = vec2(vel.y, -vel.x) * vel_scale;
  vec2 vtx_pos = origin + (- vel_n + vel_n * vtx_id * 2) * 0.25;
  col_AB = col_A;
#endif // (LINE_MODE == 1)


#if (LINE_SHADING == 1)
  float r = 0.5 * (1.0 + vel.x);
  float g = 0.5 * (1.0 + vel.y);
  float b = 0.5 * (2.0 - (r + g));
  col_AB = vec4(r, g, b, 1);
#endif // (LINE_SHADING == 1)
  
  // finish vertex coordinate
  vec2 vtx_pos_n = vtx_pos * wh_lines_rcp; // [0, 1]
  gl_Position = vec4(vtx_pos_n * 2.0 - 1.0, 0, 1); // ndc: [-1, +1]
  
}


#endif




#if SHADER_FRAG

out vec4 out_frag;

in vec4 col_AB;

void main(){
  out_frag = col_AB;
}

#endif
