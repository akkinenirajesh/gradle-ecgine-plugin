package org.ecgine.gradle.extensions;

import java.util.HashMap;
import java.util.Map;

public class Configuration {

	private Map<String, String> properties = new HashMap<>();
	private int debugPort;
	private int consolePort;
	private String ms;
	private String mx;

	public Configuration(int debugPort, int consolePort, String ms, String mx) {
		this.debugPort = debugPort;
		this.consolePort = consolePort;
		this.ms = ms;
		this.mx = mx;
	}

	public void property(String key, String value) {
		properties.put(key, value);
	}

	public Map<String, String> getProperties() {
		return properties;
	}

	public void setDebugPort(int debugPort) {
		this.debugPort = debugPort;
	}

	public void setConsolePort(int consolePort) {
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

	public int getConsolePort() {
		return consolePort;
	}

}
