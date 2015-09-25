package com.app_software.chromisstock.chromisstock;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;
import com.app_software.chromisstock.chromisstock.Data.StockProduct;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by John on 18/09/2015.
 */
public class DatabaseHandler extends SQLiteOpenHelper implements DownloadResultReceiver.Receiver {

    String TAG = "DatabaseHandler";

    // Database Version
    private static final int DATABASE_VERSION = 1;

    // Database Name
    private static final String DATABASE_NAME = "ChromisStock";

    // Contacts table name
    private static final String TABLE_PRODUCTS = "PRODUCTS";

    private Context m_Context;
    private DownloadResultReceiver m_Receiver;

    private Toast m_toaster;

    public interface DataChangeNotify {
        public void NotifyDataChanged();
    }

    private List<DataChangeNotify> m_NotifyList = new ArrayList<DataChangeNotify>();
    public void addListChangeNotify(DataChangeNotify receiver) {
        m_NotifyList.add( receiver );
    }

    protected void NotifyDataChanged() {
        Iterator<DataChangeNotify> iterator = m_NotifyList.iterator();
        while (iterator.hasNext()) {
            iterator.next().NotifyDataChanged();
        }
    }


    public interface DownloadProgressReceiver {
        public void DownloadProgressReceiver( String Msg, boolean bFinished );
    }

    private List<DownloadProgressReceiver> m_ProgressReceivers = new ArrayList<DownloadProgressReceiver>();
    public void addDownloadProgressReceiver(DownloadProgressReceiver receiver) {
        m_ProgressReceivers.add( receiver );
    }

    protected void NotifyDownloadProgress( String msg, boolean bfinished ) {
        Iterator<DownloadProgressReceiver> iterator = m_ProgressReceivers.iterator();
        while (iterator.hasNext()) {
            iterator.next().DownloadProgressReceiver(msg, bfinished);
        }
    }

