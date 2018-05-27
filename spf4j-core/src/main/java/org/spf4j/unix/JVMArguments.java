/*
 * Copyright (c) 2001-2017, Zoltan Farkas All Rights Reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * Additionally licensed with:
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.spf4j.unix;

import com.google.common.io.Files;
import com.sun.jna.StringArray;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import static com.sun.jna.Pointer.NULL;
import com.sun.jna.ptr.IntByReference;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.io.IOException;
import java.io.File;
import java.io.ByteArrayOutputStream;
import java.io.RandomAccessFile;
import java.io.DataInputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.function.Function;
import javax.annotation.Nullable;
import org.spf4j.base.Runtime;
import org.spf4j.unix.CLibrary.FILE;

/**
 * List of arguments for Java VM and application. based on class from akuma lib (http://akuma.kohsuke.org).
 */
@SuppressFBWarnings("DM_DEFAULT_ENCODING")
// default char encoding is used on purpose.
public final class JVMArguments {

  private final List<String> arguments;

  public JVMArguments(final int size) {
    arguments = new ArrayList<>(size);
  }

  public JVMArguments(final Collection<? extends String> c) {
    arguments = new ArrayList<>(c);
  }

  public String getExecutable() {
    return arguments.get(0);
  }

  /**
   * Removes the first System property.
   * @param pname the name of the system property to remove.
   * @return the value of the removed system property. or null if there is no such property.
   */
  @Nullable
  public String removeSystemProperty(final String pname) {
    Iterator<String> itr = arguments.iterator();
    itr.next(); // skip command.
    int nl = pname.length();
    while (itr.hasNext()) {
      String s = itr.next();
      if (s.startsWith("-D") && s.regionMatches(2, pname, 0, nl)) {
        int l = nl + 2;
        if (s.length() == l) {
          itr.remove();
          return "";
        } else if (s.charAt(l) == '=') {
          itr.remove();
          return s.substring(l + 1);
        }
      }
    }
    return null;
  }

  @Nullable
  public String getSystemProperty(final String pname) {
    Iterator<String> itr = arguments.iterator();
    itr.next(); // skip command.
    int nl = pname.length();
    while (itr.hasNext()) {
      String s = itr.next();
      if (s.startsWith("-D") && s.regionMatches(2, pname, 0, nl)) {
        int l = nl + 2;
        if (s.length() == l) {
          return "";
        } else if (s.charAt(l) == '=') {
          return s.substring(l + 1);
        }
      }
    }
    return null;
  }

  @Nullable
  public void createOrUpdateSystemProperty(final String pname, final Function<String, String> replacer) {
    ListIterator<String> itr = arguments.listIterator();
    itr.next(); // skip command.
    int nl = pname.length();
    while (itr.hasNext()) {
      String s = itr.next();
      if (s.startsWith("-D") && s.regionMatches(2, pname, 0, nl)) {
        int l = nl + 2;
        if (s.length() == l) {
          itr.set("-D" + pname + '=' + replacer.apply(""));
          return;
        } else if (s.charAt(l) == '=') {
          itr.set("-D" + pname + '=' + replacer.apply(s.substring(l + 1)));
          return;
        }
      }
    }
    arguments.add(1, "-D" + pname + '=' + replacer.apply(null));
  }


  public boolean hasSystemProperty(final String pname) {
    Iterator<String> itr = arguments.iterator();
    itr.next(); // skip command.
    int nl = pname.length();
    while (itr.hasNext()) {
      String s = itr.next();
      if (s.startsWith("-D") && s.regionMatches(2, pname, 0, nl)) {
        int l = nl + 2;
        if (s.length() == l) {
          return true;
        } else if (s.charAt(l) == '=') {
          return true;
        }
      }
    }
    return false;
  }
  /**
   * remove all system properties starting with a prefix.
   * @param pname the prefix
   * @return number of system properties removed.
   */
  public int removeAllSystemPropertiesStartingWith(final String pname) {
    String name = "-D" + pname;
    int nrRemoved = 0;
    Iterator<String> itr = arguments.iterator();
    itr.next();
    while (itr.hasNext()) {
      String s = itr.next();
      if (s.startsWith(name)) {
        itr.remove();
        nrRemoved++;
      }
    }
    return nrRemoved;
  }

