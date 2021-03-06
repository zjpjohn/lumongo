package org.lumongo.ui.client.places;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.ScriptElement;
import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceChangeEvent;
import com.google.gwt.place.shared.PlaceHistoryMapper;
import com.google.gwt.user.client.rpc.AsyncCallback;
import gwt.material.design.client.ui.MaterialLoader;
import org.lumongo.ui.client.ContentPresenter;
import org.lumongo.ui.client.LumongoUI;
import org.lumongo.ui.client.controllers.MainController;
import org.lumongo.ui.client.controllers.WidgetController;
import org.lumongo.ui.client.services.ServiceProvider;
import org.lumongo.ui.client.widgets.base.ToastHelper;
import org.lumongo.ui.shared.InstanceInfo;
import org.lumongo.ui.shared.UIQueryResults;
import org.lumongo.ui.shared.UIQueryState;

public class PlaceHandler implements PlaceChangeEvent.Handler {

	private Place lastPlace;
	private WidgetController widgetController;
	private final LumongoUI lumongoUI;

	public PlaceHandler(LumongoUI lumongoUI) {
		this.lumongoUI = lumongoUI;
		widgetController = new WidgetController();
	}

	public void init() {

		/*
		setGaVars("");
		initGA();
		*/

		MainController.get().getEventBus().addHandler(PlaceChangeEvent.TYPE, this);
		setupEvents();

		initPlaces();

	}

	protected void setupEvents() {

	}

	protected void initPlaces() {
		MainController.get().init(getPlaceHistoryMapper(), getDefaultPlace(), getDefaultPlace());
	}

	protected ContentPresenter getContentPresenter() {
		return lumongoUI;
	}

	protected WidgetController getWidgetController() {
		return widgetController;
	}

	protected PlaceHistoryMapper getPlaceHistoryMapper() {
		return new PlaceHistoryMapperImpl();
	}

	public Place getLastPlace() {
		return lastPlace;
	}

	public native void track(String url) /*-{
        try {
            //$wnd._gaq.push([ '_trackPageview' ]);
            $wnd._gaq.push(['_trackPageview', url]);
        } catch (e) {
        }
    }-*/;

	private void initGA() {
		Document doc = Document.get();
		ScriptElement script = doc.createScriptElement();
		script.setSrc("https://ssl.google-analytics.com/ga.js");
		script.setType("text/javascript");
		script.setLang("javascript");
		doc.getBody().appendChild(script);
	}

	private static native void setGaVars(String account) /*-{
        $wnd._gaq = $wnd._gaq || [];
        $wnd._gaq.push(['_setAccount', account]);
        $wnd._gaq.push(['_trackPageview',
            location.pathname + location.search + location.hash]);
    }-*/;

	@Override
	public void onPlaceChange(PlaceChangeEvent event) {

		final Place place = event.getNewPlace();
		track(place.getClass().getSimpleName());

		GWT.log(place.toString());

		lastPlace = place;

		setBrowserWindowTitle("LuMongo");

		handlePlaces(place);

	}

	protected final void handlePlaces(Place place) {
		if (place instanceof HomePlace) {
			displayHomePlace();
		}
		else if (place instanceof QueryPlace) {
			displayQueryPlace((QueryPlace) place);
		}
	}

	public boolean placeStillCurrent(Place place) {
		boolean current = (lastPlace == place);
		if (!current) {
			GWT.log("Place " + place + " is no longer current");
		}
		else {
			GWT.log("Place " + place + " is current");
		}
		return current;
	}

	public static void setBrowserWindowTitle(String newTitle) {
		if (Document.get() != null) {
			Document.get().setTitle(newTitle);
		}
	}

	protected final Place getDefaultPlace() {
		return new HomePlace();
	}

	protected void displayQueryPlace(QueryPlace place) {
		getContentPresenter().setContent(null);

		MaterialLoader.showLoading(true);

		if (place.getQueryId() != null) {
			// execute query and show the query/results page.
			ServiceProvider.get().getLumongoService().search(place.getQueryId(), new AsyncCallback<UIQueryResults>() {
				@Override
				public void onFailure(Throwable caught) {
					MaterialLoader.showLoading(false);
					ToastHelper.showFailure(caught.getMessage());
				}

				@Override
				public void onSuccess(UIQueryResults result) {
					MaterialLoader.showLoading(false);
					getWidgetController().getQueryView().draw(result);
					getContentPresenter().setContent(getWidgetController().getQueryView());
				}
			});
		}
		else {
			// just show the query page.
			if (UIQueryState.getIndexes() == null) {
				ServiceProvider.get().getLumongoService().getInstanceInfo(new AsyncCallback<InstanceInfo>() {
					@Override
					public void onFailure(Throwable caught) {
						MaterialLoader.showLoading(false);
						ToastHelper.showFailure(caught.getMessage());
					}

					@Override
					public void onSuccess(InstanceInfo result) {
						MaterialLoader.showLoading(false);
						UIQueryResults results = new UIQueryResults();
						results.setIndexes(result.getIndexes());
						getWidgetController().getQueryView().draw(results);
						getContentPresenter().setContent(getWidgetController().getQueryView());
					}
				});
			}
			else {
				MaterialLoader.showLoading(false);
				UIQueryResults results = new UIQueryResults();
				results.setIndexes(UIQueryState.getIndexes());
				getWidgetController().getQueryView().draw(results);
				getContentPresenter().setContent(getWidgetController().getQueryView());
			}
		}
	}

	protected void displayHomePlace() {
		getContentPresenter().setContent(null);

		// show the splash page...
		ServiceProvider.get().getLumongoService().getInstanceInfo(new AsyncCallback<InstanceInfo>() {
			@Override
			public void onFailure(Throwable caught) {
				ToastHelper.showFailure(caught.getMessage());
			}

			@Override
			public void onSuccess(InstanceInfo result) {
				UIQueryState.setIndexes(result.getIndexes());
				getWidgetController().getHomeView().drawSplashPage(result);
				getContentPresenter().setContent(getWidgetController().getHomeView());
			}
		});

	}

}
