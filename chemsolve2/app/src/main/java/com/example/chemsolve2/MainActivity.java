package com.example.chemsolve2;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;

import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


import com.unity3d.player.UnityPlayerActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Objects;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity
{
    private AppDb db;
    private ScanDao scanDao;

    private RecyclerView recycler;
    private ScanAdapter adapter;

    private MaterialButton btnSave;
    private Chip chipRecent, chipSaved;

    private final ExecutorService io = Executors.newSingleThreadExecutor();

    private long lastScanId = -1;
    private Bitmap lastBitmap = null;
    private String lastResultText = null;

    private boolean showingSaved = false;

    ImageView view;
    Button but, but2;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        view = findViewById(R.id.imageview);
        but = findViewById(R.id.button);
        but2 = findViewById(R.id.button1);

        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA}, 100);
        }

        but.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(intent, 100);
            }
        });

        but2.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Intent intent = new Intent(MainActivity.this, UnityActivity.class);
                startActivity(intent);
            }
        });

        db = AppDb.get(getApplicationContext());
        scanDao = db.scanDao();

        recycler = findViewById(R.id.recyclerScans);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ScanAdapter(item ->
        {
            // On tap: show it in the result area + preview
            lastScanId = item.id;
            lastResultText = item.resultText;

            TextView resultTextView = findViewById(R.id.textView);
            resultTextView.setText(item.resultText);

            if (item.imagePng != null)
            {
                Bitmap bmp = android.graphics.BitmapFactory.decodeByteArray(item.imagePng, 0, item.imagePng.length);
                view.setImageBitmap(bmp);
                TextView hint = findViewById(R.id.previewHint);
                hint.setVisibility(View.GONE);
            }
        });
        recycler.setAdapter(adapter);

        // Chips + Save button
                btnSave = findViewById(R.id.btnSave);
                chipRecent = findViewById(R.id.chipRecent);
                chipSaved = findViewById(R.id.chipSaved);

                chipRecent.setOnClickListener(v -> { showingSaved = false; loadScans(); });
                chipSaved.setOnClickListener(v -> { showingSaved = true; loadScans(); });

                btnSave.setOnClickListener(v ->
                {
                    if (lastScanId <= 0)
                    {
                        Toast.makeText(this, "Scan something first", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    io.execute(() ->
                    {
                        scanDao.markSaved(lastScanId);
                        runOnUiThread(() ->
                        {
                            Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show();
                            loadScans();
                        });
                    });
                });

        // Initial load
        loadScans();

    }

    @Override
    protected  void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK && data != null && data.getExtras() != null)
        {
            Bitmap bitmap = (Bitmap) data.getExtras().get("data");

            // Safety check
            if (bitmap == null) return;

            // ✅ Store bitmap for later saving into database
            lastBitmap = bitmap;

            // Show preview
            view.setImageBitmap(bitmap);

            TextView hint = findViewById(R.id.previewHint);
            hint.setVisibility(View.GONE);

            // Send to server
            new NetworkTask().execute(bitmap);
        }
    }

    private void loadScans()
    {
        io.execute(() -> {
            List<ScanItem> items = showingSaved ? scanDao.getSaved() : scanDao.getRecent();
            runOnUiThread(() -> adapter.setItems(items));
        });
    }

    private byte[] bitmapToPngBytes(Bitmap bitmap, int maxSizePx)
    {
        // Create a small thumbnail to store in DB
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        float scale = Math.min(1f, maxSizePx / (float) Math.max(w, h));
        Bitmap scaled = Bitmap.createScaledBitmap(bitmap, Math.round(w * scale), Math.round(h * scale), true);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        scaled.compress(Bitmap.CompressFormat.PNG, 90, out);
        return out.toByteArray();
    }

    private class NetworkTask extends AsyncTask<Bitmap, Void, String>
    {
        @Override
        protected String doInBackground(Bitmap... bitmaps)
        {
            Bitmap bitmap = bitmaps[0];
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            byte[] byteArray = stream.toByteArray();

            OkHttpClient client = new OkHttpClient();
            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("key", "your-key-value") // add key-value pair here
                    .addFormDataPart("image", "image.png", RequestBody.create(MediaType.parse("image/png"), byteArray))
                    .build();

            Request request = new Request.Builder()
                    .url("http://172.27.232.37:5000/interactive_shell")
                    .post(requestBody)
                    .build();

            String responseBody = null;
            try
            {
                Response response = client.newCall(request).execute();
                responseBody = Objects.requireNonNull(response.body()).string();
                Log.d("responseBody", responseBody);
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }

            return responseBody;
        }

        protected void onPostExecute(String responseBody)
        {
            // Process API response
            if (responseBody != null)
            {
                try
                {
                    JSONObject responseJson = new JSONObject(responseBody);
                    String smiles = responseJson.getString("smiles");
                    String formula = responseJson.getString("formula");
                    String iupacName = responseJson.getString("iupac_name");

                    // Save SDF file for later use
                    String sdfString = responseJson.getString("sdf");
                    FileOutputStream fos = openFileOutput("output.sdf", Context.MODE_PRIVATE);
                    fos.write(sdfString.getBytes());
                    fos.close();

                    Intent intent = new Intent(MainActivity.this, UnityActivity.class);
                    intent.putExtra("sdfFilePath", getApplicationContext().getFilesDir().getAbsolutePath() + "/output.sdf");
                    Log.d("MainActivity", "SDF file saved at: " + getFilesDir() + "/output.sdf");

                    // Display response in a TextView
                    TextView resultTextView = findViewById(R.id.textView);
                    String result = "SMILES: " + smiles + "\nFormula: " + formula + "\nIUPAC Name: " + iupacName;
                    resultTextView.setText(result);

                    // remember last result for Save button
                    lastResultText = result;

                    // store scan in database as a recent scan
                    final byte[] thumb = (lastBitmap != null)
                            ? bitmapToPngBytes(lastBitmap, 320)
                            : null;

                    io.execute(() -> {
                        ScanItem item = new ScanItem();
                        item.createdAt = System.currentTimeMillis();
                        item.resultText = result;
                        item.imagePng = thumb;
                        item.saved = false;

                        lastScanId = scanDao.insert(item);

                        runOnUiThread(() -> loadScans());
                    });
                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            else
            {
                // Display error message in a Toast
                Toast.makeText(MainActivity.this, "An error occurred", Toast.LENGTH_SHORT).show();
            }
        }
    }

}