  public void setSystemProperty(final String name, final String value) {
    removeSystemProperty(name);
    // index 0 is the executable name
    arguments.add(1, "-D" + name + '=' + value);
  }

  public void setVMArgument(final String argument) {
    if (!hasVMArgument(argument)) {
      arguments.add(1, argument);
    }
  }

  public boolean removeVMArgument(final String argument) {
    Iterator<String> itr = arguments.iterator();
    itr.next();
    while (itr.hasNext()) {
      String s = itr.next();
      if (s.equals(argument)) {
        itr.remove();
        return true;
      }
    }
    return false;
  }

  public boolean hasVMArgument(final String argument) {
    Iterator<String> itr = arguments.iterator();
    itr.next();
    while (itr.hasNext()) {
      String s = itr.next();
      if (s.equals(argument)) {
        return true;
      }
    }
    return false;
  }

  public boolean hasVMArgumentStartingWith(final String argumentPrefix) {
    Iterator<String> itr = arguments.iterator();
    itr.next();
    while (itr.hasNext()) {
      String s = itr.next();
      if (s.startsWith(argumentPrefix)) {
        return true;
      }
    }
    return false;
  }


  public void add(final String arg) {
    arguments.add(arg);
  }

  /**
   * Removes the n items from the end. Useful for removing all the Java arguments to rebuild them.
   */
  public void removeTail(final int n) {
    int size = arguments.size();
    arguments.removeAll(arguments.subList(size - n, size));
  }

  public StringArray toStringArray() {
    return new StringArray(arguments.toArray(new String[arguments.size()]));
  }

  /**
   * Gets the process argument list of the current process.
   */
  public static JVMArguments current() throws IOException {
    return of(-1);
  }

  /**
   * Gets the process argument list of the specified process ID.
   *
   * @param pid -1 to indicate the current process.
   */
  public static JVMArguments of(final int pid) throws IOException {
    String os = Runtime.OS_NAME;
    switch (os) {
      case "Linux":
        return ofLinux(pid);
      case "SunOS":
        return ofSolaris(pid);
      case "Mac OS X":
        return ofMac(pid);
      case "FreeBSD":
        return ofFreeBSD(pid);
      default:
        throw new UnsupportedOperationException("Unsupported Operating System " + os);
    }
  }

  private static JVMArguments ofLinux(final int ppid) throws IOException {
    int pid = resolvePID(ppid);
    String cmdline = Files.asCharSource(new File("/proc/" + pid + "/cmdline"), Charset.defaultCharset()).read();
    return new JVMArguments(java.util.Arrays.asList(cmdline.split("\0")));
  }

  private static int resolvePID(final int pid) {
    if (pid == -1) {
      return Runtime.PID;
    } else {
      return pid;
    }
  }

