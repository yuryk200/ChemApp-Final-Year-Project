package com.example.chemsolve2;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.unity3d.player.UnityPlayer;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private static final int REQ_CAMERA   = 100;
    private static final int REQ_GALLERY  = 200;
    private static final int REQ_HISTORY  = 300;

    private AppDb db;
    private ScanDao scanDao;
    private final ExecutorService io = Executors.newSingleThreadExecutor();

    private MaterialButton btnSave;

    private UnityPlayer unityPlayer;
    private FrameLayout unityContainer;

    private long lastScanId = -1;
    private Bitmap lastBitmap = null;
    private String lastResultText = null;

    private ImageView view;
    private Button but;

    private TextView resultTextView;
    private View resultLoadingOverlay;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MaterialButton btnRecent = findViewById(R.id.btnRecent);
        MaterialButton btnSaved  = findViewById(R.id.btnSaved);

        btnRecent.setOnClickListener(v -> openHistory("recent"));
        btnSaved.setOnClickListener(v -> openHistory("saved"));

        // Result UI
        resultTextView = findViewById(R.id.textView);
        resultLoadingOverlay = findViewById(R.id.resultLoadingOverlay);

        // Unity container (start hidden; create Unity only when needed)
        unityContainer = findViewById(R.id.unityContainer);
        unityContainer.setVisibility(View.GONE);
        unityPlayer = null;

        // Preview + Scan button
        view = findViewById(R.id.imageview);
        but = findViewById(R.id.button);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQ_CAMERA);
        }

        but.setOnClickListener(v -> {
            String[] options = {"Take Photo", "Choose from Gallery"};

            new android.app.AlertDialog.Builder(MainActivity.this)
                    .setTitle("Scan Image")
                    .setItems(options, (dialog, which) -> {
                        if (which == 0) {
                            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                            startActivityForResult(cameraIntent, REQ_CAMERA);
                        } else {
                            Intent galleryIntent = new Intent(
                                    Intent.ACTION_PICK,
                                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                            );
                            startActivityForResult(galleryIntent, REQ_GALLERY);
                        }
                    })
                    .show();
        });

        // DB
        db = AppDb.get(getApplicationContext());
        scanDao = db.scanDao();

        // Save button
        btnSave = findViewById(R.id.btnSave);
        btnSave.setOnClickListener(v -> {
            if (lastScanId <= 0) {
                Toast.makeText(this, "Scan something first", Toast.LENGTH_SHORT).show();
                return;
            }
            io.execute(() -> {
                scanDao.markSaved(lastScanId);
                runOnUiThread(() ->
                        Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show()
                );
            });
        });
    }

    private void showResultLoadingOverlay() {
        if (resultLoadingOverlay != null) resultLoadingOverlay.setVisibility(View.VISIBLE);
    }

    private void hideResultLoadingOverlay() {
        if (resultLoadingOverlay != null) resultLoadingOverlay.setVisibility(View.GONE);
    }

    private void ensureUnity() {
        if (unityPlayer != null) return;

        unityPlayer = new UnityPlayer(this);

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        );

        unityContainer.removeAllViews();
        unityContainer.addView(unityPlayer.getView(), 0, lp);

        unityPlayer.requestFocus();
    }

    private void showUnityAndLoadSdf(String sdfPath) {
        runOnUiThread(() -> {
            ensureUnity();

            unityContainer.setVisibility(View.VISIBLE);

            // The "overview then back" trick basically forces focus/surface events.
            unityPlayer.requestFocus();
            unityPlayer.windowFocusChanged(true);
            unityPlayer.resume();

            // Wait for Surface creation then send message
            unityPlayer.getView().postDelayed(() -> {
                try {
                    UnityPlayer.UnitySendMessage("dw", "LoadSdfFromPath", sdfPath);
                } catch (Exception e) {
                    Log.e("MainActivity", "UnitySendMessage failed", e);
                }
            }, 600);
        });
    }

    private void openHistory(String mode) {
        Intent i = new Intent(this, HistoryListActivity.class);
        i.putExtra(HistoryListActivity.EXTRA_MODE, mode);
        startActivityForResult(i, REQ_HISTORY);
    }

    private void hideAndDestroyUnity() {
        runOnUiThread(() -> {
            if (unityContainer != null) unityContainer.setVisibility(View.GONE);

            if (unityPlayer != null) {
                try {
                    unityPlayer.pause();
                    unityPlayer.destroy();
                } catch (Exception ignored) {}
                unityPlayer = null;
            }

            if (unityContainer != null) {
                unityContainer.removeAllViews(); // remove the Unity surface
            }
        });
    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != RESULT_OK || data == null) return;

        if (requestCode == REQ_HISTORY && resultCode == RESULT_OK && data != null) {
            hideAndDestroyUnity();
            String resultText = data.getStringExtra(HistoryListActivity.RESULT_TEXT);
            byte[] imagePng = data.getByteArrayExtra(HistoryListActivity.RESULT_IMAGE);

            if (resultText != null) {
                resultTextView.setText(resultText);
                lastResultText = resultText;
            }

            if (imagePng != null) {
                Bitmap bmp = android.graphics.BitmapFactory.decodeByteArray(imagePng, 0, imagePng.length);
                view.setImageBitmap(bmp);
                TextView hint = findViewById(R.id.previewHint);
                hint.setVisibility(View.GONE);
            }
            return;
        }

        // Camera/Gallery scan
        try {
            Bitmap bitmap = null;

            if (requestCode == REQ_CAMERA && data.getExtras() != null) {
                bitmap = (Bitmap) data.getExtras().get("data");
            } else if (requestCode == REQ_GALLERY && data.getData() != null) {
                bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), data.getData());
            }

            if (bitmap == null) return;

            lastBitmap = bitmap;
            view.setImageBitmap(bitmap);

            TextView hint = findViewById(R.id.previewHint);
            hint.setVisibility(View.GONE);

            showResultLoadingOverlay();
            unityContainer.setVisibility(View.GONE);

            new NetworkTask().execute(bitmap);

        } catch (IOException e) {
            e.printStackTrace();
            hideResultLoadingOverlay();
            Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (unityPlayer != null && unityContainer.getVisibility() == View.VISIBLE) {
            unityPlayer.resume();
            unityPlayer.windowFocusChanged(true);
        }
    }

    @Override
    protected void onPause() {
        if (unityPlayer != null) unityPlayer.pause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (unityPlayer != null) {
            unityPlayer.destroy();
            unityPlayer = null;
        }
        super.onDestroy();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (unityPlayer != null) unityPlayer.windowFocusChanged(hasFocus);
    }

    private byte[] bitmapToPngBytes(Bitmap bitmap, int maxSizePx) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        float scale = Math.min(1f, maxSizePx / (float) Math.max(w, h));
        Bitmap scaled = Bitmap.createScaledBitmap(bitmap, Math.round(w * scale), Math.round(h * scale), true);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        scaled.compress(Bitmap.CompressFormat.PNG, 90, out);
        return out.toByteArray();
    }

    private class NetworkTask extends AsyncTask<Bitmap, Void, String> {

        @Override
        protected String doInBackground(Bitmap... bitmaps) {
            Bitmap bitmap = bitmaps[0];

            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            byte[] byteArray = stream.toByteArray();

            OkHttpClient client = new OkHttpClient();
            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("key", "your-key-value")
                    .addFormDataPart("image", "image.png",
                            RequestBody.create(MediaType.parse("image/png"), byteArray))
                    .build();

            Request request = new Request.Builder()
                    .url("http://192.168.1.6:5000/interactive_shell")
                    .post(requestBody)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                return Objects.requireNonNull(response.body()).string();
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(String responseBody) {
            if (responseBody == null) {
                hideResultLoadingOverlay();
                resultTextView.setText("An error occurred. Please try again.");
                Toast.makeText(MainActivity.this, "An error occurred", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                JSONObject responseJson = new JSONObject(responseBody);
                String smiles = responseJson.getString("smiles");
                String formula = responseJson.getString("formula");
                String sdfString = responseJson.getString("sdf");

                // Save sdf locally
                FileOutputStream fos = openFileOutput("output.sdf", Context.MODE_PRIVATE);
                fos.write(sdfString.getBytes());
                fos.close();

                final String sdfPath = getFilesDir().getAbsolutePath() + "/output.sdf";

                // Update text UI
                String result = "SMILES: " + smiles + "\nFormula: " + formula;
                resultTextView.setText(result);
                lastResultText = result;

                // Show Unity + load molecule
                showUnityAndLoadSdf(sdfPath);

                hideResultLoadingOverlay();

                // Save scan into DB as recent
                final byte[] thumb = (lastBitmap != null) ? bitmapToPngBytes(lastBitmap, 320) : null;

                io.execute(() -> {
                    ScanItem item = new ScanItem();
                    item.createdAt = System.currentTimeMillis();
                    item.resultText = result;
                    item.imagePng = thumb;
                    item.saved = false;

                    lastScanId = scanDao.insert(item);
                });

            } catch (Exception e) {
                e.printStackTrace();
                hideResultLoadingOverlay();
                resultTextView.setText("Failed to parse server response.");
            }
        }
    }
}
