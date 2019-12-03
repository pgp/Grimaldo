package it.pgp.grimaldo;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.RemoteViews;

public class MainWidget extends AppWidgetProvider {

    private static final String standard = "it.pgp.grimaldo.appwidget.action.STANDARD_UPDATE";
    private static final String onDemand = "it.pgp.grimaldo.appwidget.action.ON_DEMAND_UPDATE";

    public static final String LOGTAG = "GrimaldoWidget";

    public static void updateAllDirect(Context context) {
        Log.d(MainWidget.class.getName(),"updateAllDirect");
        AppWidgetManager widgetManager = AppWidgetManager.getInstance(context);
        int[] ids = widgetManager.getAppWidgetIds(new ComponentName(context, MainWidget.class));
        widgetManager.notifyAppWidgetViewDataChanged(ids, android.R.id.list);

        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.main_widget);

        for (int appWidgetId : ids) {
            Intent forToggleIntentUpdate = new Intent(context, MainWidget.class);
            forToggleIntentUpdate.setAction(onDemand);
            forToggleIntentUpdate.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[]{appWidgetId});
            PendingIntent forToggleUpdate = PendingIntent.getBroadcast(
                    context, appWidgetId, forToggleIntentUpdate,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            remoteViews.setOnClickPendingIntent(R.id.widget_unlock, forToggleUpdate);

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
            switch (a) {
                case standard:
                    Log.d(LOGTAG,"standard");
                    break;
                case onDemand:
                    Log.d(LOGTAG,"onDemand: start challenge response");
                    SharedPreferences sp = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
                    String ip = sp.getString(MainActivity.ipLabel,"");
                    if(ip==null || ip.isEmpty()) {
                        MainActivity.showToastOnUIWithHandler("No default IP address set");
                        return;
                    }
                    String[] pks = MainActivity.getPrivateKeysNames(context);
                    if(pks == null || pks.length == 0) {
                        MainActivity.showToastOnUIWithHandler("No private keys found");
                        return;
                    }
                    String firstPrivateKey = pks[0]; // TODO to be replaced with something like getDefaultPrivateKey
                    MainActivity.doUnlock(context,ip,firstPrivateKey);
                    return;
                default:
                    break;
            }
            updateAllDirect(context);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}

