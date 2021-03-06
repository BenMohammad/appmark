package com.vipapp.appmark2.adapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import android.view.View;
import android.view.ViewGroup;

import com.vipapp.appmark2.callback.PushCallback;
import com.vipapp.appmark2.item.Item;
import com.vipapp.appmark2.menu.DefaultMenu;
import com.vipapp.appmark2.util.ClassUtils;
import com.vipapp.appmark2.util.Thread;
import com.vipapp.appmark2.util.wrapper.mActivity;
import com.vipapp.appmark2.util.wrapper.mContext;
import com.vipapp.appmark2.util.wrapper.mLayoutInflater;

import java.util.ArrayList;

public class DefaultAdapter extends RecyclerView.Adapter {

    private DefaultMenu menu;
    private ArrayList list = new ArrayList();
    private RecyclerView recyclerView;

    private ArrayList<PushCallback<Item>> callbacks = new ArrayList<>();

    public DefaultAdapter(DefaultMenu menu){
        this.menu = menu;
        menu.onAdapterReceived(this);
        update();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        return menu.getViewHolder(viewGroup, i);
    }

    private void pushArray(ArrayList arrayList){
        pushArray(arrayList, true);
    }
    public void pushArray(ArrayList arrayList, boolean need_to_notify) {
        pushArray(arrayList, need_to_notify, false);
    }
    public void pushArray(ArrayList arrayList, boolean need_to_notify, boolean force_notify){
        if(arrayList != null && (!arrayList.equals(list) || force_notify)) {
            list.clear();
            //noinspection unchecked
            list.addAll(arrayList);
            if(need_to_notify)
                Thread.ui(this::notifyDataSetChanged);
        }
    }
    public void transferObjectToMenu(Item item){
        menu.onValueReceived(item);
    }

    public void onRecyclerPushed(RecyclerView recyclerView){
        this.recyclerView = recyclerView;
    }

    @Nullable
    public RecyclerView getRecyclerView() {
        return recyclerView;
    }

    public ArrayList getList() {
        return list;
    }

    public DefaultMenu getMenu() {
        return menu;
    }

    public void update(){
        pushArray(menu.list(mContext.get()));
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int i) {
        if(list.size() > i) {
            menu.bind(viewHolder, list.get(i), i);
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    @Override
    public int getItemViewType(int position) {
        return menu.getItemViewType(position);
    }

    public void addOnPushCallback(PushCallback<Item> callback){
        callbacks.add(callback);
    }

    public void onPush(Item item){
        execCallbacks(item);
    }
    private void execCallbacks(Item item){
        for(PushCallback<Item> callback: callbacks){
            Thread.ui(() -> callback.onComplete(item));
        }
    }
}
