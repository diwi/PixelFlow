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

// STEP_SIZE can be 2, 3 or 4, ... 3 and 4 are the fastest
#define STEP_SIZE 4

// #define TEX(x, y) texelFetchOffset(tex, pos, 0, ivec2(x,y))
// #define TEX(x, y) texelFetch(tex, pos + ivec2(x,y), 0)
 #define TEX(x, y) texture(tex, (pos + ivec2(x,y) + 0.5)*wh_rcp, 0)


#define MODE_MIN 0 // set by app before compiling
#define MODE_MAX 0 // set by app before compiling

#if MODE_MAX
  #define M2(a,b) max(a, b)
#elif MODE_MIN
  #define M2(a,b) min(a, b)
#endif

#define M3(a,b,c) M2(M2(a, b), c)
#define M4(a,b,c,d) M2(M2(a, b), M2(c, d))


out vec4 out_frag;
uniform vec2 wh_rcp;
uniform ivec2 off;
uniform sampler2D tex;


void main(){

  ivec2 pos = (ivec2(gl_FragCoord.xy)-off) * STEP_SIZE;

#if (STEP_SIZE == 2)
  vec4 A0 = TEX(0,0), A1 = TEX(1,0);
  vec4 B0 = TEX(0,1), B1 = TEX(1,1);

  out_frag = M4(A0, A1, B0, B1);
#endif


#if (STEP_SIZE == 3)
  vec4 A0 = TEX(0,0), A1 = TEX(1,0), A2 = TEX(2,0);
  vec4 B0 = TEX(0,1), B1 = TEX(1,1), B2 = TEX(2,1);
  vec4 C0 = TEX(0,2), C1 = TEX(1,2), C2 = TEX(2,2);

  vec4 mA = M3(A0, A1, A2);
  vec4 mB = M3(B0, B1, B2);
  vec4 mC = M3(C0, C1, C2);
  
  out_frag = M3(mA, mB, mC);
#endif


#if (STEP_SIZE == 4)
  vec4 A0 = TEX(0,0), A1 = TEX(1,0), A2 = TEX(2,0), A3 = TEX(3,0);
  vec4 B0 = TEX(0,1), B1 = TEX(1,1), B2 = TEX(2,1), B3 = TEX(3,1); 
  vec4 C0 = TEX(0,2), C1 = TEX(1,2), C2 = TEX(2,2), C3 = TEX(3,2); 
  vec4 D0 = TEX(0,3), D1 = TEX(1,3), D2 = TEX(2,3), D3 = TEX(3,3);

  vec4 mA = M4(A0, A1, A2, A3);
  vec4 mB = M4(B0, B1, B2, B3);
  vec4 mC = M4(C0, C1, C2, C3);
  vec4 mD = M4(D0, D1, D2, D3);
  
  out_frag = M4(mA, mB, mC, mD);
#endif
 
}