  private static JVMArguments ofSolaris(final int ppid) throws IOException {
    // /proc shows different contents based on the caller's memory model, so we need to know if we are 32 or 64.
    // 32 JVMs are the norm, so err on the 32bit side.
    boolean areWe64 = "64".equals(System.getProperty("sun.arch.data.model"));
    int pid = resolvePID(ppid);
    try (RandomAccessFile psinfo = new RandomAccessFile(new File("/proc/" + pid + "/psinfo"), "r")) {
      // see http://cvs.opensolaris.org/source/xref/onnv/onnv-gate/usr/src/uts/common/sys/procfs.h
      //typedef struct psinfo {
      // int pr_flag; /* process flags */
      // int pr_nlwp; /* number of lwps in the process */
      // pid_t pr_pid; /* process id */
      // pid_t pr_ppid; /* process id of parent */
      // pid_t pr_pgid; /* process id of process group leader */
      // pid_t pr_sid; /* session id */
      // uid_t pr_uid; /* real user id */
      // uid_t pr_euid; /* effective user id */
      // gid_t pr_gid; /* real group id */
      // gid_t pr_egid; /* effective group id */
      // uintptr_t pr_addr; /* address of process */
      // size_t pr_size; /* size of process image in Kbytes */
      // size_t pr_rssize; /* resident set size in Kbytes */
      // dev_t pr_ttydev; /* controlling tty device (or PRNODEV) */
      // ushort_t pr_pctcpu; /* % of recent cpu time used by all lwps */
      // ushort_t pr_pctmem; /* % of system memory used by process */
      // timestruc_t pr_start; /* process start time, from the epoch */
      // timestruc_t pr_time; /* cpu time for this process */
      // timestruc_t pr_ctime; /* cpu time for reaped children */
      // char pr_fname[PRFNSZ]; /* name of exec'ed file */
      // char pr_psargs[PRARGSZ]; /* initial characters of arg list */
      // int pr_wstat; /* if zombie, the wait() status */
      // int pr_argc; /* initial argument count */
      // uintptr_t pr_argv; /* address of initial argument vector */
      // uintptr_t pr_envp; /* address of initial environment vector */
      // char pr_dmodel; /* data model of the process */
      // lwpsinfo_t pr_lwp; /* information for representative lwp */
      //} psinfo_t;

      // see http://cvs.opensolaris.org/source/xref/onnv/onnv-gate/usr/src/uts/common/sys/types.h
      // for the size of the various datatype.
      // see http://cvs.opensolaris.org/source/xref/onnv/onnv-gate/usr/src/cmd/ptools/pargs/pargs.c
      // for how to read this information
      psinfo.seek(8);
      if (adjust(psinfo.readInt()) != pid) {
        throw new IOException("psinfo PID mismatch: " + pid + ", " + psinfo);   // sanity check
      }
      /* The following program computes the offset:
                    #include <stdio.h>
                    #include <sys/procfs.h>
                    int main() {
                      printf("psinfo_t = %d\n", sizeof(psinfo_t));
                      psinfo_t *x;
                      x = 0;
                      printf("%x\n", &(x->pr_argc));
                    }
       */

      psinfo.seek(areWe64 ? 0xEC : 0xBC);  // now jump to pr_argc
      int argc = adjust(psinfo.readInt());
      long argp = areWe64 ? adjust(psinfo.readLong()) : to64(adjust(psinfo.readInt()));
      File asFile = new File("/proc/" + pid + "/as");
      if (areWe64) {
        // 32bit and 64bit basically does the same thing, but because the stream position
        // is computed with signed long, doing 64bit seek to a position bigger than Long.MAX_VALUE
        // requres some real hacking. Hence two different code path.
        // (RandomAccessFile uses Java long for offset, so it just can't get to anywhere beyond Long.MAX_VALUE)
        FILE fp = CLibrary.INSTANCE.fopen(asFile.getPath(), "r");
        try {
          JVMArguments args = new JVMArguments(16);
          Memory m = new Memory(8);
          for (int n = 0; n < argc; n++) {
            // read a pointer to one entry
            seek64(fp, argp + ((long) n) * 8);
            m.setLong(0, 0); // just to make sure failed read won't result in bogus value
            CLibrary.INSTANCE.fread(m, 1, 8, fp);
            long p = m.getLong(0);
            args.add(readLine(fp, p));
          }
          return args;
        } finally {
          CLibrary.INSTANCE.fclose(fp);
        }
      } else {
        try (RandomAccessFile as = new RandomAccessFile(asFile, "r")) {
          JVMArguments args = new JVMArguments(16);
          for (int n = 0; n < argc; n++) {
            // read a pointer to one entry
            as.seek(argp + n * 4);
            int p = adjust(as.readInt());

            args.add(readLine(as, p));
          }
          return args;
        }
      }
    }
  }

  /**
   * Seek to the specified position. This method handles offset bigger than {@link Long#MAX_VALUE} correctly.
   *
   * @param upos This value is interpreted as unsigned 64bit integer (even though it's typed 'long')
   */
  private static void seek64(final FILE fp, final long pupos) {
    long upos = pupos;
    CLibrary.INSTANCE.fseek(fp, 0, 0); // start at the beginning
    while (upos < 0) {
      long chunk = Long.MAX_VALUE;
      upos -= chunk;
      CLibrary.INSTANCE.fseek(fp, chunk, 1);
    }
    CLibrary.INSTANCE.fseek(fp, upos, 1);
  }

