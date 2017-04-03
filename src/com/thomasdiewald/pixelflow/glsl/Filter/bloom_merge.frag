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

// uniform sampler2D	tex_blur0;
// uniform sampler2D	tex_blur1;
// uniform sampler2D	tex_blur2;
// uniform sampler2D	tex_blur3;
// uniform sampler2D	tex_blur4;
// uniform float tex_weights[5];

#define BLUR_LAYERS 5

uniform sampler2D tex_blur[BLUR_LAYERS];
uniform float tex_weights[BLUR_LAYERS];

uniform vec2 wh_rcp; 

void main(){
  vec2 posn = gl_FragCoord.xy * wh_rcp;
       
  // vec4 bloom  = tex_weights[0] * texture(tex_blur0, posn)
	 					  // + tex_weights[1] * texture(tex_blur1, posn)
							// + tex_weights[2] * texture(tex_blur2, posn)
							// + tex_weights[3] * texture(tex_blur3, posn)
							// + tex_weights[4] * texture(tex_blur4, posn)
              // ;
              
  vec4 bloom = vec4(0.0);
  
  for(int i = 0; i < BLUR_LAYERS; i++){
    bloom += tex_weights[i] * texture(tex_blur[i], posn);
  }
         
  glFragColor = clamp(bloom, vec4(0.0), vec4(1.0));                      
}
