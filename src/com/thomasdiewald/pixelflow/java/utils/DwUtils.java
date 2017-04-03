/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */

package com.thomasdiewald.pixelflow.java.utils;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Arrays;

import com.thomasdiewald.pixelflow.java.DwPixelFlow;



public class DwUtils {

  final static public String NL = System.getProperty("line.separator");
  
  DwPixelFlow context;
  
  public DwUtils(DwPixelFlow context){
    this.context = context;
  }
  
  
  final static public int log2ceil(double val){
    return (int) Math.ceil(Math.log(val)/Math.log(2));
  }
  
  final static public float mix(float a, float b, float mix){
    return a*(1f-mix) + b * (mix);
  }


  public String[] readASCIIfile(InputStream inputstream) {
    BufferedReader reader = null;

    int num_lines = 0;
    String[] lines = new String[2048];

    try {
      reader = new BufferedReader(new InputStreamReader(inputstream));
      String line = null;

      while ((line = reader.readLine()) != null) {
        if (num_lines == lines.length) {
          lines = Arrays.copyOf(lines, num_lines << 1);
        }
        lines[num_lines++] = line;
      }
    } catch (FileNotFoundException e) {
      e.printStackTrace();
      return null;
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    } finally {
      try {
        if (reader != null) {
          reader.close();
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    return Arrays.copyOf(lines, num_lines);
  }

  
  
  
  
  
//  public InputStream createInputStream(String path) {
//
//    InputStream inputstream = null;
//
//    if (inputstream == null) {
//      URL url = DwUtils.class.getClassLoader().getResource(path);
//      inputstream = DwUtils.class.getClassLoader().getResourceAsStream(path);
//      if (url != null && inputstream != null) {
//        System.out.println("v0 url: " + url.getFile());
//        // System.out.println("v2 uri: "+ url.toURI());
//      }
//    }
//
//    if (inputstream == null) {
//      URL url = context.papplet.getClass().getResource(path);
//    
//      inputstream = context.papplet.getClass().getResourceAsStream(path);
//      if (url != null && inputstream != null) {
//        System.out.println("v1 url: " + url.getFile());
//        // System.out.println("v1 uri: "+ url.toURI());
//      } 
//    }
//
//    if (inputstream == null) {
//      inputstream = context.papplet.createInput(path);
//      if(inputstream != null){
//        System.out.println("v2 path: "+path);
//      }
//    }
//
//    if (inputstream == null) {
//      System.out.println("could not create inputstream for " + path);
//    }
//
//    return inputstream;
//  }

  
  
  private boolean DEBUG = !true;
  
  
  public InputStream createInputStream(String path) {

    InputStream inputstream = null;
    
    if (inputstream == null) {
      File file = new File(path);
      if(file.exists()){
        try {
          inputstream = new FileInputStream(file);
          if(DEBUG)System.out.println("v0 path: " + file);
        } catch (FileNotFoundException e) {
          e.printStackTrace();
        }
      }
    }

    
    if (inputstream == null) {
      URL url = DwUtils.class.getClassLoader().getResource(path);
      if(url != null){
        inputstream = DwUtils.class.getClassLoader().getResourceAsStream(path);
        if (inputstream != null) {
          if(DEBUG)System.out.println("v0 url: " + url.getFile());
        }
      }
    }
    

    if (inputstream == null) {
      URL url = context.papplet.getClass().getResource(path);
      if(url != null){
        inputstream = context.papplet.getClass().getResourceAsStream(path);
        if (inputstream != null) {
          if(DEBUG)System.out.println("v1 path: " + url);
        } 
      }
    }

    
    // no success so far, so try the processing way (slower)
    if (inputstream == null) {
      inputstream = context.papplet.createInput(path);
      if(inputstream != null){
        if(DEBUG)System.out.println("v2 path: "+path);
      }
    }

    
    if (inputstream == null) {
      System.out.println("DwUtils ERROR: could not create inputstream for " + path);
    }

    return inputstream;
  }

  
  
  
  
  
  public String[] readASCIIfile(String path) {
    InputStream inputstream = createInputStream(path);
    String[] lines = readASCIIfile(inputstream);
    return lines;
  }
  
  public String[] readASCIIfileNL(String path) {
    String[] lines = readASCIIfile(path);
    for(int i = 0; i < lines.length; i++){
      lines[i] += DwUtils.NL;
    }
    return lines;
  }


}
