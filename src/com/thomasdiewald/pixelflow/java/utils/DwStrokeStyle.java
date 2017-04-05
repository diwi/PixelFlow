package com.thomasdiewald.pixelflow.java.utils;


public class DwStrokeStyle{
  public int stroke_color = 0xFF000000;
  public float stroke_weight = 1.0f;
  public DwStrokeStyle(){}
  public DwStrokeStyle(int stroke_color, float stroke_weight){
    this.stroke_color = stroke_color;
    this.stroke_weight = stroke_weight;
  }
}