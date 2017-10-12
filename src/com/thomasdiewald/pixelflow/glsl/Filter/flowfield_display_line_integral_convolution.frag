/**
 * 
 * PixelFlow | Copyright (C) 2017 Thomas Diewald - www.thomasdiewald.com
 * 
 * https://github.com/diwi/PixelFlow.git
 * 
 * A Processing/Java library for high performance GPU-Computing.
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */

//
// Line Integral Convolution (LIC) Shader
//
// LIC is a weighted Low pass Filter along a Streamline
// The Streamline is traced by using verlet integration.
//
// Demo: https://vimeo.com/237766186
//

#version 150

#define NUM_SAMPLES 5

#define TRACE_FORWARD 1
#define TRACE_BACKWARD 1
#define APPLY_EXP_SHADING 1

out vec4 out_frag;

uniform vec2 wh_rcp;
uniform vec2 wh_vel_rcp;
uniform float acc_mult = 1.0; // timestep
uniform float vel_mult = 1.0; // damping
uniform vec2  acc_minmax = vec2(0.0, 1.0);
uniform vec2  vel_minmax = vec2(0.0, 1.0);

uniform float intensity_mult = 1.0; 
uniform float intensity_exp = 1.0; 

uniform sampler2D tex_src;
uniform sampler2D tex_acc;


void limitLength(inout vec2 vel, in vec2 lohi){
  float vel_len = length(vel);
  if(vel_len <= lohi.x){
    vel *= 0.0;
  } else {
    vel *= clamp(vel_len - lohi.x, 0.0, lohi.y) / vel_len;
  }
}

void traceStream(inout vec4 samples_sum, inout float weights_sum, in float acc_mult_dir){

  // start position
  vec4 pos = (gl_FragCoord.xy * wh_rcp).xyxy;
  float weight = 1.0;
  for(int i = 0; i < NUM_SAMPLES; i++){
    // acceleration, velocity
    vec2 acc = texture(tex_acc, pos.xy).xy;
    vec2 vel = (pos.xy - pos.zw) / wh_vel_rcp;
    // clamp 
    limitLength(acc, acc_minmax);
    limitLength(vel, vel_minmax); 
    // update position, verlet integration
    pos.zw = pos.xy;
    pos.xy += (vel * vel_mult + acc * acc_mult_dir) * wh_vel_rcp;
    // integrate
#if (APPLY_EXP_SHADING == 1)
    weight = pow(length(acc), intensity_exp);
#endif
    samples_sum += texture(tex_src, pos.xy) * weight;
    weights_sum += 1.0;
  }
}


void main(){

  vec4  samples_sum = vec4(0.0);
  float weights_sum = 0.0;

#if (TRACE_BACKWARD == 1)
  traceStream(samples_sum, weights_sum, -acc_mult);
#endif

#if (TRACE_FORWARD == 1)
  traceStream(samples_sum, weights_sum, +acc_mult);
#endif

  out_frag = intensity_mult * samples_sum / weights_sum;

}


