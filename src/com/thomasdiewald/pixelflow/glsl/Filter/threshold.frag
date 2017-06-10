/**
 * 
 * PixelFlow | Copyright (C) 2017 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */


#version 150

out vec4 glFragColor;

uniform sampler2D	tex;
uniform vec2 wh_rcp; 
uniform vec4 threshold = vec4(1.0f);

void main(){

  vec2 posn = gl_FragCoord.xy * wh_rcp;
  vec4 color = texture(tex, posn);
 
  // if(color.r < threshold.r) color.r = 0;
  // if(color.g < threshold.r) color.g = 0;
  // if(color.b < threshold.r) color.b = 0;
  
  // if(color.r < threshold.r) color.r *= 0.5;
  // if(color.g < threshold.g) color.g *= 0.5;
  // if(color.b < threshold.b) color.b *= 0.5;

  // if(color.r < threshold.r || color.g < threshold.g || color.b < threshold.b) color *= 0.5;

  
  if(color.a < threshold.a){
  
    color.a /= threshold.a;
    color.a = pow(color.a, 5);
    color.a *= threshold.a;
    //color.a = 0;
  }
  
  glFragColor = color;
  // glFragColor = vec4(1,0,0,1);
}

