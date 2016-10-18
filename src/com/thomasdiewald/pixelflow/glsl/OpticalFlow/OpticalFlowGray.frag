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
  float dt = texture(tex_curr_frame , posn).r - texture(tex_prev_frame , posn).r; // dt
  float dx = texture(tex_curr_sobelH, posn).r + texture(tex_prev_sobelH, posn).r; // dx_curr + dx_prev
  float dy = texture(tex_curr_sobelV, posn).r + texture(tex_prev_sobelV, posn).r; // dy_curr + dy_prev
  
  dt *= 3.0; dx *= 3.0; dy *= 3.0; // to match the rgb range

  
  // gradient length
  float dd = sqrt(dx*dx + dy*dy + 1);
  // optical flow
  vec2 flow = scale * dt * vec2(dx, dy) / dd; 
  
  // threshold
  float len_old = sqrt(flow.x*flow.x + flow.y*flow.y + 0.00001);
  float len_new = max(len_old - threshold, 0.0);
  flow = (len_new * flow)/len_old;
  
  glFragColor = flow;
}





