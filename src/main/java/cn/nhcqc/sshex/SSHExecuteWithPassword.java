package cn.nhcqc.sshex;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;

import org.apache.commons.cli.*;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.Range;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.IOUtils;
import net.schmizz.sshj.transport.TransportException;
import net.schmizz.sshj.transport.verification.FingerprintVerifier;

/**
 * SSH Execute with Password
 *
 * @author CHEN Qingcan
 */
public class SSHExecuteWithPassword {

	private static class Config {
		String  host;
		Integer port            = PORT_DEFAULT;
		String  username;
		String  password;
		String  command;

		Integer timeoutConnect  = 0;
		Integer timeoutCommand  = 0;
		Charset charset         = CHARSET_DEFAULT;

		@Override
		public String toString () {
			return String.format (
				"-l %s -p %d --password \"%s\" --connect-timeout %d --command-timeout %d --charset %s %s \"%s\"",
				username, port, password, timeoutConnect, timeoutCommand, charset, host, command
			);
		}
	}

	final static int PORT_MIN = 1, PORT_MAX = 0xFFFF, PORT_DEFAULT = 22, MS_SEC = 1000;
	final static Charset CHARSET_DEFAULT = StandardCharsets.UTF_8;
	final static PrintStream STDOUT = System.out, STDERR = System.err;
	final static private Logger logger = LoggerFactory.getLogger(SSHExecuteWithPassword.class);

	//------------------------------------------------------------------------
	public static void main (final String[] args) {
		try { System.exit (run (parseArgs (args))); }
		catch (Exception e) {
			STDERR.println (e.getMessage ());
//			logger.error   ("", e);
			System.exit    (1);
		}
	}

	public static int run (final Config config)
	throws IOException {
		assert config != null;
		logger.trace ("config: {}", config);

		try (SSHClient ssh = connect (config)) {
			logger.trace ("auth: {}", config.username);
			ssh.authPassword (config.username, config.password);
			ssh.setRemoteCharset (config.charset);

			try (var session = ssh.startSession ()) {
				logger.trace ("run: {} timeout: {} sec. charset: {}",
					config.command, config.timeoutCommand, config.charset);
				var cmd = session.exec (config.command);
				STDOUT.print (IOUtils.readFully (cmd.getInputStream ()).toString (config.charset));
				STDERR.print (IOUtils.readFully (cmd.getErrorStream ()).toString (config.charset));

				if (config.timeoutCommand > 0) {
					cmd.join (config.timeoutCommand, TimeUnit.SECONDS);
				} else {
					cmd.join ();
				}
				int exit = cmd.getExitStatus ();
				logger.trace ("exit status: {}", exit);
				return exit;
			}
		}
	}

	//------------------------------------------------------------------------
	private static SSHClient connect (final Config config)
	throws IOException {
		assert config != null;
		var ssh = new SSHClient ();
		String fingerprint = null;

		// TODO find a better way to get fingerprint or bypass the host key verification.
		try {
			logger.trace ("connect: {}:{} timeout: {} sec.", config.host, config.port, config.timeoutConnect);
			setSSHtimeout (ssh, config);
			ssh.loadKnownHosts ();
			ssh.connect (config.host, config.port);
			return ssh;
		} catch (TransportException e) {
			// Could not verify `ssh-ed25519` host key with fingerprint `xx:xx:xx:xx:xx:xx:xx:xx:xx:xx:xx:xx:xx:xx:xx:xx` for `{host}` on port {port}
			logger.trace (e.getMessage ());
			fingerprint = StringUtils.substringBetween (e.getMessage (), "fingerprint `", "`");
			logger.trace ("fingerprint: {}", fingerprint);
			ssh.disconnect ();
		}

		ssh = new SSHClient ();
		ssh.addHostKeyVerifier (FingerprintVerifier.getInstance (fingerprint));
		logger.trace ("connect: {}:{} timeout: {} sec.", config.host, config.port, config.timeoutConnect);
		setSSHtimeout (ssh, config);
		ssh.connect (config.host, config.port);
		return ssh;
	}

	//------------------------------------------------------------------------
	private static SSHClient setSSHtimeout (final SSHClient ssh, final Config config) {
		assert ssh != null && config != null;
		if (config.timeoutConnect > 0) {
			ssh.setConnectTimeout (config.timeoutConnect * MS_SEC);
			ssh.setTimeout (config.timeoutConnect * MS_SEC);
		}
		return ssh;
	}

	//------------------------------------------------------------------------
	private static Config parseArgs (final String[] args) {
		var options = new Options ();
		var optionm = buildOptions (options);
		try {
			CommandLine cmd = new DefaultParser ().parse (options, args);
			Config   parsed = new Config ();
			parseOptions  (cmd, optionm, parsed);
			parseLeftOver (cmd, parsed);
			parseCheck    (parsed);
			return parsed;

		} catch (ParseException e) {
			parseError (options, e);
			return null; // never here
		}
	}

