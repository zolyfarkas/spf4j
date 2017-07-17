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

/**
 * @author zoly
 */
public final class UnixConstants {
  public static final int O_RDONLY = 0;
  public static final int O_WRONLY = 1;
  public static final int O_RDWR = 2;
  public static final int O_APPEND = 8;
  public static final int O_CREAT = 512;
  public static final int O_EXCL = 2048;
  public static final int O_TRUNC = 1024;
  public static final int O_SYNC = 128;
  public static final int O_DSYNC = 4194304;
  public static final int O_NOFOLLOW = 256;
  public static final int S_IAMB = 511;
  public static final int S_IRUSR = 256;
  public static final int S_IWUSR = 128;
  public static final int S_IXUSR = 64;
  public static final int S_IRGRP = 32;
  public static final int S_IWGRP = 16;
  public static final int S_IXGRP = 8;
  public static final int S_IROTH = 4;
  public static final int S_IWOTH = 2;
  public static final int S_IXOTH = 1;
  public static final int S_IFMT = 61440;
  public static final int S_IFREG = 32768;
  public static final int S_IFDIR = 16384;
  public static final int S_IFLNK = 40960;
  public static final int S_IFCHR = 8192;
  public static final int S_IFBLK = 24576;
  public static final int S_IFIFO = 4096;
  public static final int R_OK = 4;
  public static final int W_OK = 2;
  public static final int X_OK = 1;
  public static final int F_OK = 0;
  public static final int ENOENT = 2;
  public static final int EACCES = 13;
  public static final int EEXIST = 17;
  public static final int ENOTDIR = 20;
  public static final int EINVAL = 22;
  public static final int EXDEV = 18;
  public static final int EISDIR = 21;
  public static final int ENOTEMPTY = 66;
  public static final int ENOSPC = 28;
  public static final int EAGAIN = 35;
  public static final int ENOSYS = 78;
  public static final int ELOOP = 62;
  public static final int EROFS = 30;
  public static final int ENODATA = 96;
  public static final int ERANGE = 34;
  public static final int AT_SYMLINK_NOFOLLOW = 0;
  public static final int AT_REMOVEDIR = 0;

  private UnixConstants() { }
}
