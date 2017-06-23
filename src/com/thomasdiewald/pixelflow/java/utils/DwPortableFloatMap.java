/**
 *
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 *
 * Portable FloatMap reader/writer | Copyright (c) 2014 Project Nayuki - 
 * https://www.nayuki.io/page/portable-floatmap-format-io-java
 *
 * MIT License: https://opensource.org/licenses/MIT
 *
 */ 


package com.thomasdiewald.pixelflow.java.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;


/**
 * 
 * PFM - Portable Float Format<br>
 * <br>
 * 1) http://netpbm.sourceforge.net/doc/pfm.html<br>
 * 2) http://www.pauldebevec.com/Research/HDR/PFM/<br>
 * 3) https://www.nayuki.io/page/portable-floatmap-format-io-java<br>
 * <br>
 * This version is based on 3)<br>
 * 
 * @author Nayuki (nayuki.io)
 * @author Thomas Diewald
 * 
 *
 */
public final class DwPortableFloatMap {

  public File file;

  public boolean bigEndian = true;

  // format: width/height/channels
  public boolean grayscale = false;
  public int width;
  public int height;

  // float data
  public float[] float_array;
  // byte data for faster writing pmf to a BufferedOutputStream
  private byte byte_array[];


  public DwPortableFloatMap() {
  }

  public void read(File file) throws IOException {
    if (file == null){
      throw new NullPointerException();
    }
    this.file = file;
    InputStream in = new BufferedInputStream(new FileInputStream(file));
    try {
      read(in);
    } finally {
      in.close();
    }
  }


  public void read(InputStream in) throws IOException {
    if (in == null){
      throw new NullPointerException();
    }

    String format     = readLine(in).trim();
    String dimension  = readLine(in).trim();
    String endianness = readLine(in).trim();

    grayscale = format.equals("Pf");           //  Pf ... R, PF ... RGB
    String[] tokens = dimension.split(" ", 2); // widht height
    width  = Integer.parseInt(tokens[0]);
    height = Integer.parseInt(tokens[1]);
    bigEndian = Double.parseDouble(endianness) > 0.0; // bigEndian > 0, littleEndian < 0, error ... 0

    if (width <= 0 || height <= 0){
      throw new IllegalArgumentException("width/height invalid");
    }


    // realloc only when needed
    int num_floats = width * height * (grayscale ? 1 : 3);
    if(float_array == null || float_array.length != num_floats){
      float_array = new float[num_floats];
    }
    int num_bytes = num_floats * 4;
    if(byte_array == null || byte_array.length != num_bytes){
      byte_array = new byte[num_bytes];
    }


    BufferedInputStream bis = new BufferedInputStream(in);

    int len = bis.read(byte_array);
    if(len != byte_array.length){
      throw new IOException("TODO BufferedInputStream.read(b_array) ");
    }

    // read float32 from 4 bytes 
    if(bigEndian){
      for (int i = 0, bi = 0; i < num_floats; i++) {
        int val = (byte_array[bi++] & 0xFF) << 24 | 
                  (byte_array[bi++] & 0xFF) << 16 | 
                  (byte_array[bi++] & 0xFF) <<  8 | 
                  (byte_array[bi++] & 0xFF);
        float_array[i] = Float.intBitsToFloat(val);
      }
    } else {
      for (int i = num_floats-1, bi = num_bytes-1; i >= 0; i--) {
        int val = (byte_array[bi--] & 0xFF) << 24 | 
                  (byte_array[bi--] & 0xFF) << 16 | 
                  (byte_array[bi--] & 0xFF) <<  8 | 
                  (byte_array[bi--] & 0xFF);
        float_array[i] = Float.intBitsToFloat(val);
      }
    }

  }



  private static String readLine(InputStream in) throws IOException {
    ByteArrayOutputStream bout = new ByteArrayOutputStream();
    for (int i = 0; ; i++) {
      int b = in.read();
      if (b == '\n' || b == -1)
        break;
      else if (i == 100)
        throw new IllegalArgumentException("Line too long");
      else
        bout.write(b);
    }
    return new String(bout.toByteArray(), "US-ASCII");
  }



