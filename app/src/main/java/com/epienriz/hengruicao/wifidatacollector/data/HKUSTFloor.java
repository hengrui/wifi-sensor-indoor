package com.epienriz.hengruicao.wifidatacollector.data;

/**
 * Created by hengruicao on 5/22/16.
 */
public class HKUSTFloor {
    public static HKUSTFloor[] ids = new HKUSTFloor[]{
            new HKUSTFloor("G", "Academic Building"),
            new HKUSTFloor("LG1"),
            new HKUSTFloor("LG3"),
            new HKUSTFloor("LG4"),
            new HKUSTFloor("LG5"),
            new HKUSTFloor("LG7"),
            new HKUSTFloor("1"),
            new HKUSTFloor("2"),
            new HKUSTFloor("3"),
            new HKUSTFloor("4"),
            new HKUSTFloor("5"),
            new HKUSTFloor("6"),
            new HKUSTFloor("7"),

            new HKUSTFloor("CYTG", "CYT"),
            new HKUSTFloor("CYTUG"),
            new HKUSTFloor("CYT1"),
            new HKUSTFloor("CYT2"),
            new HKUSTFloor("CYT3"),
            new HKUSTFloor("CYT4"),
            new HKUSTFloor("CYT5"),
            new HKUSTFloor("CYT6"),
            new HKUSTFloor("CYT7"),

            new HKUSTFloor("IASG", "IAS"),
            new HKUSTFloor("IAS1"),
            new HKUSTFloor("IAS2"),
            new HKUSTFloor("IAS3"),
            new HKUSTFloor("IAS4"),
            new HKUSTFloor("IAS5"),

            new HKUSTFloor("LSKG", "LSK"),
            new HKUSTFloor("LSK1"),
            new HKUSTFloor("LSK2"),
            new HKUSTFloor("LSK3"),
            new HKUSTFloor("LSK4"),
            new HKUSTFloor("LSK5"),
            new HKUSTFloor("LSK6"),
            new HKUSTFloor("LSK7")
    };

    public int id;
    public String floor;
    public String category;

    public HKUSTFloor(String mFloor) {
        floor = mFloor;
    }
    public HKUSTFloor(String mFloor, String mCategory) {
        floor = mFloor;
        category = mCategory;
    }
}
