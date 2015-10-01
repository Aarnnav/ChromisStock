package com.app_software.chromisstock.chromisstock;


import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.TextView;

import com.app_software.chromisstock.chromisstock.Data.StockProduct;

/**
 * Created by John on 21/09/2015.
 */
public class ChangeListCursorAdaptor extends CursorAdapter {

    private Context m_Context;

        public ChangeListCursorAdaptor(Context context, Cursor cursor) {
            super(context, cursor, 0);
            m_Context = context;
        }

        // The newView method is used to inflate a new view and return it,
        // you don't bind any data to the view at this point.
        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return LayoutInflater.from(context).inflate(R.layout.change_listitem, parent, false);
        }

        // The bindView method is used to bind all data to a given view
        // such as setting the text on a TextView.
        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            // Find fields to populate in inflated template
            TextView tvType = (TextView) view.findViewById(R.id.tvType);
            TextView tvField = (TextView) view.findViewById(R.id.tvField);
            TextView tvValue = (TextView) view.findViewById(R.id.tvValue);
            ImageButton ibDelete = (ImageButton) view.findViewById(R.id.ibDeleteChange);

            ibDelete.setTag( cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHandler.CHANGES_ID)) );

            // Extract properties from cursor
            int type = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHandler.CHANGES_TYPE));
            String field = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHandler.CHANGES_FIELD));
            String value = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHandler.CHANGES_TEXTVALUE));

            int sid = R.string.changetype_none;

            switch( type ) {
                case DatabaseHandler.CHANGETYPE_CHANGEVALUE:
                    sid = R.string.changetype_changevalue;
                    break;
                case DatabaseHandler.CHANGETYPE_NEWVALUE:
                    sid = R.string.changetype_newvalue;
                    break;
                case DatabaseHandler.CHANGETYPE_ADJUSTVALUE:
                    sid = R.string.changetype_adjustvalue;
                    break;
            }

            ibDelete.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    DatabaseHandler db = DatabaseHandler.getInstance(m_Context);
                    db.deleteChange( (Long) v.getTag() );
                }
            });

            // Populate fields with extracted properties
            tvType.setText( context.getResources().getString(sid) );
            tvField.setText(field);
            tvValue.setText(value);
        }
    }
