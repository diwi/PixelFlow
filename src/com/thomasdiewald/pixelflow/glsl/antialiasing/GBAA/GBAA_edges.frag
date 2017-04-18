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

in FragData {
  noperspective vec3  dist;
  flat          bvec3 horz;
} FragIn;

out vec4 fragColor;

void main() {

	// get smallest distance
	float dist = FragIn.dist.x;
	bool  horz = FragIn.horz.x;

	if (abs(FragIn.dist.y) < abs(dist)){
		dist = FragIn.dist.y;
		horz = FragIn.horz.y;
	}
	if (abs(FragIn.dist.z) < abs(dist)){
		dist = FragIn.dist.z;
		horz = FragIn.horz.z;
	}

	// get sample offset
  vec2 offset = horz ? vec2(dist, 0.5) : vec2(0.5, dist);
  fragColor = vec4(offset, 0, 1);
}

