package com.app_software.chromisstock.chromisstock;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.Toast;

import com.app_software.chromisstock.chromisstock.Data.StockProduct;
import com.google.zxing.integration.android.ScannerIntegrator;
import com.google.zxing.integration.android.ScannerResult;


/**
 * An activity representing a list of Products. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a {@link ProductDetailActivity} representing
 * item details. On tablets, the activity presents the list of items and
 * item details side-by-side using two vertical panes.
 * <p/>
 * The activity makes heavy use of fragments. The list of items is a
 * {@link ProductListFragment} and the item details
 * (if present) is a {@link ProductDetailFragment}.
 * <p/>
 * This activity also implements the required
 * {@link ProductListFragment.Callbacks} interface
 * to listen for item selections.
 */
public class ProductListActivity extends AppCompatActivity
        implements ProductListFragment.Callbacks {

    private static String TAG = "ProductListActivity";

    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    private boolean mTwoPane;
    private ProductListFragment m_ListFragment = null;

    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        getSupportFragmentManager().putFragment(outState, "m_ListFragment", m_ListFragment);
    }

    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        m_ListFragment = (ProductListFragment) getSupportFragmentManager().getFragment( savedInstanceState, "m_ListFragment");

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_product_list);

        // Restore state
        if (savedInstanceState != null) {
           m_ListFragment = (ProductListFragment) getSupportFragmentManager().getFragment( savedInstanceState, "m_ListFragment");
        } else {

            // If our layout has a container for the image selector fragment,
            // create and add it
            ViewGroup layout = (ViewGroup) findViewById(R.id.product_list);
            if (layout != null) {
                Bundle args = new Bundle();

                Intent i = getIntent();
                if( i != null ) {
                    if (Intent.ACTION_SEARCH.equals(i.getAction())) {
                        String search =  i.getStringExtra(SearchManager.QUERY);
                        args.putString( ProductListFragment.ARG_SEARCH, search );
                    }
                }

                // Add image selector fragment to the activity's container layout
                m_ListFragment = new ProductListFragment();
                m_ListFragment.setArguments(args);

                FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
                fragmentTransaction.replace(layout.getId(), m_ListFragment,
                        ProductListFragment.class.getName());

                // Commit the transaction
                fragmentTransaction.commit();
            }
        }

        if (findViewById(R.id.product_detail_container) != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-large and
            // res/values-sw600dp). If this view is present, then the
            // activity should be in two-pane mode.
            mTwoPane = true;

            // In two-pane mode, list items should be given the
            // 'activated' state when touched.
            m_ListFragment.setActivateOnItemClick(true);
        }

    }

    @Override
    public void onNewIntent(Intent intent) {  //calls twice
        super.onNewIntent(intent);
        Log.v(TAG, "onNewIntent");
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String search =  intent.getStringExtra(SearchManager.QUERY);
            Log.v(TAG, "Searching for: " + search);

            m_ListFragment.setSearch(  search  );
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_app, menu);

        // Associate searchable configuration with the SearchView
        SearchManager searchManager =
                (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView =
                (SearchView) menu.findItem(R.id.product_search).getActionView();
        searchView.setSearchableInfo(
                searchManager.getSearchableInfo(getComponentName()));

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                m_ListFragment.setSearch((query));
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                 m_ListFragment.setSearch((newText));
                return true;
            }
        });

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.settings:
                doSettings();
                return true;
            case R.id.fetch_products:
                DatabaseHandler db = DatabaseHandler.getInstance( this );
                db.ReBuildProductTable(this);
                return true;
            case R.id.send_updates:
                Toast.makeText( this, "Not yet implementated", Toast.LENGTH_SHORT ).show();
                return true;
            case R.id.use_scanner:
                ScannerIntegrator scanner = new ScannerIntegrator(this);
                scanner.initiateScan();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void doSettings() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    /**
     * Callback method from {@link ProductListFragment.Callbacks}
     * indicating that the item with the given ID was selected.
     */
    @Override
    public void onItemSelected(Long id) {
        if (mTwoPane) {
            // In two-pane mode, show the detail view in this activity by
            // adding or replacing the detail fragment using a
            // fragment transaction.
            Bundle arguments = new Bundle();
            arguments.putLong(ProductDetailFragment.ARG_ITEM_ID, id);
            ProductDetailFragment fragment = new ProductDetailFragment();
            fragment.setArguments(arguments);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.product_detail_container, fragment)
                    .commit();

        } else {
            // In single-pane mode, simply start the detail activity
            // for the selected item ID.
            Intent detailIntent = new Intent(this, ProductDetailActivity.class);
            detailIntent.putExtra(ProductDetailFragment.ARG_ITEM_ID, id);
            startActivity(detailIntent);
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
       ScannerResult scanResult = ScannerIntegrator.parseActivityResult(requestCode, resultCode, intent);
       if (scanResult != null) {
           String code = scanResult.getContents();

           if( !TextUtils.isEmpty(code) ) {
               // is this an existing barcode ?
               DatabaseHandler db = DatabaseHandler.getInstance(this);
               StockProduct product = db.lookupBarcode(code);
               if (product != null) {
                   // Directly load the product details activity
                   onItemSelected(product.getID());
               } else {
                   // Use the scanned data in a new search fragment
                   Log.v( TAG, "Barcode not in system, starting search");
                   Intent i = new Intent(this, ProductListActivity.class);
                   i.putExtra(SearchManager.QUERY, code);
                   i.setAction( Intent.ACTION_SEARCH );
                   startActivity(i);
               }
           }
       }
    }
}
