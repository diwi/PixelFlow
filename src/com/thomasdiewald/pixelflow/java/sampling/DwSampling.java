/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */


package com.thomasdiewald.pixelflow.java.sampling;


public class DwSampling {
  
  
  public static final double PI_TWO = Math.PI*2.0;
  public static final double GOLDEN_ANGLE_R = Math.PI * (3.0 - Math.sqrt(5.0));
  
  
  static public double halton(int index, int base){
    double result = 0;
    double f = 1f / base;
    int i = index;
    while (i > 0){
      result += f * (i % base);
      i /= base;
      f /= base;
    }
    return result;
  }
  static public double halton2(int index, int base){
//    int counter = 0;
    double result = 0;
    double f = 1f / base;
    int i = index;
    while (i > 0){
      result += f * (i % base);
      i /= base;
      f /= base;
//      counter++;
    }
//    System.out.println("counter = "+counter);
    return result;
  }


  
  
  static public float[] uniformSampleSphere_Halton(int index, int p1, int p2){
    double phi = halton(index, p1) * PI_TWO;    // [0,2*PI]
    double rnd = halton(index, p2) * 2.0 - 1.0; // [-1,+1]
    double rad = Math.sqrt(1.0 - rnd*rnd);
    
    double X   = Math.cos(phi) * rad;
    double Y   = Math.sin(phi) * rad;
    double Z   = rnd;
    return float3(X,Y,Z);
  }
  
  static public float[] uniformSampleSphere_Halton(int index){
    double phi = halton(index, 2) * PI_TWO;    // [0,2*PI]
    double rnd = halton(index, 3) * 2.0 - 1.0; // [-1,+1]
    
    double rad = Math.sqrt(1.0 - rnd*rnd);
    
    double X   = Math.cos(phi) * rad;
    double Y   = Math.sin(phi) * rad;
    double Z   = rnd;
    
    return float3(X,Y,Z);
  }
  
  
  static public float[] uniformSampleSphere_Halton(int index, float horizontal){
    double phi = halton(index, 2) * PI_TWO;    // [0,2*PI]
    double rnd = halton(index, 3) * 2.0 - 1.0; // [-1,+1]
    
    rnd *= horizontal;
    
    double rad = Math.sqrt(1.0 - rnd*rnd);
    
    double X   = Math.cos(phi) * rad;
    double Y   = Math.sin(phi) * rad;
    double Z   = rnd;
    
    return float3(X,Y,Z);
  }
  

  
  public static float[] uniformSampleSphere_Random() {
    double phi = Math.random() * PI_TWO;      // [0,2*PI] 
    double rnd = Math.random() * 2.0 - 1.0;   // [-1,+1]  
    double rad = Math.sqrt(1.0 - rnd*rnd);
    
    double X   = Math.cos(phi) * rad;
    double Y   = Math.sin(phi) * rad;
    double Z   = rnd;
    return float3(X,Y,Z);
  }
  
  
  
  
  
  
  
  // same as cosineSampleHemisphere(scale, 1);
  public static float[] cosineSampleHemisphere_Random(double scale){
    double phi = Math.random() * PI_TWO ;
    double rnd = Math.random();
    double rad = Math.sqrt(1.0-rnd) * scale;
    double X   = Math.cos(phi) * rad;
    double Y   = Math.sin(phi) * rad;
    double Z   = Math.sqrt(rnd);
    return float3(X,Y,Z);
  }
  
  // same as cosineSampleHemisphere(scale, 0);
  public static float[] uniformSampleHemisphere_Random(double scale){
    double phi = Math.random() * PI_TWO;
    double rnd = Math.random();
    double rad = Math.sqrt(1.0 - rnd*rnd) * scale;
    double X   = Math.cos(phi) * rad;
    double Y   = Math.sin(phi) * rad;
    double Z   = rnd;
    return float3(X,Y,Z);
  }

  // same as cosineSampleHemisphere(scale, 0);
  public static float[] uniformSampleHemisphere_Halton(int index){
    double phi = halton(index, 2) * PI_TWO;
    double rnd = halton(index, 3);
    double rad = Math.sqrt(1.0 - rnd*rnd);
    double X   = Math.cos(phi) * rad;
    double Y   = Math.sin(phi) * rad;
    double Z   = rnd;
    return float3(X,Y,Z);
  }
  
  // same as cosineSampleHemisphere(scale, 1);
  public static float[] cosineSampleHemisphere_Halton(int index){
    double phi = halton(index, 2) * PI_TWO ;
    double rnd = halton(index, 3);
    double rad = Math.sqrt(1.0-rnd);
    double X   = Math.cos(phi) * rad;
    double Y   = Math.sin(phi) * rad;
    double Z   = Math.sqrt(rnd);
    return float3(X,Y,Z);
  }
  
  public static float[] float3(double x, double y, double z){
    return new float[]{(float)x, (float)y, (float)z};
  }
  public static float[] float3(float x, float y, float z){
    return new float[]{x, y, z};
  }
}
