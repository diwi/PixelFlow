
#version 150

#define SAMPLER0 sampler2D // sampler2D, sampler3D, samplerCube
#define SAMPLER1 sampler2D // sampler2D, sampler3D, samplerCube
#define SAMPLER2 sampler2D // sampler2D, sampler3D, samplerCube
#define SAMPLER3 sampler2D // sampler2D, sampler3D, samplerCube

uniform SAMPLER0 iChannel0; // image/buffer/sound    Sampler for input textures 0
uniform SAMPLER1 iChannel1; // image/buffer/sound    Sampler for input textures 1
uniform SAMPLER2 iChannel2; // image/buffer/sound    Sampler for input textures 2
uniform SAMPLER3 iChannel3; // image/buffer/sound    Sampler for input textures 3

uniform vec3  iResolution;           // image/buffer          The viewport resolution (z is pixel aspect ratio, usually 1.0)
uniform float iTime;                 // image/sound/buffer    Current time in seconds
uniform float iTimeDelta;            // image/buffer          Time it takes to render a frame, in seconds
uniform int   iFrame;                // image/buffer          Current frame
uniform float iFrameRate;            // image/buffer          Number of frames rendered per second
uniform vec4  iMouse;                // image/buffer          xy = current pixel coords (if LMB is down). zw = click pixel
uniform vec4  iDate;                 // image/buffer/sound    Year, month, day, time in seconds in .xyzw
uniform float iSampleRate;           // image/buffer/sound    The sound sample rate (typically 44100)
uniform float iChannelTime[4];       // image/buffer          Time for channel (if video or sound), in seconds
uniform vec3  iChannelResolution[4]; // image/buffer/sound    Input texture resolution for each channel


// https://www.shadertoy.com/view/ldyGWm



/*

    Menger Sponge Variation
    -----------------------

	I got bored and dusted off some old Menger Sponge related code. There's a lot of 
	examples on this site, so I'm not bringing anything new to the table. This particular
	object was constructed via a slight variation on the formula.

	The lighting was made up as I went along, so I wouldn't pay too much attention to it. 
	However, the cheap reflections might be worth looking at.

	For anyone who's never put a Menger Sponge together, here's a very, very short, overly 
	generalized explanation:
	
	Construct a Void Cube (or repeat Void Cubes, as the case may be), which is analogous 
	to a Rubix Cube with the center mechanism removed. Create Void Cubes from the 20 cubies 
	(the remaining	smaller cubes), and continue to iterate ad infinitum.
	
	In code:

	// Repeat Void Cubes - A Void Cube is a Level-1 Menger Sponge.
	float map(vec3 p){
    	p = abs(mod(p, 3.) - 1.5); // Repeat space.
    	return min(max(p.x, p.y), min(max(p.y, p.z), max(p.x, p.z))) - 1.; // Void Cube.
	}

	// More than one level Menger Sponge - Infinitely repeated, in this case.
	float map(vec3 q){
        
		vec3 p; float d = 0.;
        
        // One Void Cube.
    	p = abs(mod(q, 3.) - 1.5);
    	d = max(d, min(max(p.x, p.y), min(max(p.y, p.z), max(p.x, p.z))) - 1.);

        // Subdividing into more Void Cubes.    
    	p = abs(mod(q, 1.) - 0.5); // Dividing above by 3.
    	d = max(d, min(max(p.x, p.y), min(max(p.y, p.z), max(p.x, p.z))) - 1./3.);
        
        // And so on.
    	p = abs(mod(q, 1./3.) - 0.5/3.); // Dividing above by 3.
    	d = max(d, min(max(p.x, p.y), min(max(p.y, p.z), max(p.x, p.z))) - 1./3./3.);
        
		// Continue on in this manner. For more levels, you'll want to loop it. There's
		// a commented out example in the code somewhere. Also, you can experiment with 
		// the code to create more interesting variants.

		return d;
	}
	
	For a more formal explanation, look up "Menger Sponge," "Cantor Sets," "Void Cube," 
	etc., on the web, or better yet, refer to the countless Menger Sponge examples
	on this site.	

	Examples:

	Menger Journey - Syntopia (A favorite of mine, and everyone else.)
	https://www.shadertoy.com/view/Mdf3z7

*/

#define FAR 40.

float hash( float n ){ return fract(cos(n)*45758.5453); }


