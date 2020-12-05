package com.ghost2911.answerfivesecond;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.SparseBooleanArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import org.xmlpull.v1.XmlPullParser;

import java.util.ArrayList;

public class ChooseQuestionsActivity extends AppCompatActivity {

    private TextView mTextView;
    public ListView lvQuestions;
    Button btnCheckAll;
    Button btnSave;
    ListView listView;
    ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_questions);

        mTextView = (TextView) findViewById(R.id.text);
        btnSave = (Button) findViewById(R.id.btnSave);
        btnCheckAll = (Button) findViewById(R.id.btnBack);

        listView = findViewById(R.id.lvQuestions);
        adapter  = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_checked, GetQuestions()){
            @RequiresApi(api = Build.VERSION_CODES.O)
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView textView = (TextView) view;
                textView.setTextColor(Color.parseColor("#FFFFFF"));
                textView.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
                textView.setMaxLines(2);
                textView.setEllipsize(TextUtils.TruncateAt.END);
                return view;
            }
        };
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        listView.setAdapter(adapter);

        TinyDB tinydb = new TinyDB(getApplicationContext());
        ArrayList<Boolean> checked = tinydb.getListBoolean("ChoosedQuestions");

        int count = listView.getCount();

        if (checked.size()==0) {
            for (int i = 0; i < count; i++)
                listView.setItemChecked(i, true);
        }
        else {
            for (int i = 0; i < count; i++)
                listView.setItemChecked(i, checked.get(i));
        }

        btnCheckAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int count = listView.getCount();
                for (int i = 0; i < count; i++)
                    listView.setItemChecked(i, true);
            }
        });
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ArrayList<Boolean> checked = new ArrayList<>();
                int count = listView.getCount();

                SparseBooleanArray sparseBooleanArray = listView.getCheckedItemPositions();
                for (int i = 0; i < count; i++) {
                    checked.add(sparseBooleanArray.get(i));
                }
                TinyDB tinydb = new TinyDB(getApplicationContext());
                tinydb.putListBoolean("ChoosedQuestions", checked);
                finish();
            }
        });
    }

    ArrayList<String> GetQuestions() {
        ArrayList<String> questions = new ArrayList<>();
        try {
            XmlPullParser parser = getResources().getXml(R.xml.questions);

            while (parser.getEventType() != XmlPullParser.END_DOCUMENT) {
                if (parser.getEventType() == XmlPullParser.START_TAG
                        && parser.getName().equals("question")) {
                    questions.add(parser.getAttributeValue(null,"text"));
                }
                parser.next();
            }
        } catch (Throwable t) {
            Toast.makeText(this,
                    "Ошибка при загрузке XML-документа: " + t.toString(), Toast.LENGTH_LONG)
                    .show();
        }
        return questions;
    }
}