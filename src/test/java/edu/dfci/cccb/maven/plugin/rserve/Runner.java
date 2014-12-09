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

import java.util.Arrays;

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

  @SuppressWarnings ("unchecked")
  public <T> T eval (String cmd) throws RserveException, REXPMismatchException {
    RConnection connection = session.attach ();
    try {
      return (T) connection.eval (cmd).asNativeJavaObject ();
    } finally {
      session = connection.detach ();
    }
  }

  public static void main (String[] args) throws Exception {
    Runner r = new Runner ();
    System.out.println (Arrays.toString ((String[]) r.eval (".libPaths()")));
    System.out.println (Arrays.toString ((String[]) r.eval (".basedir")));
    System.out.println (Arrays.toString ((double[]) r.eval ("x")));
  }
}