// Tri-Planar blending function. Based on an old Nvidia writeup:
// GPU Gems 3 - Ryan Geiss: http://http.developer.nvidia.com/GPUGems3/gpugems3_ch01.html
vec3 tex3D( sampler2D tex, in vec3 p, in vec3 n ){
   
    n = max(n*n, 0.001); // n = max((abs(n) - 0.2)*7., 0.001); // n = max(abs(n), 0.001), etc.
    n /= (n.x + n.y + n.z ); 
	return (texture(tex, p.yz)*n.x + texture(tex, p.zx)*n.y + texture(tex, p.xy)*n.z).xyz;
 
    
}

// Smooth minimum function. There are countless articles, but IQ explains it best here:
// http://iquilezles.org/www/articles/smin/smin.htm
float sminP( float a, float b, float smoothing ){

    float h = clamp( 0.5+0.5*(b-a)/smoothing, 0.0, 1.0 );
    return mix( b, a, h ) - smoothing*h*(1.0-h);
}


/*
// Regular Menger Sponge formula. Very simple, but if you're not sure, look it
// up on Wikipedia, and look up a Void Cube image.
float map(vec3 q){
    
    vec3 p;
	// Scale factor, and distance.
    float s = 3., d = 0.;
    
    for(int i=0; i<3; i++){
 		// Repeat space.
        p = abs(fract(q/s)*s - s/2.); // Equivalent to: p = abs(mod(q, s) - s/2.);
		// Repeat Void Cubes. Cubes with a cross taken out.
 		d = max(d, min(max(p.x, p.y), min(max(p.y, p.z), max(p.x, p.z))) - s/3.);
    	s /= 3.; // Divide space (each dimension) by 3.
    }
 
 	return d;    
}
*/

// Variation on a Menger Sponge (See the formula above). This one has four layers. The 
// easiest way to understand this is to comment out layers, then add them back in to 
// see what each does.
float map(vec3 q){
    
    // Layer one. The ".05" on the end varies the hole size.
 	vec3 p = abs(fract(q/3.)*3. - 1.5);
 	float d = min(max(p.x, p.y), min(max(p.y, p.z), max(p.x, p.z))) - 1. + .05;
    
    // Layer two.
    p =  abs(fract(q) - .5);
 	d = max(d, min(max(p.x, p.y), min(max(p.y, p.z), max(p.x, p.z))) - 1./3. + .05);
   
    // Layer three. 3D space is divided by two, instead of three, to give some variance.
    p =  abs(fract(q*2.)*.5 - .25);
 	d = max(d, min(max(p.x, p.y), min(max(p.y, p.z), max(p.x, p.z))) - .5/3. - .015); 

    // Layer four. The little holes, for fine detailing.
    p =  abs(fract(q*3./.5)*.5/3. - .5/6.);
 	return max(d, min(max(p.x, p.y), min(max(p.y, p.z), max(p.x, p.z))) - 1./18. - .015);
    //return max(d, max(max(p.x, p.y), p.z) - 1./18. - .024);
    //return max(d, length(p) - 1./18. - .048);
    
    //p =  abs(fract(q*3.)/3. - .5/3.);
 	//return max(d, min(max(p.x, p.y), min(max(p.y, p.z), max(p.x, p.z))) - 1./9. - .04);
}



// Very basic raymarching equation. Menger Sponge objects raymarch reasonably well. Not all surfaces do.
float trace(vec3 ro, vec3 rd){
    
    float t = 0., d;
    for(int i=0; i< 64; i++){        
        d = map(ro + rd*t);
        if (d <.0025*t || t>FAR) break;
        t += d;
    } 
    return t;
}

// The reflections are pretty subtle, so not much effort is being put into them. Only a few iterations.
float refTrace(vec3 ro, vec3 rd){

    float t = 0., d;
    for(int i=0; i< 16; i++){
        d = map(ro + rd*t);
        if (d <.0025*t || t>FAR) break;
        t += d;
    } 
    return t;
}


// Tetrahedral normal, to save a couple of "map" calls. Courtesy of IQ.
vec3 normal(in vec3 p){

    // Note the slightly increased sampling distance, to alleviate artifacts due to hit point inaccuracies.
    vec2 e = vec2(0.005, -0.005); 
    return normalize(e.xyy * map(p + e.xyy) + e.yyx * map(p + e.yyx) + e.yxy * map(p + e.yxy) + e.xxx * map(p + e.xxx));
}

/*
// Standard normal function.
vec3 normal(in vec3 p) {
	const vec2 e = vec2(0.005, 0);
	return normalize(vec3(map(p + e.xyy) - map(p - e.xyy), map(p + e.yxy) - map(p - e.yxy),	map(p + e.yyx) - map(p - e.yyx)));
}
*/

