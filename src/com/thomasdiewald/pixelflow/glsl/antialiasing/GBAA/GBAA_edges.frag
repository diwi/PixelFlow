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

noperspective in vec3  dist;
flat          in ivec3 horz;

out vec4 fragColor;

void main() {

	// get smallest distance
	float min_dist = dist.x;
	int   min_horz = horz.x;

	if (abs(dist.y) < abs(min_dist)){
		min_dist = dist.y;
		min_horz = horz.y;
	}
	if (abs(dist.z) < abs(min_dist)){
		min_dist = dist.z;
		min_horz = horz.z;
	}

	// get sample offset
  vec2 offset = (min_horz == 0) ? vec2(min_dist, 0.5) : vec2(0.5, min_dist);
  fragColor = vec4(offset, 0, 1);
}

