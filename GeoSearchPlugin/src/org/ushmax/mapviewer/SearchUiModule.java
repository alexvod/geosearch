package org.ushmax.mapviewer;

import org.ushmax.common.BadDataException;
import org.ushmax.common.ByteArraySlice;
import org.ushmax.common.Callback;
import org.ushmax.common.InByteStream;
import org.ushmax.common.Logger;
import org.ushmax.common.LoggerFactory;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;

public class SearchUiModule implements UiModule {
  private static final Logger logger = LoggerFactory.getLogger(SearchUiModule.class);
  private final KmlController kmlController;
  private final UiController uiController;
  private final ChildActivityManager activityManager;

  public SearchUiModule(UiController uiController, KmlController kmlController,
      ChildActivityManager activityManager) {
    this.kmlController = kmlController;
    this.uiController = uiController;
    this.activityManager = activityManager;
  }

  @Override
  public void onCreateMenu(Menu menu) {
    menu.add(0, 0, 200, "Search").setOnMenuItemClickListener(new OnMenuItemClickListener() {
      public boolean onMenuItemClick(MenuItem item) {
        showSearchView();
        return true;
      }  
    });
  }

  private void displaySearchResults(byte[] allResults) {
    ByteArraySlice slice = new ByteArraySlice(allResults);
    InByteStream input = new InByteStream(slice);
    try {
      kmlController.readFromStream(input);
    } catch (BadDataException e) {
      logger.debug("Search returned broken results  cannot parse: " + e);
    }
  }

  private void moveToGeocodeResult(Intent data) {
    int xcoord = Integer.parseInt(data.getStringExtra("org.alexvod.geosearch.XCOORD"));
    int ycoord = Integer.parseInt(data.getStringExtra("org.alexvod.geosearch.YCOORD"));
    logger.error("Jumping to project coordinates " + xcoord + "," + ycoord);
    uiController.moveTo(xcoord, ycoord);
  }

  private void showSearchView() {
    Intent intent = new Intent("org.alexvod.geosearch.SEARCH");
    try {
      activityManager.startActivity(intent, new Callback<Intent>() {
        @Override
        public void run(Intent data) {
          byte[] allResults = data.getByteArrayExtra("org.alexvod.geosearch.ALL_RESULTS");
          if (allResults == null) {
            logger.debug("No search results, showing single point");
            String title = data.getStringExtra("org.alexvod.geosearch.TITLE");   
            uiController.displayMessage(title);
            moveToGeocodeResult(data);
          } else {
            displaySearchResults(allResults);
          }
        }
      });
    } catch (ActivityNotFoundException e) {
      // No search application was found
      uiController.displayMessage("cannot do search..."); 
    }
  }
}
