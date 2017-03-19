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

uniform vec2 wh_rcp;
uniform int MODE = 0;

uniform sampler2D tex_src;
uniform sampler2D tex_edges;

out vec4 fragColor;

void main() {
  vec2 offset = texelFetch(tex_edges, ivec2(gl_FragCoord.xy), 0).xy;
  
  // Check geometry buffer for an edge cutting through the pixel.
	if (min(abs(offset.x), abs(offset.y)) >= 0.5){
		// If no edge was found we look in neighboring pixels' geometry information. This is necessary because
		// relevant geometry information may only be available on one side of an edge, such as on silhouette edges,
		// where a background pixel adjacent to the edge will have the background's geometry information, and not
		// the foreground's geometric edge that we need to antialias against. Doing this step covers up gaps in the
		// geometry information.

		offset = vec2(0.5);

		// We only need to check the component on neighbor samples that point towards us
		float oxL = texelFetchOffset(tex_edges, ivec2(gl_FragCoord.xy), 0, ivec2(-1, 0)).x;
		float oxR = texelFetchOffset(tex_edges, ivec2(gl_FragCoord.xy), 0, ivec2( 1, 0)).x;
		float oyT = texelFetchOffset(tex_edges, ivec2(gl_FragCoord.xy), 0, ivec2( 0,-1)).y;
		float oyB = texelFetchOffset(tex_edges, ivec2(gl_FragCoord.xy), 0, ivec2( 0, 1)).y;
    
    // Check range of neighbor pixels' distance and use if edge cuts this pixel.
		if (abs(oxL - 0.75) < 0.25) offset = vec2(oxL - 1.0, 0.5); // Left  x-offset [ 0.5 ..  1.0] cuts this pixel
		if (abs(oxR + 0.75) < 0.25) offset = vec2(oxR + 1.0, 0.5); // Right x-offset [-1.0 .. -0.5] cuts this pixel
		if (abs(oyT - 0.75) < 0.25) offset = vec2(0.5, oyT - 1.0); // Up    y-offset [ 0.5 ..  1.0] cuts this pixel
		if (abs(oyB + 0.75) < 0.25) offset = vec2(0.5, oyB + 1.0); // Down  y-offset [-1.0 .. -0.5] cuts this pixel
	} 
  
	// Convert distance to texture coordinate shift
  // vec2 off = (offset >= vec2(0))? vec2(0.5) : vec2(-0.5);
  vec2 off = step(-offset, vec2(0.0)) - 0.5;
	offset = off - offset;
  
	// Blend pixel with neighbor pixel using texture filtering and shifting the coordinate appropriately.
  fragColor = texture(tex_src, (gl_FragCoord.xy + offset) * wh_rcp);

}

