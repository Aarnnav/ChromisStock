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
import android.text.TextUtils;
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
    private static final int DATABASE_VERSION = 2;

    // Database Name
    private static final String DATABASE_NAME = "ChromisStock";

    // Table names
    private static final String TABLE_PRODUCTS = "PRODUCTS";
    private static final String TABLE_CHANGES = "CHANGES";

    public static final String CHANGES_ID = "_id";
    public static final String CHANGES_PRODUCT = "PRODUCTID";
    public static final String CHANGES_TYPE = "CHANGETYPE";
    public static final String CHANGES_FIELD = "FIELD";
    public static final String CHANGES_TEXTVALUE = "TEXTVALUE";
    public static final String CHANGES_BLOBVALUE = "BLOBVALUE";

    public static final int CHANGETYPE_NONE  = 0;
    public static final int CHANGETYPE_ADJUSTVALUE = 1;
    public static final int CHANGETYPE_CHANGEVALUE = 2;
    public static final int CHANGETYPE_NEWVALUE = 3;

    private Context m_Context;
    private DownloadResultReceiver m_Receiver;
    private Toast m_toaster;

    public interface DataChangeNotify {
        public void NotifyDataChanged();
    }

    private static DatabaseHandler mInstance = null;

    public static DatabaseHandler getInstance(Context ctx) {

        // Use the application context, which will ensure that you
        // don't accidentally leak an Activity's context.
        // See this article for more information: http://bit.ly/6LRzfx
        if (mInstance == null) {
            mInstance = new DatabaseHandler(ctx.getApplicationContext());
        }
        return mInstance;
    }

    /**
     * Constructor should be private to prevent direct instantiation.
     * make call to static factory method "getInstance()" instead.
     */
    private DatabaseHandler(Context ctx) {
        super(ctx, DATABASE_NAME, null, DATABASE_VERSION);
        m_Context = ctx;
    }


    private List<DataChangeNotify> m_NotifyList = new ArrayList<DataChangeNotify>();

    public void addChangeNotify(DataChangeNotify receiver) {
        m_NotifyList.remove(receiver);  // Avoid multiple registrations
        m_NotifyList.add( receiver );
    }

    public void removeChangeNotify(DataChangeNotify receiver) {
        m_NotifyList.remove(receiver);
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
            // NOTE that StockProduct.HASCHANGES not included - this field is generate on the fly
    };


    private Bundle ProductFieldsToBundle( Cursor cursor ) {
        Bundle values = new Bundle();
        int index = 0;
        values.putLong(StockProduct.ID, cursor.getLong(index++));
        String chromisID = cursor.getString(index++);
        values.putString(StockProduct.CHROMISID, chromisID );
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

        // See if there are change records
        values.putBoolean(StockProduct.HASCHANGES, hasChanges(chromisID ) );

        return values;
    }

    private ContentValues ProductBundleToContentValues( Bundle bundle ) {
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
        // NOTE that StockProduct.HASCHANGES not included - this field is generate on the fly

        return values;
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
// Only uncomment the next line if testing
//        emptyTables( db);
    }

     // Creating Tables
    @Override
    public void onCreate(SQLiteDatabase db) {
        createTables(db);
    }

    // Upgrading database
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        // Version 2 introduced a table to hold DB changes
        if( oldVersion == 1 && newVersion == 2) {
            Log.v( TAG, "Database upgrading from version 1 to version 2");
            createChangesTable( db );
            oldVersion = 2;
        }
    }

    public void createTables(SQLiteDatabase db) {
        createProductTable( db );
        createChangesTable(db);
    }

    public void createChangesTable(SQLiteDatabase db) {

        if( db == null ) {
            db = this.getWritableDatabase();
        }

        String CREATE_CHANGES_TABLE = "CREATE TABLE " + TABLE_CHANGES + "("
                + CHANGES_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + CHANGES_PRODUCT + " STRING,"
                + CHANGES_TYPE + " INTEGER,"
                + CHANGES_FIELD + " TEXT,"
                + CHANGES_TEXTVALUE + " TEXT, "
                + CHANGES_BLOBVALUE + " BLOB "
                + ")";

        db.execSQL(CREATE_CHANGES_TABLE);

    }

    public void createProductTable(SQLiteDatabase db) {

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
                // NOTE that StockProduct.HASCHANGES not created - this is generate on the fly
                + ")";

        db.execSQL(CREATE_PRODUCTS_TABLE);

    }

    public void dropTables(SQLiteDatabase db) {

        if( db == null ) {
            db = this.getWritableDatabase();
        }

        // Drop older table if existed
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CHANGES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PRODUCTS);
    }

    public void emptyTables( SQLiteDatabase db ) {

        if( db == null ) {
            db = this.getWritableDatabase();
        }

        dropTables(db);
        createTables(db);
    }

    public void emptyTables() {
        emptyTables(null);
    }

    public void emptyProductTable() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PRODUCTS);
        createProductTable( db );
    }

    private void showToast( String Msg, int duration ) {
        if (m_toaster != null) {
            m_toaster.cancel();
        }

        m_toaster = Toast.makeText( m_Context, Msg, duration );
        m_toaster.show();
    }

    public void ReBuildProductTable( Context ctx,  SQLiteDatabase db ) {

        if( db == null ) {
            db = this.getWritableDatabase();
        }

        showToast("Database Download Started", Toast.LENGTH_SHORT);

        emptyProductTable();

        m_Receiver = new DownloadResultReceiver(new Handler());
        m_Receiver.setReceiver(this);

        SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(m_Context);
        String connection = SP.getString("database_url", null);
        String user = SP.getString("database_user", null );
        String pwd = SP.getString("database_password", null );
        String location = SP.getString("location", null );

        if( connection == null || user == null || pwd == null ) {
            showToast ("Missing connection settings", Toast.LENGTH_LONG );

            // Fire the settings activity
            Intent intent = new Intent( ctx, SettingsActivity.class);
            intent.addFlags( intent.FLAG_ACTIVITY_NEW_TASK );
            m_Context.startActivity( intent );

        } else {
            DownloadStockData.startActionDownloadData( ctx, m_Receiver, connection, user, pwd, location);
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

    public void ReBuildProductTable( Context ctx  ) {
        ReBuildProductTable(ctx, null);
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
            showToast(msg, Toast.LENGTH_LONG);
        NotifyDownloadProgress(msg, bComplete);
    }

    // Adding new contact
    public void addProduct(StockProduct product, boolean bKeepChanges, boolean bNoNotify ) {
        SQLiteDatabase db = this.getWritableDatabase();

        if( bKeepChanges ) {
            // We are to retain any change records so apply them to this product before saving
            applyChanges( product );
        } else {
            deleteChanges( product.getValueString( StockProduct.CHROMISID) );
        }

        ContentValues values = ProductBundleToContentValues(product.getValues());
        values.remove(StockProduct.ID);
        values.remove((StockProduct.HASCHANGES));

        // Inserting Row
        db.insert(TABLE_PRODUCTS, null, values);
        db.close(); // Closing database connection

        if( !bNoNotify )
            NotifyDataChanged();

    }

    // Getting single product by barcode
    public StockProduct lookupBarcode( String barcode ) {
        StockProduct product = null;

        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_PRODUCTS, m_ProductFields, StockProduct.BARCODE + "=?", new String[]{ barcode }, null, null, null, null);

        if (cursor != null) {

            if( cursor.moveToFirst() ) {
                product = new StockProduct(ProductFieldsToBundle(cursor));
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
                product = new StockProduct(ProductFieldsToBundle(cursor));
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
                    productlist.add(new StockProduct(ProductFieldsToBundle(cursor)));
                } while (cursor.moveToNext());
            }
            cursor.close();
        }

        // return product list
        return productlist;
    }

    // Getting a minimal set of product attributes for a product list view
    public Cursor getProductListCursor( String Filter, String [] FilterArgs, String OrderBy ) {
        Cursor cursor = null;

        try {
            SQLiteDatabase db = this.getReadableDatabase();
            cursor = db.query(TABLE_PRODUCTS,  new String [] {StockProduct.ID},
                    Filter, FilterArgs, null, null, OrderBy, null);

        } catch ( SQLiteException e) {
            Log.d(TAG, e.toString());
        }

        return cursor;
    }

    // Getting selected Products
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

        ContentValues values = ProductBundleToContentValues(product.getValues());
        Long id = product.getID();
        values.remove(StockProduct.ID);

        // updating row
        int ret = db.update(TABLE_PRODUCTS, values, StockProduct.ID + " = ?",
                new String[]{id.toString()});

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

    public boolean isNumberField( String field ) {

        if( TextUtils.isEmpty( field ) ) {
            return false;
        }

        if( field.compareTo( StockProduct.QTY_INSTOCK ) == 0 ||
                field.compareTo( StockProduct.BUYPRICE ) == 0 ||
                field.compareTo( StockProduct.QTY_MAX ) == 0 ||
                field.compareTo( StockProduct.QTY_MIN ) == 0 ||
                field.compareTo( StockProduct.SELLPRICE ) == 0 ) {
            return true;
        } else {
            return false;
        }
    }

    // Add a stock level adjustment change
    public void addChange( String chromisID, int changeType, String field, String value ) {

        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put( CHANGES_PRODUCT, chromisID );
        values.put(CHANGES_TYPE, changeType );
        values.put(CHANGES_FIELD, field );
        values.put(CHANGES_TEXTVALUE, value );

        // First delete any existing changes on the same field (over-write)
        db.execSQL( "DELETE FROM " + TABLE_CHANGES + " WHERE "
                + CHANGES_PRODUCT + "='" + chromisID + "' AND "
                + CHANGES_FIELD + "='" + field + "'"  );

        // updating row
        long ret = db.insert(TABLE_CHANGES, null, values);

        NotifyDataChanged();
    }

    // Apply any changes found in the Changes table to the given StockProduct
    public void applyChanges( StockProduct product ) {

        Cursor c = getChangesCursor(product.getValueString(StockProduct.CHROMISID));
        if( c != null ) {

            int colType = c.getColumnIndexOrThrow(CHANGES_TYPE);
            int colField = c.getColumnIndexOrThrow(CHANGES_FIELD);
            int colTextValue = c.getColumnIndexOrThrow(CHANGES_TEXTVALUE);
            int colBlobValue = c.getColumnIndexOrThrow(CHANGES_BLOBVALUE);

            while (c.moveToNext()) {
                int changeType = c.getInt(colType );
                String field =  c.getString( colField );

                switch(changeType ) {
                    case CHANGETYPE_CHANGEVALUE:
                    case CHANGETYPE_NEWVALUE:
                        if( isNumberField( field ) ) {
                            Double value = Double.valueOf(c.getString(colTextValue));
                            product.setValueDouble( field, value);
                        } else {
                            product.setValueString( field, c.getString(colTextValue) );
                        }
                        break;

                    case CHANGETYPE_ADJUSTVALUE:
                        Double value = product.getValueDouble( field );  // Use field name in change record
                        value += Double.valueOf(c.getString(colTextValue));                 // Use adjustment in change record
                        product.setValueDouble( field, value);
                        break;

                    default:
                        break;
                }
            }
            c.close();
        }
    }

    // Does the StockProduct have any change records
    public boolean hasChanges( String chromisID ) {
        boolean bRet = false;

        Cursor c = getChangesCursor( chromisID );
        if( c != null ) {
            if (c.moveToNext()) {
                bRet = true;
            }
            c.close();
        }

        return bRet;
    }

    // Delete a specific change record
    public void deleteChange( Long changeID  ) {
        SQLiteDatabase db = this.getWritableDatabase();

        try {
            String query = "DELETE FROM " + TABLE_CHANGES + " WHERE " + CHANGES_ID + "=" + changeID;
            db.execSQL(query);
        } catch ( SQLiteException e) {
            Log.d(TAG, e.toString());
        }

        NotifyDataChanged();

    }

    // Delete anychange records for this product
    public void deleteChanges( String productID  ) {
        SQLiteDatabase db = this.getWritableDatabase();

        try {
            String query = "DELETE FROM " + TABLE_CHANGES + " WHERE " + CHANGES_PRODUCT + "='" + productID + "'";
            db.execSQL(query);
        } catch ( SQLiteException e) {
            Log.d(TAG, e.toString());
        }

        NotifyDataChanged();

    }

    String[] m_ChangeFields = new String [] {
            CHANGES_ID,
            CHANGES_PRODUCT,
            CHANGES_TYPE,
            CHANGES_FIELD,
            CHANGES_TEXTVALUE,
            CHANGES_BLOBVALUE
    };

    // Getting a cursor for any changes
    public Cursor getChangesCursor( String Filter, String [] FilterArgs, String OrderBy   ) {
        Cursor cursor = null;

        try {
            SQLiteDatabase db = this.getReadableDatabase();
            cursor = db.query(TABLE_CHANGES, m_ChangeFields, Filter, FilterArgs, null, null, OrderBy, null);
        } catch ( SQLiteException e) {
            Log.d(TAG, e.toString());
        }

        return cursor;
    }

    // Getting a cursor for any changes on the given Chrmois product id
    public Cursor getChangesCursor( String chromisProductID   ) {
        Cursor cursor = null;

        try {
            SQLiteDatabase db = this.getReadableDatabase();
            cursor = db.query(TABLE_CHANGES, m_ChangeFields,
                    CHANGES_PRODUCT + "='" + chromisProductID + "'", null, null, null, null, null);
        } catch ( SQLiteException e) {
            Log.d(TAG, e.toString());
        }

        return cursor;
    }

}
