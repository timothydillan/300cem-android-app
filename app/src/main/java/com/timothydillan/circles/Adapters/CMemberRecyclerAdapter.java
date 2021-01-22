package com.timothydillan.circles.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.timothydillan.circles.Models.User;
import com.timothydillan.circles.R;

import java.util.ArrayList;

public class CMemberRecyclerAdapter extends RecyclerView.Adapter<CMemberRecyclerAdapter.MyViewHolder> {
    private final ArrayList<User> listOfMembers;
    private final RecyclerViewClickListener listener;
    private final @LayoutRes int layoutId;

    public CMemberRecyclerAdapter(ArrayList<User> listOfMembers, RecyclerViewClickListener listener, @LayoutRes int layoutId) {
        this.listOfMembers = listOfMembers;
        this.listener = listener;
        this.layoutId = layoutId;
    }

    public void updateInformation(ArrayList<User> members){
        listOfMembers.clear();
        listOfMembers.addAll(members);
        notifyDataSetChanged();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private final TextView memberName;
        private final TextView lastSeen;
        public MyViewHolder(final View v) {
            super(v);
            memberName = v.findViewById(R.id.memberName);
            lastSeen = v.findViewById(R.id.lastSeenMember);
            v.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            listener.onClick(v, getAdapterPosition());
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
        String name = listOfMembers.get(position).getFullName();
        String lastSharingTime = "Since " + listOfMembers.get(position).getLastSharingTime();
        holder.memberName.setText(name);
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
