/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */


#version 150

out vec4 glFragColor;
uniform sampler2D	tex;
uniform vec2   wh; 

// MIN MAX ELIMINATION 
#define _S2_(a,b) data_a_ = data[a]; data[a] = min(data[a], data[b]); data[b] = max(data_a_, data[b]);

#define _MN_3_(_1,_2,_3) _S2_(_1,_2); _S2_(_1,_3); 
#define _MX_3_(_1,_2,_3) _S2_(_2,_3); _S2_(_1,_3); 

#define _MNMX_3_(_1,_2,_3) _MX_3_(_1,_2,_3); _S2_(_1,_2); 
#define _MNMX_4_(_1,_2,_3,_4) _S2_(_1,_2); _S2_(_3,_4); _S2_(_1,_3); _S2_(_2,_4); 
#define _MNMX_5_(_1,_2,_3,_4,_5) _S2_(_1,_2); _S2_(_3,_4); _MN_3_(_1,_3,_5); _MX_3_(_2,_4,_5); 
#define _MNMX_6_(_1,_2,_3,_4,_5,_6) _S2_(_1,_2); _S2_(_3,_4); _S2_(_5,_6); _MN_3_(_1,_3,_5); _MX_3_(_2,_4,_6); 

void main(){
  vec2 posn = gl_FragCoord.xy / wh;
  
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

  
  vec4 data_a_;

  _MNMX_6_(0,1,2,3,4,5);
  _MNMX_5_(1,2,3,4,6);
  _MNMX_4_(2,3,4,7);
  _MNMX_3_(3,4,8);
  
  glFragColor = data_a_;
  
}



