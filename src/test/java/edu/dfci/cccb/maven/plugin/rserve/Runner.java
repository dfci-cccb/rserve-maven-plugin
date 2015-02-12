/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301  USA
 */

package edu.dfci.cccb.maven.plugin.rserve;

import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RSession;
import org.rosuda.REngine.Rserve.RserveException;

public class Runner {

  private RSession session;

  public Runner (String host, int port) throws RserveException {
    session = new RConnection (host, port).detach ();
  }

  public Runner (String host) throws RserveException {
    this (host, 6311);
  }

  public Runner (int port) throws RserveException {
    this ("localhost", port);
  }

  public Runner () throws RserveException {
    this (6311);
  }

  public REXP eval (String cmd) throws RserveException, REXPMismatchException {
    RConnection connection = session.attach ();
    try {
      return connection.eval (cmd);
    } finally {
      session = connection.detach ();
    }
  }

  public static void main (String[] args) throws Exception {
    Runner r = new Runner ();
    try {
      dump ("", (REXP) r.eval ("h"));
    } catch (Exception e) {}
    r.eval ("h<- 7");
    System.out.println (r.eval ("h").asDouble ());

    REXP e = r.eval
              ("expression (VARIABLE <- try (binder (callback = function (binder) { "
               +
               "define ('PARAMETER', function () VALUE, binder); define ('PARAMETER', "
               + "function () VALUE, binder); inject (FUNCTION, binder); })))");

    dump ("", e);
  }

  private static void dump (String prefix, REXP e) throws REXPMismatchException {
    if (e.isList ()) {
      System.out.println (prefix + e.getClass ());
      for (Object o : e.asList ())
        dump (prefix + "  ", (REXP) o);
    } else if (e.isSymbol ())
      System.out.println (prefix + e.getClass () + ":" + e.asString () + ":" + e.toDebugString ());
    else if (e.isString ())
      System.out.println (prefix + e.getClass () + ":" + e.asString ());
    else
      System.out.println (prefix + e.getClass () + "*****");
  }
}
