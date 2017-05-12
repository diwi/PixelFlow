/**
 * 
 * PixelFlow | Copyright (C) 2017 Thomas Diewald (www.thomasdiewald.com)
 * 
 * src  - www.github.com/diwi/PixelFlow
 * 
 * A Processing/Java library for high performance GPU-Computing.
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */

 
/**
 *
 * GBAA - geometry buffer pass
 * 
 * original work (C++, HLSL) by Emil Persson, 2011, http://www.humus.name
 * article: http://www.humus.name/index.php?page=3D&ID=87
 * 
 * HLSL-GLSL port by Thomas Diewald
 *
 */


#version 150

layout (triangles) in;
layout (triangle_strip, max_vertices = 3) out;
 
uniform vec2 wh;
 
noperspective out vec3  dist;
flat          out ivec3 horz;

float ComputeDist(vec2 pos0, vec2 pos1, vec2 pos2, inout int major_dir){
	vec2 dir = normalize(pos1 - pos0);
	vec2 normal = vec2(-dir.y, dir.x);
	float dist = dot(pos0, normal) - dot(pos2, normal);

	// Check major direction
  if(abs(normal.x) > abs(normal.y)){
    major_dir = 0;
    return dist / normal.x;
  } else {
    major_dir = 1;
    return dist / normal.y;
  }
}


void main(){

  // triangle in ndc (before persp div)
  vec4 v0_in = gl_in[0].gl_Position;
  vec4 v1_in = gl_in[1].gl_Position;
  vec4 v2_in = gl_in[2].gl_Position;
  
  // triangle in screen space
  vec2 v0 = (v0_in.xy/v0_in.w * 0.5 + 0.5) * wh;
  vec2 v1 = (v1_in.xy/v1_in.w * 0.5 + 0.5) * wh;
  vec2 v2 = (v2_in.xy/v2_in.w * 0.5 + 0.5) * wh;
         
  // normal distances to opposite triangle side in screen space            
	float v0_dist = ComputeDist(v1, v2, v0, horz.x);
	float v1_dist = ComputeDist(v2, v0, v1, horz.y);               
  float v2_dist = ComputeDist(v0, v1, v2, horz.z);   
 
  gl_Position = v0_in;                    
  dist = vec3(v0_dist, 0, 0);
  EmitVertex();
  
  gl_Position = v1_in;                     
  dist = vec3(0, v1_dist, 0);
  EmitVertex();
  
  gl_Position = v2_in;                
  dist = vec3(0, 0, v2_dist);
  EmitVertex();
  
  EndPrimitive();
}