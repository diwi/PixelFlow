package com.thomasdiewald.pixelflow.java.accelerationstructures;

import java.util.Arrays;


// just my very simple implementation of a stack
public class DwStack<T>{
  
  private T[] stack;
  private int ptr = 0;
  private int size = 10;
  
  @SuppressWarnings("unchecked")
  public DwStack(){
    stack = (T[]) new Object[size];
  }
  
  public void push(T object){
    if(ptr >= size){
      size  = (int) Math.ceil(size * 1.3333f);
      stack = Arrays.copyOf(stack, size);
    }
    stack[ptr++] = object;
  }
  
  public T pop(){
    return stack[--ptr];
  }
  
  public int size(){
    return ptr;
  }
  
  public boolean isEmpty(){
    return ptr == 0;
  }
}