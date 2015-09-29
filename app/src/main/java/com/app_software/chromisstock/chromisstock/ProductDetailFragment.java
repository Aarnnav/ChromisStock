package com.app_software.chromisstock.chromisstock;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.app_software.chromisstock.chromisstock.Data.ProductList;
import com.app_software.chromisstock.chromisstock.Data.StockProduct;

/**
 * A fragment representing a single Product detail screen.
 * This fragment is either contained in a {@link ProductListActivity}
 * in two-pane mode (on tablets) or a {@link ProductDetailActivity}
 * on handsets.
 */
public class ProductDetailFragment extends Fragment {
    /**
     * The fragment argument representing the item ID that this fragment
     * represents.
     */
    public static final String ARG_ITEM_ID = StockProduct.ID;

    /**
     * The content this fragment is presenting.
     */
    private StockProduct mItem;
    private DatabaseHandler m_db;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ProductDetailFragment() {
    }

    public void setDatabase( DatabaseHandler db ) {
        m_db = db;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(ARG_ITEM_ID)) {
            if( m_db == null ) {
                m_db = DatabaseHandler.getInstance( getActivity() );
            }

            Long id = getArguments().getLong(ARG_ITEM_ID);
            mItem = m_db.getProduct( id );
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_product_detail, container, false);

        // Show the dummy content as text in a TextView.
        if (mItem != null) {
            ((EditText) rootView.findViewById(R.id.edit_name)).setText(mItem.getValueString(StockProduct.NAME));
            ((EditText) rootView.findViewById(R.id.edit_reference)).setText(mItem.getValueString(StockProduct.REFERENCE));
            ((EditText) rootView.findViewById(R.id.edit_barcode)).setText(mItem.getValueString(StockProduct.BARCODE));
            ((EditText) rootView.findViewById(R.id.edit_instock)).setText( String.format("%.0f", mItem.getValueDouble(StockProduct.QTY_INSTOCK)) );
            ((EditText) rootView.findViewById(R.id.edit_minqty)).setText( String.format("%.0f", mItem.getValueDouble(StockProduct.QTY_MIN)) );
            ((EditText) rootView.findViewById(R.id.edit_maxqty)).setText( String.format("%.0f", mItem.getValueDouble(StockProduct.QTY_MAX)) );
            ((EditText) rootView.findViewById(R.id.edit_pricebuy)).setText( String.format("%.2f", mItem.getValueDouble(StockProduct.BUYPRICE)) );
            ((EditText) rootView.findViewById(R.id.edit_pricesell)).setText(String.format("%.2f", mItem.getValueDouble(StockProduct.SELLPRICE)));
            ((TextView) rootView.findViewById(R.id.txt_location)).setText(mItem.getValueString(StockProduct.LOCATION));

            byte [] image = mItem.getValueByteArray(StockProduct.IMAGE);
            if( image != null ) {
                Bitmap bmp = BitmapFactory.decodeByteArray(image, 0, image.length);
                ((ImageView) rootView.findViewById(R.id.iv_image) ).setImageBitmap(bmp);
            }
        }

        return rootView;
    }
}
