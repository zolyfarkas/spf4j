///*
// * Copyright (c) 2001-2017, Zoltan Farkas All Rights Reserved.
// *
// * This library is free software; you can redistribute it and/or
// * modify it under the terms of the GNU Lesser General Public
// * License as published by the Free Software Foundation; either
// * version 2.1 of the License, or (at your option) any later version.
// *
// * This library is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// * GNU General Public License for more details.
// *
// * You should have received a copy of the GNU Lesser General Public
// * License along with this program; if not, write to the Free Software
// * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
// *
// * Additionally licensed with:
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *      http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//package org.spf4j.concurrent;
//
//import java.util.ArrayList;
//import java.util.Collection;
//import java.util.Iterator;
//import java.util.List;
//import java.util.Map;
//import java.util.concurrent.BlockingQueue;
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.concurrent.TimeUnit;
//
///**
// *
// * @author Zoltan Farkas
// */
//public class ThreadLocalBlockingQueue<T> implements BlockingQueue<T> {
//
//  private final ThreadLocal<List<T>> localBuffer;
//
//  private final Map<Thread, List<T>> buffers;
//
//  private final int localBufferSize;
//
//
//  private final Object sync = new Object();
//
//  public ThreadLocalBlockingQueue(final int localBufferSize) {
//    this.localBufferSize = localBufferSize;
//    buffers = new ConcurrentHashMap<>();
//    localBuffer = new ThreadLocal<List<T>>() {
//      @Override
//      protected List<T> initialValue() {
//        List<T> result = new ArrayList<>(localBufferSize);
//        buffers.put(Thread.currentThread(), result);
//        return result;
//      }
//    };
//  }
//
//  @Override
//  public boolean add(final T e) {
//    if (!offer(e)) {
//      throw new IllegalStateException("Cannot add " + e);
//    }
//    return true;
//  }
//
//  @Override
//  public boolean offer(T e) {
//    List<T> b = localBuffer.get();
//    int size;
//    synchronized (b) {
//      size = b.size();
//      if (size >= localBufferSize) {
//        return false;
//      }
//      b.add(e);
//    }
//    if (size + 1 >= localBufferSize) {
//      synchronized (sync) {
//        sync.notifyAll();
//      }
//    }
//    return true;
//  }
//
//  @Override
//  public void put(T e) throws InterruptedException {
//    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
//  }
//
//  @Override
//  public boolean offer(T e, long timeout, TimeUnit unit) throws InterruptedException {
//    List<T> b = localBuffer.get();
//    int size;
//    synchronized (b) {
//      while ((size = b.size()) >= localBufferSize) {
//        b.wait();
//      }
//      b.add(e);
//    }
//  }
//
//  @Override
//  public T take() throws InterruptedException {
//    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
//  }
//
//  @Override
//  public T poll(long timeout, TimeUnit unit) throws InterruptedException {
//    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
//  }
//
//  @Override
//  public int remainingCapacity() {
//    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
//  }
//
//  @Override
//  public boolean remove(Object o) {
//    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
//  }
//
//  @Override
//  public boolean contains(Object o) {
//    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
//  }
//
//  @Override
//  public int drainTo(Collection<? super T> c) {
//    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
//  }
//
//  @Override
//  public int drainTo(Collection<? super T> c, int maxElements) {
//    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
//  }
//
//  @Override
//  public T remove() {
//    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
//  }
//
//  @Override
//  public T poll() {
//    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
//  }
//
//  @Override
//  public T peek() {
//    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
//  }
//
//  @Override
//  public int size() {
//    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
//  }
//
//  @Override
//  public boolean isEmpty() {
//    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
//  }
//
//  @Override
//  public Iterator<T> iterator() {
//    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
//  }
//
//  @Override
//  public Object[] toArray() {
//    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
//  }
//
//  @Override
//  public <T> T[] toArray(T[] a) {
//    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
//  }
//
//  @Override
//  public boolean containsAll(Collection<?> c) {
//    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
//  }
//
//  @Override
//  public boolean addAll(Collection<? extends T> c) {
//    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
//  }
//
//  @Override
//  public boolean removeAll(Collection<?> c) {
//    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
//  }
//
//  @Override
//  public boolean retainAll(Collection<?> c) {
//    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
//  }
//
//  @Override
//  public void clear() {
//    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
//  }
//
//  @Override
//  public T element() {
//    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
//  }
//
//}
