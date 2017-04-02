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

uniform sampler2D	tex_blur0;
uniform sampler2D	tex_blur1;
uniform sampler2D	tex_blur2;
uniform sampler2D	tex_blur3;
uniform sampler2D	tex_blur4;

uniform float tex_weights[5] = float[](1.0, 1.0, 1.0, 1.0, 1.0);

uniform vec2  wh_rcp; 

void main(){
  vec2 posn = gl_FragCoord.xy * wh_rcp;
       
  glFragColor = tex_weights[0] * texture(tex_blur0, posn) + 
	 							tex_weights[1] * texture(tex_blur1, posn) + 
								tex_weights[2] * texture(tex_blur2, posn) + 
								tex_weights[3] * texture(tex_blur3, posn) + 
								tex_weights[4] * texture(tex_blur4, posn);
}
