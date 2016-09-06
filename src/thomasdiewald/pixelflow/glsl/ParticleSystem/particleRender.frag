/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */


#version 150

precision mediump float;
precision mediump int;

out vec4 glFragColor;

in vec2 velocity;
// in vec2 velocity_new;
// in float pressure;
uniform int       display_mode;
uniform sampler2D tex_sprite;


const int STEPS = 4;
const vec3 PALLETTE[] = vec3[](
  vec3(  50,   25,    0)/255.0,    
  vec3(  25,  100,  255)/255.0, 
  vec3(   0,  150,  255)/255.0, 
  vec3( 100,  150,  255)/255.0
);

vec3 getShading(float val){
  val = clamp(val, 0.0, 1.0);
  float lum_steps = val * (STEPS-1);
  float frac = fract(lum_steps);
  int id = int(floor(lum_steps));
  return mix(PALLETTE[id], PALLETTE[id+1], frac);
}


void main(){

  vec4 shading_mask = vec4(1);
  
  if(display_mode == 1){
    shading_mask = texture(tex_sprite, gl_PointCoord);
  } else 
  if(display_mode == 2){
    vec2 pc = abs(gl_PointCoord * 2.0 - 1.0);// [-1, 1]
    float len = clamp(length(pc), 0.0, 1.0);
    shading_mask.a = 1.0 - len;
  }

  float len = length(velocity) * 0.05;
  len = clamp(len, 0.0, 1.0);
  
  // vec4 velocity_col = vec4(0.5-len, len*0.5, 1, 0.5 + len);
  // glFragColor = velocity_col * shading_mask;
  
  vec3 shading = getShading(len);
  glFragColor = vec4(shading, 1) * shading_mask;

}


