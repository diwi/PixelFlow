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
uniform vec2  wh_rcp; 
uniform int   radius;
uniform ivec2 dir;

void main(){
  vec4 blur = texture(tex, gl_FragCoord.xy * wh_rcp);

  for(int i = 1; i <= +radius; i++){
    blur += texture(tex, (gl_FragCoord.xy + dir * i) * wh_rcp);
    blur += texture(tex, (gl_FragCoord.xy - dir * i) * wh_rcp);
  }
  
  glFragColor = blur / float(radius * 2 + 1);
}




