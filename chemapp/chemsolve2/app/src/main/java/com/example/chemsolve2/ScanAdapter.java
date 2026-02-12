package com.example.chemsolve2;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ScanAdapter extends RecyclerView.Adapter<ScanAdapter.VH>
{

    public interface OnItemClick
    {
        void onClick(ScanItem item);
    }

    private final OnItemClick onItemClick;
    private final List<ScanItem> items = new ArrayList<>();
    private final SimpleDateFormat fmt = new SimpleDateFormat("MMM d, HH:mm", Locale.getDefault());

    public ScanAdapter(OnItemClick onItemClick)
    {
        this.onItemClick = onItemClick;
    }

    public void setItems(List<ScanItem> newItems)
    {
        items.clear();
        if (newItems != null) items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_scan, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position)
    {
        ScanItem item = items.get(position);

        h.title.setText(fmt.format(new Date(item.createdAt)));

        String preview = item.resultText == null ? "" : item.resultText.replace("\n", "  •  ");
        if (preview.length() > 90) preview = preview.substring(0, 90) + "…";
        h.subtitle.setText(preview);

        if (item.imagePng != null)
        {
            Bitmap bmp = BitmapFactory.decodeByteArray(item.imagePng, 0, item.imagePng.length);
            h.thumb.setImageBitmap(bmp);
        } 
        else 
        {
            h.thumb.setImageDrawable(null);
        }

        h.itemView.setOnClickListener(v -> onItemClick.onClick(item));
    }

    @Override
    public int getItemCount()
    {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder
    {
        ImageView thumb;
        TextView title, subtitle;

        VH(@NonNull View itemView)
        {
            super(itemView);
            thumb = itemView.findViewById(R.id.thumb);
            title = itemView.findViewById(R.id.title);
            subtitle = itemView.findViewById(R.id.subtitle);
        }
    }
}
