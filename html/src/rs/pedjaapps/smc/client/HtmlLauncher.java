package rs.pedjaapps.smc.client;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.gwt.GwtApplication;
import com.badlogic.gdx.backends.gwt.GwtApplicationConfiguration;
import com.badlogic.gdx.backends.gwt.preloader.Preloader;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.InlineHTML;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;

import de.golfgl.gdxgamesvcs.GpgsClient;
import de.golfgl.gdxgamesvcs.IGameServiceIdMapper;
import de.golfgl.gdxgamesvcs.KongClient;
import de.golfgl.smc.gpgs.GpgsMapper;
import rs.pedjaapps.smc.MaryoGame;

public class HtmlLauncher extends GwtApplication {

    // padding is to avoid scrolling in iframes, set to 20 if you have problems
    private static final int PADDING = 20;
    private GwtApplicationConfiguration cfg;

    @Override
    public GwtApplicationConfiguration getConfig() {
        int w = Window.getClientWidth() - PADDING;
        int h = Window.getClientHeight() - PADDING;
        cfg = new GwtApplicationConfiguration(w, h);
        Window.enableScrolling(false);
        Window.setMargin("0");
        Window.addResizeHandler(new ResizeListener());
        return cfg;
    }

    @Override
    public ApplicationListener createApplicationListener() {
        MaryoGame maryoGame = new MaryoGame(null);
        maryoGame.isRunningOn = Window.Navigator.getUserAgent();
        maryoGame.gsClient = new KongClient();

        IGameServiceIdMapper<String> gpgsMapper = new GpgsMapper();

        maryoGame.gpgsClient = new GpgsClient() {
            @Override
            public boolean submitEvent(String eventId, int increment) {
                eventId = gpgsMapper.mapToGsId(eventId);

                if (eventId != null)
                    return super.submitEvent(eventId, increment);
                else
                    return false;
            }
        }.setGpgsAchievementIdMapper(gpgsMapper)
                .setGpgsLeaderboardIdMapper(gpgsMapper)
                .initialize(GpgsMapper.CLIENT_ID, true);
        return maryoGame;
    }

    @Override
    public Preloader.PreloaderCallback getPreloaderCallback() {
        return createPreloaderPanel(GWT.getHostPageBaseURL() + "logo_preload.png");
    }

    @Override
    protected void adjustMeterPanel(Panel meterPanel, Style meterStyle) {
        meterPanel.setStyleName("gdx-meter");
	    meterPanel.addStyleName("nostripes");
    }

    class ResizeListener implements ResizeHandler {
        @Override
        public void onResize(ResizeEvent event) {
            int width = event.getWidth() - PADDING;
            int height = event.getHeight() - PADDING;
            getRootPanel().setWidth("" + width + "px");
            getRootPanel().setHeight("" + height + "px");
            getApplicationListener().resize(width, height);
            Gdx.graphics.setWindowedMode(width, height);
        }
    }
}