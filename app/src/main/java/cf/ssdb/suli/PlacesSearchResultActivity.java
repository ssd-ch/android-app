package cf.ssdb.suli;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlacesSearchResultActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_places_search_result);

        setTitle(R.string.title_places_search_result);

        Intent intent = getIntent();
        String classname = intent.getStringExtra("data0"); //授業名
        String person = intent.getStringExtra("data1");    //担当者
        String weekday = intent.getStringExtra("data2"); //曜日 -1:指定なし 0〜4:月〜金
        String time = intent.getStringExtra("data3");    //コマ　0:指定なし 1〜7:1〜7コマ

        classname = "%"+classname+"%";
        person = "%"+person+"%";
        if(weekday.equals("-1")) weekday = "%";
        if(time.equals("0")) time = "%";

        ListView listView = (ListView) findViewById(R.id.listView);

        DBAdapter dbAdapter = new DBAdapter(this);
        dbAdapter.openDB();
        Cursor cursor = dbAdapter.searchDB("ClassroomDivide", null,
                "classname like ? and person like ? and weekday like ? and time like ?", new String[]{classname,person,weekday,time});

        if (cursor.moveToFirst()) {

            List<Map<String, String>> dataList = new ArrayList<Map<String, String>>();

            int col_place = cursor.getColumnIndex("place");
            int col_text = cursor.getColumnIndex("cell_text");

            do {
                if(!cursor.getString(col_text).equals("")) {
                    Map<String, String> data = new HashMap<String, String>();
                    data.put("place", cursor.getString(col_place));
                    data.put("text", cursor.getString(col_text));
                    dataList.add(data);
                }
            } while (cursor.moveToNext());
            cursor.close();

            String[] from = {"text","place"};
            int[] to = {android.R.id.text1,android.R.id.text2};
            SimpleAdapter adapter = new SimpleAdapter(this, dataList, android.R.layout.simple_list_item_2, from, to);
            listView.setAdapter(adapter);

        } else {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.dialog_title_search_result)
                    .setMessage(R.string.dialog_message_search_result_place)
                    .setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            //okボタンが押された時の処理
                            finish();
                        }
                    })
                    .show();
        }

        cursor.close();
        dbAdapter.closeDB();
    }
}