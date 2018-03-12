package com.arecmetafora.getmethere.app;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.List;

class OfflineLocationsAdapter extends RecyclerView.Adapter<OfflineLocationsAdapter.ViewHolder> {

    private List<OfflineLocation> mOfflineLocations;
    private Callback mListener;

    interface Callback {
        void onSelectedLocation(OfflineLocation location);
    }

    OfflineLocationsAdapter(Callback listener) {
        this.mListener = listener;
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private ImageView mImage;
        private TextView mDescription;
        private ViewHolder(View itemView) {
            super(itemView);
            mImage = itemView.findViewById(R.id.offline_location_image);
            mDescription = itemView.findViewById(R.id.offline_location_description);

            itemView.setOnClickListener(v -> {
                if(mListener != null) {
                    mListener.onSelectedLocation(mOfflineLocations.get(getLayoutPosition()));
                }
            });
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.offline_location_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        OfflineLocation location = mOfflineLocations.get(position);
        holder.mDescription.setText(location.description);
        Picasso.get().load(location.mapFile).into(holder.mImage);
    }

    @Override
    public int getItemCount() {
        return mOfflineLocations == null ? 0 : mOfflineLocations.size();
    }

    void setItems(List<OfflineLocation> offlineLocations) {
        mOfflineLocations = offlineLocations;
        notifyDataSetChanged();
    }

    OfflineLocation removeItem(int position) {
        OfflineLocation removedItem = mOfflineLocations.remove(position);
        notifyItemRemoved(position);
        return removedItem;
    }
}
