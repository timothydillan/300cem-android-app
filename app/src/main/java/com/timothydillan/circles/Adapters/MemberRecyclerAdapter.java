package com.timothydillan.circles.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import com.timothydillan.circles.Models.User;
import com.timothydillan.circles.R;
import com.timothydillan.circles.UI.VolleyImageRequest;
import com.timothydillan.circles.Utils.CircleUtil;
import com.timothydillan.circles.Utils.UserUtil;

import java.util.ArrayList;

public class MemberRecyclerAdapter extends RecyclerView.Adapter<MemberRecyclerAdapter.MyViewHolder> {
    private static ImageLoader imageLoader;
    private final ArrayList<User> listOfMembers;
    private final RecyclerViewClickListener listener;
    private final @LayoutRes int layoutId;

    public MemberRecyclerAdapter(Context context, RecyclerViewClickListener listener) {
        this.listOfMembers = CircleUtil.getInstance().getCircleMembers();
        this.listener = listener;
        this.layoutId = R.layout.member_list;
        imageLoader = VolleyImageRequest.getInstance(context).getImageLoader();
    }

    public void updateInformation(ArrayList<User> members) {
        listOfMembers.clear();
        listOfMembers.addAll(members);
        notifyDataSetChanged();
    }

    public User getMember(int position) {
        return listOfMembers.get(position);
    }

    public class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private final TextView memberName;
        private final ImageView deleteMember;
        private final ImageView editMember;
        private final NetworkImageView memberImage;
        public MyViewHolder(final View v) {
            super(v);
            memberName = v.findViewById(R.id.memberName);
            deleteMember = v.findViewById(R.id.deleteMember);
            memberImage = v.findViewById(R.id.memberImageView);
            memberImage.setDefaultImageResId(R.drawable.logo);
            editMember = v.findViewById(R.id.editMember);
            if (UserUtil.getUserRole().equals("Admin")) {
                deleteMember.setOnClickListener(v1 -> {
                    int memberClickedIndex = getAdapterPosition();
                    // May return NO_POSITION as the RecylerView layout changes, need to do a sanity check
                    // so that it doesn't crash.
                    if (memberClickedIndex != RecyclerView.NO_POSITION)
                        listener.onDeleteClick(memberClickedIndex);
                });
                editMember.setOnClickListener(v1 -> {
                    int memberClickedIndex = getAdapterPosition();
                    // May return NO_POSITION as the RecylerView layout changes, need to do a sanity check
                    // so that it doesn't crash.
                    if (memberClickedIndex != RecyclerView.NO_POSITION)
                        listener.onEditClick(memberClickedIndex);
                });
            } else {
                deleteMember.setVisibility(View.GONE);
                editMember.setVisibility(View.GONE);
            }
        }

        @Override
        public void onClick(View v) { }
    }

    @NonNull
    @Override
    public MemberRecyclerAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false);
        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull MemberRecyclerAdapter.MyViewHolder holder, int position) {
        User user = listOfMembers.get(position);
        if (user.equals(UserUtil.getInstance().getCurrentUser())) {
            holder.deleteMember.setVisibility(View.GONE);
            holder.editMember.setVisibility(View.GONE);
        }
        String fullName = user.getFirstName() + " " + user.getLastName();
        String profilePicUrl = user.getProfilePicUrl();
        if (!profilePicUrl.isEmpty()) {
            imageLoader.get(profilePicUrl, ImageLoader.getImageListener(holder.memberImage, R.drawable.logo, android.R.drawable.ic_dialog_alert));
            holder.memberImage.setImageUrl(user.getProfilePicUrl(), imageLoader);
        }
        holder.memberName.setText(fullName);
    }

    @Override
    public int getItemCount() {
        return listOfMembers.size();
    }

    public interface RecyclerViewClickListener {
        void onDeleteClick(int position);
        void onEditClick(int position);
    }
}
