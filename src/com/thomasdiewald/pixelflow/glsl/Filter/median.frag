/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */


#version 150

#define MEDIAN_3x3 0
#define MEDIAN_5x5 0

out vec4 glFragColor;
uniform sampler2D	tex;
uniform vec2 wh_rcp; 

// MIN MAX ELIMINATION 
#define _S2_(a,b) data_a_ = data[a]; data[a] = min(data[a], data[b]); data[b] = max(data_a_, data[b]);

#define _MN_3_(_1,_2,_3) _S2_(_1,_2); _S2_(_1,_3); 
#define _MX_3_(_1,_2,_3) _S2_(_2,_3); _S2_(_1,_3); 

#define _MN_4_(_1,_2,_3,_4) _MN_3_(_2,_3,_4); _S2_(_1,_2);
#define _MN_5_(_1,_2,_3,_4,_5) _MN_4_(_2,_3,_4,_5); _S2_(_1,_2);
#define _MN_6_(_1,_2,_3,_4,_5,_6) _MN_5_(_2,_3,_4,_5,_6); _S2_(_1,_2);
#define _MN_7_(_1,_2,_3,_4,_5,_6,_7) _MN_6_(_2,_3,_4,_5,_6,_7); _S2_(_1,_2);

#define _MX_4_(_1,_2,_3,_4) _MX_3_(_1,_2,_3); _S2_(_3,_4);
#define _MX_5_(_1,_2,_3,_4,_5) _MX_4_(_1,_2,_3,_4); _S2_(_4,_5);
#define _MX_6_(_1,_2,_3,_4,_5,_6) _MX_5_(_1,_2,_3,_4,_5); _S2_(_5,_6);
#define _MX_7_(_1,_2,_3,_4,_5,_6,_7) _MX_6_(_1,_2,_3,_4,_5,_6); _S2_(_6,_7);
#define _MX_8_(_1,_2,_3,_4,_5,_6,_7,_8) _MX_7_(_1,_2,_3,_4,_5,_6,_7); _S2_(_7,_8);

#define _MNMX_3_(_1,_2,_3) _MX_3_(_1,_2,_3); _S2_(_1,_2); 
#define _MNMX_4_(_1,_2,_3,_4) _S2_(_1,_2); _S2_(_3,_4); _S2_(_1,_3); _S2_(_2,_4); 
#define _MNMX_5_(_1,_2,_3,_4,_5) _S2_(_1,_2); _S2_(_3,_4); _MN_3_(_1,_3,_5); _MX_3_(_2,_4,_5); 
#define _MNMX_6_(_1,_2,_3,_4,_5,_6) _S2_(_1,_2); _S2_(_3,_4); _S2_(_5,_6); _MN_3_(_1,_3,_5); _MX_3_(_2,_4,_6); 
#define _MNMX_7_(_1,_2,_3,_4,_5,_6,_7) _S2_(_1,_2); _S2_(_3,_4); _S2_(_5,_6); _MN_4_(_1,_3,_5,_7); _MX_4_(_2,_4,_6,_7); 
#define _MNMX_8_(_1,_2,_3,_4,_5,_6,_7,_8) _S2_(_1,_2); _S2_(_3,_4); _S2_(_5,_6); _S2_(_7,_8); _MN_4_(_1,_3,_5,_7); _MX_4_(_2,_4,_6,_8); 
#define _MNMX_9_(_1,_2,_3,_4,_5,_6,_7,_8,_9) _S2_(_1,_2); _S2_(_3,_4); _S2_(_5,_6); _S2_(_7,_8); _MN_5_(_1,_3,_5,_7,_9); _MX_5_(_2,_4,_6,_8,_9); 
#define _MNMX_10_(_1,_2,_3,_4,_5,_6,_7,_8,_9,_10) _S2_(_1,_2); _S2_(_3,_4); _S2_(_5,_6); _S2_(_7,_8); _S2_(_9,_10); _MN_5_(_1,_3,_5,_7,_9); _MX_5_(_2,_4,_6,_8,_10); 
#define _MNMX_11_(_1,_2,_3,_4,_5,_6,_7,_8,_9,_10,_11) _S2_(_1,_2); _S2_(_3,_4); _S2_(_5,_6); _S2_(_7,_8); _S2_(_9,_10); _MN_6_(_1,_3,_5,_7,_9,_11); _MX_6_(_2,_4,_6,_8,_10,_11); 
#define _MNMX_12_(_1,_2,_3,_4,_5,_6,_7,_8,_9,_10,_11,_12) _S2_(_1,_2); _S2_(_3,_4); _S2_(_5,_6); _S2_(_7,_8); _S2_(_9,_10); _S2_(_11,_12); _MN_6_(_1,_3,_5,_7,_9,_11); _MX_6_(_2,_4,_6,_8,_10,_12); 
#define _MNMX_13_(_1,_2,_3,_4,_5,_6,_7,_8,_9,_10,_11,_12,_13) _S2_(_1,_2); _S2_(_3,_4); _S2_(_5,_6); _S2_(_7,_8); _S2_(_9,_10); _S2_(_11,_12); _MN_7_(_1,_3,_5,_7,_9,_11,_13); _MX_7_(_2,_4,_6,_8,_10,_12,_13); 
#define _MNMX_14_(_1,_2,_3,_4,_5,_6,_7,_8,_9,_10,_11,_12,_13,_14) _S2_(_1,_2); _S2_(_3,_4); _S2_(_5,_6); _S2_(_7,_8); _S2_(_9,_10); _S2_(_11,_12); _S2_(_13,_14); _MN_7_(_1,_3,_5,_7,_9,_11,_13); _MX_7_(_2,_4,_6,_8,_10,_12,_14); 