	//------------------------------------------------------------------------
	/**
	 * @param options [out]
	 */
	private static Map<String, Option> buildOptions (final Options options) {
		var map = new HashMap<String,Option>();
		map.put ("charset",
			Option.builder ()
			.longOpt ("charset")
			.hasArg ()
			.desc ("defaults to UTF-8")
			.build ());
		map.put ("command-timeout",
			Option.builder ()
			.longOpt ("command-timeout")
			.hasArg ()
			.desc ("in seconds")
			.build ());
		map.put ("connect-timeout",
			Option.builder ()
			.longOpt ("connect-timeout")
			.hasArg ()
			.desc ("equals to -o ConnectTimeout=X")
			.build ());
		map.put ("login-name",
			Option.builder ("l")
			.longOpt ("login-name")
			.hasArg ()
			.desc ("login name")
			.build ());
		map.put ("option",
			Option.builder ("o")
			.longOpt ("option")
			.hasArg ()
			.desc ("ConnectTimeout=X (where X is in seconds)")
			.build ());
		map.put ("port",
			Option.builder ("p")
			.longOpt ("port")
			.hasArg ()
			.desc ("port")
			.build ());
		map.put ("password",
			Option.builder ()
			.longOpt ("password")
			.hasArg ()
			.desc ("password")
			.build ());
		map.put ("help",
			Option.builder ("h")
			.longOpt ("help")
			.desc ("usage")
			.build ());
		assert options != null;
		map.values ().stream ().forEach (a -> options.addOption (a));
		return map;
	}

	//------------------------------------------------------------------------
	private static void parseOptions (final CommandLine cmd, final Map<String, Option> optionm, final Config parsed)
	throws ParseException {
		if (cmd.hasOption (optionm.get ("help")))
			throw new ParseException ("");
		String value;

		value = cmd.getOptionValue (optionm.get ("login-name"));
		if (value != null) parsed.username = value;

		value = cmd.getOptionValue (optionm.get ("port"));
		if (value != null) parsed.port = NumberUtils.toInt (value);

		value = cmd.getOptionValue (optionm.get ("password"));
		if (value != null) parsed.password = value;

		value = cmd.getOptionValue (optionm.get ("option"));
		if (value != null) {
			value = StringUtils.substringAfter (value, "ConnectTimeout=");
			parsed.timeoutConnect = NumberUtils.toInt (value);
		}

		value = cmd.getOptionValue (optionm.get ("connect-timeout"));
		if (value != null) parsed.timeoutConnect = NumberUtils.toInt (value);

		value = cmd.getOptionValue (optionm.get ("command-timeout"));
		if (value != null) parsed.timeoutCommand = NumberUtils.toInt (value);

		value = cmd.getOptionValue (optionm.get ("charset"));
		if (value != null) {
			if (Charset.isSupported (value)) {
				parsed.charset = Charset.forName (value);
			} else {
				logger.warn ("charset {} not supported, defaults to {}", value, parsed.charset.name ());
			}
		}
	}

	//------------------------------------------------------------------------
	private static void parseLeftOver (final CommandLine cmd, final Config parsed)
	throws ParseException {
		assert cmd != null && parsed != null;
		final int n = cmd.getArgs ().length;
		if (n < 2) return;

		parsed.host = cmd.getArgs ()[0];
		int i = parsed.host.lastIndexOf ('@');
		if (i >= 0) {
			parsed.username = parsed.host.substring (0, i);
			parsed.host     = parsed.host.substring (i + 1);
		}

		parsed.command = StringUtils.join (Arrays.copyOfRange (cmd.getArgs (), 1, n), ' ');
	}

	//------------------------------------------------------------------------
	private static void parseCheck (final Config parsed) throws ParseException {
		if (parsed.host     == null)
			throw new ParseException ("host required");
		if (! Range.between (PORT_MIN, PORT_MAX).contains (parsed.port))
			throw new ParseException ("port required");
		if (parsed.username    == null)
			throw new ParseException ("user required");
		if (parsed.password == null)
			throw new ParseException ("password required");
		if (parsed.command  == null)
			throw new ParseException ("command required");
	}

	//------------------------------------------------------------------------
	private static void parseError (final Options options, final ParseException e) {
		if (! e.getMessage ().isEmpty ()) STDERR.println (e.getMessage ());
		usage (options);
		System.exit (1);
	}

	//------------------------------------------------------------------------
	private static void usage (final Options options) {
		String cmd = "ssh-password [user@]host command", header = "\nSSH Execute with Password\n\n", footer = "";
		boolean usage = true;
		new HelpFormatter().printHelp (cmd, header, options, footer, usage);
	}

}
