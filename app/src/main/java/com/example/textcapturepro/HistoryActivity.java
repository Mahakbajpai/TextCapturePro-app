package com.example.textcapturepro;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HistoryActivity extends AppCompatActivity {

    private static final String PREF_NAME = "ocr_history";
    private static final String KEY_HISTORY = "history_list";

    ListView listView;
    List<String> historyList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        listView = findViewById(R.id.listHistory);
        historyList = loadHistory();

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(this,
                        android.R.layout.simple_list_item_1,
                        historyList);

        listView.setAdapter(adapter);

        listView.setOnItemClickListener((parent, view, position, id) -> {
            String selectedText = historyList.get(position);

            Intent intent =
                    new Intent(HistoryActivity.this,
                            TextDisplayActivity.class);
            intent.putExtra("recognized_text", selectedText);
            startActivity(intent);
        });
    }

    private List<String> loadHistory() {

        SharedPreferences prefs =
                getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        String data = prefs.getString(KEY_HISTORY, "");

        if (data.isEmpty()) {
            return new ArrayList<>();
        }

        return new ArrayList<>(Arrays.asList(data.split("\\|\\|\\|")));
    }
}
