package de.autopolish.nativeapp;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

final class ApiClient {
    static final String BASE_URL = "https://auftraege.auto-polish.de";
    interface ConnectionFactory { HttpURLConnection open(String path) throws IOException; }
    static ConnectionFactory connections = path -> (HttpURLConnection) new URL(BASE_URL + path).openConnection();

    interface Callback {
        void complete(JSONObject result, Exception error);
    }

    private ApiClient() {}

    static JSONObject login(String username, String password) throws Exception {
        JSONObject body = new JSONObject();
        body.put("username", username);
        body.put("password", password);
        return request("POST", "/api/auth/login", body);
    }

    static JSONObject orders(String query) throws Exception {
        String path = "/api/orders";
        if (query != null && !query.trim().isEmpty()) {
            path += "?q=" + URLEncoder.encode(query.trim(), "UTF-8");
        }
        return request("GET", path, null);
    }

    static JSONObject orderStats() throws Exception {
        return request("GET", "/api/orders?stats=1", null);
    }

    static JSONObject order(String id) throws Exception {
        return request("GET", "/api/orders?id=" + URLEncoder.encode(id, "UTF-8"), null);
    }

    static JSONObject saveOrder(String id, JSONObject payload) throws Exception {
        if (id == null || id.isEmpty()) return request("POST", "/api/orders", payload);
        return request("PUT", "/api/orders?id=" + URLEncoder.encode(id, "UTF-8"), payload);
    }

    static JSONObject updateStatus(String id, String status) throws Exception {
        JSONObject body = new JSONObject();
        body.put("status", status);
        return request("PATCH", "/api/orders?id=" + URLEncoder.encode(id, "UTF-8"), body);
    }

    static JSONObject deleteOrder(String id, String systemPassword) throws Exception {
        JSONObject body = new JSONObject();
        body.put("password", systemPassword);
        return request("DELETE", "/api/orders?id=" + URLEncoder.encode(id, "UTF-8"), body);
    }

    static String pdfUrl(String id) {
        return BASE_URL + "/api/orders/" + id + "/pdf";
    }

    static JSONObject request(String method, String path, JSONObject body) throws Exception {
        return request(method, path, body, null);
    }

    static JSONObject request(String method, String path, JSONObject body, String systemPassword) throws Exception {
        HttpURLConnection connection = connections.open(path);
        connection.setRequestMethod(method);
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(25000);
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("User-Agent", "Auto-Polish-Native-Android/1.3.17");
        if (systemPassword != null) connection.setRequestProperty("x-system-password", systemPassword);
        try {
        if (body != null) {
            byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            connection.setFixedLengthStreamingMode(bytes.length);
            try (OutputStream output = connection.getOutputStream()) {
                output.write(bytes);
            }
        }
        int status = connection.getResponseCode();
        InputStream stream = status >= 200 && status < 300 ? connection.getInputStream() : connection.getErrorStream();
        String text = read(stream);
        JSONObject result = text.isEmpty() ? new JSONObject() : new JSONObject(text);
        if (status < 200 || status >= 300) {
            throw new IOException(result.optString("error", "Serverfehler (" + status + ")"));
        }
        return result;
        } finally { connection.disconnect(); }
    }

    static String upload(byte[] bytes, String id, String kind, String mime) throws Exception {
        if(bytes.length > 4*1024*1024)throw new IOException("Das Bild ist zu groß.");
        String boundary="AutoPolish"+java.util.UUID.randomUUID().toString().replace("-","");
        HttpURLConnection c=connections.open("/api/uploads");
        c.setRequestMethod("POST");c.setConnectTimeout(15000);c.setReadTimeout(60000);c.setDoOutput(true);
        c.setRequestProperty("Content-Type","multipart/form-data; boundary="+boundary);
        java.io.ByteArrayOutputStream body=new java.io.ByteArrayOutputStream();
        String prefix="--"+boundary+"\r\nContent-Disposition: form-data; name=\"orderId\"\r\n\r\n"+id+"\r\n--"+boundary+"\r\nContent-Disposition: form-data; name=\"kind\"\r\n\r\n"+kind+"\r\n--"+boundary+"\r\nContent-Disposition: form-data; name=\"file\"; filename=\"image."+(mime.equals("image/png")?"png":"jpg")+"\"\r\nContent-Type: "+mime+"\r\n\r\n";
        body.write(prefix.getBytes(StandardCharsets.UTF_8));body.write(bytes);body.write(("\r\n--"+boundary+"--\r\n").getBytes(StandardCharsets.UTF_8));
        c.setFixedLengthStreamingMode(body.size());
        try {try(OutputStream out=c.getOutputStream()){body.writeTo(out);}int status=c.getResponseCode();JSONObject result=new JSONObject(read(status<400?c.getInputStream():c.getErrorStream()));if(status>=400)throw new IOException(result.optString("error","Bild konnte nicht gespeichert werden."));return result.getString("key");} finally {c.disconnect();}
    }

    static byte[] download(String path, int limit) throws Exception {return download(path,limit,60000);}
    static byte[] download(String path, int limit,int timeout) throws Exception {
        if(!path.startsWith("/api/"))throw new IOException("Ungültiger Dokumentpfad.");
        HttpURLConnection c=connections.open(path);c.setConnectTimeout(15000);c.setReadTimeout(timeout);
        try {if(c.getResponseCode()!=200)throw new IOException("Dokument konnte nicht geladen werden ("+c.getResponseCode()+").");try(InputStream in=c.getInputStream();java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream()){byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1){if(out.size()+n>limit)throw new IOException("Die Datei ist zu groß.");out.write(buffer,0,n);}return out.toByteArray();}}finally{c.disconnect();}
    }

    private static String read(InputStream stream) throws IOException {
        if (stream == null) return "";
        StringBuilder result = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) result.append(line);
        }
        return result.toString();
    }
}

