/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */


#version 150


#define SHADER_VERT 0
#define SHADER_FRAG 0


// uniforms are shared
uniform vec2      wh_viewport;
uniform int       display_mode;
uniform ivec2     num_particles;
uniform sampler2D tex_particles;
uniform sampler2D tex_sprite;
// uniform sampler2D tex_velocity;
// uniform sampler2D tex_pressure;


#if SHADER_VERT

out vec4 particle;
out float my_PointSize;
// out vec2 velocity_new;
// out float pressure;

void main(){

  // get point index / vertex index
  int point_id = gl_VertexID;

  // get position (xy)
  int row = point_id / num_particles.x;
  int col = point_id - num_particles.x * row;
  
  // compute texture location [0, 1]
  vec2 posn = (vec2(col, row)+0.5) / vec2(num_particles); 
  
  // get particel pos, vel
  particle = texture(tex_particles, posn);
 
  // get fluid data at current location
  // velocity_new = texture(tex_velocity, particle.xy).xy;
  // pressure = texture(tex_pressure, particle).x;
  
  // finish vertex coordinate
  gl_Position = vec4(particle.xy * 2.0 - 1.0, 0, 1); // ndc: [-1, +1]
  gl_PointSize = 1.0;
  if(display_mode != 0){
    gl_PointSize = 3.0 + sqrt(length(particle.zw)) * 0.5;
  } 
  my_PointSize = gl_PointSize;
}

#endif // #if SHADER_VERT


#define INVERT 0
#define STEPS 4
const vec4 PALLETTE[] = vec4[](
  vec4(  50.0,   25.0,    0.0, 255.0)/255.0,    
  vec4(  25.0,  100.0,  255.0, 255.0)/255.0, 
  vec4(   0.0,  150.0,  255.0, 255.0)/255.0, 
  vec4( 100.0,  150.0,  255.0, 255.0)/255.0
);

vec4 getShading(float val){
  val = clamp(val, 0.0, 0.99999);
#if INVERT
    val = 1.0 - val;
#endif
  float lum_steps = val * (STEPS-1);
  float frac = fract(lum_steps);
  int id = int(floor(lum_steps));
  return mix(PALLETTE[id], PALLETTE[id+1], frac);
}




#if SHADER_FRAG


in vec4 particle;
in float my_PointSize;

out vec4 out_frag;

void main(){

  vec4 shading_mask = vec4(1);
  
  vec2 my_PointCoord = ((particle.xy * wh_viewport) - gl_FragCoord.xy) / my_PointSize + 0.5;
  
  if(display_mode == 1){
    shading_mask = texture(tex_sprite, my_PointCoord);
  } else 
  if(display_mode == 2){
    vec2 pc = abs(my_PointCoord * 2.0 - 1.0);// [-1, 1]
    float len = clamp(length(pc), 0.0, 1.0);
    shading_mask.a = 1.0 - len;
  }

  float len = length(particle.zw) * 0.05;
  len = clamp(len, 0.0, 1.0);
  
  // vec4 velocity_col = vec4(0.5-len, len*0.5, 1, 0.5 + len);
  // glFragColor = velocity_col * shading_mask;
  
  out_frag = getShading(len) * shading_mask;

}


#endif // #if SHADER_VERT







