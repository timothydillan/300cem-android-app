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
import com.google.android.material.card.MaterialCardView;
import com.timothydillan.circles.Models.User;
import com.timothydillan.circles.R;
import com.timothydillan.circles.UI.VolleyImageRequest;
import com.timothydillan.circles.Utils.CircleUtil;
import com.timothydillan.circles.Utils.HealthUtil;

import java.util.ArrayList;
import java.util.Date;

public class HealthRecyclerAdapter extends RecyclerView.Adapter<HealthRecyclerAdapter.MyViewHolder> {
    private static ImageLoader imageLoader;
    private Context ctx;
    private final ArrayList<User> listOfMembers;
    private final RecyclerViewClickListener listener;
    private final @LayoutRes int layoutId;

    public HealthRecyclerAdapter(Context context, RecyclerViewClickListener listener) {
        ctx = context;
        this.listOfMembers = CircleUtil.getInstance().getCircleMembers();
        this.listener = listener;
        this.layoutId = R.layout.member_health_list;
        imageLoader = VolleyImageRequest.getInstance(context).getImageLoader();
    }

    public void updateInformation(ArrayList<User> members) {
        listOfMembers.clear();
        listOfMembers.addAll(members);
        notifyDataSetChanged();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private final MaterialCardView parentLayout;
        private final TextView memberName;
        private final TextView heartEmoji;
        private final TextView heartRate;
        private final TextView stepCount;
        private final TextView runningTime;
        private final TextView cyclingTime;
        private final NetworkImageView memberImage;
        public MyViewHolder(final View v) {
            super(v);
            parentLayout = v.findViewById(R.id.parentLayout);
            memberName = v.findViewById(R.id.memberName);
            memberImage = v.findViewById(R.id.memberImageView);
            heartEmoji = v.findViewById(R.id.heartEmoji);
            heartRate = v.findViewById(R.id.heartRateTextView);
            stepCount = v.findViewById(R.id.stepCountTextView);
            runningTime = v.findViewById(R.id.runningTimeTextView);
            cyclingTime = v.findViewById(R.id.cyclingTimeTextView);
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
        if (position % 2 == 0) {
            holder.parentLayout.setStrokeColor(ctx.getResources().getColor(R.color.logo_color_red));
        }

        HealthUtil healthUtil = HealthUtil.getInstance();
        healthUtil.initializeContext(ctx);

        User user = listOfMembers.get(position);
        String fullName = user.getFirstName() + " " + user.getLastName();
        String profilePicUrl = user.getProfilePicUrl();
        String heartRate = "0 bpm";
        String stepCount = "0 steps";
        String runningTime = "0 sec";
        String cyclingTime = "0 sec";

        if (user.getHeartRate() != null && !user.getHeartRate().isEmpty()) {
            heartRate = user.getHeartRate() + " bpm";
        }

        if (healthUtil.getActivityCount(user.getStepCount(), new Date()) != null) {
            stepCount = healthUtil.stepsToReadableFormat(Long.parseLong(healthUtil.getActivityCount(user.getStepCount(), new Date())));
        }

        if (healthUtil.getActivityCount(user.getRunningActivity(), new Date()) != null) {
            runningTime = healthUtil.millisToReadableFormat(Long.parseLong(healthUtil.getActivityCount(user.getRunningActivity(), new Date())));
        }

        if (healthUtil.getActivityCount(user.getCyclingActivity(), new Date()) != null) {
            cyclingTime = healthUtil.millisToReadableFormat(Long.parseLong(healthUtil.getActivityCount(user.getCyclingActivity(), new Date())));
        }

        if (!profilePicUrl.isEmpty()) {
            imageLoader.get(profilePicUrl, ImageLoader.getImageListener(holder.memberImage, R.drawable.logo, android.R.drawable.ic_dialog_alert));
            holder.memberImage.setImageUrl(user.getProfilePicUrl(), imageLoader);
        }

        holder.heartRate.setText(heartRate);
        holder.stepCount.setText(stepCount);
        holder.runningTime.setText(runningTime);
        holder.cyclingTime.setText(cyclingTime);
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
