package com.grayfox.android.http;

import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;

class UrlConnectionRequestBuilder extends RequestBuilder {

    private static final int DEFAULT_TIMEOUT = 30000;
    private static final String TAG = UrlConnectionRequestBuilder.class.getSimpleName();

    private HttpURLConnection connection;
    private ArrayList<String[]> formParams;

    UrlConnectionRequestBuilder(String url) {
        try {
            Log.d(TAG, "URL=" + url);
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setConnectTimeout(DEFAULT_TIMEOUT);
            connection.setReadTimeout(DEFAULT_TIMEOUT);
            formParams = new ArrayList<>();
        } catch (IOException e) {
            Log.e(TAG, "Error opening connection", e);
        }
    }

    @Override
    public UrlConnectionRequestBuilder setTimeout(int timeout) {
        connection.setConnectTimeout(timeout);
        connection.setReadTimeout(timeout);
        return this;
    }

    @Override
    public UrlConnectionRequestBuilder setMethod(Method method) {
        Log.d(TAG, "Method=" + method);
        switch (method) {
            case POST:
                connection.setDoOutput(true);
                break;
            case GET:case OPTIONS:case HEAD:case PUT:case DELETE:case TRACE:
                try {
                    connection.setRequestMethod(method.getValue());
                } catch (ProtocolException e) {
                    Log.e(TAG, "Error setting request method", e);
                }
                break;
        }
        return this;
    }

    @Override
    public UrlConnectionRequestBuilder setData(String data) {
        Log.d(TAG, "Data=" + data);
        BufferedOutputStream out = null;
        try {
            out = new BufferedOutputStream(connection.getOutputStream());
            out.write(data.getBytes());
        } catch (IOException e) {
            Log.e(TAG, "Error opening stream", e);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    Log.e(TAG, "Error closing stream", e);
                }
            }
        }
        return this;
    }

    @Override
    public UrlConnectionRequestBuilder addFormParam(String name, String value) {
        Log.d(TAG, "Param={" + name + ", " + value + "}");
        formParams.add(new String[] {name, value});
        return this;
    }

    @Override
    public UrlConnectionRequestBuilder setHeader(Header header, String value) {
        Log.d(TAG, "Header={" + header.getValue() + ", " + value + "}");
        connection.setRequestProperty(header.getValue(), value);
        return this;
    }

    private String encodeFormParams() {
        StringBuilder result = new StringBuilder();
        boolean first = true;

        for (String[] pair : formParams) {
            if (first) first = false;
            else result.append("&");

            try {
                result.append(URLEncoder.encode(pair[0], Charset.UTF_8.getValue()));
                result.append("=");
                result.append(URLEncoder.encode(pair[1], Charset.UTF_8.getValue()));
            } catch (UnsupportedEncodingException e) {
                Log.e(TAG, "Error encoding form paramaters", e);
            }
        }

        return result.toString();
    }

    @Override
    public Integer make() {
        if (!formParams.isEmpty()) {
            String encodedParams = encodeFormParams();
            setData(encodedParams);
        }
        try {
            int responseCode = connection.getResponseCode();
            Log.d(TAG, "responseCode=" + responseCode);
            return responseCode;
        } catch (IOException e) {
            Log.e(TAG, "Error getting response code", e);
        } finally {
            connection.disconnect();
        }

        return null;
    }

    @Override
    public String makeForResult() {
        String responseText = null;
        BufferedReader reader = null;
        if (!formParams.isEmpty()) {
            String encodedParams = encodeFormParams();
            setData(encodedParams);
        }
        try {
            reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            int responseCode = connection.getResponseCode();
            Log.d(TAG, "responseCode=" + responseCode);
            switch (responseCode) {
                case 200:case 201:
                    StringBuilder stringBuilder = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) stringBuilder.append(line).append('\n');
                    responseText = stringBuilder.toString();
                    break;
            }
        } catch (IOException e) {
            Log.e(TAG, "Error getting response", e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    Log.e(TAG, "Error closing stream", e);
                }
            }
            connection.disconnect();
        }

        Log.d(TAG, "responseText=" + responseText);
        return  responseText;
    }
}