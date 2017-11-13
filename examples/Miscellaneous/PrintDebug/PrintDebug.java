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


package Miscellaneous.PrintDebug;



import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GL3;
import com.thomasdiewald.pixelflow.java.DwPixelFlow;
import processing.core.PApplet;
import processing.opengl.PJOGL;


public class PrintDebug extends PApplet {
 
  //
  // Quick dump for hardware/system and OpenGL information.
  //

  DwPixelFlow context;
  
  public void settings() {
    size(200, 200, P2D);
    // PJOGL.profile = 3;
  }
  
  public void setup() {
    
    context = new DwPixelFlow(this);
    

    // Library
    context.print();
    

    // OpenGL/GLSL/Driver/Vendor
    context.printGL();
    
    
    // VRAM info, currently available for AMD/NVidia devices
    context.printGL_MemoryInfo();
    
    
    // available OpenGL/GLSL extentions
    context.printGL_Extensions();
    
    //
    // further:
    // https://www.khronos.org/registry/OpenGL/extensions
    //
    // https://www.khronos.org/registry/OpenGL/extensions/INTEL/
    //
    // https://www.khronos.org/registry/OpenGL/extensions/AMD/
    // https://www.khronos.org/registry/OpenGL/extensions/ATI/
    //
    // https://www.khronos.org/registry/OpenGL/extensions/NV/
    // https://www.khronos.org/registry/OpenGL/extensions/NVX/
    //
    // https://www.khronos.org/registry/OpenGL/extensions/KHR/
    // ...
    //
    
    
    
    // print a list of all OpenGL states
    // https://www.khronos.org/registry/OpenGL-Refpages/gl4/html/glGet.xhtml
    
    // parseGLGet();
    
    
    
    
    // checkout some states
    println("\n\n\n");
    println("GL_MAJOR_VERSION                            " + getInt(GL2.GL_MAJOR_VERSION                           ));
    println("GL_MINOR_VERSION                            " + getInt(GL2.GL_MINOR_VERSION                           ));
    println("GL_TIMESTAMP                                " + getInt(GL2.GL_TIMESTAMP                               ));
    println("GL_NUM_EXTENSIONS                           " + getInt(GL2.GL_NUM_EXTENSIONS                          ));
    println();                                                                                                             
    println("GL_MAX_TEXTURE_SIZE                         " + getInt(GL2.GL_MAX_TEXTURE_SIZE                        ));
    println("GL_MAX_3D_TEXTURE_SIZE                      " + getInt(GL2.GL_MAX_3D_TEXTURE_SIZE                     ));
    println("GL_MAX_CUBE_MAP_TEXTURE_SIZE                " + getInt(GL2.GL_MAX_CUBE_MAP_TEXTURE_SIZE               ));
    println("GL_MAX_ARRAY_TEXTURE_LAYERS                 " + getInt(GL2.GL_MAX_ARRAY_TEXTURE_LAYERS                ));
    println("GL_MAX_RECTANGLE_TEXTURE_SIZE               " + getInt(GL2.GL_MAX_RECTANGLE_TEXTURE_SIZE              ));                                                                       
    println("GL_MAX_RENDERBUFFER_SIZE                    " + getInt(GL2.GL_MAX_RENDERBUFFER_SIZE                   ));
    println("GL_MAX_TEXTURE_BUFFER_SIZE                  " + getInt(GL2.GL_MAX_TEXTURE_BUFFER_SIZE                 ));
    println("GL_MAX_TEXTURE_IMAGE_UNITS                  " + getInt(GL2.GL_MAX_TEXTURE_IMAGE_UNITS                 ));
    println("GL_MAX_TEXTURE_LOD_BIAS                     " + getInt(GL2.GL_MAX_TEXTURE_LOD_BIAS                    ));
    println("GL_MAX_DEPTH_TEXTURE_SAMPLES                " + getInt(GL2.GL_MAX_DEPTH_TEXTURE_SAMPLES               ));
    println();                                                                                                           
    println("GL_MAX_DRAW_BUFFERS                         " + getInt(GL2.GL_MAX_DRAW_BUFFERS                        ));
    println("GL_MAX_FRAMEBUFFER_WIDTH                    " + getInt(GL2.GL_MAX_FRAMEBUFFER_WIDTH                   ));
    println("GL_MAX_FRAMEBUFFER_HEIGHT                   " + getInt(GL2.GL_MAX_FRAMEBUFFER_HEIGHT                  ));
    println("GL_MAX_FRAMEBUFFER_LAYERS                   " + getInt(GL2.GL_MAX_FRAMEBUFFER_LAYERS                  ));
    println("GL_MAX_FRAMEBUFFER_SAMPLES                  " + getInt(GL2.GL_MAX_FRAMEBUFFER_SAMPLES                 ));
    println("GL_MAX_VIEWPORT_DIMS                        " + getInt(GL2.GL_MAX_VIEWPORT_DIMS                       ));
    println("GL_MAX_VIEWPORTS                            " + getInt(GL3.GL_MAX_VIEWPORTS                           ));
    println();                                                                                                          
    println("GL_NUM_PROGRAM_BINARY_FORMATS               " + getInt(GL2.GL_NUM_PROGRAM_BINARY_FORMATS              ));
    println("GL_NUM_SHADER_BINARY_FORMATS                " + getInt(GL2.GL_NUM_SHADER_BINARY_FORMATS               ));
    println("GL_MAX_PROGRAM_TEXEL_OFFSET                 " + getInt(GL2.GL_MAX_PROGRAM_TEXEL_OFFSET                ));
    println("GL_MIN_PROGRAM_TEXEL_OFFSET                 " + getInt(GL2.GL_MIN_PROGRAM_TEXEL_OFFSET                ));
    println();                                             
    println("GL_MAX_VERTEX_ATOMIC_COUNTERS               " + getInt(GL2.GL_MAX_VERTEX_ATOMIC_COUNTERS              ));
    println("GL_MAX_VERTEX_ATTRIBS                       " + getInt(GL2.GL_MAX_VERTEX_ATTRIBS                      ));
    println("GL_MAX_VERTEX_SHADER_STORAGE_BLOCKS         " + getInt(GL3.GL_MAX_VERTEX_SHADER_STORAGE_BLOCKS        ));
    println("GL_MAX_VERTEX_TEXTURE_IMAGE_UNITS           " + getInt(GL2.GL_MAX_VERTEX_TEXTURE_IMAGE_UNITS          ));
    println("GL_MAX_VERTEX_UNIFORM_COMPONENTS            " + getInt(GL2.GL_MAX_VERTEX_UNIFORM_COMPONENTS           ));
    println("GL_MAX_VERTEX_UNIFORM_BLOCKS                " + getInt(GL2.GL_MAX_VERTEX_UNIFORM_BLOCKS               ));
    println("GL_MAX_VERTEX_UNIFORM_VECTORS               " + getInt(GL2.GL_MAX_VERTEX_UNIFORM_VECTORS              ));
    println();                                                                                                            
    println("GL_MAX_FRAGMENT_INPUT_COMPONENTS            " + getInt(GL3.GL_MAX_FRAGMENT_INPUT_COMPONENTS           ));
    println("GL_MAX_FRAGMENT_UNIFORM_COMPONENTS          " + getInt(GL2.GL_MAX_FRAGMENT_UNIFORM_COMPONENTS         ));
    println("GL_MAX_FRAGMENT_UNIFORM_VECTORS             " + getInt(GL2.GL_MAX_FRAGMENT_UNIFORM_VECTORS            ));
    println("GL_MAX_FRAGMENT_UNIFORM_BLOCKS              " + getInt(GL2.GL_MAX_FRAGMENT_UNIFORM_BLOCKS             ));
    println();                                                                                                               
    println("GL_MAX_VARYING_COMPONENTS                   " + getInt(GL2.GL_MAX_VARYING_COMPONENTS                  ));
    println("GL_MAX_VARYING_VECTORS                      " + getInt(GL2.GL_MAX_VARYING_VECTORS                     ));
    println("GL_MAX_VARYING_FLOATS                       " + getInt(GL2.GL_MAX_VARYING_FLOATS                      ));
    println();                                                                                                               
    println("GL_MAX_UNIFORM_BUFFER_BINDINGS              " + getInt(GL2.GL_MAX_UNIFORM_BUFFER_BINDINGS             ));
    println("GL_MAX_UNIFORM_BLOCK_SIZE                   " + getInt(GL2.GL_MAX_UNIFORM_BLOCK_SIZE                  ));
    println("GL_MAX_UNIFORM_LOCATIONS                    " + getInt(GL3.GL_MAX_UNIFORM_LOCATIONS                   ));
    println();                                                                                                         
    println("GL_MAX_COMBINED_FRAGMENT_UNIFORM_COMPONENTS " + getInt(GL2.GL_MAX_COMBINED_FRAGMENT_UNIFORM_COMPONENTS));
    println("GL_MAX_COMBINED_GEOMETRY_UNIFORM_COMPONENTS " + getInt(GL2.GL_MAX_COMBINED_GEOMETRY_UNIFORM_COMPONENTS));
    println("GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS         " + getInt(GL2.GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS        ));
    println("GL_MAX_COMBINED_UNIFORM_BLOCKS              " + getInt(GL2.GL_MAX_COMBINED_UNIFORM_BLOCKS             ));
    println("GL_MAX_ELEMENTS_VERTICES                    " + getInt(GL2.GL_MAX_ELEMENTS_VERTICES                   ));
    println("GL_MAX_ELEMENTS_INDICES                     " + getInt(GL2.GL_MAX_ELEMENTS_INDICES                    ));


    exit();
  }
  
  
  
