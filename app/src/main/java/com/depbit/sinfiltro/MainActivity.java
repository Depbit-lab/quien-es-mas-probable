package com.depbit.sinfiltro;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Window;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebResourceRequest;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Locale;

public class MainActivity extends Activity {
    private WebView webView;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = getWindow();
        window.setStatusBarColor(Color.rgb(11,11,14));
        window.setNavigationBarColor(Color.rgb(11,11,14));

        webView = new WebView(this);
        setContentView(webView);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setUserAgentString(settings.getUserAgentString() + " QuienEsMasProbable/0.4.0");
        webView.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                if ("file".equals(uri.getScheme())) return false;
                try { startActivity(new Intent(Intent.ACTION_VIEW, uri)); } catch (Exception ignored) {}
                return true;
            }
        });
        webView.setWebChromeClient(new WebChromeClient());
        webView.addJavascriptInterface(new AndroidBridge(), "Android");
        webView.loadUrl("file:///android_asset/index.html");
    }

    @Override public void onBackPressed() {
        if (webView == null) { super.onBackPressed(); return; }
        // Un <dialog> modal no crea historial, asi que sin esto el boton atras cerraria la app
        // dejando al usuario atrapado en el formulario.
        webView.evaluateJavascript(
            "(function(){var d=document.querySelector('dialog[open]');if(!d)return false;d.close();return true;})()",
            value -> {
                if ("true".equals(value)) return;
                if (webView.canGoBack()) webView.goBack(); else finish();
            });
    }

    public class AndroidBridge {
        private final SecureRandom random = new SecureRandom();

        @JavascriptInterface public String getPubkey() {
            try { return NostrCrypto.getPublicKeyHex(getSecretKey()); }
            catch (Exception e) { return ""; }
        }

        @JavascriptInterface public String signEvent(String templateJson) {
            try {
                JSONObject input = new JSONObject(templateJson);
                int kind = input.getInt("kind");
                long createdAt = input.getLong("created_at");
                JSONArray tags = input.getJSONArray("tags");
                String content = input.getString("content");
                byte[] sk = getSecretKey();
                String pubkey = NostrCrypto.getPublicKeyHex(sk);

                JSONArray serial = new JSONArray();
                serial.put(0); serial.put(pubkey); serial.put(createdAt); serial.put(kind); serial.put(tags); serial.put(content);
                String canonical = input.optString("canonical", serial.toString());
                byte[] idBytes = NostrCrypto.sha256(canonical.getBytes(StandardCharsets.UTF_8));
                byte[] aux = new byte[32]; random.nextBytes(aux);
                byte[] signature = NostrCrypto.schnorrSign(idBytes, sk, aux);

                JSONObject event = new JSONObject();
                event.put("id", NostrCrypto.hex(idBytes));
                event.put("pubkey", pubkey);
                event.put("created_at", createdAt);
                event.put("kind", kind);
                event.put("tags", tags);
                event.put("content", content);
                event.put("sig", NostrCrypto.hex(signature));
                return event.toString();
            } catch (Exception e) { return ""; }
        }

        @JavascriptInterface public boolean verifyEvent(String eventJson, String canonical) {
            try {
                JSONObject event = new JSONObject(eventJson);
                JSONArray serial = new JSONArray();
                serial.put(0); serial.put(event.getString("pubkey")); serial.put(event.getLong("created_at"));
                serial.put(event.getInt("kind")); serial.put(event.getJSONArray("tags")); serial.put(event.getString("content"));
                byte[] id = NostrCrypto.sha256((canonical == null || canonical.isEmpty() ? serial.toString() : canonical).getBytes(StandardCharsets.UTF_8));
                if (!NostrCrypto.hex(id).equalsIgnoreCase(event.getString("id"))) return false;
                return NostrCrypto.schnorrVerify(id, NostrCrypto.unhex(event.getString("pubkey")), NostrCrypto.unhex(event.getString("sig")));
            } catch (Exception e) { return false; }
        }

        @JavascriptInterface public void shareText(String title, String text) {
            runOnUiThread(() -> {
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("text/plain"); intent.putExtra(Intent.EXTRA_SUBJECT, title); intent.putExtra(Intent.EXTRA_TEXT, text);
                startActivity(Intent.createChooser(intent, getString(R.string.share_text_chooser)));
            });
        }

        @JavascriptInterface public void shareApp() {
            runOnUiThread(() -> {
                try {
                    File source = new File(getApplicationInfo().sourceDir);
                    File dir = new File(getCacheDir(), "shared"); if (!dir.exists()) dir.mkdirs();
                    File target = new File(dir, "QuienEsMasProbable.apk"); copy(source, target);
                    Uri uri = Uri.parse("content://" + getPackageName() + ".files/apk/QuienEsMasProbable.apk");
                    Intent intent = new Intent(Intent.ACTION_SEND);
                    intent.setType("application/vnd.android.package-archive");
                    intent.putExtra(Intent.EXTRA_STREAM, uri);
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(Intent.createChooser(intent, getString(R.string.share_app_chooser)));
                } catch (Exception ignored) {}
            });
        }

        private byte[] getSecretKey() throws Exception {
            String stored = getSharedPreferences("nostr_identity", MODE_PRIVATE).getString("secret", null);
            if (stored != null && stored.length() == 64) return NostrCrypto.unhex(stored);
            byte[] key = new byte[32];
            do { random.nextBytes(key); } while (!NostrCrypto.isValidSecret(key));
            getSharedPreferences("nostr_identity", MODE_PRIVATE).edit().putString("secret", NostrCrypto.hex(key)).apply();
            return key;
        }
    }

    private static void copy(File source, File target) throws Exception {
        try (FileInputStream in = new FileInputStream(source); FileOutputStream out = new FileOutputStream(target)) {
            byte[] buffer = new byte[8192]; int count; while ((count = in.read(buffer)) > 0) out.write(buffer, 0, count);
        }
    }
}
