/**
 * 
 * PixelFlow | Copyright (C) 2017 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */
 
 
// Multipass Min/Max

#version 150

#define STEP_SIZE 2
// #define GET_TEX(v) texelFetchOffset(tex, pos, 0, v)
// #define GET_TEX(v) texelFetch(tex, pos + v, 0)
#define GET_TEX(v) texture(tex, (pos + v + 0.5)*wh_rcp, 0)
#define GET_MAX(a,b,c,d) max(max(a, b), max(c, d))
#define GET_MIN(a,b,c,d) min(min(a, b), min(c, d))

out vec4 out_frag;
uniform vec2 wh_rcp;
uniform sampler2D	tex;


void main(){

#if (STEP_SIZE == 2)
  ivec2 pos = ivec2(gl_FragCoord.xy) * STEP_SIZE;
  
  vec4 A0 = GET_TEX(ivec2(0,0)); vec4 A1 = GET_TEX(ivec2(1,0));
  vec4 B0 = GET_TEX(ivec2(0,1)); vec4 B1 = GET_TEX(ivec2(1,1));

  out_frag = GET_MAX(A0, A1, B0, B1);
#endif


#if (STEP_SIZE == 4)
  ivec2 pos = ivec2(gl_FragCoord.xy) * STEP_SIZE;

  vec4 A0 = GET_TEX(ivec2(0,0)); vec4 A1 = GET_TEX(ivec2(1,0)); vec4 A2 = GET_TEX(ivec2(2,0)); vec4 A3 = GET_TEX(ivec2(3,0));
  vec4 B0 = GET_TEX(ivec2(0,1)); vec4 B1 = GET_TEX(ivec2(1,1)); vec4 B2 = GET_TEX(ivec2(2,1)); vec4 B3 = GET_TEX(ivec2(3,1)); 
  vec4 C0 = GET_TEX(ivec2(0,2)); vec4 C1 = GET_TEX(ivec2(1,2)); vec4 C2 = GET_TEX(ivec2(2,2)); vec4 C3 = GET_TEX(ivec2(3,2)); 
  vec4 D0 = GET_TEX(ivec2(0,3)); vec4 D1 = GET_TEX(ivec2(1,3)); vec4 D2 = GET_TEX(ivec2(2,3)); vec4 D3 = GET_TEX(ivec2(3,3));

  vec4 mA = GET_MAX(A0, A1, A2, A3);
  vec4 mB = GET_MAX(B0, B1, B2, B3);
  vec4 mC = GET_MAX(C0, C1, C2, C3);
  vec4 mD = GET_MAX(D0, D1, D2, D3);
  
  out_frag = GET_MAX(mA, mB, mC, mD);
#endif
 

}





