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

import static java.lang.System.err;
import static java.lang.System.getenv;
import static java.lang.System.out;
import static java.nio.channels.Channels.newChannel;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URL;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;

public abstract class AbstractRProcessRserveMojo extends AbstractRserveMojo {

  public static final String R = "R";
  public static final String R_HOME = "R_HOME";

  private @Parameter (property = "R") File r;
  private @Parameter String onRequest;
  private @Parameter String onInitialize;

  private final boolean debug;
  private @Parameter boolean inheritIO;

  private @Parameter (required = true, defaultValue = "${project.build.directory}/rserve") File rserveInstallDirectory;
  private @Parameter (required = true, defaultValue = "http://rforge.net/src/contrib/Rserve_1.8-1.tar.gz") URL rserveSource;

  protected AbstractRProcessRserveMojo (boolean debug) {
    this.debug = inheritIO = debug;
  }

  protected class R {
    protected String rBinary () {
      if (r == null) {
        try {
          String r;
          if ((r = getenv (R)) != null)
            return r;
          else if ((r = getenv (R_HOME)) != null)
            return new File (new File (r, "bin"), "R").getAbsolutePath ();
        } catch (SecurityException e) {
          getLog ().warn ("Unable to infer R binary location from system environment due to security settings");
        }
        return "R";
      } else
        return AbstractRProcessRserveMojo.this.r.getAbsolutePath ();
    }

    protected ProcessBuilder rBuilder () {
      return new ProcessBuilder (rBinary (), "--no-save");
    }

    protected Process r () throws IOException {
      return rBuilder ().start ();
    }

    protected Process r;
    protected PrintStream feed;

    public R () throws IOException {
      this.r = r ();
      this.feed = new PrintStream (new BufferedOutputStream (r.getOutputStream ()));
      if (inheritIO) {
        class Sink extends Thread {
          private final InputStream source;
          private final OutputStream target;

          public Sink (InputStream source, OutputStream target) {
            setDaemon (true);
            this.source = source;
            this.target = target;
            new Thread () {
              {
                setDaemon (true);
              }

              public void run () {
                for (;;)
                  try {
                    sleep (3000);
                  } catch (InterruptedException e) {
                    break;
                  } finally {
                    try {
                      Sink.this.target.flush ();
                    } catch (IOException e) {}
                  }
              }
            }.start ();
          }

          @Override
          public void run () {
            for (;;)
              try {
                target.write (source.read ());
              } catch (IOException e) {
                try {
                  target.flush ();
                  break;
                } catch (IOException e1) {}
              }
          }
        };

        new Sink (r.getInputStream (), new BufferedOutputStream (out)).start ();
        new Sink (r.getErrorStream (), new BufferedOutputStream (err)).start ();
      }
    }

    public void evaluate (String command) {
      feed.println (command);
      feed.flush ();
    }

    public void join () throws MojoExecutionException, InterruptedException {
      for (;;)
        if (r.waitFor () != 0) {
          throw new MojoExecutionException ("Non zero return from R process");
        } else
          break;
    }
  }

  protected void install (R r) throws IOException {
    File source = new File (rserveInstallDirectory, "Rserve.tar.gz");
    if (!source.exists ()) {
      rserveInstallDirectory.mkdirs ();
      ReadableByteChannel rbc = newChannel (rserveSource.openStream ());
      try (FileOutputStream fos = new FileOutputStream (source)) {
        fos.getChannel ().transferFrom (rbc, 0, Long.MAX_VALUE);
      }
    }
    r.evaluate (".libPaths ('" + rserveInstallDirectory.getAbsolutePath () + "');");
    if (!new File (rserveInstallDirectory, "Rserve").exists ())
      r.evaluate ("install.packages ('" + source.getAbsolutePath () + "', type = 'source', repo = NULL);");
  }

  protected void prepare (R r) {
    if (onInitialize != null)
      r.evaluate (onInitialize);
  }

  protected List<String> rserveArgsArgs () {
    List<String> argsArgs = new ArrayList<> ();
    argsArgs.add ("--no-save");
    if (configure () != null)
      for (Entry<String, String> entry : configure ().entrySet ()) {
        argsArgs.add ("--RS-set");
        argsArgs.add (entry.getKey ().replaceAll ("'", "\'") + "=" + entry.getValue ().replaceAll ("'", "\'"));
      }
    if (onRequest != null) {
      argsArgs.add ("--RS-set");
      argsArgs.add ("eval=" + onRequest);
    }
    return argsArgs;
  }

  protected Map<String, String> rserveArgs () {
    Map<String, String> args = new HashMap<> ();
    args.put ("debug", debug ? "TRUE" : "FALSE");
    StringBuilder argsArgs = new StringBuilder ();
    for (String argArg : rserveArgsArgs ())
      argsArgs.append (", '" + argArg + "'");
    args.put ("args", "c (" + argsArgs.substring (2) + ")");
    return args;
  }

  protected void rserve (R r) {
    StringBuilder args = new StringBuilder ();
    for (Entry<String, String> arg : rserveArgs ().entrySet ())
      args.append (", " + arg.getKey () + " = " + arg.getValue ());
    r.evaluate ("Rserve::Rserve (" + args.substring (2) + ");");
  }

  @Override
  public void execute () throws MojoExecutionException, MojoFailureException {
    try {
      R r = new R ();
      install (r);
      prepare (r);
      rserve (r);
      getLog ().info ("Started Rserve on port " + port ());
      try {
        r.join ();
      } catch (InterruptedException e) {
        try {
          r.evaluate ("q ();");
        } catch (Throwable e2) {}
      }
    } catch (IOException e) {
      throw new MojoExecutionException ("Unable to start Rserve", e);
    }
  }
}
