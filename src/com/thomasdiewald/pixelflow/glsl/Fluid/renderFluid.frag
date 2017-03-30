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

uniform vec2 wh;
uniform int display_mode;

uniform sampler2D tex_density    ;
uniform sampler2D tex_temperature;
uniform sampler2D tex_pressure   ;
uniform sampler2D tex_velocity   ;
uniform sampler2D tex_obstacleC  ;
uniform sampler2D tex_obstacleN  ;

const int STEPS = 5;
const vec3 PALLETTE[] = vec3[](
  vec3(   0,    0,    0)/255.0,    
  vec3(  255,  200,   0)/255.0, 
  vec3(   0,  150,  255)/255.0, 
  vec3( 100,  178,  255)/255.0, 
  vec3( 255,  255,  255)/255.0
);

vec3 getShading(float val){
  val = clamp(val, 0.0, 0.99999);
  float lum_steps = val * (STEPS-1);
  float frac = fract(lum_steps);
  int id = int(floor(lum_steps));
  return mix(PALLETTE[id], PALLETTE[id+1], frac);
}


void main(){

  vec2 posn = gl_FragCoord.xy / wh;
  
  // DENSITY
  if(display_mode == 0)
  {
    vec4 dVal = texture(tex_density, posn);
    dVal.a = clamp(dVal.a, 0.0, 1.0);
    glFragColor = vec4(dVal.xyz/dVal.a, dVal.a);
  }
  
  // TEMPERATURE
  if(display_mode == 1)
  {
    float tVal = texture(tex_temperature, posn).x; 
    vec3 color = (tVal >= 0) ? vec3(1,0.25,0) : vec3(0,0.25,1);
    glFragColor = vec4(color, abs(tVal));
  }
  
  // PRESSURE
  if(display_mode == 2)
  {
    float pVal = texture(tex_pressure, posn).x; 
    vec3 color = (pVal >= 0) ? vec3(1,0.25,0) : vec3(0,0.25,1);
    glFragColor = vec4(color, abs(pVal));
  }
  
  // VELOCITY
  if(display_mode == 3)
  {
    vec2 vVal = texture(tex_velocity, posn).xy; 
    float v_len = length(vVal);
    // vec2 v_norm = normalize(vVal);
    
    // glFragColor = vec4(v_norm *0.5 + 0.5, 0, v_len);
    // glFragColor = vec4(abs(vVal) * 0.1, 0, v_len*0.5);
    glFragColor = vec4(1, 1, 1, v_len*0.25);
  
    vec3 shading = getShading(v_len*0.025);
    glFragColor = vec4(shading, 1);
    
    // float r = 0.5 * (1.0 + vVal.x / (v_len + 0.0001f));
    // float g = 0.5 * (1.0 + vVal.y / (v_len + 0.0001f));
    // float b = 0.5 * (2.0 - (r + g));
    // glFragColor = vec4(r,g,b,1);
  }
  
  // OBSTACLES
  if(display_mode == 4){
    float oC = texture(tex_obstacleC, posn).x;

    glFragColor = vec4(1);
    if(oC == 1.0){
      glFragColor = vec4(1,1,0, 1);
    } else {
      vec4  oN = texture(tex_obstacleN, posn);
      if(oN.x == 1.0) glFragColor = vec4(1,0,0, 1);
      if(oN.y == 1.0) glFragColor = vec4(1,0,0, 1);
      if(oN.z == 1.0) glFragColor = vec4(0,0,1, 1);
      if(oN.w == 1.0) glFragColor = vec4(0,0,1, 1);
    }
  }
  
}

