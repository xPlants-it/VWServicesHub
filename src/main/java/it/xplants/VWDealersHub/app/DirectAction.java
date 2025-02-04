// Generated by the Maven Archetype Plug-in
package it.xplants.VWDealersHub.app;

import it.xplants.VWDealersHub.components.Main;

import com.webobjects.appserver.WOActionResults;
import com.webobjects.appserver.WORequest;
import er.extensions.appserver.ERXDirectAction;
import er.extensions.appserver.ERXResponse;

public class DirectAction extends ERXDirectAction {
	public DirectAction(WORequest request) {
		super(request);
		helper = new DealerHubHelper(request);
	}

	DealerHubHelper helper = null;

	public WOActionResults defaultAction() {
		return pageWithName(Main.class.getName());
	}

	@Override
	public WOActionResults performActionNamed(String actionName) {

		if (helper.redirectToNewPlatform && helper.newPlatformBaseURL != null) {
			return helper.toNewPlatform();
		}
		return new ERXResponse("page not found", 404);
	}

}