// Ambient occlusion, for that self shadowed look.
// XT95 came up with this particular version. Very nice.
//
// Hemispherical SDF AO - https://www.shadertoy.com/view/4sdGWN
// Alien Cocoons - https://www.shadertoy.com/view/MsdGz2
float calculateAO(in vec3 p, in vec3 n){
    
	float ao = 0.0, l;
	const float nbIte = 6.0;
	const float falloff = 1.;
    
    const float maxDist = 1.;
    for(float i=1.; i<nbIte+.5; i++){
    
        l = (i + hash(i))*.5/nbIte*maxDist;
        ao += (l - map( p + n*l ))/ pow(1. + l, falloff);
    }
	
    return clamp( 1.-ao/nbIte, 0., 1.);
}


// Cheap shadows are hard. In fact, I'd almost say, shadowing repeat objects - in a setting like this - with limited 
// iterations is impossible... However, I'd be very grateful if someone could prove me wrong. :)
float softShadow(vec3 ro, vec3 lp, float k){

    // More would be nicer. More is always nicer, but not really affordable... Not on my slow test machine, anyway.
    const int maxIterationsShad = 16; 
    
    vec3 rd = (lp-ro); // Unnormalized direction ray.

    float shade = 1.0;
    float dist = 0.05;    
    float end = max(length(rd), 0.001);
    float stepDist = end/float(maxIterationsShad);
    
    rd /= end;

    // Max shadow iterations - More iterations make nicer shadows, but slow things down. Obviously, the lowest 
    // number to give a decent shadow is the best one to choose. 
    for (int i=0; i<maxIterationsShad; i++){

        float h = map(ro + rd*dist);
        //shade = min(shade, k*h/dist);
        shade = min(shade, smoothstep(0.0, 1.0, k*h/dist)); // Subtle difference. Thanks to IQ for this tidbit.
        //dist += min( h, stepDist ); // So many options here: dist += clamp( h, 0.0005, 0.2 ), etc.
        dist += clamp(h, 0.02, 0.25);
        
        // Early exits from accumulative distance function calls tend to be a good thing.
        if (h<0.001 || dist > end) break; 
    }

    // I've added 0.5 to the final shade value, which lightens the shadow a bit. It's a preference thing.
    return min(max(shade, 0.) + 0.5, 1.0); 
}

/*
// Cool curve function, by Shadertoy user, Nimitz.
//
// It gives you a scalar curvature value for an object's signed distance function, which 
// is pretty handy for all kinds of things. Here's it's used to darken the crevices.
//
// From an intuitive sense, the function returns a weighted difference between a surface 
// value and some surrounding values - arranged in a simplex tetrahedral fashion for minimal
// calculations, I'm assuming. Almost common sense... almost. :)
//
// Original usage (I think?) - Cheap curvature: https://www.shadertoy.com/view/Xts3WM
// Other usage: Xyptonjtroz: https://www.shadertoy.com/view/4ts3z2
float curve(in vec3 p){

    //const float eps = 0.05, amp = 4.0, ampInit = 0.5;
    const float eps = 0.15, amp = 2.5, ampInit = 0.0;

    vec2 e = vec2(-1., 1.)*eps; //0.05->3.5 - 0.04->5.5 - 0.03->10.->0.1->1.
    
    float t1 = map(p + e.yxx), t2 = map(p + e.xxy);
    float t3 = map(p + e.xyx), t4 = map(p + e.yyy);
    
    return clamp((t1 + t2 + t3 + t4 - 4.*map(p))*amp + ampInit, 0., 1.);
}
*/

