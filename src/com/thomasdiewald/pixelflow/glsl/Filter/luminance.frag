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
uniform vec2 wh_rcp; 
uniform vec3 luminance = vec3(0.2989, 0.5870, 0.1140);

void main(){
  glFragColor = texture(tex, gl_FragCoord.xy * wh_rcp);
  glFragColor.rgb = vec3(dot(glFragColor.rgb, luminance));
}





