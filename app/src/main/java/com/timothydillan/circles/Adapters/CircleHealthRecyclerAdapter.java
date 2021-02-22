package com.timothydillan.circles.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.timothydillan.circles.R;
import com.timothydillan.circles.Utils.HealthUtil;

import java.util.ArrayList;

public class CircleHealthRecyclerAdapter extends RecyclerView.Adapter<CircleHealthRecyclerAdapter.MyViewHolder> {
    private Context ctx;
    private final ArrayList<ArrayList<String>> listOfHealthFactors;
    private final RecyclerViewClickListener listener;
    private final @LayoutRes int layoutId;

    public CircleHealthRecyclerAdapter(Context context, RecyclerViewClickListener listener) {
        ctx = context;
        this.listOfHealthFactors = HealthUtil.getInstance().getCircleHealthInformation(null);
        this.listener = listener;
        this.layoutId = R.layout.circle_health_list;
    }

    public void updateInformation(ArrayList<ArrayList<String>> information) {
        listOfHealthFactors.clear();
        listOfHealthFactors.addAll(information);
        notifyDataSetChanged();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private final MaterialCardView parentLayout;
        private final TextView healthTitle;
        private final TextView dailyHealthCount;
        private final TextView weeklyHealthCount;
        private final TextView monthlyHealthCount;
        public MyViewHolder(final View v) {
            super(v);
            parentLayout = v.findViewById(R.id.parentLayout);
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

        if (position % 2 == 0) {
            holder.parentLayout.setStrokeColor(ctx.getResources().getColor(R.color.logo_color_red));
        }

        // 0 -> Steps, 1 -> Walking, 2 -> Running, 3 -> Cycling
        // The position of each item in the recyclerview follows the order of which the items is added unto the arraylist.
        // Since we, well I, don't know how to dynamically set the titles from the arraylist, so I'm manually going through
        // every position and setting the title accordingly.
        switch (position) {
            case 0:
                holder.healthTitle.setText(getEmojiByUnicode(0x1F463) + " Total Steps");
                break;
            case 1:
                holder.healthTitle.setText(getEmojiByUnicode(0x1F6B6) + " Walking");
                break;
            case 2:
                holder.healthTitle.setText(getEmojiByUnicode(0x1F3C3) + " Running");
                break;
            case 3:
                holder.healthTitle.setText(getEmojiByUnicode(0x1F6B4) + " Cycling");
                break;
        }

        holder.dailyHealthCount.setText(listOfHealthFactors.get(position).get(0));
        holder.weeklyHealthCount.setText(listOfHealthFactors.get(position).get(1));
        holder.monthlyHealthCount.setText(listOfHealthFactors.get(position).get(2));
    }

    private String getEmojiByUnicode(int unicode){
        return new String(Character.toChars(unicode));
    }

    @Override
    public int getItemCount() {
        return listOfHealthFactors.size();
    }

    public interface RecyclerViewClickListener {
        void onClick(View v, int position);
    }
}
