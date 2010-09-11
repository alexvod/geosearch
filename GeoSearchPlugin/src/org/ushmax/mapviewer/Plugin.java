package org.ushmax.mapviewer;

import org.ushmax.common.Factory;

public class Plugin implements AbstractPlugin {

  public void onLoad(final MapViewerApp app) {
    uiModuleFactory = new Factory<UiModule, ActivityData>() {
      @Override
      public UiModule create(ActivityData activityData) {
        return new SearchUiModule(activityData.uiController, app.kmlController, activityData.activityManager);
      }
    };
    app.uiModules.add(uiModuleFactory);
  }

  public void onUnLoad(MapViewerApp app) {
    app.uiModules.remove(uiModuleFactory);
  }
  
  private Factory<UiModule, ActivityData> uiModuleFactory;
}
