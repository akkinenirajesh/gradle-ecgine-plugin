package org.ecgine.gradle;

import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.Console;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.SwingUtilities;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.ecgine.gradle.extensions.EcgineExtension;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.tasks.TaskAction;
import org.json.JSONObject;

public class EcgineLoginTask extends DefaultTask {

	@TaskAction
	public void login() throws Exception {
		String args = (String) getProject().getProperties().get("emailId");
		if (args == null) {
			throw new GradleException("Please provide ecgine credentials. -PemailId=EMAILID");
		}
		String password = (String) getProject().getProperties().get("password");
		if (password == null) {
			password = readPwd();
		}
		String apiKey = getApiKey(args, password);
		System.out.println("Successfully got the apikey: " + apiKey);
		File gradleproperties = new File("gradle.properties");
		if (!gradleproperties.exists()) {
			gradleproperties.createNewFile();
		}
		Properties properties = new Properties();
		properties.load(new FileInputStream(gradleproperties));
		properties.put("apikey", apiKey);
		properties.store(new FileWriter(gradleproperties), "added apikey");
		System.out.println("added apikey in gradle.properties file");
	}

	private String readPwd() {
		Console console = System.console();
		if (console != null) {
			char[] readPassword = console.readPassword("\nPlease enter password :");
			return new String(readPassword);
		}
		final JPasswordField jpf = new JPasswordField();
		JOptionPane jop = new JOptionPane(jpf, JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
		JDialog dialog = jop.createDialog("Password:");
		dialog.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentShown(ComponentEvent e) {
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						jpf.requestFocusInWindow();
					}
				});
			}
		});
		dialog.setVisible(true);
		int result = (Integer) jop.getValue();
		dialog.dispose();
		char[] password = null;
		if (result == JOptionPane.OK_OPTION) {
			password = jpf.getPassword();
		}
		if (password != null) {
			return new String(password);
		}
		return "";
	}

	private String getApiKey(String username, String pwd) {
		EcgineExtension ext = (EcgineExtension) getProject().getExtensions().getByName(EcgineExtension.NAME);
		try {
			String url = ext.getLoginUrl();
			System.out.println("Getting API key: " + url);
			List<NameValuePair> params = new LinkedList<NameValuePair>();
			params.add(new BasicNameValuePair("email", username));
			params.add(new BasicNameValuePair("password", pwd));
			HttpClient client = ext.getHttpClient();
			HttpGet request = new HttpGet(url + "?" + URLEncodedUtils.format(params, "utf-8"));
			HttpResponse response = client.execute(request);
			int code = response.getStatusLine().getStatusCode();
			if (code != 200) {
				EntityUtils.consume(response.getEntity());
				throw new GradleException("StatusCode:" + code + " URL:" + url);
			}
			String json = EntityUtils.toString(response.getEntity());
			JSONObject result = new JSONObject(json);
			if (result.getInt("code") == 1) {
				return result.getString("message");
			} else {
				throw new GradleException("Request failed:" + result.getString("message"));
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
