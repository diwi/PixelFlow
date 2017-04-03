/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */

package com.thomasdiewald.pixelflow.java.utils;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import processing.core.PApplet;
import processing.core.PGraphics;

public class DwFrameCapture {
  
  public PApplet papplet;
  public boolean AUTO_CAPTURE = !true;
  public float   jpeg_compression = 0.8f;
  
  public String root_dir = "";
  

  private ExecutorService executorService;
  private long start_ms = 0;
  private int frame_count = 0;
  
  public int  num_processors;
  public long freeMemory    ;
  public long maxMemory     ;
  public long totalMemory   ;
  public long usedMemory   ;

  public DwFrameCapture(PApplet papplet, String root_dir){
    this.papplet = papplet;
    this.papplet.registerMethod("post", this);
    this.papplet.registerMethod("dispose", this);
    this.start_ms = System.currentTimeMillis();
    this.root_dir = root_dir;
    
    
    num_processors = Runtime.getRuntime().availableProcessors();
    freeMemory     = Runtime.getRuntime().freeMemory ();
    maxMemory      = Runtime.getRuntime().maxMemory  ();
    totalMemory    = Runtime.getRuntime().totalMemory();
    
//    System.out.printf("processors: %d cores   \n", num_processors);
//    System.out.printf("Maximum memory: %5d mb \n", (freeMemory  >> 20));
//    System.out.printf("Free memory:    %5d mb \n", (maxMemory   >> 20));
//    System.out.printf("Total memory:   %5d mb \n", (totalMemory >> 20));
//
//    File[] roots = File.listRoots();
//    for (File root : roots) {
//      long root_TotalSpace  = root.getTotalSpace ();
//      long root_FreeSpace   = root.getFreeSpace  ();
//      long root_UsableSpace = root.getUsableSpace();
//      
//      System.out.printf("root: %s            \n", root.getAbsolutePath());
//      System.out.printf("TotalSpace:  %5d gb \n", (root_TotalSpace >> 30));
//      System.out.printf("FreeSpace:   %5d gb \n", (root_FreeSpace  >> 30));
//      System.out.printf("UsableSpace: %5d gb \n", (root_UsableSpace>> 30));
//    }
    
    this.executorService = Executors.newFixedThreadPool(num_processors);
    
  }
   
  public void dispose(){
    stop();
  }
  
  
  
  public void post() {
    if(AUTO_CAPTURE){
      capture();
    }
  }
  
  
  public void start(){
    AUTO_CAPTURE = true;
  }
  
  public void stop(){
    executorService.shutdown();
    
    try {
      executorService.awaitTermination(1, TimeUnit.DAYS);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    if(AUTO_CAPTURE){
      File file = createFilename();
      File dir = new File(file.getParent());
      long size_dir  = folderSize(dir);
      long size_file = size_dir/frame_count;
      
      String size_dir_str  = NumberFormat.getNumberInstance(Locale.US).format(size_dir  >> 10);
      String size_file_str = NumberFormat.getNumberInstance(Locale.US).format(size_file >> 10);
      System.out.printf(">> Captured %d frames (%s kb)  > dir: \"%s\" (%s kb)\n", frame_count, size_file_str, dir, size_dir_str);
    }
  }
  

  
  
  public static long folderSize(File directory) {
    long length = 0;
    for (File file : directory.listFiles()) {
      if (file.isFile())
        length += file.length();
      else
        length += folderSize(file);
    }
    return length;
}
  
  
  
  
  
  
  
  
  public void capture(){
    File          file  = createFilename();
    BufferedImage image = createBufferedImage();
    Frame         frame = new Frame(image, file, jpeg_compression);
    
    executorService.submit(frame);
//    new Thread(frame).start();
    
    freeMemory     = Runtime.getRuntime().freeMemory ();
    maxMemory      = Runtime.getRuntime().maxMemory  ();
    totalMemory    = Runtime.getRuntime().totalMemory();
    
    usedMemory =  totalMemory;
  }
  
  
  public BufferedImage createBufferedImage(){
    PGraphics pg = papplet.g;
  //  BufferedImage img_native = (BufferedImage) papplet.getGraphics().getNative();
  //  System.out.println(pg.format == PConstants.ARGB);
  //  System.out.println( img_native.getType() == BufferedImage.TYPE_INT_ARGB);
    pg.loadPixels();   
    BufferedImage img = new BufferedImage(pg.width, pg.height, BufferedImage.TYPE_INT_RGB);
    WritableRaster wr = img.getRaster();
    wr.setDataElements(0, 0, pg.width, pg.height, pg.pixels);
    return img;
  }
  

  public File createFilename(){
    Class<?> this_ = papplet.getClass();
    File dir_cur = new File(root_dir+this_.getCanonicalName().replaceAll("[.]", "/")+"/");
    File dir_new = new File(dir_cur.getParent()+"/out/"+this_.getSimpleName()+"_"+start_ms+"/");
    if(!dir_new.exists()) {
      dir_new.mkdirs();
    }
    String filename = String.format("%s_%07d%s" , this_.getSimpleName(), frame_count++, ".jpg");
    File file = new File(dir_new, filename);
    return file;
  }


  static class Frame implements Runnable {
    
    float jpeg_compression = 0.9f;
    BufferedImage img;
    File filename;
    
    public Frame(BufferedImage img, File filename, float jpeg_compression){
      this.img = img ;
      this.filename = filename;
      this.jpeg_compression = jpeg_compression;
    }

    public void run() {
      save();
    }
    
    public void save(){
      try {
        
        BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(filename));
        
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        ImageWriteParam param = writer.getDefaultWriteParam();

        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(jpeg_compression);
        
        writer.setOutput(ImageIO.createImageOutputStream(output));
        writer.write(null, new IIOImage(img, null, null), param);
        writer.dispose();
        output.flush();
        output.close();
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      
//      try {
//        BufferedOutputStream imageOutputStream = new BufferedOutputStream(new FileOutputStream(filename));
//        ImageIO.write(img, "JPG", imageOutputStream);
//        imageOutputStream.close();
//      } catch (IOException e) {
//        e.printStackTrace();
//      }
    }
    

  }
  
  
}
