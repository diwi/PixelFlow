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


#version 150 


#define TWO_PI 6.2831855
// GOLDEN_ANGLE_R =  PI * (3 - sqrt(5))
#define GOLDEN_ANGLE_R 2.3999631 

#define SPAWN_RADIAL 0
#define SPAWN_RECT 0

struct RadialSpawn {
  int   num; // count
  vec2  pos; // center
  vec2  dim; // dimension
  vec2  vel; // velocity
  float off; // offset (radius), TODO
};

struct RectSpawn {
  ivec2 num; // count in u and v
  vec2  pos; // center
  vec2  dim; // dimension
  vec2  vel; // velocity
  vec2  off; // offset (position), TODO
};


out vec4 out_pos;

#if SPAWN_RADIAL
  uniform RadialSpawn spawn;
#elif SPAWN_RECT
  uniform RectSpawn spawn;
#endif

uniform ivec2 lo_hi;
uniform vec2  wh_viewport_rcp;
uniform ivec2 wh_position;
uniform sampler2D tex_position;


void main(){
  
  // prepare particle index, based on the current fragment position
  ivec2 tex_loc = ivec2(gl_FragCoord.xy);
  int   particle_idx = tex_loc.y * wh_position.x + tex_loc.x;
  
  // check if the current fragment is one of those particles to spawn now
  if(particle_idx >= lo_hi.x && particle_idx < lo_hi.y){

#if SPAWN_RADIAL

    // fibonacci pattern
    float idx = float(particle_idx - lo_hi.x);
    float idxn = (idx+0.1) / float(spawn.num);
    // vec2 dim = spawn.dim * sqrt(idxn * idxn * idxn);
    vec2 dim = spawn.dim * sqrt(idxn); // uniform distribution
    float rot = idx * GOLDEN_ANGLE_R + spawn.off;
    vec2 pos = dim * vec2(cos(rot), sin(rot));

#elif SPAWN_RECT

    int idx = particle_idx - lo_hi.x;
    int row = idx / spawn.num.x;
    int col = idx - spawn.num.x * row;
    vec2 pos = spawn.dim * vec2(col + 0.5, row + 0.5) / vec2(spawn.num);
    
#endif

    vec2 pos_cur = spawn.pos + pos;
    vec2 pos_old = pos_cur - spawn.vel;

    out_pos = vec4(pos_cur, pos_old) * wh_viewport_rcp.xyxy;
  } else {
    out_pos = texelFetch(tex_position, tex_loc, 0);
  }

}

