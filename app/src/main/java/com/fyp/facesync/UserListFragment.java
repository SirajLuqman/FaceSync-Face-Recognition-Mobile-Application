package com.fyp.facesync;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class UserListFragment extends Fragment {

    private String listType;
    private RecyclerView rvFragmentList;
    private List<UserMember> fullList = new ArrayList<>(); // To store original data

    public static UserListFragment newInstance(String type) {
        UserListFragment fragment = new UserListFragment();
        Bundle args = new Bundle();
        args.putString("list_type", type);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            listType = getArguments().getString("list_type");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_user_list, container, false);
        rvFragmentList = view.findViewById(R.id.rvFragmentList);
        rvFragmentList.setLayoutManager(new LinearLayoutManager(getContext()));
        rvFragmentList.setAdapter(new MemberAdapter(new ArrayList<>()));

        loadData();
        return view;
    }

    private void loadData() {
        // 1. Get the Dynamic IP from Settings
        android.content.SharedPreferences prefs = requireContext().getSharedPreferences("FaceSyncPrefs", android.content.Context.MODE_PRIVATE);
        String ip = prefs.getString("server_ip", "10.0.2.2");
        String dynamicBaseUrl = "http://" + ip + ":5000/";

        // 2. Use your central RetrofitClient instead of building a new one
        ApiService apiService = RetrofitClient.getClient(dynamicBaseUrl).create(ApiService.class);

        apiService.getUsers(listType).enqueue(new Callback<List<UserMember>>() {
            @Override
            public void onResponse(Call<List<UserMember>> call, Response<List<UserMember>> response) {
                if (!isAdded() || getContext() == null) return;
                if (response.isSuccessful() && response.body() != null) {
                    fullList = response.body(); // Save original data
                    rvFragmentList.setAdapter(new MemberAdapter(fullList));
                }
            }

            @Override
            public void onFailure(Call<List<UserMember>> call, Throwable t) {}
        });
    }

    // Logic to filter list based on search query
    public void filter(String query) {
        List<UserMember> filteredList = new ArrayList<>();
        for (UserMember item : fullList) {
            if (item.name.toLowerCase().contains(query.toLowerCase()) ||
                    item.id.toLowerCase().contains(query.toLowerCase())) {
                filteredList.add(item);
            }
        }
        rvFragmentList.setAdapter(new MemberAdapter(filteredList));
    }

    public static class UserMember {
        public String name, role, id;
        public UserMember(String name, String role, String id) {
            this.name = name; this.role = role; this.id = id;
        }
    }

    class MemberAdapter extends RecyclerView.Adapter<MemberAdapter.MemberViewHolder> {
        List<UserMember> list;
        MemberAdapter(List<UserMember> list) { this.list = list; }

        @NonNull
        @Override
        public MemberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user, parent, false);
            return new MemberViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull MemberViewHolder holder, int position) {
            UserMember user = list.get(position);
            holder.name.setText(user.name);
            holder.details.setText("ID: " + user.id + " | " + user.role);
            // Set the click listener for the trash icon
            holder.ivDelete.setOnClickListener(v -> {
                // Show confirmation dialog before deleting
                // Pass R.style.CustomAlertDialog as the second argument
                new android.app.AlertDialog.Builder(v.getContext(), R.style.CustomAlertDialog)
                        .setTitle("Confirm Delete")
                        .setMessage("Are you sure you want to delete " + user.name + "?")
                        .setPositiveButton("Delete", (dialog, which) -> {
                            deleteUserFromDatabase(user, position);
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        }

        private void deleteUserFromDatabase(UserMember user, int position) {
            // 1. Get current IP again to be safe
            android.content.SharedPreferences prefs = requireContext().getSharedPreferences("FaceSyncPrefs", android.content.Context.MODE_PRIVATE);
            String ip = prefs.getString("server_ip", "10.0.2.2");
            String dynamicBaseUrl = "http://" + ip + ":5000/";

            // 2. Pass the URL to the client
            ApiService apiService = RetrofitClient.getClient(dynamicBaseUrl).create(ApiService.class);

            // 2. Call the API (listType is either "user" or "admin")
            apiService.deleteUser(listType, user.id).enqueue(new retrofit2.Callback<okhttp3.ResponseBody>() {
                @Override
                public void onResponse(retrofit2.Call<okhttp3.ResponseBody> call, retrofit2.Response<okhttp3.ResponseBody> response) {
                    if (response.isSuccessful()) {
                        // 3. Remove from local list
                        list.remove(position);

                        // 4. Animate the removal
                        notifyItemRemoved(position);

                        // 5. Update positions of remaining items to prevent crashes
                        notifyItemRangeChanged(position, list.size());

                        // 6. Keep search data in sync
                        fullList.remove(user);

                        android.widget.Toast.makeText(getContext(), "User Deleted", android.widget.Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(retrofit2.Call<okhttp3.ResponseBody> call, Throwable t) {
                    android.widget.Toast.makeText(getContext(), "Server Error: " + t.getMessage(), android.widget.Toast.LENGTH_SHORT).show();
                }
            });
        }

        @Override
        public int getItemCount() { return list.size(); }

        class MemberViewHolder extends RecyclerView.ViewHolder {
            TextView name, details;
            ImageView ivDelete;
            MemberViewHolder(View itemView) {
                super(itemView);
                name = itemView.findViewById(R.id.tvUserName);
                details = itemView.findViewById(R.id.tvUserDetails);
                ivDelete = itemView.findViewById(R.id.ivDelete);
            }
        }
    }
}