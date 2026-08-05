package com.depbit.sinfiltro;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Insets;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.widget.FrameLayout;
import android.window.OnBackInvokedDispatcher;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebResourceRequest;

import org.json.JSONArray;
import org.json.JSONObject;

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
        // Desde Android 15 estas dos llamadas no hacen nada: el color de las barras sale del
        // fondo de la ventana. Se quedan porque en Android 14 y anteriores siguen siendo utiles.
        window.setStatusBarColor(Color.rgb(11,11,14));
        window.setNavigationBarColor(Color.rgb(11,11,14));

        webView = new WebView(this);
        FrameLayout root = new FrameLayout(this);
        root.addView(webView, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(root);
        reserveSystemBarSpace(root);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setUserAgentString(settings.getUserAgentString() + " QuienEsMasProbable/0.7.1");
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

        // Con targetSdk 36 el sistema activa el gesto atras predictivo y deja de llamar a
        // onBackPressed(). Registramos el callback nuevo; en Android 12 y anteriores no existe
        // y sigue entrando por onBackPressed(), asi que ambos caminos hacen lo mismo.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_DEFAULT, this::goBack);
        }
    }

    // Android 16 obliga a dibujar de borde a borde. Reservamos aqui el hueco de las barras y
    // le devolvemos al WebView unos insets a cero, para que el env(safe-area-inset-*) del CSS
    // no vuelva a sumar el mismo margen y el contenido acabe con el doble de separacion.
    private void reserveSystemBarSpace(View root) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return;
        root.setOnApplyWindowInsetsListener((view, windowInsets) -> {
            int types = WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout();
            Insets bars = windowInsets.getInsets(types);
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return new WindowInsets.Builder(windowInsets).setInsets(types, Insets.NONE).build();
        });
    }

    @Override public void onBackPressed() {
        if (webView == null) { super.onBackPressed(); return; }
        goBack();
    }

    private void goBack() {
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

        private byte[] getSecretKey() throws Exception {
            String stored = getSharedPreferences("nostr_identity", MODE_PRIVATE).getString("secret", null);
            if (stored != null && stored.length() == 64) return NostrCrypto.unhex(stored);
            byte[] key = new byte[32];
            do { random.nextBytes(key); } while (!NostrCrypto.isValidSecret(key));
            getSharedPreferences("nostr_identity", MODE_PRIVATE).edit().putString("secret", NostrCrypto.hex(key)).apply();
            return key;
        }
    }
}
