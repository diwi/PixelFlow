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
  public T pop(int idx){
    T item = stack[idx];
    stack[idx] = stack[--ptr];
    return item;
  }
  
  public int size(){
    return ptr;
  }
  
  public boolean isEmpty(){
    return ptr == 0;
  }
  
  public T[] copyToArray(T[] array){
    if(array == null){
      return null;
    }
    System.arraycopy(stack, 0, array, 0, Math.min(array.length, size()));
    return array;
  }
  
}