package com.timothydillan.circles.Adapters;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import com.timothydillan.circles.Models.User;
import com.timothydillan.circles.R;
import com.timothydillan.circles.UI.VolleyImageRequest;
import com.timothydillan.circles.Utils.HealthUtil;

import java.util.ArrayList;
import java.util.Date;

public class HealthRecyclerAdapter extends RecyclerView.Adapter<HealthRecyclerAdapter.MyViewHolder> {
    private static ImageLoader imageLoader;
    private final ArrayList<User> listOfMembers;
    private final RecyclerViewClickListener listener;
    private final @LayoutRes int layoutId;

    public HealthRecyclerAdapter(Context context, RecyclerViewClickListener listener, @LayoutRes int layoutId) {
        this.listOfMembers = new ArrayList<>();
        this.listener = listener;
        this.layoutId = layoutId;
        imageLoader = VolleyImageRequest.getInstance(context).getImageLoader();
    }

    public void updateInformation(ArrayList<User> members) {
        listOfMembers.clear();
        listOfMembers.addAll(members);
        notifyDataSetChanged();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private final TextView memberName;
        private final TextView heartEmoji;
        private final TextView heartRate;
        private final TextView stepCount;
        private final NetworkImageView memberImage;
        public MyViewHolder(final View v) {
            super(v);
            memberName = v.findViewById(R.id.memberName);
            memberImage = v.findViewById(R.id.memberImageView);
            heartEmoji = v.findViewById(R.id.heartEmoji);
            heartRate = v.findViewById(R.id.heartRateTextView);
            stepCount = v.findViewById(R.id.stepCountTextView);
            memberImage.setDefaultImageResId(R.drawable.logo);
            ObjectAnimator scaleDown = ObjectAnimator.ofPropertyValuesHolder(heartEmoji,
                    PropertyValuesHolder.ofFloat("scaleX", 1.2f),
                    PropertyValuesHolder.ofFloat("scaleY", 1.2f));
            scaleDown.setDuration(300);

            scaleDown.setRepeatCount(ObjectAnimator.INFINITE);
            scaleDown.setRepeatMode(ObjectAnimator.REVERSE);

            scaleDown.start();
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
    public HealthRecyclerAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false);
        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull HealthRecyclerAdapter.MyViewHolder holder, int position) {
        User user = listOfMembers.get(position);
        String fullName = user.getFirstName() + " " + user.getLastName();
        String profilePicUrl = user.getProfilePicUrl();
        String heartRate = user.getHeartRate();
        String stepCount = HealthUtil.getSteps(user.getStepCount(), new Date());
        if (!profilePicUrl.isEmpty()) {
            imageLoader.get(profilePicUrl, ImageLoader.getImageListener(holder.memberImage, R.drawable.logo, android.R.drawable.ic_dialog_alert));
            holder.memberImage.setImageUrl(user.getProfilePicUrl(), imageLoader);
        }
        holder.heartRate.setText(heartRate + " bpm");
        holder.stepCount.setText(stepCount + " steps");
        holder.memberName.setText(fullName);
    }

    @Override
    public int getItemCount() {
        return listOfMembers.size();
    }

    public interface RecyclerViewClickListener {
        void onClick(View v, int position);
    }
}
