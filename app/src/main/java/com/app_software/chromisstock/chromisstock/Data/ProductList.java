package com.app_software.chromisstock.chromisstock.Data;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.preference.PreferenceManager;

import com.app_software.chromisstock.chromisstock.DatabaseHandler;
import com.app_software.chromisstock.chromisstock.DownloadResultReceiver;
import com.app_software.chromisstock.chromisstock.DownloadStockData;


import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Helper class for providing content for user interface
 */
public class ProductList implements DownloadResultReceiver.Receiver {
    String TAG = "ProductList";

    public static List<StockProduct> ITEMS;
    public static Map<Long, StockProduct> ITEM_MAP = new HashMap<Long, StockProduct>();

    private DownloadResultReceiver m_Receiver;
    private Context m_Context;

    public void addItem(StockProduct item) {
        ITEMS.add(item);
        ITEM_MAP.put(item.getID(), item);
    }

    private ListChangeNotify m_Notify;
    public interface ListChangeNotify {
        public void NotifyListChanged();
    }

    public void setListChangeNotify(ListChangeNotify receiver) {
        m_Notify = receiver;
    }

    protected void NotifyListChanged() {
        if (m_Notify != null) {
            m_Notify.NotifyListChanged();
        }
    }

    ///

    public ProductList(Context c) {

        m_Context = c;
        DatabaseHandler db = DatabaseHandler.getInstance(c);

        ITEMS = db.getAllProducts();

        if( ProductList.ITEMS.isEmpty() ) {
            // Empty database - start the service to fill it from ChromisPOS
            /* Starting Download Service */

            m_Receiver = new DownloadResultReceiver(new Handler());
            m_Receiver.setReceiver( this );
            Intent intent = new Intent(Intent.ACTION_SYNC, null, m_Context , DownloadStockData.class);

            SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(m_Context);
            String connection = SP.getString("database_url", null );
            String user = SP.getString("database_user", null );
            String pwd = SP.getString("database_password", null );

            DownloadStockData.startActionDownloadData(m_Context, m_Receiver, connection, user, pwd);

        }


    }

    // Receives the result from the download service
    @Override
    public void onReceiveResult(int resultCode, Bundle resultData) {

        if( resultCode == DownloadStockData.STATUS_DOWNLOAD_FINISHED) {

            DatabaseHandler db = DatabaseHandler.getInstance(m_Context);

            ITEMS.clear();
            ITEMS = db.getAllProducts();

            NotifyListChanged();
        }
    }
}
