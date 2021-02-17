package com.timothydillan.circles.Models;
import java.util.HashMap;

public class ItemModel {
    private final HashMap<Integer, HashMap<String, Class<?>>> settingsList = new HashMap<>();
    private int index = 0;

    public void addItem(String itemTitle, Class<?> activity) {
        HashMap<String, Class<?>> item = new HashMap<>();
        item.put(itemTitle, activity);
        this.settingsList.put(index, item);
        index++;
    }

    public HashMap<Integer, HashMap<String, Class<?>>> getMap() {
        return this.settingsList;
    }

    public String getItemName(int position) {
        for (String itemName : this.settingsList.get(position).keySet())
            return itemName;
        return "";
    }

    public Class<?> getItemActivity(int position) {
        for (Class<?> itemActivity : this.settingsList.get(position).values())
            return itemActivity;
        return null;
    }
}
