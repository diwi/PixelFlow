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

uniform vec2 wh_rcp;

vec2 posn = gl_FragCoord.xy * wh_rcp;


#define TEX_LAYERS 0 // set by application at compile time

#define TM(tex,mad) texture(tex, posn) * mad.x + mad.y


#if (TEX_LAYERS == 1)

uniform vec2      m0;
uniform sampler2D t0;
void main(){
  out_frag = TM(t0,m0);   
}

#elif (TEX_LAYERS == 2)

uniform vec2      m0, m1;
uniform sampler2D t0, t1;
void main(){
  out_frag = TM(t0,m0) + TM(t1,m1);   
}

#elif (TEX_LAYERS == 3)

uniform vec2      m0, m1, m2;
uniform sampler2D t0, t1, t2;
void main(){
  out_frag = TM(t0,m0) + TM(t1,m1) + TM(t2,m2);   
}

#elif (TEX_LAYERS == 4)

uniform vec2      m0, m1, m2, m3;
uniform sampler2D t0, t1, t2, t3;
void main(){
  out_frag = TM(t0,m0) + TM(t1,m1) + TM(t2,m2) + TM(t3,m3);   
}

#elif (TEX_LAYERS == 5)

uniform vec2      m0, m1, m2, m3, m4;
uniform sampler2D t0, t1, t2, t3, t4;
void main(){
  out_frag = TM(t0,m0) + TM(t1,m1) + TM(t2,m2) + TM(t3,m3)+ TM(t4,m4);   
}

#elif (TEX_LAYERS >= 6)

uniform vec2      mN[TEX_LAYERS];
uniform sampler2D tN[TEX_LAYERS];
void main(){
  out_frag = vec4(0.0);
  for(int i = 0; i < TEX_LAYERS; i++){
    out_frag += TM(tN[i], mN[i]);
  }                 
}

#endif