void main(){
  vec2 posn = gl_FragCoord.xy * wh_rcp;
  
  vec4 data_a_;
  
#if MEDIAN_3x3
 
  vec4 data[9];
  data[0] = textureOffset(tex, posn, ivec2(-1,-1));
  data[1] = textureOffset(tex, posn, ivec2( 0,-1));
  data[2] = textureOffset(tex, posn, ivec2(+1,-1));  
  data[3] = textureOffset(tex, posn, ivec2(-1, 0));
  data[4] = textureOffset(tex, posn, ivec2( 0, 0));
  data[5] = textureOffset(tex, posn, ivec2(+1, 0));
  data[6] = textureOffset(tex, posn, ivec2(-1,+1));
  data[7] = textureOffset(tex, posn, ivec2( 0,+1));
  data[8] = textureOffset(tex, posn, ivec2(+1,+1));

  _MNMX_6_(0,1,2,3,4,5);
  _MNMX_5_(1,2,3,4,6);
  _MNMX_4_(2,3,4,7);
  _MNMX_3_(3,4,8);
  
#endif // MEDIAN_3x3 
  
  
#if MEDIAN_5x5
  
  vec4 data[25];
  data[ 0] = textureOffset(tex, posn, ivec2(-2,-2));
  data[ 1] = textureOffset(tex, posn, ivec2(-1,-2));
  data[ 2] = textureOffset(tex, posn, ivec2( 0,-2));
  data[ 3] = textureOffset(tex, posn, ivec2(+1,-2));
  data[ 4] = textureOffset(tex, posn, ivec2(+2,-2));
        
  data[ 5] = textureOffset(tex, posn, ivec2(-2,-1));
  data[ 6] = textureOffset(tex, posn, ivec2(-1,-1));
  data[ 7] = textureOffset(tex, posn, ivec2( 0,-1));
  data[ 8] = textureOffset(tex, posn, ivec2(+1,-1));
  data[ 9] = textureOffset(tex, posn, ivec2(+2,-1));
  
  data[10] = textureOffset(tex, posn, ivec2(-2, 0));
  data[11] = textureOffset(tex, posn, ivec2(-1, 0));
  data[12] = textureOffset(tex, posn, ivec2( 0, 0));
  data[13] = textureOffset(tex, posn, ivec2(+1, 0));
  data[14] = textureOffset(tex, posn, ivec2(+2, 0));
  
  data[15] = textureOffset(tex, posn, ivec2(-2,+1));
  data[16] = textureOffset(tex, posn, ivec2(-1,+1));
  data[17] = textureOffset(tex, posn, ivec2( 0,+1));
  data[18] = textureOffset(tex, posn, ivec2(+1,+1));
  data[19] = textureOffset(tex, posn, ivec2(+2,+1));
  
  data[20] = textureOffset(tex, posn, ivec2(-2,+2));
  data[21] = textureOffset(tex, posn, ivec2(-1,+2));
  data[22] = textureOffset(tex, posn, ivec2( 0,+2));
  data[23] = textureOffset(tex, posn, ivec2(+1,+2));
  data[24] = textureOffset(tex, posn, ivec2(+2,+2));

  _MNMX_14_(0,1,2,3,4,5,6,7,8,9,10,11,12,13);
  _MNMX_13_(1,2,3,4,5,6,7,8,9,10,11,12,14);
  _MNMX_12_(2,3,4,5,6,7,8,9,10,11,12,15);
  _MNMX_11_(3,4,5,6,7,8,9,10,11,12,16);
  _MNMX_10_(4,5,6,7,8,9,10,11,12,17);
  _MNMX_9_(5,6,7,8,9,10,11,12,18);
  _MNMX_8_(6,7,8,9,10,11,12,19);
  _MNMX_7_(7,8,9,10,11,12,20);
  _MNMX_6_(8,9,10,11,12,21);
  _MNMX_5_(9,10,11,12,22);
  _MNMX_4_(10,11,12,23);
  _MNMX_3_(11,12,24);
  
#endif // MEDIAN_5x5
  
  glFragColor = data_a_;
}



