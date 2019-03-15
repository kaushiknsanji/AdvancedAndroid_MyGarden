package com.example.android.mygarden;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.example.android.mygarden.provider.PlantContract;
import com.example.android.mygarden.ui.PlantDetailActivity;
import com.example.android.mygarden.utils.PlantUtils;

import static com.example.android.mygarden.provider.PlantContract.BASE_CONTENT_URI;
import static com.example.android.mygarden.provider.PlantContract.PATH_PLANTS;

/**
 * COMPLETED (2): Create a RemoteViewsService class and a RemoteViewsFactory class with:
 * - onDataSetChanged querying the list of all plants in the database
 * - getViewAt creating a RemoteView using the plant_widget layout
 * - getViewAt setting the fillInIntent for widget_plant_image with the plant ID as extras
 **/
public class GridWidgetService extends RemoteViewsService {
    /**
     * To be implemented by the derived service to generate appropriate factories for
     * the data.
     *
     * @param intent
     */
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new GridRemoteViewsFactory(this.getApplicationContext(), intent);
    }
}

class GridRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {

    private Context mContext;
    private Cursor mCursor;

    GridRemoteViewsFactory(Context context, Intent intent) {
        mContext = context;
    }

    /**
     * Called when your factory is first constructed. The same factory may be shared across
     * multiple RemoteViewAdapters depending on the intent passed.
     */
    @Override
    public void onCreate() {

    }

    /**
     * Called when notifyDataSetChanged() is triggered on the remote adapter. This allows a
     * RemoteViewsFactory to respond to data changes by updating any internal references.
     * <p>
     * Note: expensive tasks can be safely performed synchronously within this method. In the
     * interim, the old data will be displayed within the widget.
     *
     * @see AppWidgetManager#notifyAppWidgetViewDataChanged(int[], int)
     */
    @Override
    public void onDataSetChanged() {
        //URI to get all Plant info
        Uri PLANT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_PLANTS).build();

        //Close cursor if active
        if (mCursor != null && !mCursor.isClosed()) {
            mCursor.close();
        }

        //Get all Plant info ordered by creation time
        mCursor = mContext.getContentResolver().query(
                PLANT_URI,
                null,
                null,
                null,
                PlantContract.PlantEntry.COLUMN_CREATION_TIME
        );
    }

    /**
     * Called when the last RemoteViewsAdapter that is associated with this factory is
     * unbound.
     */
    @Override
    public void onDestroy() {
        //Close cursor if active
        if (mCursor != null && !mCursor.isClosed()) {
            mCursor.close();
        }
    }

    /**
     * See {@link android.widget.Adapter#getCount()}
     *
     * @return Count of items.
     */
    @Override
    public int getCount() {
        //Return 0 when Cursor is not initialized
        if (mCursor == null) {
            return 0;
        }
        //Return the total number of records in the Cursor
        return mCursor.getCount();
    }

    /**
     * See {@link android.widget.Adapter#getView(int, View, ViewGroup)}.
     * <p>
     * Note: expensive tasks can be safely performed synchronously within this method, and a
     * loading view will be displayed in the interim. See {@link #getLoadingView()}.
     *
     * @param position The position of the item within the Factory's data set of the item whose
     *                 view we want.
     * @return A RemoteViews object corresponding to the data at the specified position.
     */
    @Override
    public RemoteViews getViewAt(int position) {
        //Return null when Cursor is not initialized
        if (mCursor == null || mCursor.getCount() == 0) {
            return null;
        }

        //Point Cursor to the item position passed
        mCursor.moveToPosition(position);

        //Get the data of the Plant Item
        int idIndex = mCursor.getColumnIndex(PlantContract.PlantEntry._ID);
        int createTimeIndex = mCursor.getColumnIndex(PlantContract.PlantEntry.COLUMN_CREATION_TIME);
        int waterTimeIndex = mCursor.getColumnIndex(PlantContract.PlantEntry.COLUMN_LAST_WATERED_TIME);
        int plantTypeIndex = mCursor.getColumnIndex(PlantContract.PlantEntry.COLUMN_PLANT_TYPE);
        long plantId = mCursor.getLong(idIndex);
        long timeNow = System.currentTimeMillis();
        long wateredAt = mCursor.getLong(waterTimeIndex);
        long createdAt = mCursor.getLong(createTimeIndex);
        int plantType = mCursor.getInt(plantTypeIndex);
        //Get the Plant Image
        int imgRes = PlantUtils.getPlantImageRes(mContext, timeNow - createdAt, timeNow - wateredAt, plantType);

        //Construct the RemoteViews Object for the Collection Item
        RemoteViews rv = new RemoteViews(mContext.getPackageName(), R.layout.plant_widget);
        //Update image
        rv.setImageViewResource(R.id.widget_plant_image, imgRes);
        //Update plant ID text
        rv.setTextViewText(R.id.widget_plant_name, String.valueOf(plantId));
        //Always Hide the Water Drop image in GridView mode
        rv.setViewVisibility(R.id.widget_water_button, View.GONE);

        //Fill in the OnClick Pending Intent Template using the current Plant Id
        Bundle extras = new Bundle(1);
        extras.putLong(PlantDetailActivity.EXTRA_PLANT_ID, plantId);
        Intent fillInIntent = new Intent();
        fillInIntent.putExtras(extras);
        rv.setOnClickFillInIntent(R.id.widget_plant_image, fillInIntent);

        //Return the RemoteViews Object setup for Collection Item
        return rv;
    }

    /**
     * This allows for the use of a custom loading view which appears between the time that
     * {@link #getViewAt(int)} is called and returns. If null is returned, a default loading
     * view will be used.
     *
     * @return The RemoteViews representing the desired loading view.
     */
    @Override
    public RemoteViews getLoadingView() {
        //Use Default Loading View
        return null;
    }

    /**
     * See {@link android.widget.Adapter#getViewTypeCount()}.
     *
     * @return The number of types of Views that will be returned by this factory.
     */
    @Override
    public int getViewTypeCount() {
        //All GridView Items are same in the collection
        return 1;
    }

    /**
     * See {@link android.widget.Adapter#getItemId(int)}.
     *
     * @param position The position of the item within the data set whose row id we want.
     * @return The id of the item at the specified position.
     */
    @Override
    public long getItemId(int position) {
        //Returning the position passed as the id of the Item
        return position;
    }

    /**
     * See {@link android.widget.Adapter#hasStableIds()}.
     *
     * @return True if the same id always refers to the same object.
     */
    @Override
    public boolean hasStableIds() {
        //Fixed Item Ids, hence returning true
        return true;
    }
}
