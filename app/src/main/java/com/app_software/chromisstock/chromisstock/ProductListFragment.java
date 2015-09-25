package com.app_software.chromisstock.chromisstock;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.app_software.chromisstock.chromisstock.Data.ProductList;
import com.app_software.chromisstock.chromisstock.Data.StockProduct;


/**
 * A list fragment representing a list of Products. This fragment
 * also supports tablet devices by allowing list items to be given an
 * 'activated' state upon selection. This helps indicate which item is
 * currently being viewed in a {@link ProductDetailFragment}.
 * <p/>
 * Activities containing this fragment MUST implement the {@link Callbacks}
 * interface.
 */
public class ProductListFragment extends ListFragment implements DatabaseHandler.DataChangeNotify
{

    private static String TAG = "ProductListFragment";

    public static final String ARG_SEARCH = "SEARCH";

    /**
     * The serialization (saved instance state) Bundle key representing the
     * activated item position. Only used on tablets.
     */
    private static final String STATE_ACTIVATED_POSITION = "activated_position";

    /**
     * The fragment's current callback object, which is notified of list item
     * clicks.
     */
    private Callbacks mCallbacks = sCallbacks;
    DatabaseHandler m_db;

    /**
     * The current activated item position. Only used on tablets.
     */
    private int mActivatedPosition = ListView.INVALID_POSITION;

    private ProductListCursorAdaptor m_Adaptor;
    private String m_Search;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ProductListFragment() {
    }

    public void setSearch( String search ) {
        m_Search = search;
        Log.v( TAG, "New Search: " + search );

        setNewListAdaptor();
    }

    public String getSearch() {
        return m_Search;
    }

    private void setNewListAdaptor() {

        if( m_db == null ) {
            m_db = new DatabaseHandler( getActivity() );
        }

        String select = null;
        String [] args = null;
        String partial = "%" + m_Search + "%";

        if ( !TextUtils.isEmpty( m_Search ) ) {
            select =  StockProduct.BARCODE + " = ? OR " + StockProduct.CHROMISID + " = ? OR " + StockProduct.NAME + " LIKE ?";
            args = new String[]{m_Search, m_Search, partial};
        }

        Cursor curs = m_db.getProductCursor( select, args, StockProduct.NAME);
        if( curs == null ) {
            Toast.makeText( getContext(), "Database error - rebuilding", Toast.LENGTH_LONG).show();

            m_db.ReBuildTables( getContext() );
        } else {

            m_Adaptor = new ProductListCursorAdaptor(getActivity(), curs);
            setListAdapter(m_Adaptor);
        }

        String noItems = null;
        if( m_Adaptor == null ) {
            noItems = getString(R.string.no_items);
        } else if( m_Adaptor.getCount() == 0 ) {
            // Set an empty list
            if( !TextUtils.isEmpty( m_Search ) ) {
                noItems = getString(R.string.no_match) + " " + m_Search;
            } else {
                noItems = getString(R.string.no_items);
            }
        }

        if( !TextUtils.isEmpty( noItems ) ) {
            setEmptyText(  noItems );
            setListAdapter(new ArrayAdapter(getActivity(), R.layout.product_listitem));
        }
    }

    @Override
    public void NotifyDataChanged() {
        // Database contents have changed
        setNewListAdaptor();
    }

    /**
     * A callback interface that all activities containing this fragment must
     * implement. This mechanism allows activities to be notified of item
     * selections.
     */
    public interface Callbacks {
        /**
         * Callback for when an item has been selected.
         */
        public void onItemSelected( Long id);
    }

    /**
     * A dummy implementation of the {@link Callbacks} interface that does
     * nothing. Used only when this fragment is not attached to an activity.
     */
    private static Callbacks sCallbacks = new Callbacks() {
        @Override
        public void onItemSelected( Long id) {
        }
    };


    public void setDatabase( DatabaseHandler db ) {
        m_db = db;
        m_db.addListChangeNotify(this);

        setNewListAdaptor();
    }

    public DatabaseHandler getDatabase( ) {
        return m_db;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            m_Search = savedInstanceState.getString("m_Search");
            Log.d(TAG, "onCreate: Restored search " + m_Search);
        } else {

            if (getArguments() != null) {
                if (getArguments().containsKey(ARG_SEARCH)) {
                    m_Search = getArguments().getString(ARG_SEARCH);
                }
            }
        }

        setNewListAdaptor();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Restore the previously serialized activated item position.
        if (savedInstanceState != null
                && savedInstanceState.containsKey(STATE_ACTIVATED_POSITION)) {
            setActivatedPosition(savedInstanceState.getInt(STATE_ACTIVATED_POSITION));
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // Activities containing this fragment must implement its callbacks.
        if (!(activity instanceof Callbacks)) {
            throw new IllegalStateException("Activity must implement fragment's callbacks.");
        }

        mCallbacks = (Callbacks) activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();

        // Reset the active callbacks interface to the dummy implementation.
        mCallbacks = sCallbacks;
    }

    @Override
    public void onListItemClick(ListView listView, View view, int position, long id) {
        super.onListItemClick(listView, view, position, id);

        mCallbacks.onItemSelected(new Long(id));
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mActivatedPosition != ListView.INVALID_POSITION) {
            // Serialize and persist the activated item position.
            outState.putInt(STATE_ACTIVATED_POSITION, mActivatedPosition);
        }

        outState.putString("m_Search", m_Search);
        Log.d(TAG, "Saved search " + m_Search);

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState != null) {
            m_Search = savedInstanceState.getString("m_Search");
            Log.d(TAG, "onActivityCreated: Restored search " + m_Search);
        }
    }

    /**
     * Turns on activate-on-click mode. When this mode is on, list items will be
     * given the 'activated' state when touched.
     */
    public void setActivateOnItemClick(boolean activateOnItemClick) {
        // When setting CHOICE_MODE_SINGLE, ListView will automatically
        // give items the 'activated' state when touched.
        getListView().setChoiceMode(activateOnItemClick
                ? ListView.CHOICE_MODE_SINGLE
                : ListView.CHOICE_MODE_NONE);
    }

    private void setActivatedPosition(int position) {
        if (position == ListView.INVALID_POSITION) {
            getListView().setItemChecked(mActivatedPosition, false);
        } else {
            getListView().setItemChecked(position, true);
        }

        mActivatedPosition = position;
    }
}
