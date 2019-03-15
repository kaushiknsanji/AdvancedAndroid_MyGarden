package com.example.android.mygarden;

/*
* Copyright (C) 2017 The Android Open Source Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*  	http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatDelegate;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import com.example.android.mygarden.provider.PlantContract;
import com.example.android.mygarden.ui.MainActivity;
import com.example.android.mygarden.ui.PlantDetailActivity;

public class PlantWidgetProvider extends AppWidgetProvider {

    // setImageViewResource to update the widget’s image
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int imgRes, long plantId, boolean showWater, int appWidgetId) {

        // COMPLETED (6): Set the PendingIntent template in getGardenGridRemoteView to launch PlantDetailActivity

        //Enables to use Vector Drawables in Drawable Container attributes
        //To be called before inflation of Views
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);

        // COMPLETED (4): separate the updateAppWidget logic into getGardenGridRemoteView and getSinglePlantRemoteView
        // COMPLETED (5): Use getAppWidgetOptions to get widget width and use the appropriate RemoteView method

        //Get the current width to decide on Single Plant View vs Garden Grid View
        Bundle widgetOptions = appWidgetManager.getAppWidgetOptions(appWidgetId);
        int width = widgetOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
        RemoteViews rv;
        //Get the appropriate RemoteView based on the current width
        if (width < 300) {
            rv = getSinglePlantRemoteView(context, imgRes, plantId, showWater);
        } else {
            rv = getGardenGridRemoteView(context);
        }
        //Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, rv);
    }

    /**
     * Creates and Returns the RemoteViews to be displayed in the GridView mode Widget
     *
     * @param context The calling Context
     * @return The RemoteViews for the GridView mode Widget
     */
    private static RemoteViews getGardenGridRemoteView(Context context) {
        //Construct the RemoteViews Object for GridView Collection
        RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.widget_grid_view);

        //Set an Intent to the GridWidgetService that acts as an Adapter to the GridView
        Intent intent = new Intent(context, GridWidgetService.class);
        rv.setRemoteAdapter(R.id.widget_grid_view, intent);

        //Set an Intent to launch the PlantDetailActivity when the collection is clicked
        Intent appIntent = new Intent(context, PlantDetailActivity.class);
        PendingIntent appPendingIntent = PendingIntent.getActivity(context, 0, appIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        rv.setPendingIntentTemplate(R.id.widget_grid_view, appPendingIntent);

        //Handle empty garden states
        rv.setEmptyView(R.id.widget_grid_view, R.id.empty_view);

        //Returning the RemoteViews Object setup for the Collection
        return rv;
    }

    /**
     * Creates and Returns the RemoteViews to be displayed in the Single Plant mode Widget
     *
     * @param context   The calling Context
     * @param imgRes    The Image Resource for Single Plant mode
     * @param plantId   The database Id of the Plant
     * @param showWater Boolean to show/hide the water drop button
     * @return The RemoteViews for Single Plant mode widget
     */
    private static RemoteViews getSinglePlantRemoteView(Context context, int imgRes, long plantId, boolean showWater) {
        //Set Click Handler to open the DetailActivity for the Plant Id,
        //or the Main Activity if the Plant Id is invalid
        Intent intent;
        if (plantId == PlantContract.INVALID_PLANT_ID) {
            intent = new Intent(context, MainActivity.class);
        } else { // Set on click to open the corresponding detail activity
            Log.d(PlantWidgetProvider.class.getSimpleName(), "plantId=" + plantId);
            intent = new Intent(context, PlantDetailActivity.class);
            intent.putExtra(PlantDetailActivity.EXTRA_PLANT_ID, plantId);
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        // Construct the RemoteViews object
        RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.plant_widget);
        // Update image
        rv.setImageViewResource(R.id.widget_plant_image, imgRes);
        // Update plant ID text
        rv.setTextViewText(R.id.widget_plant_name, String.valueOf(plantId));
        // Show/Hide the water drop button
        if (showWater) rv.setViewVisibility(R.id.widget_water_button, View.VISIBLE);
        else rv.setViewVisibility(R.id.widget_water_button, View.INVISIBLE);
        // Widgets allow click handlers to only launch pending intents
        rv.setOnClickPendingIntent(R.id.widget_plant_image, pendingIntent);
        // Add the wateringservice click handler
        Intent wateringIntent = new Intent(context, PlantWateringService.class);
        wateringIntent.setAction(PlantWateringService.ACTION_WATER_PLANT);
        wateringIntent.putExtra(PlantWateringService.EXTRA_PLANT_ID, plantId);
        PendingIntent wateringPendingIntent = PendingIntent.getService(context, 0, wateringIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        rv.setOnClickPendingIntent(R.id.widget_water_button, wateringPendingIntent);
        //Return the RemoteView
        return rv;
    }

    /**
     * Updates all widget instances given the Widget IDs and display information
     *
     * @param context The calling Context
     * @param appWidgetManager The Widget Manager
     * @param imgRes The Image Resource for Single Plant mode
     * @param plantId The database Id of the Plant
     * @param showWater Boolean to show/hide the water drop button
     * @param appWidgetIds Array of Widget IDs to be updated
     */
    public static void updatePlantWidgets(Context context, AppWidgetManager appWidgetManager,
                                          int imgRes, long plantId, boolean showWater, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, imgRes, plantId, showWater, appWidgetId);
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        //Start the intent service update widget action, the service takes care of updating the widgets UI
        PlantWateringService.startActionUpdatePlantWidgets(context);
    }

    /**
     * Called in response to the {@link AppWidgetManager#ACTION_APPWIDGET_OPTIONS_CHANGED}
     * broadcast when this widget has been layed out at a new size.
     *
     * @param context          The {@link Context Context} in which this receiver is
     *                         running.
     * @param appWidgetManager A {@link AppWidgetManager} object you can call {@link
     *                         AppWidgetManager#updateAppWidget} on.
     * @param appWidgetId      The appWidgetId of the widget whose size changed.
     * @param newOptions       The appWidgetId of the widget whose size changed.
     * @see AppWidgetManager#ACTION_APPWIDGET_OPTIONS_CHANGED
     */
    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);
        PlantWateringService.startActionUpdatePlantWidgets(context);
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        // Perform any action when one or more AppWidget instances have been deleted
    }

    @Override
    public void onEnabled(Context context) {
        // Perform any action when an AppWidget for this provider is instantiated
    }

    @Override
    public void onDisabled(Context context) {
        // Perform any action when the last AppWidget instance for this provider is deleted
    }

}
