package com.example.bai_tap_lon.server;

import  java.io.*;
import  java.net.*;
import  java.util.*;

public class AuctionServer {
    private static final int PORT=8080;
    private static final Map<String,String> users = new HashMap<>(); //cach luu tru bang java, sau doi thanh DB

    //tao tai khoan mau
    static {
        users.put("Admin","Admin");
    }


}
