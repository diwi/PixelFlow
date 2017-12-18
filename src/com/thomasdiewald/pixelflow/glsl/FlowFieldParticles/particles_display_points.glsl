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


#define SHADER_VERT 0
#define SHADER_FRAG 0

#define SHADING_TYPE 0 // either 0 or 1 ...

uniform float     point_size;
uniform ivec2     wh_position;
uniform vec2      wh_viewport;
uniform float     shader_collision_mult = 1.0;
uniform sampler2D tex_collision;
uniform sampler2D tex_position;
uniform sampler2D tex_sprite;
uniform vec4      col_A = vec4(1, 1, 1, 1.0);
uniform vec4      col_B = vec4(0, 0, 0, 0.0);

#if SHADER_VERT

out vec4 particle;

void main(){

  // get point index / vertex index
  int point_id = gl_VertexID;

  // get position (xy)
  int row = point_id / wh_position.x;
  int col = point_id - wh_position.x * row;
  
  // get particle position
  particle = texelFetch(tex_position, ivec2(col, row), 0);

  gl_Position  = vec4(particle.xy * 2.0 - 1.0, 0, 1); // ndc: [-1, +1]
  gl_PointSize = point_size;
}

#endif // #if SHADER_VERT



#if (SHADER_FRAG == 1)

// #define INVERT 0
// #define STEPS 2
// uniform vec4 PALLETTE[STEPS] = {
  // vec4(0.25, 0.50, 1.00, 1.0),
  // vec4(1.00, 0.50, 0.25, 1.0)
// };

// vec4 getShading(float val){
  // val = clamp(val, 0.0, 0.99999);
// #if INVERT
    // val = 1.0 - val;
// #endif
  // float lum_steps = val * (STEPS-1);
  // float frac = fract(lum_steps);
  // int id = int(floor(lum_steps));
  // return mix(PALLETTE[id], PALLETTE[id+1], frac);
// }


out vec4 out_frag;
in vec4 particle;

void main(){
  
  vec2  velocity = (particle.xy - particle.zw) * wh_viewport;
  float pressure = texture(tex_collision, particle.xy).r;
  float mult = pressure + length(velocity) * 2.0;
  mult *= shader_collision_mult;
 
  vec2 my_PointCoord = ((particle.xy * wh_viewport) - gl_FragCoord.xy) / point_size + 0.5;
  float falloff = 1.0 - texture(tex_sprite, my_PointCoord.xy).a; // my_PointCoord ...gl_PointCoord
 
#if (SHADING_TYPE == 0)
  out_frag  = mix(col_A, col_B, falloff);
  out_frag *= 1.0 + mult;
#endif
  
#if (SHADING_TYPE == 1)
  out_frag  = mix(col_A, col_B, max(0.0, 1.0-mult));
  // out_frag = getShading(mult);
  out_frag *= (1.0 - falloff*falloff) * mult;
#endif

  out_frag  = clamp(out_frag, 0.0, 1.0);
}

#endif // #if SHADER_FRAG