  /**
   * {@link DataInputStream} reads a value in big-endian, so convert it to the correct value on little-endian systems.
   */
  private static int adjust(final int i) {
    if (Runtime.IS_LITTLE_ENDIAN) {
      return (i << 24) | ((i << 8) & 0x00FF0000) | ((i >> 8) & 0x0000FF00) | (i >>> 24);
    } else {
      return i;
    }
  }

  private static long adjust(final long i) {
    if (Runtime.IS_LITTLE_ENDIAN) {
      return (i << 56)
              | ((i << 40) & 0x00FF000000000000L)
              | ((i << 24) & 0x0000FF0000000000L)
              | ((i << 8) & 0x000000FF00000000L)
              | ((i >> 8) & 0x00000000FF000000L)
              | ((i >> 24) & 0x0000000000FF0000L)
              | ((i >> 40) & 0x000000000000FF00L)
              | (i >> 56);
    } else {
      return i;
    }
  }

  /**
   * int to long conversion with zero-padding.
   */
  private static long to64(final int i) {
    return i & 0xFFFFFFFFL;
  }

  private static String readLine(final RandomAccessFile as, final int p) throws IOException {
    as.seek(to64(p));
    ByteArrayOutputStream buf = new ByteArrayOutputStream();
    int ch;
    while ((ch = as.read()) > 0) {
      buf.write(ch);
    }
    return buf.toString();
  }

  private static String readLine(final FILE as, final long p) {
    seek64(as, p);
    Memory m = new Memory(1);
    ByteArrayOutputStream buf = new ByteArrayOutputStream();
    while (true) {
      if (CLibrary.INSTANCE.fread(m, 1, 1, as) == 0) {
        break;
      }
      byte b = m.getByte(0);
      if (b == 0) {
        break;
      }
      buf.write(b);
    }
    return buf.toString();
  }

  /**
   * Mac support
   *
   * See http://developer.apple.com/qa/qa2001/qa1123.html http://www.osxfaq.com/man/3/kvm_getprocs.ws
   * http://matburt.net/?p=16 (libkvm is removed from OSX) where is kinfo_proc?
   * http://lists.apple.com/archives/xcode-users/2008/Mar/msg00781.html
   *
   * This code uses sysctl to get the arg/env list:
   * http://www.psychofx.com/psi/trac/browser/psi/trunk/src/arch/macosx/macosx_process.c which came from
   * http://www.opensource.apple.com/darwinsource/10.4.2/top-15/libtop.c
   *
   * sysctl is defined in libc.
   *
   * PS source code for Mac: http://www.opensource.apple.com/darwinsource/10.4.1/adv_cmds-79.1/ps.tproj/
   */
  @SuppressFBWarnings("PRMC_POSSIBLY_REDUNDANT_METHOD_CALLS")
  private static JVMArguments ofMac(final int pid) {
    // local constants
    final int ctlKern = 1;
    final int kernArgMax = 8;
    final int kernProcArgs2 = 49;
    final int sizeOfInt = Native.getNativeSize(int.class);
    IntByReference ibf = new IntByReference();

    IntByReference argmaxRef = new IntByReference(0);
    IntByReference size = new IntByReference(sizeOfInt);

    // for some reason, I was never able to get sysctlbyname work.
//        if(LIBC.sysctlbyname("kern.argmax", argmaxRef.getPointer(), size, NULL, ibf)!=0)
    if (CLibrary.INSTANCE.sysctl(new int[]{ctlKern, kernArgMax}, 2, argmaxRef.getPointer(), size, NULL, ibf) != 0) {
      throw new UnsupportedOperationException("Failed to get kernl.argmax: "
              + CLibrary.INSTANCE.strerror(Native.getLastError()));
    }

    int argmax = argmaxRef.getValue();
    StringArrayMemory m = new StringArrayMemory(argmax, sizeOfInt);
    size.setValue(argmax);
    if (CLibrary.INSTANCE.sysctl(new int[]{ctlKern, kernProcArgs2, resolvePID(pid)}, 3, m, size, NULL, ibf) != 0) {
      throw new UnsupportedOperationException("Failed to obtain ken.procargs2: "
              + CLibrary.INSTANCE.strerror(Native.getLastError()));
    }
    /*
         * Make a sysctl() call to get the raw argument space of the
         * process.  The layout is documented in start.s, which is part
         * of the Csu project.  In summary, it looks like:
         *
         * /---------------\ 0x00000000
         * :               :
         * :               :
         * |---------------|
         * | argc          |
         * |---------------|
         * | arg[0]        |
         * |---------------|
         * :               :
         * :               :
         * |---------------|
         * | arg[argc - 1] |
         * |---------------|
         * | 0             |
         * |---------------|
         * | env[0]        |
         * |---------------|
         * :               :
         * :               :
         * |---------------|
         * | env[n]        |
         * |---------------|
         * | 0             |
         * |---------------| <-- Beginning of data returned by sysctl()
         * | exec_path     |     is here.
         * |:::::::::::::::|
         * |               |
         * | String area.  |
         * |               |
         * |---------------| <-- Top of stack.
         * :               :
         * :               :
         * \---------------/ 0xffffffff
     */
    int nargs = m.readInt();
    JVMArguments args = new JVMArguments(nargs);
    m.readString(); // exec path
    for (int i = 0; i < nargs; i++) {
      m.skip0();
      args.add(m.readString());
    }
    return args;
  }

