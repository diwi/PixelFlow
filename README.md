![PixelFlow Header](http://thomasdiewald.com/processing/libraries/pixelflow/PixelFlow_header.jpg)

# PixelFlow
**A Processing/Java library for high performance GPU-Computing (GLSL).**


<br>

## Features

```
+ Fluid Simulation (GLSL)
+ Fluid Particle Systems (GLSL)
+ Optical Flow  (GLSL)
+ Harris Corner Detection  (GLSL)
+ Image Processing Filters (GLSL)
	- Bilateral Filter
	- Box Blur
	- Custom Convolution Kernel
	- DoG (Difference of Gaussian)
	- Gaussian Blur
	- Laplace
	- MedianFilter
	- Sobel
	- ...
+ Softbody Dynamics (CPU, GLSL is coming)
  - 2D and 3D
  - Collision Detection
  - Cloth, Grids, Chains, ...
  - Particle Systems
  - etc ...
+ Skylight Renderer
  - Interactive/realtime viewport renderer
  - Ambient Occlusion
  - Diffuse Shading
  - ShadowMapping
+ AntiAliasing
  - FXAA
  - SMAA
  - GBAA
+ Utils
  - HalfEdge
  - Subdivision Polyhedra
  - Sampling
```  


JavaDoc: http://thomasdiewald.com/processing/libraries/pixelflow/reference/index.html

<br>

## Download
+ [Releases] (https://github.com/diwi/PixelFlow/releases)
+ [PixelFlow Website] (http://thomasdiewald.com/processing/libraries/pixelflow)
+ Processing IDE -> Library Manager
 
<br>

## videos

#### Skylight Renderer
[<img src="https://i.vimeocdn.com/video/621790715.jpg" alt="alt text" width="30%">](https://vimeo.com/206696210 "Skylight - Cloth Simulation")
[<img src="https://i.vimeocdn.com/video/621790926.jpg" alt="alt text" width="30%">](https://vimeo.com/206696403 "Skylight - Basic")
[<img src="https://i.vimeocdn.com/video/621791014.jpg" alt="alt text" width="30%">](https://vimeo.com/206696738 "Skylight - Poisson Spheres")

#### Softbody Dynamics
[<img src="https://vimeo.com/184854758/og_image_watermark/59441739" alt="alt text" width="30%">](https://vimeo.com/184854758 "SoftBody Dynamics 3D - Playground, Cloth Simulation")
[<img src="https://vimeo.com/184854746/og_image_watermark/594416647" alt="alt text" width="30%">](https://vimeo.com/184854746 "SoftBody Dynamics 3D - Cloth Simulation")
[<img src="https://vimeo.com/184853892/og_image_watermark/594415861" alt="alt text" width="30%">](https://vimeo.com/184853892 SoftBody Dynamics 2D - Playground")

#### Computational Fluid Dynamics
[<img src="https://vimeo.com/184850259/og_image_watermark/594412638" alt="alt text" width="30%">](https://vimeo.com/184850259 "WindTunnel")
[<img src="https://vimeo.com/184850254/og_image_watermark/594412429" alt="alt text" width="30%">](https://vimeo.com/184850254 "StreamLines")
[<img src="https://vimeo.com/184849960/og_image_watermark/594410553" alt="alt text" width="30%">](https://vimeo.com/184849960 "Verlet Particle Collision System")
[<img src="https://vimeo.com/184849959/og_image_watermark/594412244" alt="alt text" width="30%">](https://vimeo.com/184849959 "Fluid Particles")
[<img src="https://vimeo.com/184849892/og_image_watermark/594411994" alt="alt text" width="30%">](https://vimeo.com/184849892 "Liquid Painting - M.C. Escher")
[<img src="https://vimeo.com/184849880/og_image_watermark/594411757" alt="alt text" width="30%">](https://vimeo.com/184849880 "Liquid Text")

More Videos on [Vimeo](https://vimeo.com/user56436843).

<br>

## Getting Started - FLuid Simulation


```java

// FLUID SIMULATION EXAMPLE
import com.thomasdiewald.pixelflow.java.DwPixelFlow;
import com.thomasdiewald.pixelflow.java.fluid.DwFluid2D;

// fluid simulation
DwFluid2D fluid;

// render target
PGraphics2D pg_fluid;

public void setup() {
  size(800, 800, P2D);

  // library context
  DwPixelFlow context = new DwPixelFlow(this);

  // fluid simulation
  fluid = new DwFluid2D(context, width, height, 1);

  // some fluid parameters
  fluid.param.dissipation_velocity = 0.70f;
  fluid.param.dissipation_density  = 0.99f;

  // adding data to the fluid simulation
  fluid.addCallback_FluiData(new  DwFluid2D.FluidData() {
    public void update(DwFluid2D fluid) {
      if (mousePressed) {
        float px     = mouseX;
        float py     = height-mouseY;
        float vx     = (mouseX - pmouseX) * +15;
        float vy     = (mouseY - pmouseY) * -15;
        fluid.addVelocity(px, py, 14, vx, vy);
        fluid.addDensity (px, py, 20, 0.0f, 0.4f, 1.0f, 1.0f);
        fluid.addDensity (px, py, 8, 1.0f, 1.0f, 1.0f, 1.0f);
      }
    }
  });

  pg_fluid = (PGraphics2D) createGraphics(width, height, P2D);
}


public void draw() {    
  // update simulation
  fluid.update();

  // clear render target
  pg_fluid.beginDraw();
  pg_fluid.background(0);
  pg_fluid.endDraw();

  // render fluid stuff
  fluid.renderFluidTextures(pg_fluid, 0);

  // display
  image(pg_fluid, 0, 0);
}

```
<img src="https://github.com/diwi/PixelFlow/blob/master/examples/Fluid_GetStarted/out/GetStarted.jpg" alt="result" width="50%">

<br>
<br>

## Installation, Processing IDE

- Download [Processing 3.x.x] (https://processing.org/download/?processing)
- Install PixelFlow via the Library Manager.
- Or manually, unzip and put the extracted PixelFlow folder into the libraries folder of your Processing sketches. Reference and examples are included in the PixelFlow folder. 

#### Platforms
Windows, Linux, MacOSX


<br>

## Dependencies, to run the examples

 - **Video, by the Processing Foundation**<br>
   https://processing.org/reference/libraries/video/index.html
   
 - **ControlP5, by Andreas Schlegel**<br>
   http://www.sojamo.de/libraries/controlP5
   
 - **PeasyCam, by Jonathan Feinberg**<br>
   http://mrfeinberg.com/peasycam


