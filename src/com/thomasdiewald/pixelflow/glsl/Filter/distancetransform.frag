/**
 * 
 * PixelFlow | Copyright (C) 2017 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */

 
 //
 // resouces
 //
 // Jumpflood Algorithm (JFA)
 //
 // Jump Flooding in GPU with Applications to Voronoi Diagram and Distance Transform
 // www.comp.nus.edu.sg/~tants/jfa/i3d06.pdf
 //

 


#version 150

#define PASS_INIT 0
#define PASS_DTNN 0

#define POS_MAX 0x7FFF // == 32767 == ((1<<15) - 1)
#define LENGTH_SQ(dir) ((dir).x*(dir).x + (dir).y*(dir).y)

// --------------------------------------------------------------
// PASS 1 - init positions from given mask
// --------------------------------------------------------------
#if PASS_INIT

out ivec2 out_pos;
uniform vec4 mask = vec4(1,1,1,0);
uniform int XOR = 0; // 1 to invert mask
uniform sampler2D	tex_mask;

void main(){
  // ivec2 pos = ivec2(gl_FragCoord.xy);
  // bool mask = any(notEqual(texelFetch(tex_mask, pos, 0).rgb, vec3(0.0)));
  // out_pos = mask ? pos : ivec2(POS_MAX);
  
  // ivec2 pos = ivec2(gl_FragCoord.xy);
  // vec4 rgba = texelFetch(tex_mask, pos, 0);
  // out_pos = (all(equal(rgba, mask))) ? pos : ivec2(POS_MAX);
  
  
  
  ivec2 pos = ivec2(gl_FragCoord.xy);
  vec4 rgba = texelFetch(tex_mask, pos, 0);
  
  vec4 diff = rgba - mask;
  float diff_sq = dot(diff, diff);
  int ismask = int(1.0 - step(diff_sq, 0.0)); // rgba == mask ? 0 : 1
  out_pos = (ismask ^ XOR) == 0 ? pos : ivec2(POS_MAX);
}

#endif // PASS_INIT



// --------------------------------------------------------------
// PASS 2 - update positions (nearest)
// --------------------------------------------------------------
#if PASS_DTNN

#define TEXACCESS 0

#if (TEX_READ_VERSION == 0) 
  // needs the position to be clamped
  #define getDTNN(tex, off) texelFetch(tex, clamp(pos + off, ivec2(0), wh - 1), 0).xy
#endif
#if (TEX_READ_VERSION == 1) 
  // needs wrap-param GL_CLAMP_TO_EDGE
  #define getDTNN(tex, off) texture(tex, (gl_FragCoord.xy + off) / wh).xy
#endif

out ivec2 out_dtnn;

uniform isampler2D tex_dtnn;
uniform ivec3 jump;
uniform ivec2 wh;

ivec2 pos  = ivec2(gl_FragCoord.xy);
ivec2 dtnn = ivec2(POS_MAX);
int   dmin = LENGTH_SQ(dtnn);

void DTNN(const in ivec2 off){
  ivec2 dtnn_cur = getDTNN(tex_dtnn, off);
  ivec2 ddxy = dtnn_cur - pos;
  int dcur = LENGTH_SQ(ddxy);
  if(dcur < dmin){
    dmin = dcur;
    dtnn = dtnn_cur;
  }
}

void main(){
  dtnn = getDTNN(tex_dtnn, jump.yy);
  ivec2 ddxy = dtnn - pos;
  dmin = LENGTH_SQ(ddxy);
  
  DTNN(jump.xx); DTNN(jump.yx); DTNN(jump.zx);
  DTNN(jump.xy);                DTNN(jump.zy);
  DTNN(jump.xz); DTNN(jump.yz); DTNN(jump.zz);

  out_dtnn = dtnn;
}

// void main(){
  // DTNN(jump.xx); DTNN(jump.yx); DTNN(jump.zx);
  // DTNN(jump.xy); DTNN(jump.yy); DTNN(jump.zy);
  // DTNN(jump.xz); DTNN(jump.yz); DTNN(jump.zz);

  // out_dtnn = dtnn;
// }

#endif // PASS_DTNN

