/*
 * Copyright (c) 2001, Zoltan Farkas All Rights Reserved.
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
 * IMPORTANT: This file is also Licensed with the BDS license.
 */
package org.spf4j.base;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import javax.annotation.Nonnull;

/**
 * some "standard" process exit codes from:
 * http://tldp.org/LDP/abs/html/index.html
 * https://www.freebsd.org/cgi/man.cgi?query=sysexits&apropos=0&sektion=0&manpath=FreeBSD+4.3-RELEASE&format=html
 * http://journal.thobe.org/2013/02/jvms-and-kill-signals.html
 * @author zoly
 */
public enum SysExits {

  /**
   * Everything is OK.
   */
  OK(0),

  /**
   * Catch all for general errors.
   */
  EX_GENERAL(1),

  /**
   * Shell build in miss-use.
   */
  EX_SHELL_BUILTIN_MISSUSE(2),

  /**
   * The command was used incorrectly, e.g., with the wrong number of arguments, a bad flag, a bad syntax in a
   * parameter, or whatever.
   */
  EX_USAGE(64),
  /**
   * The input data was incorrect in some way. This should only be used for user's data and not system files.
   */
  EX_DATAERR(65),
  /**
   * An input file (not a system file) did not exist or was not readable. This could also include errors like ``No
   * message'' to a mailer (if it cared to catch it).
   */
  EX_NOINPUT(66),
  /**
   * The user specified did not exist. This might be used for mail addresses or remote logins.
   */
  EX_NOUSER(67),
  /**
   * The host specified did not exist. This is used in mail addresses or network requests.
   */
  EX_NOHOST(68),
  /**
   * A service is unavailable. This can occur if a support program or file does not exist. This can also be used as a
   * catchall message when something you wanted to do doesn't work, but you don't know why.
   */
  EX_UNAVAILABLE(69),
  /**
   * An internal software error has been detected. This should be limited to non-operating system related errors as
   * possible.
   */
  EX_SOFTWARE(70),
  /**
   * An operating system error has been detected. This is intended to be used for such things as ``cannot fork'',
   * ``cannot create pipe'', or the like. It includes things like getuid returning a user that does not exist in the
   * passwd file.
   */
  EX_OSERR(71),
  /**
   * Some system file (e.g., /etc/passwd, /var/run/utmp,etc.) does not exist, cannot be opened, or has some sort of
   * error (e.g., syntax error).
   */
  EX_OSFILE(72),
  /**
   * A (user specified) output file cannot be created.
   */
  EX_CANTCREAT(73),
  /**
   * An error occurred while doing I/O on some file.
   */
  EX_IOERR(74),
  /**
   * Temporary failure, indicating something that is not really an error. In sendmail, this means that a mailer (e.g.)
   * could not create a connection, and the request should be reattempted later.
   */
  EX_TEMPFAIL(75),
  /**
   * The remote system returned something that was ``not possible'' during a protocol exchange.
   */
  EX_PROTOCOL(76),
  /**
   * You did not have sufficient permission to perform the operation. This is not intended for file system problems,
   * which should use EX_NOINPUT or EX_CANTCREAT, but rather for higher level permissions.
   */
  EX_NOPERM(77),
  /**
   * Something was found in an unconfigured or misconfigured state.
   */
  EX_CONFIG(78),


  /**
   * cannot execute invoked command.
   */
  EX_CANNOT_EXEC_CMD(126),

  /**
   * Command not found.
   */
  EX_CMD_NOT_FOUND(127),

  /**
   * Invalid argument to exit.
   */
  EX_INVALID_ARG_TO_EXIT(128),

  /**
   * Section caused by exit due to signal.
   * where signal name is same on Linux, Solaris and MacOS enum has the appropriate name.
   * for linux see: http://man7.org/linux/man-pages/man7/signal.7.html
   * or run man signal on you OS of choice.
   */
  EX_SIG_HUP(129),
  EX_SIG_INT(130),
  EX_SIG_QUIT(131),
  EX_SIG_ILL(132),
  EX_SIG_TRAP(133),
  EX_SIG_ABRT(134),
  EX_SIG_7(135),
  EX_SIG_FPE(136),
  EX_SIG_KILL(137),
  EX_SIG_10(138),
  EX_SIG_11(139),
  EX_SIG_12(140),
  EX_SIG_PIPE(141),
  EX_SIG_ALRM(142),
  EX_SIG_TERM(143),
  EX_SIG_16(144),
  EX_SIG_17(145),
  EX_SIG_18(146),
  EX_SIG_19(147),
  EX_SIG_20(148),
  EX_SIG_21(149),
  EX_SIG_22(150),
  EX_SIG_23(151),
  EX_SIG_24(152),
  EX_SIG_25(153),
  EX_SIG_26(154),
  EX_SIG_27(155),
  EX_SIG_28(156),
  EX_SIG_29(157),
  EX_SIG_30(158),
  EX_SIG_31(159),

  EX_STATUS_OUT_OF_RANGE(255),
  /**
   * Any return codes not explicitly defined will be associated with "EX_UNKNOWN"
   */
  EX_UNKNOWN(-1);

  SysExits(final int code) {
    this.exitCode = code;
  }

  final int exitCode;

  public int exitCode() {
    return exitCode;
  }

  public boolean isOk() {
    return exitCode == 0;
  }

  public boolean isError() {
    return exitCode != 0;
  }

  private static final TIntObjectMap<SysExits> CODE2ENUM;

  static {
    SysExits[] values = SysExits.values();
    TIntObjectMap<SysExits> c2e = new TIntObjectHashMap<>(values.length);
    for (SysExits e : values) {
      if (c2e.put(e.exitCode(), e) != null) {
        throw new ExceptionInInitializerError("Duplicate exit code " + e);
      }
    }
    CODE2ENUM = c2e;
  }


  /**
   * @param exitCode
   * @return corresponding enum.
   */
  @Nonnull
  public static SysExits fromCode(final int exitCode) {
    SysExits result =  CODE2ENUM.get(exitCode);
    if (result == null) {
      return SysExits.EX_UNKNOWN;
    } else {
      return result;
    }
  }

}
