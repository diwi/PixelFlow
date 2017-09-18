/**
 * 
 * PixelFlow | Copyright (C) 2017 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */


#version 150

out vec4 out_frag;

uniform vec4 FG_mask;
uniform int  FG_invert = 0; // 1 to invert FG_mask
uniform sampler2D tex_scene;

ivec2 pos = ivec2(gl_FragCoord.xy);

int isFG(const in vec4 rgba){
  vec4 diff = rgba - FG_mask;
  float diff_sq = dot(diff, diff);
  return int(step(diff_sq, 0.0)) ^ FG_invert; // (rgba == FG_mask) ? 1 : 0
}

// returns 1 if the pixel is FG (ForeGround)
#define getFG(x,y) isFG(texelFetchOffset(tex_scene, pos, 0, ivec2(x,y)))

void main(){

  int FG = getFG( 0, 0);
  int t  = getFG( 0,-1);
  int b  = getFG( 0,+1);
  int l  = getFG(-1, 0);
  int r  = getFG(+1, 0);
  
  // int tl = getFG(-1,-1);
  // int tr = getFG(+1,-1);
  // int bl = getFG(-1,+1);
  // int br = getFG(+1,+1);
  
  int sum = t + b + l + r;
  // int sum = t + b + l + r + tl + tr + bl + br;

  // int EDGE = sum < 4 ? FG : 0;
  int EDGE = FG * (1 - int(sum / 4.0));
  out_frag = vec4(FG, EDGE, 0, 1);
}

