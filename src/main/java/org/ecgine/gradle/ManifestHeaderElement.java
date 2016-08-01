package org.ecgine.gradle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class ManifestHeaderElement {

	private List<String> values = new ArrayList<String>();

	private Map<String, String> attributes = new HashMap<String, String>();

	private Map<String, String> directives = new HashMap<String, String>();

	public List<String> getValues() {
		return values;
	}

	public void addValue(String value) {
		values.add(value);
	}

	public Map<String, String> getAttributes() {
		return attributes;
	}

	public void addAttribute(String name, String value) {
		attributes.put(name, value);
	}

	public Map<String, String> getDirectives() {
		return directives;
	}

	public void addDirective(String name, String value) {
		directives.put(name, value);
	}

	public boolean equals(Object obj) {
		if (!(obj instanceof ManifestHeaderElement)) {
			return false;
		}
		ManifestHeaderElement other = (ManifestHeaderElement) obj;
		if (other.values.size() != values.size()) {
			return false;
		}
		for (String value : values) {
			if (!other.values.contains(value)) {
				return false;
			}
		}
		if (other.directives.size() != directives.size()) {
			return false;
		}
		for (Entry<String, String> directive : directives.entrySet()) {
			if (!directive.getValue().equals(other.directives.get(directive.getKey()))) {
				return false;
			}
		}
		if (other.attributes.size() != attributes.size()) {
			return false;
		}
		for (Entry<String, String> attribute : attributes.entrySet()) {
			if (!attribute.getValue().equals(other.attributes.get(attribute.getKey()))) {
				return false;
			}
		}
		return true;
	}

	public String toString() {
		String string = "";
		Iterator<String> itValues = values.iterator();
		while (itValues.hasNext()) {
			string = string.concat(itValues.next());
			if (itValues.hasNext()) {
				string = string.concat(";");
			}
		}
		for (Entry<String, String> directive : directives.entrySet()) {
			string = string.concat(";");
			string = string.concat(directive.getKey());
			string = string.concat(":=");
			string = string.concat(directive.getValue());
		}
		for (Entry<String, String> attribute : attributes.entrySet()) {
			string = string.concat(";");
			string = string.concat(attribute.getKey());
			string = string.concat("=");
			string = string.concat(attribute.getValue());
		}
		return string;
	}

}
