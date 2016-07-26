package org.ecgine.gradle;

public class EcgineURLs {
	private static final String CREATE_EBUNDLE = "/api/createebundle";
	private static final String CREATE_PACKAGE = "/api/createpackage";
	private static final String CREATE_PACKAGE_VERSION = "/api/createpackageversion";
	private static final String MASTER_BUNDLE = "com.vimukti.ecgine.master";
	private static String URL = "https://vimukti.ecgine.com/";

	public static String getCreateEbundleUrl() {
		StringBuilder b = new StringBuilder();
		b.append(URL);
		b.append(MASTER_BUNDLE);
		b.append(CREATE_EBUNDLE);
		return b.toString();
	}

	public static String getCreatePackageVersionUrl() {
		StringBuilder b = new StringBuilder();
		b.append(URL);
		b.append(MASTER_BUNDLE);
		b.append(CREATE_PACKAGE_VERSION);
		return b.toString();
	}

	public static String getCreatePackageUrl() {
		StringBuilder b = new StringBuilder();
		b.append(URL);
		b.append(MASTER_BUNDLE);
		b.append(CREATE_PACKAGE);
		return b.toString();
	}
}
