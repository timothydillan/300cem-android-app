package com.timothydillan.circles.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.timothydillan.circles.Models.ItemModel;
import com.timothydillan.circles.R;

public class RecyclerAdapter extends RecyclerView.Adapter<RecyclerAdapter.MyViewHolder> {
    private final ItemModel listOfItems;
    private final RecyclerViewClickListener listener;
    private final @LayoutRes int layoutId;

    public RecyclerAdapter(ItemModel listOfItems, RecyclerViewClickListener listener, @LayoutRes int layoutId) {
        this.listOfItems = listOfItems;
        this.listener = listener;
        this.layoutId = layoutId;
    }

    public class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private final TextView itemName;

        public MyViewHolder(final View v) {
            super(v);
            itemName = v.findViewById(R.id.itemName);
            v.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            listener.onClick(v, getAdapterPosition());
        }
    }

    @NonNull
    @Override
    public RecyclerAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false);
        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerAdapter.MyViewHolder holder, int position) {
        String name = listOfItems.getItemName(position);
        holder.itemName.setText(name);
    }

    @Override
    public int getItemCount() {
        return listOfItems.getMap().size();
    }

    public interface RecyclerViewClickListener {
        void onClick(View v, int position);
    }
}
