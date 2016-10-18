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

#define SUM_RGB(v) ((v).r + (v).g + (v).b)
#define AVG_RGB(v) (SUM_RGB(v) / 3.0)

out vec2 glFragColor;

uniform sampler2D	tex_curr_frame ; // current  image
uniform sampler2D	tex_prev_frame ; // previous image
uniform sampler2D	tex_curr_sobelH; // current  gradient horizontal
uniform sampler2D	tex_curr_sobelV; // current  gradient vertical
uniform sampler2D	tex_prev_sobelH; // previous gradient horizontal
uniform sampler2D	tex_prev_sobelV; // previous gradient vertical

uniform vec2  wh;                  // size rendertarget
uniform float scale;               // scale flow
uniform float threshold = 1.0;     // flow intensity threshold

void main(){
  
  vec2 posn = gl_FragCoord.xy / wh;
  
  // dt, dx, dy
  vec3 dt_ = texture(tex_curr_frame , posn).rgb - texture(tex_prev_frame , posn).rgb; // dt
  vec3 dx_ = texture(tex_curr_sobelH, posn).rgb + texture(tex_prev_sobelH, posn).rgb; // dx_curr + dx_prev
  vec3 dy_ = texture(tex_curr_sobelV, posn).rgb + texture(tex_prev_sobelV, posn).rgb; // dy_curr + dy_prev
  
  
#define VERSION 1

#if (VERSION == 0)

  // gradient length
  vec3 dd_ = sqrt(dx_*dx_ + dy_*dy_ + 1.0);
  // optical flow
  vec3 vx = scale * dt_ * dx_ / dd_;
  vec3 vy = scale * dt_ * dy_ / dd_;
  // sum (or average) rgb-channels
  vec2 flow = vec2(SUM_RGB(vx), SUM_RGB(vy));
  
#elif (VERSION == 1)
  
  // sum (or average) rgb-channels
  float dt = SUM_RGB(dt_), dx = SUM_RGB(dx_),  dy = SUM_RGB(dy_);
  // gradient length
  float dd = sqrt(dx*dx + dy*dy + 1.0);
  // optical flow
  vec2 flow = scale * dt * vec2(dx, dy) / dd; 
  
#endif

  // threshold
  float len_old = sqrt(flow.x*flow.x + flow.y*flow.y + 0.00001);
  float len_new = max(len_old - threshold, 0.0);
  // len_new = len_new * len_old/(len_new + 0.00001);
  flow = (len_new * flow)/len_old;
  

  glFragColor = flow;


}