    public DatabaseHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        m_Context = context;
    }

    String[] m_ProductFields = new String [] {
            StockProduct.ID,
            StockProduct.CHROMISID,
            StockProduct.NAME,
            StockProduct.REFERENCE,
            StockProduct.CATEGORY,
            StockProduct.BARCODE,
            StockProduct.LOCATION,
            StockProduct.TAXCODE,
            StockProduct.BUYPRICE,
            StockProduct.SELLPRICE,
            StockProduct.QTY_INSTOCK,
            StockProduct.QTY_MIN,
            StockProduct.QTY_MAX,
            StockProduct.IMAGE
    };


    private Bundle FieldsToBundle( Cursor cursor ) {
        Bundle values = new Bundle();
        int index = 0;
        values.putLong(StockProduct.ID, cursor.getLong(index++));
        values.putString(StockProduct.CHROMISID, cursor.getString(index++));
        values.putString(StockProduct.NAME, cursor.getString(index++));
        values.putString(StockProduct.REFERENCE, cursor.getString(index++));
        values.putString(StockProduct.CATEGORY, cursor.getString(index++));
        values.putString(StockProduct.BARCODE, cursor.getString(index++));
        values.putString(StockProduct.LOCATION, cursor.getString(index++));
        values.putString(StockProduct.TAXCODE, cursor.getString(index++));
        values.putDouble(StockProduct.BUYPRICE, cursor.getDouble(index++));
        values.putDouble(StockProduct.SELLPRICE, cursor.getDouble(index++));
        values.putDouble(StockProduct.QTY_INSTOCK, cursor.getDouble(index++));
        values.putDouble(StockProduct.QTY_MIN, cursor.getDouble(index++));
        values.putDouble(StockProduct.QTY_MAX, cursor.getDouble(index++));
        values.putByteArray(StockProduct.IMAGE, cursor.getBlob(index++));

        return values;
    }

    private ContentValues BundleToContentValues( Bundle bundle ) {
        ContentValues values = new ContentValues();

        values.put(StockProduct.ID, bundle.getLong(StockProduct.ID));
        values.put(StockProduct.CHROMISID, bundle.getString(StockProduct.CHROMISID));
        values.put(StockProduct.NAME, bundle.getString(StockProduct.NAME));
        values.put(StockProduct.REFERENCE, bundle.getString(StockProduct.REFERENCE));
        values.put(StockProduct.CATEGORY, bundle.getString(StockProduct.CATEGORY));
        values.put(StockProduct.BARCODE, bundle.getString(StockProduct.BARCODE));
        values.put(StockProduct.LOCATION, bundle.getString(StockProduct.LOCATION));
        values.put(StockProduct.TAXCODE, bundle.getString(StockProduct.TAXCODE));
        values.put(StockProduct.BUYPRICE, bundle.getDouble(StockProduct.BUYPRICE));
        values.put(StockProduct.SELLPRICE, bundle.getDouble(StockProduct.SELLPRICE));
        values.put(StockProduct.QTY_INSTOCK, bundle.getDouble(StockProduct.QTY_INSTOCK));
        values.put(StockProduct.QTY_MIN, bundle.getDouble(StockProduct.QTY_MIN));
        values.put(StockProduct.QTY_MAX, bundle.getDouble(StockProduct.QTY_MAX));
        values.put(StockProduct.IMAGE, bundle.getByteArray(StockProduct.IMAGE));

        return values;
    }

     // Creating Tables
    @Override
    public void onCreate(SQLiteDatabase db) {
        createTables(db);
    }

    // Upgrading database
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older table if existed
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PRODUCTS);

        // Create tables again
        onCreate(db);
    }

    public void createTables(SQLiteDatabase db) {

        if( db == null ) {
            db = this.getWritableDatabase();
        }

        String CREATE_PRODUCTS_TABLE = "CREATE TABLE " + TABLE_PRODUCTS + "("
                + StockProduct.ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"  // Local ID - different from ID used in Chromis db
                + StockProduct.CHROMISID + " TEXT,"
                + StockProduct.NAME + " TEXT,"
                + StockProduct.REFERENCE + " TEXT,"
                + StockProduct.CATEGORY + " TEXT,"
                + StockProduct.BARCODE + " TEXT,"
                + StockProduct.LOCATION + " TEXT,"
                + StockProduct.TAXCODE + " TEXT,"
                + StockProduct.BUYPRICE + " DOUBLE,"
                + StockProduct.SELLPRICE + " DOUBLE,"
                + StockProduct.QTY_INSTOCK + " DOUBLE, "
                + StockProduct.QTY_MIN + " DOUBLE, "
                + StockProduct.QTY_MAX + " DOUBLE, "
                + StockProduct.IMAGE + " BLOB "
                + ")";

        db.execSQL(CREATE_PRODUCTS_TABLE);
    }

    public void dropTables(SQLiteDatabase db) {

        if( db == null ) {
            db = this.getWritableDatabase();
        }

        // Drop older table if existed
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PRODUCTS);
    }

    public void emptyTables( SQLiteDatabase db ) {

        if( db == null ) {
            db = this.getWritableDatabase();
        }

        dropTables( db );
        createTables(db);
    }

    public void emptyTables() {
        emptyTables(null);
    }

    private void showToast( String Msg, int duration ) {
        if (m_toaster != null) {
            m_toaster.cancel();
        }

        m_toaster = Toast.makeText( m_Context, Msg, duration );
        m_toaster.show();
    }

    public void ReBuildTables( Context ctx,  SQLiteDatabase db ) {

        if( db == null ) {
            db = this.getWritableDatabase();
        }

        showToast("Database Download Started", Toast.LENGTH_SHORT);

        dropTables(db);

        // Create tables again
        createTables(db);

        m_Receiver = new DownloadResultReceiver(new Handler());
        m_Receiver.setReceiver(this);

        SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(m_Context);
        String connection = SP.getString("database_url", null );
        String user = SP.getString("database_user", null );
        String pwd = SP.getString("database_password", null );

        if( connection == null || user == null || pwd == null ) {
            showToast ("Missing connection settings", Toast.LENGTH_LONG );

            // Fire the settings activity
            Intent intent = new Intent( ctx, SettingsActivity.class);
            m_Context.startActivity( intent );

        } else {
            DownloadStockData.startActionDownloadData( ctx, m_Receiver, connection, user, pwd);
        }

    }

    public void testConnection( Context ctx  ) {

        showToast("Testing connection", Toast.LENGTH_SHORT);
        SQLiteDatabase db = this.getWritableDatabase();

        m_Receiver = new DownloadResultReceiver(new Handler());
        m_Receiver.setReceiver(this);


        SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(m_Context);
        String connection = SP.getString("database_url", null );
        String user = SP.getString("database_user", null );
        String pwd = SP.getString("database_password", null );

        DownloadStockData.startActionTestConnect(ctx, m_Receiver, connection, user, pwd);
    }

    public void ReBuildTables( Context ctx  ) {
        ReBuildTables(ctx, null);
    }

        @Override
    public void onReceiveResult(int resultCode, Bundle resultData) {
        int msgID = R.string.dbstatus_unknown;
        boolean bComplete = false;

        if( resultCode == DownloadStockData.STATUS_DOWNLOAD_FINISHED ) {
            // Need to notify all interested parties the DB content has changed
            msgID = R.string.dbstatus_download_complete;
            bComplete = true;
            NotifyDataChanged();
        } else if( resultCode == DownloadStockData.STATUS_CONNECTION_OK ) {
            msgID = R.string.dbstatus_connection_ok;
            bComplete = true;
        } else if( resultCode == DownloadStockData.STATUS_ERROR ) {
            msgID = R.string.dbstatus_communication_error;
            Log.e(TAG, "DB Communications failed " + resultData.toString());
            bComplete = true;
        } else if( resultCode == DownloadStockData.STATUS_RUNNING ) {
            msgID = R.string.dbstatus_coomunicating;
        }

        String msg = m_Context.getResources().getString(msgID);
            showToast( msg, Toast.LENGTH_LONG);
        NotifyDownloadProgress(msg, bComplete);
    }

    // Adding new contact
    public void addProduct(StockProduct product) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = BundleToContentValues(product.getValues());
        values.remove(StockProduct.ID);

        // Inserting Row
        db.insert(TABLE_PRODUCTS, null, values);
        db.close(); // Closing database connection

        NotifyDataChanged();

    }

    // Getting single product by barcode
    public StockProduct lookupBarcode( String barcode ) {
        StockProduct product = null;

        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_PRODUCTS, m_ProductFields, StockProduct.BARCODE + "=?", new String[]{ barcode }, null, null, null, null);

        if (cursor != null) {

            if( cursor.moveToFirst() ) {
                product = new StockProduct(FieldsToBundle(cursor));
            }

            cursor.close();
        }

        return product;
    }

    // Getting single product by database ID
    public StockProduct getProduct( Long id) {
        StockProduct product = null;

        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_PRODUCTS, m_ProductFields, StockProduct.ID + "=?", new String[]{id.toString()}, null, null, null, null);

        if (cursor != null) {

            if( cursor.moveToFirst() ) {
                product = new StockProduct(FieldsToBundle(cursor));
            }

            cursor.close();
        }

        return product;
    }

    // Getting All Products
    public List<StockProduct> getAllProducts() {

        List<StockProduct> productlist = new ArrayList<StockProduct>();

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_PRODUCTS, m_ProductFields, null, null, null, null, null, null);

        if (cursor != null) {
            // looping through all rows and adding to list
            if (cursor.moveToFirst()) {
                do {
                    productlist.add(new StockProduct(FieldsToBundle(cursor)));
                } while (cursor.moveToNext());
            }
            cursor.close();
        }

        // return product list
        return productlist;
    }

    // Getting All Products
    public Cursor getProductCursor( String Filter, String [] FilterArgs, String OrderBy ) {
        Cursor cursor = null;

        try {
            SQLiteDatabase db = this.getReadableDatabase();
            cursor = db.query(TABLE_PRODUCTS, m_ProductFields, Filter, FilterArgs, null, null, OrderBy, null);
        } catch ( SQLiteException e) {
            Log.d(TAG, e.toString());
        }

        return cursor;
    }

    // Getting All Products
    public Cursor getProductCursor( ) {

        return (getProductCursor(null, null, null));
    }

    // Getting products Count
    public int getProductsCount() {
        int count = 0;

        String countQuery = "SELECT  * FROM " + TABLE_PRODUCTS;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(countQuery, null);

        if (cursor != null) {
            count = cursor.getCount();
            cursor.close();
        }

        // return count
        return count;
    }

    // Updating single product
    public int updateProduct(StockProduct product) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = BundleToContentValues(product.getValues());
        Long id = product.getID();
        values.remove(StockProduct.ID);

        // updating row
        int ret = db.update(TABLE_PRODUCTS, values, StockProduct.ID  + " = ?",
                new String[] { id.toString() } );

        NotifyDataChanged();

        return ret;
    }

    // Deleting single product
    public void deleteProduct(StockProduct product) {

        SQLiteDatabase db = this.getWritableDatabase();
        Long id = product.getID();

        db.delete(TABLE_PRODUCTS,  StockProduct.ID + " = ?",
                new String[]{ id.toString() });

        NotifyDataChanged();

    }

}
