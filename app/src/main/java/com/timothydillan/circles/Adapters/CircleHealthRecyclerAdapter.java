package com.timothydillan.circles.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.toolbox.ImageLoader;
import com.timothydillan.circles.R;
import com.timothydillan.circles.UI.VolleyImageRequest;
import com.timothydillan.circles.Utils.HealthUtil;

import java.util.ArrayList;
import java.util.Date;

public class CircleHealthRecyclerAdapter extends RecyclerView.Adapter<CircleHealthRecyclerAdapter.MyViewHolder> {
    private static ImageLoader imageLoader;
    private final ArrayList<String> listOfSteps;
    private final RecyclerViewClickListener listener;
    private final @LayoutRes int layoutId;

    public CircleHealthRecyclerAdapter(Context context, RecyclerViewClickListener listener, @LayoutRes int layoutId) {
        this.listOfSteps = new ArrayList<>();
        this.listener = listener;
        this.layoutId = layoutId;
        imageLoader = VolleyImageRequest.getInstance(context).getImageLoader();
    }

    public void updateInformation(ArrayList<String> information) {
        listOfSteps.clear();
        listOfSteps.addAll(information);
        notifyDataSetChanged();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private final TextView healthTitle;
        private final TextView dailyHealthCount;
        private final TextView weeklyHealthCount;
        private final TextView monthlyHealthCount;
        public MyViewHolder(final View v) {
            super(v);
            healthTitle = v.findViewById(R.id.healthTitle);
            dailyHealthCount = v.findViewById(R.id.dailyHealthCount);
            weeklyHealthCount = v.findViewById(R.id.weeklyHealthCount);
            monthlyHealthCount = v.findViewById(R.id.monthlyHealthCount);
            v.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            int memberClickedIndex = getAdapterPosition();
            // May return NO_POSITION as the RecylerView layout changes, need to do a sanity check
            // so that it doesn't crash.
            if (memberClickedIndex != RecyclerView.NO_POSITION && listener != null)
                listener.onClick(v, memberClickedIndex);
        }
    }

    @NonNull
    @Override
    public CircleHealthRecyclerAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false);
        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull CircleHealthRecyclerAdapter.MyViewHolder holder, int position) {
        holder.dailyHealthCount.setText(listOfSteps.get(0) + " steps");
        holder.weeklyHealthCount.setText(listOfSteps.get(1) + " steps");
        holder.monthlyHealthCount.setText(listOfSteps.get(2) + " steps");
    }

    @Override
    public int getItemCount() {
        // return listOfHealthFactors.size();
        // listOfHealthFactors should include:
            /*
            1. Steps
            2. Cycling
            3. Exercise
            4. Average Heart Rate
            */
        // TODO: FIX
        return listOfSteps.size();
    }

    public interface RecyclerViewClickListener {
        void onClick(View v, int position);
    }
}
