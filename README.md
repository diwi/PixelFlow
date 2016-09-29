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
	- Box Blur.java
	- Custom Convolution Kernel
	- DoG (Difference of Gaussian)
	- Gaussian Blur
	- Laplace
	- MedianFilter
	- Sobel
+ Softbody Dynamics (CPU, GLSL is coming)
  - 2D and 3D
  - Collision Detection
  - Cloth, Grids, Chains, ...
  - Particle Systems
  - etc ...
+ Utils
  - HalfEdge
  - Subdivision Polyhedra
```    


<br>
## Download
+ [Releases] (https://github.com/diwi/PixelFlow/releases)
+ [PixelFlow Website] (http://thomasdiewald.com/processing/libraries/pixelflow)
+ Processing IDE -> Library Manager
 

<br>
## videos

[Vimeo](https://vimeo.com/user56436843)

#### Softbody Dynamics
[<img src="https://vimeo.com/184854758/og_image_watermark/59441739" alt="alt text" width="30%">](https://vimeo.com/184854758 "SoftBody Dynamics 3D - Playground, Cloth Simulation")
[<img src="https://vimeo.com/184854746/og_image_watermark/594416647" alt="alt text" width="30%">](https://vimeo.com/184854746 "SoftBody Dynamics 3D - Cloth Simulation")
[<img src="https://vimeo.com/184853892/og_image_watermark/594415861" alt="alt text" width="30%">](https://vimeo.com/184853892 SoftBody Dynamics 2D - Playground")
[<img src="https://vimeo.com/184853892/og_image_watermark/594415861" alt="alt text" width="30%">](https://vimeo.com/184853883 SoftBody Dynamics 2D - Connected Bodies")

#### Computational Fluid Dynamics
[<img src="https://vimeo.com/184850259/og_image_watermark/594412638" alt="alt text" width="30%">](https://vimeo.com/184850259 "WindTunnel")
[<img src="https://vimeo.com/184850254/og_image_watermark/594412429" alt="alt text" width="30%">](https://vimeo.com/184850254 "StreamLines")
[<img src="https://vimeo.com/184849960/og_image_watermark/594410553" alt="alt text" width="30%">](https://vimeo.com/184849960 "Verlet Particle Collision System")
[<img src="https://vimeo.com/184849959/og_image_watermark/594412244" alt="alt text" width="30%">](https://vimeo.com/184849959 "Fluid Particles")
[<img src="https://vimeo.com/184849892/og_image_watermark/594411994" alt="alt text" width="30%">](https://vimeo.com/184849892 "Liquid Painting - M.C. Escher")
[<img src="https://vimeo.com/184849880/og_image_watermark/594411757" alt="alt text" width="30%">](https://vimeo.com/184849880 "Liquid Text")


<br>
## Getting Started


```java

// FLUID SIMULATION EXAMPLE

import com.thomasdiewald.pixelflow.java.Fluid;
import com.thomasdiewald.pixelflow.java.PixelFlow;

// fluid simulation
Fluid fluid;

// render targets
PGraphics2D pg_fluid;


public void setup() {
  size(800, 800, P2D);
  
  // library context
  PixelFlow context = new PixelFlow(this);

  // fluid simulation
  fluid = new Fluid(context, width, height, 1);
  
  // set some fluid paramaters
  fluid.param.dissipation_velocity = 0.70f;
  fluid.param.dissipation_density  = 0.99f;

  // adding data to the fluid simulation
  fluid.addCallback_FluiData(new Fluid.FluidData() {
    public void update(Fluid fluid) {
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

  // render-target
  pg_fluid = (PGraphics2D) createGraphics(width, height, P2D);

  frameRate(60);
}


public void draw() {    
  // update simulation
  fluid.update();

  // clear render target
  pg_fluid.beginDraw();
  pg_fluid.background(0);
  pg_fluid.endDraw();

  // render
  fluid.renderFluidTextures(pg_fluid, 0);

  // display
  image(pg_fluid, 0, 0);
}
```
<img src="https://github.com/diwi/PixelFlow/blob/master/examples/Fluid_GetStarted/out/GetStarted.jpg" alt="result" width="50%">

<br>
## Platforms
Windows, Linux, MacOSX


<br>
## Processing Version
- [Processing 3.2.1] (https://processing.org/download/?processing)


<br>
### Installation, Processing IDE
- Install via the Library Manager
- Or manually. Unzip and put the extracted PixelFlow folder into the libraries folder of your Processing sketches. Reference and examples are included in the PixelFlow folder. 