void mainImage( out vec4 fragColor, in vec2 fragCoord ){
    
    
    // Unit direction ray vector: Note the absence of a divide term. I came across this via a comment 
    // Shadertoy user "Coyote" made. I'm pretty happy with this.
    vec3 rd = (vec3(2.*fragCoord - iResolution.xy, iResolution.y)); // Normalizing below.
    
    // Barrel distortion. Looks interesting, but I like it because it fits more of the scene in.
    // If you comment this out, make sure you normalize the line above.
    rd = normalize(vec3(rd.xy, sqrt(max(rd.z*rd.z - dot(rd.xy, rd.xy)*.2, 0.))));
    
    // Rotating the ray with Fabrice's cost cuttting matrix. I'm still pretty happy with this also. :)
    vec2 m = sin(vec2(0, 1.57079632) + iTime/4.);
    rd.xy = mat2(m.y, -m.x, m)*rd.xy;
    rd.xz = mat2(m.y, -m.x, m)*rd.xz;
    
    
    // Ray origin, set off in the Z direction.
    vec3 ro = vec3(0.0, 0.0, iTime);
    vec3 lp = ro  + vec3(0.0, 1.0, 0.0); // Light, near the ray origin.
    
    // Initiate the scene color to black.
    vec3 col = vec3(0);
    
    float t = trace(ro, rd); // Raymarch.
    
    // Scene hit, so do some lighting.
    if(t<FAR){
    
        vec3 sp = ro + rd*t; // Surface position.
        vec3 sn = normal(sp); // Surface normal.
        vec3 ref = reflect(rd, sn); // Reflected ray.

		const float ts = 2.; // Texture scale.
        vec3 oCol = tex3D(iChannel0, sp*ts, sn); // Texture color at the surface point.
        
 		
        // Darker toned wood paneling. Very fancy. :)
        vec3 q = abs(mod(sp, 3.) - 1.5);
        if (max(max(q.x, q.y), q.z) < 1.063) oCol = oCol*vec3(.7, .85, 1.); 

        // Bringing out the texture colors a bit.
        oCol = smoothstep(0.0, 1.0, oCol);
 
        float sh = softShadow(sp, lp, 16.); // Soft shadows.
        float ao = calculateAO(sp, sn); // Self shadows.

        vec3 ld = lp - sp; // Light direction.
        float lDist = max(length(ld), 0.001); // Light to surface distance.
        ld /= lDist; // Normalizing the light direction vector.

        float diff = max(dot(ld, sn), 0.); // Diffuse component.
        float spec = pow(max(dot(reflect(-ld, sn), -rd), 0.), 12.); // Specular.
        //float fres = clamp(1.0 + dot(rd, sn), 0.0, 1.0); // Fresnel reflection term.

        float atten = 1.0 / (1.0 + lDist*0.25 + lDist*lDist*.1); // Attenuation.
        
        
        // Secondary camera light, just to light up the dark areas a bit more. It's here just
        // to add a bit of ambience, and its effects are subtle, so its attenuation 
        // will be rolled into the attenuation above.
        diff += max(dot(-rd, sn), 0.)*.45;
        spec += pow(max(dot(reflect(rd, sn), -rd), 0.), 12.)*.45;
        
        // Based on Eiffie's suggestion. It's an improvement, but I've commented out, 
        // for the time being.
        //spec *= curve(sp); 


		// REFLECTION BLOCK.
        //
        // Cheap reflection: Not entirely accurate, but the reflections are pretty subtle, so not much 
        // effort is being put in.
        float rt = refTrace(sp + ref*0.1, ref); // Raymarch from "sp" in the reflected direction.
        vec3 rsp = sp + ref*rt; // Reflected surface hit point.
        vec3 rsn = normal(rsp); // Normal at the reflected surface.
        
        vec3 rCol = tex3D(iChannel0, rsp*ts, rsn); // Texel at "rsp."
        q = abs(mod(rsp, 3.) - 1.5);
        if (max(max(q.x, q.y), q.z)<1.063) rCol = rCol*vec3(.7, .85, 1.);  
        // Toning down the power of the reflected color, simply because I liked the way it looked more. 
        rCol = sqrt(rCol); 
        float rDiff = max(dot(rsn, normalize(lp-rsp)), 0.); // Diffuse at "rsp" from the main light.
        rDiff += max(dot(rsn, normalize(-rd-rsp)), 0.)*.45; // Diffuse at "rsp" from the camera light.
        
        float rlDist = length(lp - rsp);
        float rAtten = 1./(1.0 + rlDist*0.25 + rlDist*rlDist*.1);
        rCol = min(rCol, 1.)*(rDiff + vec3(.5, .6, .7))*rAtten; // Reflected color. Not accurate, but close enough.
        //
    	// END REFLECTION BLOCK.
        

        // Combining the elements above to light and color the scene.
        col = oCol*(diff + vec3(.5, .6, .7)) + vec3(.5, .7, 1)*spec*2. + rCol*0.25;


        // Shading the scene color, clamping, and we're done.
        col = min(col*atten*sh*ao, 1.);
        
        
        
        
        
         
    }
    
    // Working in a bit of a blue fadeout in the distance. Totally fake. I chose blue to counter all
    // that walnut. Seemed like a good idea at the time. :)
    col = mix(col, vec3(.55, .75, 1.), smoothstep(0., FAR - 15., t));////1.-exp(-0.01*t*t)

    
	fragColor = vec4(col, 1.0);
    
}









