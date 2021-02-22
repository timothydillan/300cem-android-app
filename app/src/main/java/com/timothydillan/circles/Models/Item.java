package com.timothydillan.circles.Models;
import java.util.HashMap;

public class Item {
    private final HashMap<Integer, HashMap<String, Object>> listOfItems = new HashMap<>();
    private int index = 0;

    public void addItem(String itemTitle, Object activity) {
        HashMap<String, Object> item = new HashMap<>();
        item.put(itemTitle, activity);
        this.listOfItems.put(index, item);
        index++;
    }

    public HashMap<Integer, HashMap<String, Object>> getMap() {
        return this.listOfItems;
    }

    public String getItemName(int position) {
        for (String itemName : this.listOfItems.get(position).keySet())
            return itemName;
        return "";
    }

    public Object getItemValue(int position) {
        for (Object itemValue : this.listOfItems.get(position).values())
            return itemValue;
        return null;
    }
}
