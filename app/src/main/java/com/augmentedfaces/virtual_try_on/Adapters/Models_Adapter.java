package com.augmentedfaces.virtual_try_on.Adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.augmentedfaces.virtual_try_on.R;

import java.util.ArrayList;

public class Models_Adapter extends RecyclerView.Adapter<Models_Adapter.MyViewHolder> {

    private ArrayList<Integer> models;
    private Context context;

    public class MyViewHolder extends RecyclerView.ViewHolder {
        public ImageView img_model;

        public MyViewHolder(View view) {
            super(view);
            img_model = view.findViewById(R.id.imageButton);

        }
    }


    public Models_Adapter(Context context, ArrayList<Integer> models) {
        this.context = context;
        this.models = models;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.models_list_row, parent, false);


        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        holder.img_model.setImageDrawable(ResourcesCompat.getDrawable(context.getResources(),
                models.get(position), null));

    }

    @Override
    public int getItemCount() {
        return models.size();
    }

}
