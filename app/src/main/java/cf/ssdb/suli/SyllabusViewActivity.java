package cf.ssdb.suli;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ListView;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

public class SyllabusViewActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_syllabus_view);

        setTitle(R.string.title_syllabus_view);

        Intent intent = getIntent();
        String url = intent.getStringExtra("url");

        //コンストラクタの引数に表示させるパーツを指定
        SyllabusInfoReceiver receiver = new SyllabusInfoReceiver(this);
        //非同期処理を行う
        receiver.execute(url);
    }

    private class SyllabusInfoReceiver extends AsyncTask<String, String, Elements> {
        private Activity parent_activity;

        public SyllabusInfoReceiver(Activity activity){
            parent_activity = activity ;
        }

        @Override
        public Elements doInBackground(String... params) {
            String url = params[0]; //URL
            try {

                Document doc = Jsoup.connect(url).get();
                Elements data = doc.select("table");
                return data;

            } catch (Exception e) {

                return null;
            }
        }

        public void onPostExecute(Elements result) {

            if(result==null){
                new AlertDialog.Builder(SyllabusViewActivity.this)
                        .setTitle(R.string.error_title_data_response)
                        .setMessage(R.string.error_message_response_refuse)
                        .setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener(){
                            public void onClick(DialogInterface dialog, int which) {
                                //okボタンが押された時の処理
                                SyllabusViewActivity.this.finish();
                            }
                        })
                        .show();

            }else {

                ListView listview = (ListView) parent_activity.findViewById(R.id.listView1);

                List<SectionHeaderData> sectionList = new ArrayList<SectionHeaderData>();
                List<List<SectionRowData>> rowList = new ArrayList<List<SectionRowData>>();

                //基本データ
                sectionList.add(new SectionHeaderData(getResources().getString(R.string.syllabus_basic), ""));
                List<SectionRowData> sectionDatalist = new ArrayList<SectionRowData>();
                Elements tdData = result.select("table.normal").get(0).select("td");
                Elements thData = result.select("table.normal").get(0).select("th");
                for (int i = 0; i < tdData.size(); i++) {
                    sectionDatalist.add(new SectionRowData(thData.get(i).text(), tdData.get(i).text(), ""));
                }
                rowList.add(sectionDatalist);

                //講義内容
                sectionList.add(new SectionHeaderData(getResources().getString(R.string.syllabus_contents), ""));
                List<SectionRowData> sectionDatalist2 = new ArrayList<SectionRowData>();
                Elements tdData2 = result.select("table.normal").get(1).select("td");
                Elements thData2 = result.select("table.normal").get(1).select("th");
                final String BR = System.getProperty("line.separator");
                for (int i = 0; i < tdData2.size(); i++) {
                    String str = tdData2.get(i).html().replaceAll("<br>", BR);
                    sectionDatalist2.add(new SectionRowData(thData2.get(i).text(), str.replaceAll("<.+?>",""),""));
                }
                rowList.add(sectionDatalist2);

                //担当教員
                sectionList.add(new SectionHeaderData(getResources().getString(R.string.syllabus_teacher_list), ""));
                Elements tdData3 = result.select("table.normal").get(2).select("td");
                List<SectionRowData> sectionDatalist3 = new ArrayList<SectionRowData>();
                for (int i = 0; i < tdData3.size(); i += 2) {
                    sectionDatalist3.add(new SectionRowData(tdData3.get(i).text(), tdData3.get(i + 1).text(), ""));
                }
                rowList.add(sectionDatalist3);

                //講義場所
                DBAdapter dbAdapter = new DBAdapter(parent_activity);
                dbAdapter.openDB();

                String where = "( classname like ? or person like ? ) and " + CreateWhereTime(tdData.get(7).text());

                Cursor cursor = dbAdapter.searchDB("ClassroomDivide", null, where, new String[]
                        {"%"+tdData.get(4).text()+"%" , "%"+tdData.get(10).text().replaceAll("　.*","")+"%"});

                String place = getString(R.string.syllabus_place_not_found);
                String message = getString(R.string.syllabus_place_not_found_message);

                sectionList.add(new SectionHeaderData(getResources().getString(R.string.syllabus_place), ""));
                List<SectionRowData> sectionDatalist0 = new ArrayList<SectionRowData>();

                if(cursor.moveToFirst()) {
                    do {
                        place = cursor.getString(cursor.getColumnIndex("place"));
                        message = (getResources().getStringArray(R.array.weekday))[Integer.valueOf(cursor.getString(cursor.getColumnIndex("weekday")))]
                                + " " + (getResources().getStringArray(R.array.time_period))[Integer.valueOf(cursor.getString(cursor.getColumnIndex("time")))-1];
                        if (cursor.getString(cursor.getColumnIndex("person")).matches(".*" + tdData.get(10).text().replaceAll("　", ".*") + ".*")) {
                            sectionDatalist0.add(new SectionRowData(place, message, "")); //複数場所がある場合を考慮
                        }
                    } while (cursor.moveToNext());
                }

                if(sectionDatalist0.size() <= 0) { //複数候補から１つも該当しなかった場合または１つのみ該当した場合
                    sectionDatalist0.add(new SectionRowData( place, message, ""));
                }

                rowList.add(sectionDatalist0);

                cursor.close();
                dbAdapter.closeDB();

                CustomSectionListAdapter adapter = new CustomSectionListAdapter(parent_activity, sectionList, rowList);
                listview.setAdapter(adapter);
            }
        }

        private String CreateWhereTime(String text){

            String where = "";

            String[] weekday_array = StringUtil.matcherSubString(text,"[月火水木金土日]+").split(",");
            String[] time_array = StringUtil.matcherSubString(text,"\\(.{1,2}限,([2468]+|10|12|14)").replaceAll("\\(.{1,2}限,","").split(",");

            for (int i = 0; i < weekday_array.length; i++) {

                String day;

                switch (weekday_array[i]) {
                    case "月":
                        day = "0";
                        break;
                    case "火":
                        day = "1";
                        break;
                    case "水":
                        day = "2";
                        break;
                    case "木":
                        day = "3";
                        break;
                    case "金":
                        day = "4";
                        break;
                    case "土":
                        day = "5";
                        break;
                    case "日":
                        day = "6";
                        break;
                    default:
                        day = "-1";
                }

                if(!time_array[i].matches("[2468]|10|12|14"))
                    time_array[i] = "0";

                where = where.concat("( weekday = " + day + " and time = " + Integer.valueOf(time_array[i])/2 + " ) ");
                if (i + 1 < weekday_array.length)
                    where = where.concat("or ");
            }

            return " ( " + where + " ) ";
        }
    }
}
