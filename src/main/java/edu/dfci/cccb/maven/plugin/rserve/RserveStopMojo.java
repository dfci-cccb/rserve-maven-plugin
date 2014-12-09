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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

@Mojo (name = "stop")
public class RserveStopMojo extends AbstractRserveMojo {

  private @Parameter (required = true, defaultValue = "localhost") String host;

  /* (non-Javadoc)
   * @see org.apache.maven.plugin.Mojo#execute() */
  @Override
  public void execute () throws MojoExecutionException, MojoFailureException {
    try {
      new RConnection (host, port ()).shutdown ();
      getLog ().info ("Shutdown Rserve on " + ("localhost".equals (host) ? "" : (host + ":")) + port ());
    } catch (RserveException e) {
      throw new MojoExecutionException ("Unable to shutdown Rserve", e);
    }
  }
}
