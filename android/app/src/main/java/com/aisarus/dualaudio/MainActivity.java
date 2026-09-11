package com.aisarus.dualaudio;

import android.app.Activity;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.graphics.Bitmap;
import android.graphics.Color;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.io.InputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;

import fi.iki.elonen.NanoHTTPD;

public class MainActivity extends Activity {
    private final Handler main = new Handler(Looper.getMainLooper());
    private TextView status;
    private TextView urlText;
    private ImageView qrView;
    private LocalHttpServer http;
    private AudioWsServer ws;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        buildUi();
        startServers();
    }

    private TextView text(String s, int sp, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s); t.setTextSize(sp); t.setTextColor(Color.rgb(20,20,20));
        t.setPadding(24,16,24,16);
        if (bold) t.setTypeface(null, android.graphics.Typeface.BOLD);
        return t;
    }

    private void buildUi() {
        ScrollView sv = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(24,32,24,32);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        sv.addView(root);

        root.addView(text("Dual Audio Local", 30, true));
        root.addView(text("Телефон = локальный сервер + приёмник звука. Sony подключи к телефону. Мобильный интернет не нужен.", 16, false));
        status = text("Запускаю локальный сервер…", 18, true); root.addView(status);
        qrView = new ImageView(this);
        root.addView(qrView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 700));
        urlText = text("", 18, true); urlText.setGravity(Gravity.CENTER); root.addView(urlText);
        root.addView(text("1) Включи hotspot на телефоне.\n2) Подключи iPad к hotspot.\n3) Отсканируй QR штатной камерой iPad.\n4) На странице iPad выбери фильм и нажми CONNECT.\n5) Звук с iPad пойдёт в Sony через этот телефон.\n\nМожно полностью выключить мобильные данные после запуска hotspot.", 16, false));
        setContentView(sv);
    }

    private void startServers() {
        try {
            http = new LocalHttpServer(8080); http.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
            ws = new AudioWsServer(new InetSocketAddress(8081)); ws.start();
            refreshAddress();
            status.setText("Сервер запущен ✓ Жду iPad");
        } catch (Exception e) {
            status.setText("Ошибка запуска: " + e.getMessage());
        }
    }

    private void refreshAddress() {
        List<String> ips = privateIps();
        String ip = ips.isEmpty() ? "192.168.43.1" : ips.get(0);
        String url = "http://" + ip + ":8080";
        urlText.setText(url);
        try { qrView.setImageBitmap(makeQr(url, 900)); } catch (Exception e) { status.setText("QR error: "+e.getMessage()); }
    }

    private List<String> privateIps() {
        List<String> out = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> ens = NetworkInterface.getNetworkInterfaces();
            for (NetworkInterface ni : Collections.list(ens)) {
                if (!ni.isUp() || ni.isLoopback()) continue;
                for (InetAddress a : Collections.list(ni.getInetAddresses())) {
                    if (!(a instanceof Inet4Address) || a.isLoopbackAddress()) continue;
                    String s = a.getHostAddress();
                    if (s.startsWith("192.168.") || s.startsWith("10.") || isPrivate172(s)) out.add(s);
                }
            }
        } catch (Exception ignored) {}
        out.sort(Comparator.comparingInt(s -> s.startsWith("192.168.") ? 0 : (s.startsWith("172.") ? 1 : 2)));
        return out;
    }
    private boolean isPrivate172(String s) {
        try { String[] p=s.split("\\."); int n=Integer.parseInt(p[1]); return p[0].equals("172") && n>=16 && n<=31; } catch(Exception e){return false;}
    }

    private Bitmap makeQr(String value, int size) throws Exception {
        BitMatrix m = new MultiFormatWriter().encode(value, BarcodeFormat.QR_CODE, size, size);
        Bitmap b = Bitmap.createBitmap(size,size,Bitmap.Config.RGB_565);
        for(int y=0;y<size;y++) for(int x=0;x<size;x++) b.setPixel(x,y,m.get(x,y)?Color.BLACK:Color.WHITE);
        return b;
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        try { if(http!=null) http.stop(); } catch(Exception ignored){}
        try { if(ws!=null) ws.stop(); } catch(Exception ignored){}
    }

    class LocalHttpServer extends NanoHTTPD {
        LocalHttpServer(int port) { super(port); }
        @Override public Response serve(IHTTPSession session) {
            try {
                InputStream in = getAssets().open("index.html");
                byte[] data = in.readAllBytes(); in.close();
                return newFixedLengthResponse(Response.Status.OK, "text/html; charset=utf-8", new String(data, StandardCharsets.UTF_8));
            } catch(Exception e) {
                return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", e.toString());
            }
        }
    }

    class AudioWsServer extends WebSocketServer {
        private AudioTrack track;
        AudioWsServer(InetSocketAddress a) { super(a); setReuseAddr(true); }
        @Override public void onOpen(WebSocket conn, ClientHandshake hs) { main.post(() -> status.setText("iPad подключён ✓")); }
        @Override public void onClose(WebSocket conn, int code, String reason, boolean remote) { main.post(() -> status.setText("iPad отключился")); releaseTrack(); }
        @Override public void onMessage(WebSocket conn, String msg) {
            if (!msg.startsWith("config:")) return;
            try {
                String[] p=msg.substring(7).split(",");
                int rate=Integer.parseInt(p[0]); int channels=Integer.parseInt(p[1]);
                setupTrack(rate,channels);
                main.post(() -> status.setText("Аудио идёт в телефон ✓"));
            } catch(Exception e){ main.post(() -> status.setText("Audio config error: "+e.getMessage())); }
        }
        @Override public void onMessage(WebSocket conn, ByteBuffer bytes) {
            AudioTrack t=track; if(t==null) return;
            byte[] b=new byte[bytes.remaining()]; bytes.get(b);
            t.write(b,0,b.length,AudioTrack.WRITE_BLOCKING);
        }
        @Override public void onError(WebSocket conn, Exception ex) { main.post(() -> status.setText("WebSocket: "+ex.getMessage())); }
        @Override public void onStart() {}

        private synchronized void setupTrack(int rate,int channels){
            releaseTrack();
            int mask=channels==1?AudioFormat.CHANNEL_OUT_MONO:AudioFormat.CHANNEL_OUT_STEREO;
            int min=AudioTrack.getMinBufferSize(rate,mask,AudioFormat.ENCODING_PCM_16BIT);
            AudioAttributes attrs=new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MOVIE).build();
            AudioFormat fmt=new AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(rate).setChannelMask(mask).build();
            track=new AudioTrack(attrs,fmt,Math.max(min*4,32768),AudioTrack.MODE_STREAM,AudioManager.AUDIO_SESSION_ID_GENERATE);
            track.play();
        }
        private synchronized void releaseTrack(){ try{if(track!=null){track.pause();track.flush();track.release();}}catch(Exception ignored){} track=null; }
    }
}
