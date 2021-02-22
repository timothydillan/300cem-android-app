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
import com.android.volley.toolbox.NetworkImageView;
import com.google.android.material.card.MaterialCardView;
import com.timothydillan.circles.Models.User;
import com.timothydillan.circles.R;
import com.timothydillan.circles.UI.VolleyImageRequest;
import com.timothydillan.circles.Utils.CircleUtil;
import com.timothydillan.circles.Utils.MoodUtil;

import java.util.ArrayList;

public class MoodRecyclerAdapter extends RecyclerView.Adapter<MoodRecyclerAdapter.MyViewHolder> {
    private static ImageLoader imageLoader;
    private Context ctx;
    private final ArrayList<User> listOfMembers;
    private final RecyclerViewClickListener listener;
    private final @LayoutRes int layoutId;

    public MoodRecyclerAdapter(Context context, RecyclerViewClickListener listener) {
        ctx = context;
        this.listOfMembers = MoodUtil.getInstance().getMemberMoodInformation();
        this.listener = listener;
        this.layoutId = R.layout.circle_mood_list;
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
        private final TextView description;
        private final TextView emojiTextView;
        private final NetworkImageView memberImage;
        public MyViewHolder(final View v) {
            super(v);
            parentLayout = v.findViewById(R.id.parentLayout);
            memberName = v.findViewById(R.id.memberName);
            memberImage = v.findViewById(R.id.memberImageView);
            description = v.findViewById(R.id.description);
            emojiTextView = v.findViewById(R.id.emojiTextView);
            memberImage.setDefaultImageResId(R.drawable.logo);
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
    public MoodRecyclerAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false);
        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull MoodRecyclerAdapter.MyViewHolder holder, int position) {
        User user = listOfMembers.get(position);

        // We'll set the stroke color of each member's card according to their mood.
        // If the mood is negative, we'll set the color to blue.
        // If they're unwell, we'll set the color to green. Also if their OK/Neutral, we'll set it to red.
        if (user.getMood().equals("Negative")) {
            holder.parentLayout.setStrokeColor(ctx.getResources().getColor(R.color.logo_color_blue));
        } else if (user.getMood().equals("Unwell")) {
            holder.parentLayout.setStrokeColor(ctx.getResources().getColor(R.color.logo_color_green));
        } else {
            holder.parentLayout.setStrokeColor(ctx.getResources().getColor(R.color.logo_color_red));
        }

        String fullName = user.getFirstName() + " " + user.getLastName();
        String profilePicUrl = user.getProfilePicUrl();
        String description = user.getFirstName() + MoodUtil.getMoodDescription(user.getMood());
        String emoji = MoodUtil.getMoodEmoji(user.getMood());

        if (!profilePicUrl.isEmpty()) {
            imageLoader.get(profilePicUrl, ImageLoader.getImageListener(holder.memberImage, R.drawable.logo, android.R.drawable.ic_dialog_alert));
            holder.memberImage.setImageUrl(user.getProfilePicUrl(), imageLoader);
        }

        holder.memberName.setText(fullName);
        holder.description.setText(description);
        holder.emojiTextView.setText(emoji);
    }

    @Override
    public int getItemCount() {
        return listOfMembers.size();
    }

    public User getMember(int position) {
        return listOfMembers.get(position);
    }

    public interface RecyclerViewClickListener {
        void onClick(View v, int position);
    }
}