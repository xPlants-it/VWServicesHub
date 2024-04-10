package it.xplants.VWDealersHub.app;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import org.apache.cayenne.Cayenne;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.hc.client5.http.classic.methods.HttpHead;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import com.webobjects.appserver.WOActionResults;
import com.webobjects.appserver.WORequest;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSMutableDictionary;

import er.extensions.appserver.ERXApplication;
import er.extensions.appserver.ERXResponse;
import er.extensions.foundation.ERXMutableURL;
import er.extensions.foundation.ERXStringUtilities;
import it.xtro.XTContainer;

public class DealerHubHelper {

	public DealerHubHelper(WORequest request) {
		this.request = request;
		ObjectContext oc = ((Application) Application.application()).getObjectContext();

		String server_name = request.headerForKey("server_name");
		if (server_name == null)
			server_name = request.headerForKey("SERVER_NAME");

		if (server_name != null) {
			vwsite = ObjectSelect.query(XTContainer.class).where(XTContainer.THE_NAME.eq(server_name)).selectFirst(oc);
		}
		if (vwsite == null) {
			log.error("vwsite not found with servername null\n" + request);
			vwsite = Cayenne.objectForPK(oc, XTContainer.class, 569);
		}
		JSONObject ex = new JSONObject(vwsite.getTheExtras());
		newPlatformBaseURL = ex.optString("newPlatformBaseURL", null);
		dealerSiteName = vwsite.getTheName();

		redirectToNewPlatform = !vwsite.isFlagActive() && newPlatformBaseURL != null;

	}

	XTContainer vwsite;
	private WORequest request;
	final String newPlatformBaseURL;
	final String dealerSiteName;
	final Boolean redirectToNewPlatform;

	private final static Logger log = Logger.getLogger(DealerHubHelper.class);

	private Map<String, String> redirectMap;

	public Map<String, String> getRedirectMap() {
		if (redirectMap == null) {
			redirectMap = new HashMap<>();
			try {
				String fileSrc = ERXStringUtilities.stringFromURL(ERXApplication.erxApplication().resourceManager().pathURLForResourceNamed("new-platform-redirect-map.csv", null, null));
				Scanner scanner = new Scanner(fileSrc);
				while (scanner.hasNextLine()) {
					String line = scanner.nextLine();
//					System.out.println(line);
					String[] tokens = line.split("\t");
					if (tokens.length > 1)
						redirectMap.put(tokens[0], tokens[1]);
					else
						log.error(line);
				}
				scanner.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return redirectMap;
	}

	public String getRedirect(String k) {
		if (k == null)
			throw new NullPointerException();
		String res = getRedirectMap().get(k);
		if (res == null) {
			log.info(k);
			Set<String> keyset = getRedirectMap().keySet();
			for (String string : keyset) {
				if (string.endsWith("*")) {
					if (k.matches(string)) {
						res = getRedirectMap().get(string);
//						log.info("wildcard match found! " + k + " >> " + res);
						break;
					}
				}
			}
		}
//		log.info(k + " >> " + res);
		return res;
	}

	public WOActionResults toNewPlatform() {

		String uri = request.uri();
		String appName = ERXApplication.application().name();
		uri = uri.replace(appName + ".woa/1", appName + ".woa");

		String name = request.stringFormValueForKey("name") != null ? request.stringFormValueForKey("name").replace(dealerSiteName, "") : null;

//		log.info("uri " + uri);
//		log.info("name " + name);

		ERXResponse redirect = new ERXResponse(301);
		boolean isRedirect = request.headerForKey("redirect_url") != null;
		log.debug("isRedirect " + isRedirect);
		log.debug("redirect_url " + request.headerForKey("redirect_url"));
		String path = request.stringFormValueForKey("path");
		log.debug("path " + request.headerForKey("path"));
		if (isRedirect) {
			if (name != null) {
				try {

					if (name.startsWith("/promozioni/promozioni-volkswagen/")) {
						String[] t = name.split("/");
						int i = 0;
						for (String string : t) {
							log.debug("T[" + i + "] : " + string);
							i++;
						}
						if (t.length > 3) {
							String modello = t[3];
							log.debug("Modello : " + modello);
							name = "/acquista-volkswagen/promozioni/promozioni-volkswagen/" + modello;
						}
					}
					String redirectHardCoded = getRedirect(name);
					if (redirectHardCoded != null)
						name = verifiedPathToNewPlatform(redirectHardCoded);
					else
						name += ".html";

					if (!name.endsWith(".html"))
						name += ".html";

					ERXMutableURL murl = new ERXMutableURL(newPlatformBaseURL + name);
					NSMutableDictionary<String, NSArray<Object>> req = request.formValues().mutableClone();
					req.remove("name");
					req.remove("lang");
					for (String k : req.allKeys()) {
						NSArray<Object> pk = req.get(k);
						for (Object pko : pk) {
							murl.addQueryParameter(k, pko.toString());
						}
					}
					redirect.setHeader(murl.toExternalForm(), "Location");
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			if (path != null) {
				try {
					// https://www.concessionarie-volkswagen.it/vicentini/acquista-volkswagen.html
					String u = "/acquista-volkswagen.html";
					if (path.indexOf("volkswagen") == -1) {
						u = "/acquista-volkswagen/usato-das-weltauto/ricerca-veicoli.html";
					}
					ERXMutableURL murl = new ERXMutableURL(newPlatformBaseURL + u);
					NSMutableDictionary<String, NSArray<Object>> req = request.formValues().mutableClone();
					req.remove("name");
					req.remove("lang");
					req.remove("path");
					for (String k : req.allKeys()) {
						NSArray<Object> pk = req.get(k);
						for (Object pko : pk) {
							murl.addQueryParameter(k, pko.toString());

						}
					}
					redirect.setHeader(murl.toExternalForm(), "Location");
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		} else {
			String redirectHardCoded = getRedirect(uri);
			if (redirectHardCoded == null && name != null)
				redirectHardCoded = getRedirect(name);
			if (redirectHardCoded != null) {
				redirect.setHeader(newPlatformBaseURL + verifiedPathToNewPlatform(redirectHardCoded), "Location");
			} else
				redirect.setHeader(newPlatformBaseURL + ".html", "Location");
		}

		String location = redirect.headerForKey("Location");
		CloseableHttpResponse res = null;
		CloseableHttpClient httpclient = null;
		try {
			HttpHead head = new HttpHead(location);
			httpclient = HttpClients.createDefault();
			res = httpclient.execute(head);
			int code = res.getCode();
			if (code < HttpStatus.SC_SUCCESS || code >= HttpStatus.SC_REDIRECTION) {
				redirect.setHeader(newPlatformBaseURL + ".html", "Location");
			} else {

			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (res != null) {
				try {
					res.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		return redirect;

	}

	private String verifiedPathToNewPlatform(String u) {
		if (u == null || u.length() == 0 || "/".equals(u))
			return ".html";
		return u.startsWith("/") ? u : "/" + u;
	}

}
