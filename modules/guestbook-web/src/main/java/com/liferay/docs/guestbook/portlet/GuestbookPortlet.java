package com.liferay.docs.guestbook.portlet;

import com.liferay.docs.guestbook.constants.GuestbookPortletKeys;
import com.liferay.docs.guestbook.model.Guestbook;
import com.liferay.docs.guestbook.model.GuestbookEntry;
import com.liferay.docs.guestbook.service.GuestbookEntryLocalService;
import com.liferay.docs.guestbook.service.GuestbookLocalService;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.portlet.bridges.mvc.MVCPortlet;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.ServiceContextFactory;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.PortalUtil;

import java.io.IOException;
import java.util.List;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.Portlet;
import javax.portlet.PortletException;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Vishal
 */
@Component(immediate = true, property = { "com.liferay.portlet.display-category=category.social",
		"com.liferay.portlet.header-portlet-css=/css/main.css", "com.liferay.portlet.instanceable=false",
		"javax.portlet.display-name=Guestbook", "javax.portlet.init-param.template-path=/",
		"javax.portlet.init-param.view-template=/guestbook/view.jsp",
		"javax.portlet.name=" + GuestbookPortletKeys.GUESTBOOK, "javax.portlet.resource-bundle=content.Language",
		"javax.portlet.security-role-ref=power-user,user" }, service = Portlet.class)
public class GuestbookPortlet extends MVCPortlet {

	private static final Log LOGGER = LogFactoryUtil.getLog(GuestbookPortlet.class.getName());

	@Reference
	private GuestbookEntryLocalService guestbookEntryLocalService;

	@Reference
	private GuestbookLocalService guestbookLocalService;

	@Override
	public void render(RenderRequest renderRequest, RenderResponse renderResponse)
			throws IOException, PortletException {
		LOGGER.info("inside render phase");

		try {
			ServiceContext serviceContext = ServiceContextFactory.getInstance(Guestbook.class.getName(), renderRequest);
			long groupId = serviceContext.getScopeGroupId();
			long guestbookId = ParamUtil.getLong(renderRequest, "guestbookId");
			List<Guestbook> guestbooks = guestbookLocalService.getGuestbooks(groupId);

			if (guestbooks.isEmpty()) {
				Guestbook guestbook = guestbookLocalService.addGuestbook(serviceContext.getUserId(), "Main",
						serviceContext);
				guestbookId = guestbook.getGuestbookId();
			}
			if (guestbookId == 0) {
				guestbookId = guestbooks.get(0).getGuestbookId();
			}
			renderRequest.setAttribute("guestbookId", guestbookId);
		} catch (Exception e) {
			throw new PortletException(e);
		}
		super.render(renderRequest, renderResponse);
	}

	public void addEntry(ActionRequest request, ActionResponse response) throws PortalException {
		ServiceContext serviceContext = ServiceContextFactory.getInstance(GuestbookEntry.class.getName(), request);

		String name = ParamUtil.getString(request, "name");
		String email = ParamUtil.getString(request, "email");
		String message = ParamUtil.getString(request, "message");
		long guestbookId = ParamUtil.getLong(request, "guestbookId");
		long entryId = ParamUtil.getLong(request, "entryId");

		if (entryId > 0) {
			try {
				guestbookEntryLocalService.updateGuestbookEntry(serviceContext.getUserId(), guestbookId, entryId, name,
						email, message, serviceContext);
//				response.getRenderParameters().setValue("guestbookId", Long.toString(guestbookId));
				response.setRenderParameter("guestbookId", Long.toString(guestbookId));
			} catch (Exception e) {
				System.out.println(e);
				PortalUtil.copyRequestParameters(request, response);
				response.setRenderParameter("mvcPath", "/guestbook/edit_entry.jsp");
			}
		} else {
			try {
				guestbookEntryLocalService.addEntry(serviceContext.getUserId(), guestbookId, name, email, message,
						serviceContext);
				response.setRenderParameter("guestbookId", Long.toString(guestbookId));
			} catch (Exception e) {
				System.out.println(e);
				PortalUtil.copyRequestParameters(request, response);
				response.setRenderParameter("mvcPath", "/guestbook/edit_entry.jsp");
			}
			guestbookEntryLocalService.updateGuestbookEntry(serviceContext.getUserId(), guestbookId, entryId, name,
					email, message, serviceContext);
			response.setRenderParameter("guestbookId", Long.toString(guestbookId));
		}
	}

	public void deleteEntry(ActionRequest request, ActionResponse response) throws PortalException {
		long entryId = ParamUtil.getLong(request, "entryId");
		long guestbookId = ParamUtil.getLong(request, "guestbookId");
		ServiceContext serviceContext = ServiceContextFactory.getInstance(GuestbookEntry.class.getName(), request);
		try {
			response.setRenderParameter("guestbookId", Long.toString(guestbookId));
			guestbookEntryLocalService.deleteGuestbookEntry(entryId);
		} catch (Exception e) {
			LOGGER.info(e.getMessage());
		}
	}

}