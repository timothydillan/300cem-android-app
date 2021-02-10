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
import com.timothydillan.circles.Models.User;
import com.timothydillan.circles.R;
import com.timothydillan.circles.UI.VolleyImageRequest;
import com.timothydillan.circles.Utils.LocationUtil;
import java.util.ArrayList;

public class CMemberRecyclerAdapter extends RecyclerView.Adapter<CMemberRecyclerAdapter.MyViewHolder> {
    private static ImageLoader imageLoader;
    private final ArrayList<User> listOfMembers;
    private final RecyclerViewClickListener listener;
    private final @LayoutRes int layoutId;
    private LocationUtil locationUtil;

    public CMemberRecyclerAdapter(Context context, RecyclerViewClickListener listener, @LayoutRes int layoutId) {
        this.listOfMembers = new ArrayList<>();
        this.listener = listener;
        this.layoutId = layoutId;
        locationUtil = new LocationUtil(context);
        imageLoader = VolleyImageRequest.getInstance(context).getImageLoader();
    }

    public void updateInformation(ArrayList<User> members) {
        listOfMembers.clear();
        listOfMembers.addAll(members);
        notifyDataSetChanged();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private final TextView memberName;
        private final TextView memberLocation;
        private final TextView lastSeen;
        private final NetworkImageView memberImage;
        public MyViewHolder(final View v) {
            super(v);
            memberName = v.findViewById(R.id.memberName);
            lastSeen = v.findViewById(R.id.lastSeenMember);
            memberImage = v.findViewById(R.id.memberImageView);
            memberImage.setDefaultImageResId(R.drawable.logo);
            memberLocation = v.findViewById(R.id.memberLocation);
            v.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            int memberClickedIndex = getAdapterPosition();
            // May return NO_POSITION as the RecylerView layout changes, need to do a sanity check
            // so that it doesn't crash.
            if (memberClickedIndex != RecyclerView.NO_POSITION)
                listener.onClick(v, memberClickedIndex);
        }
    }

    @NonNull
    @Override
    public CMemberRecyclerAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false);
        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull CMemberRecyclerAdapter.MyViewHolder holder, int position) {
        User user = listOfMembers.get(position);
        String fullName = user.getFirstName() + " " + user.getLastName();
        String lastSharingTime = "Since " + user.getLastSharingTime();
        String profilePicUrl = user.getProfilePicUrl();
        if (!profilePicUrl.isEmpty()) {
            imageLoader.get(profilePicUrl, ImageLoader.getImageListener(holder.memberImage, R.drawable.logo, android.R.drawable.ic_dialog_alert));
            holder.memberImage.setImageUrl(user.getProfilePicUrl(), imageLoader);
        }
        holder.memberName.setText(fullName);
        holder.memberLocation.setText(locationUtil.getAddress(user.getLatitude(), user.getLongitude()));
        holder.lastSeen.setText(lastSharingTime);
    }

    @Override
    public int getItemCount() {
        return listOfMembers.size();
    }

    public interface RecyclerViewClickListener {
        void onClick(View v, int position);
    }
}