  int[] vint = new int[1];
  int getInt(int pname){
    context.gl.glGetIntegerv(pname, vint, 0);
    return vint[0];
  }
  
  
  float[] vfloat = new float[1];
  float getFloat(int pname){
    context.gl.glGetFloatv(pname, vfloat, 0); 
    return vfloat[0];
  }
  
  
  void parseGLGet(){
    String[] lines = context.utils.readASCIIfile("data/GL_GET_INFO.txt");
    int idx = -1;
    String[] list_pnames = new String[lines.length];
    String[] list_docs   = new String[lines.length];
    
    for(int i = 0; i < lines.length; i++){
      String line = lines[i].trim();
      
      if(line.startsWith("GL_")){
        idx++;
        String[] token = line.split(" ");
        list_pnames[idx] = token[0];
        list_docs  [idx] = "";
//        if(token.length != 1){
//          System.out.println(line);
//        }
      } else {
        list_docs[idx] += line;
      }
    }
    
    System.out.println("\n\n");
    System.out.println("Available GL States to get");
    for(int i = 0; i <= idx; i++){
      System.out.printf("[%3d] %-50s - %s\n", i,  list_pnames[i], list_docs[i]);
    }
    System.out.println("\n\n");
  }

  
  
  

  public static void main(String args[]) {
    PApplet.main(new String[] { PrintDebug.class.getName() });
  }
}