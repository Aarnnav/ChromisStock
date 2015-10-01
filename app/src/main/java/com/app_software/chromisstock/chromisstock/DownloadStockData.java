package com.app_software.chromisstock.chromisstock;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.text.TextUtils;
import android.util.Log;

import com.app_software.chromisstock.chromisstock.Data.StockProduct;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p/>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class DownloadStockData extends IntentService {
    String TAG = "ProductList";

    public static final int DB_UNKNOWN = 0;
    public static final int DB_CHROMIS_UNKNOWN = 1;     // Unknown (future) Chromis database
    public static final int DB_UNICENTA_UNKNOWN = 2;    // Unknown (future or past) UnicentaPOS database
    public static final int DB_CHROMIS_SUPPORTED = 3;   // A supported Chromis database

    public static final String ACTION_TESTCONNECT = "com.app_software.chromisstock.chromisstock.action.TESTCONNECT";
    public static final String ACTION_DOWNLOADDATA = "com.app_software.chromisstock.chromisstock.action.DOWNLOADDATA";

    public static final String EXTRA_CONNECTION = "com.app_software.chromisstock.chromisstock.extra.CONNECTION";
    public static final String EXTRA_USERNAME = "com.app_software.chromisstock.chromisstock.extra.USERNAME";
    public static final String EXTRA_PASSWORD = "com.app_software.chromisstock.chromisstock.extra.PASSWORD";
    public static final String EXTRA_RECEIVER = "com.app_software.chromisstock.chromisstock.extra.RECEIVER";
    public static final String EXTRA_LOCATION = "com.app_software.chromisstock.chromisstock.extra.LOCATION";

    public static final int STATUS_ERROR = -1;
    public static final int STATUS_RUNNING = 0;
    public static final int STATUS_DOWNLOAD_FINISHED = 1;
    public static final int STATUS_CONNECTION_OK = 2;

    private int m_dbVariant = DB_UNKNOWN;
    public int getDBVariant() {
        return m_dbVariant;
    }

    /**
     * Starts this service to perform action TESTCONNECT with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    public static void startActionTestConnect(Context context,  ResultReceiver receiver, String connection, String uname, String pwd) {
        Intent intent = new Intent(context, DownloadStockData.class);

        intent.setAction(ACTION_TESTCONNECT);
        intent.putExtra(EXTRA_CONNECTION, connection);
        intent.putExtra(EXTRA_USERNAME, uname);
        intent.putExtra(EXTRA_PASSWORD, pwd);
        intent.putExtra(EXTRA_RECEIVER, receiver);

        context.startService(intent);
    }

    /**
     * Starts this service to perform action DOWNLOADDATA with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    // TODO: Customize helper method
    public static void startActionDownloadData(Context context, ResultReceiver receiver, String connection, String uname, String pwd, String location) {
        Intent intent = new Intent(context, DownloadStockData.class);
        intent.setAction(ACTION_DOWNLOADDATA);
        intent.putExtra(EXTRA_CONNECTION, connection);
        intent.putExtra(EXTRA_USERNAME, uname);
        intent.putExtra(EXTRA_PASSWORD, pwd);
        intent.putExtra(EXTRA_RECEIVER, receiver);
        intent.putExtra(EXTRA_LOCATION, location);

        context.startService(intent);
    }

    public DownloadStockData() {
        super("DownloadStockData");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final ResultReceiver receiver = intent.getParcelableExtra(EXTRA_RECEIVER);
            Bundle bundle =intent.getExtras();

            final String action = intent.getAction();
            if (ACTION_TESTCONNECT.equals(action)) {

                Log.v( TAG, "ACTION_TESTCONNECT");

                receiver.send(STATUS_RUNNING, bundle);
                if( handleActionTestConnect(bundle) ) {
                    receiver.send(STATUS_CONNECTION_OK, bundle);
                } else {
                    bundle.putString(Intent.EXTRA_TEXT, m_LastError );
                    receiver.send(STATUS_ERROR, bundle);
                }
            } else if (ACTION_DOWNLOADDATA.equals(action)) {
                Log.v( TAG, "ACTION_DOWNLOADDATA");

                receiver.send(STATUS_RUNNING, bundle);
                if( handleActionDownloadData( bundle, receiver ) ) {
                    receiver.send(STATUS_DOWNLOAD_FINISHED, bundle);
                } else {
                    bundle.putString(Intent.EXTRA_TEXT, m_LastError );
                    receiver.send(STATUS_ERROR, bundle);
                }
            }
        }
    }

    private Connection m_dbConn = null;
    private String m_LastError;

    private boolean Connect(String connection, String uname, String pwd ) {
        boolean bResult = false;
        String dbId = null;
        String dbVersion = null;

        try {
            Class.forName("com.mysql.jdbc.Driver").newInstance();

            if( m_dbConn != null ) {
                CloseConnection();
            }
            m_dbConn = DriverManager.getConnection(connection, uname, pwd);

            if( m_dbConn == null ) {
                m_LastError = "Connection failed";
                Log.e(TAG, m_LastError );
            } else {
                Statement stmt = m_dbConn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT ID, VERSION FROM APPLICATIONS" );
                if(rs.next()) {
                    dbId = rs.getString(1);
                    dbVersion = rs.getString(2);
                }
                rs.close();
                stmt.close();

                m_dbVariant = DB_UNKNOWN;
                // Determine if we support this database version
                if( dbId == null || dbVersion == null ) {
                    m_LastError = "Database not recognised - No ID or VERSION in APPLICATIONS table";
                    Log.e(TAG, m_LastError);
                } else if( dbId.compareTo("chromispos") == 0 ) {
                    bResult = true;

                    if( dbVersion.startsWith( "0.") )
                    {
                        m_dbVariant = DB_CHROMIS_SUPPORTED;
                    } else {
                        m_dbVariant = DB_CHROMIS_UNKNOWN;
                        m_LastError = "Database Version " + dbId + ": " + dbVersion + " not known - we will continue but this may cause errors";
                        Log.w(TAG, m_LastError);
                    }
                } else if( dbId.compareTo("unicentaopos") == 0 ) {
                    bResult = true;
                    m_dbVariant = DB_UNICENTA_UNKNOWN;

                    m_LastError = "This is a Unicentaopos database - we will continue but this may cause errors";
                    Log.w(TAG, m_LastError);
                }

                if(   m_dbVariant == DB_UNKNOWN ) {
                    m_LastError = "Database not recognised " + dbId + ": " + dbVersion;
                    Log.e(TAG, m_LastError);
                }
            }
        } catch (java.sql.SQLException e) {
            m_LastError =  Log.getStackTraceString(e);
            Log.e(TAG, m_LastError);
        } catch (ClassNotFoundException e) {
            m_LastError =  Log.getStackTraceString(e);
            Log.e(TAG, m_LastError);
        } catch (InstantiationException e) {
            m_LastError =  Log.getStackTraceString(e);
            Log.e(TAG, m_LastError);
        } catch (IllegalAccessException e) {
            m_LastError =  Log.getStackTraceString(e);
            Log.e(TAG, m_LastError);
        }

        if( !bResult )
            CloseConnection();

        return bResult;
    }

    private void CloseConnection() {
        try {
            if( m_dbConn != null ) {
                m_dbConn.close();
                m_dbConn = null;
            }
        } catch (java.sql.SQLException e) {
            m_LastError =  Log.getStackTraceString(e);
            Log.e(TAG, m_LastError);
        }
    }

    /**
     * Handle action TestConnect in the provided background thread with the provided
     * parameters.
     */
    private boolean handleActionTestConnect( Bundle bundle) {

        final String connection = bundle.getString(EXTRA_CONNECTION);
        final String uname = bundle.getString(EXTRA_USERNAME);
        final String pwd = bundle.getString(EXTRA_PASSWORD);

      if( !Connect( connection, uname, pwd ) ) {
          Log.e( TAG, "handleActionTestConnect: Connection failed");
          return false;
      }

        CloseConnection();

        Log.v(TAG, "handleActionTestConnect: Test connection succeeded");
        return true;
    }


    /**
     * Handle action Download Data in the provided background thread with the provided
     * parameters.
     */
    private boolean handleActionDownloadData( Bundle bundle , ResultReceiver receiver ) {
        boolean bResult = false;

        final String connection = bundle.getString(EXTRA_CONNECTION);
        final String uname = bundle.getString(EXTRA_USERNAME);
        final String pwd = bundle.getString(EXTRA_PASSWORD);
        final String location = bundle.getString(EXTRA_LOCATION);

        try {
            if( !Connect( connection, uname, pwd ) ) {
                Log.e(TAG, "handleActionDownloadData: Connection failed");
                return false;
            }

            // Select the data from the remote DB and add to the local DB
            DatabaseHandler dbLocal = DatabaseHandler.getInstance(this);

            // Clear the database of all current product data
            dbLocal.emptyProductTable();

            Statement stmt = m_dbConn.createStatement();

            // In future, this is the sort of place where getDBVariant would be called to see if we need
            // to modify the select statement to suit different versions of the database
            String query = "SELECT " +
                    "PRODUCTS.ID AS ID, " +
                    "LOCATIONS.NAME AS LOCATION, " +
                    "PRODUCTS.REFERENCE AS REFERENCE, " +
                    "PRODUCTS.NAME AS NAME, " +
                    "CATEGORIES.NAME AS CATEGORY, " +
                    "PRODUCTS.CODE AS BARCODE, " +
                    "PRODUCTS.PRICEBUY AS BUYPRICE, " +
                    "PRODUCTS.PRICESELL AS SELLPRICE, " +
                    "TAXCATEGORIES.NAME AS TAXCODE, "+
                    "PRODUCTS.IMAGE AS IMAGE, " +
                    "SUM(STOCKCURRENT.UNITS) AS INSTOCK, " +
                    "COALESCE(STOCKLEVEL.STOCKSECURITY, 0) AS STOCKMINIMUM, " +
                    "COALESCE(STOCKLEVEL.STOCKMAXIMUM, 0) AS STOCKMAXIMUM " +
                    "FROM STOCKCURRENT " +
                    "JOIN LOCATIONS ON STOCKCURRENT.LOCATION = LOCATIONS.ID " +
                    "JOIN PRODUCTS ON STOCKCURRENT.PRODUCT = PRODUCTS.ID " +
                    "JOIN CATEGORIES ON PRODUCTS.CATEGORY = CATEGORIES.ID " +
                    "JOIN TAXCATEGORIES ON PRODUCTS.TAXCAT = TAXCATEGORIES.ID " +
                    "LEFT OUTER JOIN STOCKLEVEL ON STOCKCURRENT.LOCATION = STOCKLEVEL.LOCATION AND STOCKCURRENT.PRODUCT = STOCKLEVEL.PRODUCT ";
            if(!TextUtils.isEmpty(location) ) {
                query = query + "WHERE LOCATIONS.NAME = '" + location + "' ";
            }
            query = query + "GROUP BY STOCKCURRENT.LOCATION, LOCATIONS.NAME, PRODUCTS.REFERENCE, PRODUCTS.NAME, PRODUCTS.CATEGORY, CATEGORIES.NAME, " +
                    "PRODUCTS.PRICEBUY, PRODUCTS.PRICESELL, PRODUCTS.STOCKVOLUME, PRODUCTS.STOCKCOST, STOCKLEVEL.STOCKSECURITY, STOCKLEVEL.STOCKMAXIMUM " +
                    "ORDER BY STOCKCURRENT.LOCATION, CATEGORIES.NAME, PRODUCTS.NAME";

            ResultSet rs = stmt.executeQuery( query );
            int ticker = 500;

            while(rs.next()) {
                Bundle values = new Bundle();
                int index=1;
                values.putString(StockProduct.CHROMISID, rs.getString(index++));
                values.putString(StockProduct.LOCATION, rs.getString(index++));
                values.putString(StockProduct.REFERENCE, rs.getString(index++));
                values.putString(StockProduct.NAME, rs.getString(index++));
                values.putString(StockProduct.CATEGORY, rs.getString(index++));
                values.putString(StockProduct.BARCODE, rs.getString(index++));
                values.putDouble(StockProduct.BUYPRICE, rs.getDouble(index++));
                values.putDouble(StockProduct.SELLPRICE, rs.getDouble(index++));
                values.putString(StockProduct.TAXCODE, rs.getString(index++));
                values.putByteArray(StockProduct.IMAGE, rs.getBytes(index++));
                values.putDouble(StockProduct.QTY_INSTOCK, rs.getDouble(index++));
                values.putDouble(StockProduct.QTY_MIN,rs.getDouble(index++));
                values.putDouble(StockProduct.QTY_MAX, rs.getDouble(index++));
                dbLocal.addProduct( new StockProduct( values ), true, true );

                if( --ticker == 0 ) {
                    // Send keep alive messages to caller
                    receiver.send(STATUS_RUNNING, bundle);
                    ticker = 500;
                }
            }
            rs.close();
            stmt.close();
            bResult = true;

            Log.v(TAG, "handleActionDownloadData: Download completed");

        } catch (java.sql.SQLException e) {
            m_LastError =  Log.getStackTraceString(e);
            Log.e(TAG, m_LastError);
        }

        CloseConnection();

        return bResult;
    }
}
