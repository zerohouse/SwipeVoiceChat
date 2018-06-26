package com.example.renov.swipevoicechat.Network.network;

import com.android.volley.NetworkResponse;
import com.android.volley.Response;

public interface VolleyResponseListener<T> extends Response.Listener<T>, Response.ErrorListener {

	void onParse(NetworkResponse response);
}