  public void write(File file, float[] data, int w, int h) throws IOException {
    if (file == null)
      throw new NullPointerException();
    this.file = file;
    OutputStream out = new BufferedOutputStream(new FileOutputStream(file));
    try {
      write(out, data, w, h);
    } finally {
      out.close();
    }
  }


  public void write(OutputStream out, float[] data, int w, int h) throws IOException {
    if (out == null)
      throw new NullPointerException();

    this.width = w;
    this.height = h;
    this.float_array = data;


    // Write header text data. Must use Unix newlines, not universal style
    PrintWriter pout = new PrintWriter(new OutputStreamWriter(out, "US-ASCII"));

    if (width <= 0 || height <= 0){
      throw new IllegalArgumentException("width/height invalid");
    }

    int num_pixels = width * height;
    int num_floats = float_array.length;

    if(num_floats == num_pixels * 1) { grayscale = true; }
    else if(num_floats == num_pixels * 3) { grayscale = false; }
    else throw new IllegalStateException("f_array length does match neither format (Grayscale or RGB).");

    pout.print((grayscale ? "Pf" : "PF") + "\n");
    pout.print(width + " " + height + "\n");
    pout.print((bigEndian ? "1.0" : "-1.0") + "\n");
    pout.flush();

    // realloc only when needed
    int num_bytes = num_floats * 4;
    if(byte_array == null || byte_array.length != num_bytes){
      byte_array = new byte[num_bytes];
    }

    // write float32 as 4 bytes
    if(bigEndian){
      for (int i = 0, bi = 0; i < num_floats; i++) {
        int val = Float.floatToRawIntBits(float_array[i]);
        byte_array[bi++] = (byte) ((val >> 24) & 0xFF);
        byte_array[bi++] = (byte) ((val >> 16) & 0xFF);
        byte_array[bi++] = (byte) ((val >>  8) & 0xFF);
        byte_array[bi++] = (byte) ((val      ) & 0xFF);
      }
    } else {
      for (int i = 0, bi = 0; i < num_floats; i++) {
        int val = Float.floatToRawIntBits(float_array[i]);
        byte_array[bi++] = (byte) ((val      ) & 0xFF);
        byte_array[bi++] = (byte) ((val >>  8) & 0xFF);
        byte_array[bi++] = (byte) ((val >> 16) & 0xFF);
        byte_array[bi++] = (byte) ((val >> 24) & 0xFF);
      }
    }

    // output
    BufferedOutputStream bout = new BufferedOutputStream(out);
    bout.write(byte_array);
  }










  public void debugCompare(DwPortableFloatMap othr){
    if(this.width     != othr.width    ) System.out.println("width:    "+this.width +" != "+othr.width);
    if(this.height    != othr.height   ) System.out.println("height:   "+this.height +" != "+othr.height);
    if(this.bigEndian != othr.bigEndian) System.out.println("endian:   "+this.bigEndian +" != "+othr.bigEndian);
    if(this.grayscale != othr.grayscale) System.out.println("grayscale:"+this.grayscale +" != "+othr.grayscale);

    if( this.float_array.length != othr.float_array.length){
      System.out.println("different pixels lengths");
      return;
    }

    int err_cnt = 0;
    for(int i = 0; i < float_array.length; i++){
      if(err_cnt < 20 && this.float_array[i] != othr.float_array[i]){
        System.out.println("pixels["+i+"] "+this.float_array[i]+" != "+othr.float_array[i]);
        err_cnt++;
      }

    }

  }


  public void debugPrint(){
    System.out.println("pfm.file:     "+file);
    System.out.println("pfm.width     "+width);
    System.out.println("pfm.height    "+height);
    System.out.println("pfm.grayscale "+grayscale);
  }

}
