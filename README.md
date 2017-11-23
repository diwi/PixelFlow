![PixelFlow Header](http://thomasdiewald.com/processing/libraries/pixelflow/PixelFlow_header.jpg)

# PixelFlow
**A Processing/Java library for high performance GPU-Computing (GLSL).**


<br>

## Features

```
+ Fluid Simulation (GLSL)
    - Jos Stam, Real-Time Fluid Dynamics for Games
    - Particle Sytems
    - Flow Field Visualisation
    - Streamlines
    - ...
+ Flow Field Particles (GLSL)
    - Collision Detection, Particle <-> Particle
    - Collision Detection, Particle <-> Obstacle
    - Cohesion
    - Verlet Integration
    - FlowField/SDF(Signed Distance Field) based
    - Streamlines
    - ...
+ Softbody Dynamics (CPU, GLSL is coming)
    - 2D and 3D
    - Collision Detection
    - Cloth, Grids, Chains, Rigid Folding ...
    - Particle Systems
    - ...
+ Skylight Renderer (GLSL)
    - Interactive/Realtime Viewport Renderer
    - Ambient Occlusion
    - Diffuse Shading
    - ShadowMapping
    - ...
+ PostProcessing Filters (GLSL)
    - Box Blur
    - Binomial Blur
    - Gauss Blur
    - Gauss Blur Pyramid
    - Median
    - Bilateral Filter
    - Custom Convolution Kernel
    - DoG (Difference of Gaussian)
    - BackgroundSubtraction
    - Difference
    - Laplace
    - Sobel
    - Gamma Correction
    - Luminace
    - Thresholding
    - Harris Corner Detection
    - Optical Flow
    - Bloom
    - Depth of Field (DoF)
    - Liquid FX Filter
    - Summed Area Table (SAT)
    - Distance Transform (Jumpflood), Voronoi, Distance Map
    - Min/Max (global)
    - Min/Max (local)
    - Merge Shader
    - FlowField
    - Line Integral Convolution (LIC) / Streamlines
    - ...
+ AntiAliasing (GLSL)
    - MSAA (Processing Default)
    - FXAA
    - SMAA
    - GBAA/DEAA
+ Shadertoy (GLSL)
    - Wrapper for running existing Shadertoy sketches inside Processing.
+ Utils
    - HalfEdge
    - Subdivision Polyhedra
    - Sampling
    - GLSL-Shader PreProcessor (#define, #include)
    - GLSL-Shader Uniform Caching
    - ...
```  


JavaDoc: http://thomasdiewald.com/processing/libraries/pixelflow/reference/index.html

<br>

## Download
+ [Releases](https://github.com/diwi/PixelFlow/releases)
+ [PixelFlow Website](http://thomasdiewald.com/processing/libraries/pixelflow)
+ Processing IDE -> Library Manager
 
<br>

## Videos / Examples / Demos / Applications

#### Skylight Renderer
[<img src="https://i.vimeocdn.com/video/661407332.jpg" alt="alt text" width="30%">](https://vimeo.com/238654801 "Skylight Renderer - Ambient Occlusion / Soft Shadows")
[<img src="https://i.vimeocdn.com/video/621790715.jpg" alt="alt text" width="30%">](https://vimeo.com/206696210 "Skylight - Cloth Simulation")
[<img src="https://i.vimeocdn.com/video/621791014.jpg" alt="alt text" width="30%">](https://vimeo.com/206696738 "Skylight - Poisson Spheres")
[<img src="https://i.vimeocdn.com/video/635968099.jpg" alt="alt text" width="30%">](https://vimeo.com/218485498 "Rigid Body - Menger Sponge")
[<img src="https://i.vimeocdn.com/video/627412633.jpg" alt="alt text" width="30%">](https://vimeo.com/211395605 "Rigid Origami Simulation")


#### Softbody Dynamics
[<img src="https://vimeo.com/184854758/og_image_watermark/59441739" alt="alt text" width="30%">](https://vimeo.com/184854758 "SoftBody Dynamics 3D - Playground, Cloth Simulation")
[<img src="https://vimeo.com/184854746/og_image_watermark/594416647" alt="alt text" width="30%">](https://vimeo.com/184854746 "SoftBody Dynamics 3D - Cloth Simulation")
[<img src="https://vimeo.com/184853892/og_image_watermark/594415861" alt="alt text" width="30%">](https://vimeo.com/184853892 "SoftBody Dynamics 2D - Playground")


#### Computational Fluid Dynamics
[<img src="https://vimeo.com/184850259/og_image_watermark/594412638" alt="alt text" width="30%">](https://vimeo.com/184850259 "WindTunnel")
[<img src="https://vimeo.com/184850254/og_image_watermark/594412429" alt="alt text" width="30%">](https://vimeo.com/184850254 "StreamLines")
[<img src="https://vimeo.com/184849960/og_image_watermark/594410553" alt="alt text" width="30%">](https://vimeo.com/184849960 "Verlet Particle Collision System")
[<img src="https://vimeo.com/184849959/og_image_watermark/594412244" alt="alt text" width="30%">](https://vimeo.com/184849959 "Fluid Particles")
[<img src="https://vimeo.com/184849892/og_image_watermark/594411994" alt="alt text" width="30%">](https://vimeo.com/184849892 "Liquid Painting - M.C. Escher")
[<img src="https://vimeo.com/184849880/og_image_watermark/594411757" alt="alt text" width="30%">](https://vimeo.com/184849880 "Liquid Text")

#### Optical Flow
[<img src="https://i.vimeocdn.com/video/594413465.jpg" alt="alt text" width="30%">](https://vimeo.com/184850333 "Optical Flow - Fluid Simulation - MovieClip")
[<img src="https://i.vimeocdn.com/video/644816088.jpg" alt="alt text" width="30%">](https://vimeo.com/225484146 "MovieWall")
[<img src="https://i.vimeocdn.com/video/645059994.jpg" alt="alt text" width="30%">](https://vimeo.com/225671748 "VoxelCapture")

#### Flow Field Particle Simulation
[<img src="https://i.vimeocdn.com/video/659213047.jpg" alt="alt text" width="30%">](https://vimeo.com/236955859 "Flow Field Particles - Trails HD")
[<img src="https://i.vimeocdn.com/video/659220662.jpg" alt="alt text" width="30%">](https://vimeo.com/236964112 "Flow Field Particles - Trails HD - 100K Particles")
[<img src="https://i.vimeocdn.com/video/659039289.jpg" alt="alt text" width="30%">](https://vimeo.com/236821149 "Flow Field Particles - DevDemo [R&D]")

#### Flow Field - LIC
[<img src="https://i.vimeocdn.com/video/660270815.jpg" alt="alt text" width="30%">](https://vimeo.com/237766186 "LIC - WindTunnel")
[<img src="https://i.vimeocdn.com/video/659919142.jpg" alt="alt text" width="30%">](https://vimeo.com/237491566 "LIC - First Results")

#### Realtime 2D Radiosity - Global Illumination (GI)
[<img src="https://i.vimeocdn.com/video/668126268.jpg" alt="alt text" width="30%">](https://vimeo.com/243877934 "Tetris + Realtime Radiosity")
[<img src="https://i.vimeocdn.com/video/668525007.jpg" alt="alt text" width="30%">](https://vimeo.com/244191105 "Radiant Poisson Disks")
[<img src="https://i.vimeocdn.com/video/646193678.jpg" alt="alt text" width="30%">](https://vimeo.com/226554155 "Realtime GI - LiquidFun/Box2D")
[<img src="https://i.vimeocdn.com/video/646483311.jpg" alt="alt text" width="30%">](https://vimeo.com/226784500 "Realtime GI - LiquidFun/Box2D")
[<img src="https://i.vimeocdn.com/video/646679859.jpg" alt="alt text" width="30%">](https://vimeo.com/226939350 "Realtime GI - LiquidFun/Box2D")
[<img src="https://i.vimeocdn.com/video/630695092.jpg" alt="alt text" width="30%">](https://vimeo.com/214264003 "Realtime GI - Random Modelling")
[<img src="https://i.vimeocdn.com/video/646490228.jpg" alt="alt text" width="30%">](https://vimeo.com/226790885 "Realtime GI - Cornell Box")

#### Space Syntax

[<img src="https://i.vimeocdn.com/video/648427506.jpg" alt="alt text" width="30%">](https://vimeo.com/228387816 "SpaceSyntax - Realtime Local Evaluations")
[<img src="https://i.vimeocdn.com/video/654202699.jpg" alt="alt text" width="30%">](https://vimeo.com/232981476 "SpaceSyntax - Realtime Global Evaluations")
[<img src="https://i.vimeocdn.com/video/653671271.jpg" alt="alt text" width="30%">](https://vimeo.com/232576820 "FlowField Pathfinding - Particle Simulation")

<br>

More Videos on [Vimeo](https://vimeo.com/user56436843).

<br>

## Getting Started - Fluid Simulation


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
<br>

<br>

## Installation, Processing IDE

- Download [Processing 3](https://processing.org/download/?processing)
- Install PixelFlow via the Library Manager.
- Or manually, unzip and put the extracted PixelFlow folder into the libraries folder of your Processing sketches. Reference and examples are included in the PixelFlow folder. 

- Also make sure you have the latest graphics card driver installed!

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

 - **HE_Mesh, by Frederik Vanhoutte**<br>
   https://github.com/wblut/HE_Mesh
   
<br>

## Processing/Java Alternatives

### JRubyArt
[JRubyArt](https://github.com/ruby-processing/JRubyArt) is a ruby wrapper for processing by [Martin Prout (monkstone)](https://github.com/monkstone)
 - Blog: https://monkstone.github.io/
 - Setup: http://ruby-processing.github.io/JRubyArt/
 - Demos: [JRubyArt Pixelflow Examples](https://github.com/ruby-processing/JRubyArt-examples/tree/master/external_library/java/PixelFlow)
