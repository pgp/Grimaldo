package it.pgp.grimaldo;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;

public class MainWidget extends AppWidgetProvider {

    public static final String LOGTAG = "GrimaldoWidget";

    public static void updateAllDirect(Context context) {
        Log.d(LOGTAG,"updateAllDirect");
        AppWidgetManager widgetManager = AppWidgetManager.getInstance(context);
        int[] ids = widgetManager.getAppWidgetIds(new ComponentName(context, MainWidget.class));
        widgetManager.notifyAppWidgetViewDataChanged(ids, android.R.id.list);
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.main_widget);

        for (int appWidgetId : ids) {
            Intent launchIntent = new Intent(context, DoUnlockActivity.class);
            PendingIntent launchPendingIntent = PendingIntent.getActivity(context, 0, launchIntent, 0);
            remoteViews.setOnClickPendingIntent(R.id.widget_unlock, launchPendingIntent);
            widgetManager.updateAppWidget(appWidgetId, remoteViews);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        String a = intent.getAction();
        Log.d(LOGTAG,"onReceive action: "+intent.getAction());
        if (a == null) return;
        if (MainActivity.mainActivityContext == null) {
            MainActivity.mainActivityContext = context;
            MainActivity.refreshToastHandler(context);
        }
        try {
            updateAllDirect(context);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}

