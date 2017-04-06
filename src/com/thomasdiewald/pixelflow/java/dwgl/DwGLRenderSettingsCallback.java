package com.thomasdiewald.pixelflow.java.dwgl;

import com.thomasdiewald.pixelflow.java.DwPixelFlow;

public interface DwGLRenderSettingsCallback{
  void set(DwPixelFlow context, int x, int y, int w, int h);
}