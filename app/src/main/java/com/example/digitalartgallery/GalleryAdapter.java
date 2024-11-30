package com.example.digitalartgallery;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.List;

public class GalleryAdapter extends RecyclerView.Adapter<GalleryAdapter.GalleryViewHolder> {

    private List<DocumentSnapshot> galleryItems;

    public void setGalleryItems(List<DocumentSnapshot> galleryItems) {
        this.galleryItems = galleryItems;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public GalleryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.gallery_item, parent, false);
        return new GalleryViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull GalleryViewHolder holder, int position) {
        DocumentSnapshot document = galleryItems.get(position);
        String imageUrl = document.getString("imageUrl");
        String caption = document.getString("caption");
        String email = document.getString("email");

        holder.tvUsername.setText(email);
        holder.tvCaption.setText(caption);

        // Use Glide to load image from the URL
        Glide.with(holder.itemView.getContext()).load(imageUrl).into(holder.uploadedImage);
    }

    @Override
    public int getItemCount() {
        return galleryItems != null ? galleryItems.size() : 0;
    }

    public static class GalleryViewHolder extends RecyclerView.ViewHolder {
        TextView tvUsername, tvCaption;
        ImageView uploadedImage;

        public GalleryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUsername = itemView.findViewById(R.id.tvUsername);
            uploadedImage = itemView.findViewById(R.id.uploadedImage);
            tvCaption = itemView.findViewById(R.id.tvCaption);
        }
    }
}
