package com.example.chemsolve2;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HistoryListActivity extends AppCompatActivity 
{

    public static final String EXTRA_MODE = "mode";
    public static final String RESULT_TEXT = "resultText";
    public static final String RESULT_IMAGE = "imagePng";

    private AppDb db;
    private ScanDao scanDao;

    private RecyclerView recycler;
    private ScanAdapter adapter;

    private final ExecutorService io = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history_list);

        String mode = getIntent().getStringExtra(EXTRA_MODE);
        if (mode == null) mode = "recent";

        TextView title = findViewById(R.id.title);
        title.setText(mode.equals("saved") ? "Saved" : "Recent");

        MaterialButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        db = AppDb.get(getApplicationContext());
        scanDao = db.scanDao();

        recycler = findViewById(R.id.recyclerHistory);
        recycler.setLayoutManager(new LinearLayoutManager(this));

        adapter = new ScanAdapter(item -> 
        {
            // return selection back to MainActivity
            Intent data = new Intent();
            data.putExtra(RESULT_TEXT, item.resultText);
            data.putExtra(RESULT_IMAGE, item.imagePng);
            setResult(RESULT_OK, data);
            finish();
        });

        recycler.setAdapter(adapter);

        loadList(mode);
    }

    private void loadList(String mode) 
    {
        io.execute(() -> 
        {
            List<ScanItem> items = mode.equals("saved")
                    ? scanDao.getSaved()
                    : scanDao.getRecent();

            runOnUiThread(() -> adapter.setItems(items));
        });
    }

    @Override
    protected void onDestroy() 
    {
        super.onDestroy();
        io.shutdown();
    }
}
