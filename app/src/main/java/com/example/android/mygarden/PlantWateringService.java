package com.example.android.mygarden;

import android.app.IntentService;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.Nullable;

import com.example.android.mygarden.provider.PlantContract;
import com.example.android.mygarden.utils.PlantUtils;

/**
 * {@link IntentService} class for handling asynchronous task requests in a background thread.
 * @author Kaushik N Sanji
 */
public class PlantWateringService extends IntentService {
    /** COMPLETED (2): Create a plant watering service that extends IntentService and supports the
    action ACTION_WATER_PLANTS which updates last_watered timestamp for all plants still alive */

    public static final String ACTION_WATER_PLANTS = BuildConfig.APPLICATION_ID + ".action.water_plants";
    //Used to name the worker thread, important only for debugging.
    private static final String SERVICE_TAG = PlantWateringService.class.getName();

    /**
     * Creates an IntentService {@link PlantWateringService}
     */
    public PlantWateringService() {
        super(SERVICE_TAG);
    }

    /**
     * This method is invoked on the worker thread with a request to process.
     * Only one Intent is processed at a time, but the processing happens on a
     * worker thread that runs independently from other application logic.
     * So, if this code takes a long time, it will hold up other requests to
     * the same IntentService, but it will not hold up anything else.
     * When all requests have been handled, the IntentService stops itself,
     * so you should not call {@link #stopSelf}.
     *
     * @param intent The value passed to {@link
     *               Context#startService(Intent)}.
     *               This may be null if the service is being restarted after
     *               its process has gone away; see
     *               {@link Service#onStartCommand}
     *               for details.
     */
    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_WATER_PLANTS.equals(action)) {
                handleActionWaterPlants();
            }
        }
    }

    /**
     * Handles {@link #ACTION_WATER_PLANTS} with the provided parameters in the background thread
     */
    private void handleActionWaterPlants() {
        Uri PLANTS_URI = PlantContract.BASE_CONTENT_URI.buildUpon().appendPath(PlantContract.PATH_PLANTS).build();
        ContentValues contentValues = new ContentValues();
        long timeNow = System.currentTimeMillis();
        contentValues.put(PlantContract.PlantEntry.COLUMN_LAST_WATERED_TIME, timeNow);
        // Update only plants that are still alive
        getContentResolver().update(
                PLANTS_URI,
                contentValues,
                PlantContract.PlantEntry.COLUMN_LAST_WATERED_TIME + " > ? ",
                new String[]{String.valueOf(timeNow - PlantUtils.MAX_AGE_WITHOUT_WATER)});
    }

    /**
     * Starts the service {@link PlantWateringService} to perform the action {@link #ACTION_WATER_PLANTS}
     * If the service is already running, then the current action will be queued.
     *
     * @param context The context to start the service {@link PlantWateringService}
     */
    public static void startActionWaterPlants(Context context) {
        Intent intent = new Intent(context, PlantWateringService.class);
        intent.setAction(ACTION_WATER_PLANTS);
        context.startService(intent);
    }
}
