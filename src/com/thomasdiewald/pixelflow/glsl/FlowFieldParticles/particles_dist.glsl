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

uniform float     point_size;
uniform ivec2     wh_position;
uniform vec2      wh_viewport;
uniform sampler2D tex_position;

#if SHADER_VERT

out vec4 particle;

void main(){
  // get point index / vertex index
  int point_id = gl_VertexID;

  // get position (xy)
  int row = point_id / wh_position.x;
  int col = point_id - wh_position.x * row;
  
  // get particle position, velocity
  particle = texelFetch(tex_position, ivec2(col, row), 0);

  gl_Position  = vec4(particle.xy * 2.0 - 1.0, 0, 1); // ndc: [-1, +1]
  gl_PointSize = point_size;
}

#endif // #if SHADER_VERT


#if SHADER_FRAG

#define POINT_SHADER 0 // 0, 1, 2

out float out_frag;
in vec4 particle;

void main(){
  vec2 my_PointCoord = ((particle.xy * wh_viewport) - gl_FragCoord.xy) / point_size + 0.5; // [0, 1]
  //out_frag = max(0, 1.0 - length(my_PointCoord * 2.0 - 1.0));
  // out_frag = max(0, 1.0 - length(gl_PointCoord * 2.0 - 1.0));
  
  vec2 pc = my_PointCoord * 2.0 - 1.0;
  
#if   (0 == POINT_SHADER)
  out_frag = max(0, 1.0 - length(pc));
#elif (1 == POINT_SHADER)
  out_frag = max(0, 1.0 - dot(pc,pc));
#elif (2 == POINT_SHADER)
  out_frag = max(0, 1.0 - sqrt(length(pc)));
#endif

  // out_frag = smoothstep(0.0, 1.0, out_frag);
  // out_frag = out_frag * out_frag * (3.0 - 2.0 * out_frag);
}

#endif // #if SHADER_FRAG




