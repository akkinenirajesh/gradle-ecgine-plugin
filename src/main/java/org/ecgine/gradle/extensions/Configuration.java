package org.ecgine.gradle.extensions;

import java.util.HashMap;
import java.util.Map;

public class Configuration {

	private Map<String, String> properties = new HashMap<>();
	private int debugPort;
	private int consolePort;
	private String ms;
	private String mx;
	private String ss;

	public Configuration(int debugPort, int consolePort, String ms, String mx, String ss) {
		this.debugPort = debugPort;
		this.consolePort = consolePort;
		this.ms = ms;
		this.mx = mx;
		this.ss = ss;
	}

	public void property(String key, String value) {
		properties.put(key, value);
	}

	public Map<String, String> getProperties() {
		return properties;
	}

	public void debugPort(int debugPort) {
		this.debugPort = debugPort;
	}

	public void ms(String ms) {
		this.ms = ms;
	}

	public void mx(String mx) {
		this.mx = mx;
	}

	public void ss(String ss) {
		this.ss = ss;
	}

	public void consolePort(int consolePort) {
		this.consolePort = consolePort;
	}

	public int getDebugPort() {
		return debugPort;
	}

	public String getMs() {
		return ms;
	}

	public String getMx() {
		return mx;
	}

	public String getSs() {
		return ss;
	}

	public int getConsolePort() {
		return consolePort;
	}

	@Override
	public String toString() {
		return "debugPort:" + debugPort + ",consolePort:" + consolePort + ",ms:" + ms + ",mx:" + mx;
	}
}
