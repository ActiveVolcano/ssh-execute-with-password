package cn.nhcqc.sshextest;

import java.io.*;
import org.junit.jupiter.api.*;

import cn.nhcqc.sshex.SSHExecute;

public class SSHExecuteTest {

	//------------------------------------------------------------------------
	private static PrintStream      OUT         = System.out;

	//------------------------------------------------------------------------
	@Test
	public void testRun () {
		var config      = new SSHExecute.Config ();
		config.host     = "192.168.0.1";
		config.port     = 22;
		config.username = "alibaba";
		config.password = "OpenSesame";
		config.command  = "ls -l --color /tmp";
		config.timeoutConnect = 10;
		config.timeoutCommand = 10;
		try {
			int exit    = new SSHExecute ().run (config);
			OUT.printf ("%n(exit status = %d)%n", exit);
		} catch (IOException e) {
			OUT.println (e.getMessage ());
		}
	}

}