  @SuppressFBWarnings("EQ_DOESNT_OVERRIDE_EQUALS")
  private static final class StringArrayMemory extends Memory {

    private long offset = 0;
    private final int sizeOfInt;

    StringArrayMemory(final long l, final int sizeOfInt) {
      super(l);
      this.sizeOfInt = sizeOfInt;
    }

    private int readInt() {
      int r = getInt(offset);
      offset += sizeOfInt;
      return r;
    }

    private String readString() {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      byte ch;
      while ((ch = getByte(offset++)) != '\0') {
        baos.write(ch);
      }
      return baos.toString();
    }

    void skip0() {
      // skip trailing '\0's
      while (getByte(offset) == '\0') {
        offset++;
      }
    }
  }

  private static JVMArguments ofFreeBSD(final int pid) {
    // taken from sys/sysctl.h
    final int ctlKern = 1;
    final int kernArgMax = 8;
    final int kernProc = 14;
    final int kernProcArgs = 7;

    IntByReference ibr = new IntByReference();
    IntByReference sysctlArgMax = new IntByReference();
    IntByReference size = new IntByReference();

    size.setValue(4);
    if (CLibrary.INSTANCE.sysctl(new int[]{ctlKern, kernArgMax},
            2, sysctlArgMax.getPointer(), size, NULL, ibr) != 0) {
      throw new UnsupportedOperationException("Failed to sysctl kern.argmax");
    }

    int argmax = sysctlArgMax.getValue();
    Memory m = new Memory(argmax);
    size.setValue(argmax);

    if (CLibrary.INSTANCE.sysctl(new int[]{ctlKern, kernProc, kernProcArgs, resolvePID(pid)},
            4, m, size, NULL, ibr) != 0) {
      throw new UnsupportedOperationException();
    }

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ArrayList<String> lArgs = new ArrayList<String>();
    byte ch;
    int offset = 0;
    while (offset < size.getValue()) {
      while ((ch = m.getByte(offset++)) != '\0') {
        baos.write(ch);
      }
      lArgs.add(baos.toString());
      baos.reset();
    }

    return new JVMArguments(lArgs);
  }

  @Override
  public String toString() {
    return "JVMArguments{" + "arguments=" + arguments + '}';
  }

  @Override
  public int hashCode() {
    return 11 * 7 + Objects.hashCode(this.arguments);
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final JVMArguments other = (JVMArguments) obj;
    return Objects.equals(this.arguments, other.arguments);
  }

  public String[] toArray() {
    return arguments.toArray(new String[arguments.size()]);
  }

}
