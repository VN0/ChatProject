package com.yzx.chat.core.net;

import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.yzx.chat.configure.Constants;
import com.yzx.chat.core.AppClient;
import com.yzx.chat.core.entity.DecryptedResponse;
import com.yzx.chat.core.entity.EncryptedRequest;
import com.yzx.chat.core.entity.JsonRequest;
import com.yzx.chat.core.net.framework.ApiProxy;
import com.yzx.chat.core.net.framework.HttpConverter;
import com.yzx.chat.core.net.framework.PartContent;
import com.yzx.chat.core.util.AESUtil;
import com.yzx.chat.core.util.Base64Util;
import com.yzx.chat.core.util.ECCUtil;
import com.yzx.chat.core.util.ECDHUtil;
import com.yzx.chat.core.util.LogUtil;

import java.io.IOException;
import java.lang.reflect.Type;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Locale;
import java.util.Map;

import javax.crypto.SecretKey;

import androidx.annotation.Nullable;

/**
 * Created by YZX on 2017年10月15日.
 * 生命太短暂,不要去做一些根本没有人想要的东西
 */


public class ApiHelper {

    public static final Gson GSON;
    private static final ApiProxy API_PROXY;

    private static final String CLIENT_PUBLIC_KEY;
    private static final SecretKey AES_KEY;
    private static final Signature CLIENT_SIGNATURE;
    private static final Signature SERVER_SIGNATURE;

    static {
        GSON = new GsonBuilder()
                .serializeNulls()
                .enableComplexMapKeySerialization()
                .disableHtmlEscaping()
                .registerTypeAdapter(String.class, new TypeAdapter<String>() {
                    @Override
                    public String read(JsonReader reader) throws IOException {
                        if (reader.peek() == JsonToken.NULL) {
                            reader.nextNull();
                            return "";
                        }
                        return reader.nextString();
                    }

                    @Override
                    public void write(JsonWriter writer, String value) throws IOException {
                        if (value == null) {
                            writer.value("");
                            return;
                        }
                        writer.value(value);
                    }
                })
                .create();

        API_PROXY = new ApiProxy(Constants.URL_API_BASE, new HttpConverter() {

            @Nullable
            @Override
            public byte[] convertRequest(String url, Map<String, Object> requestParams) {
                JsonRequest jsonRequest = new JsonRequest();
                jsonRequest.setDeviceID(AppClient.getInstance().getDeviceID());
                jsonRequest.setToken(AppClient.getInstance().getToken());
                jsonRequest.setParams(requestParams);
                String strData = GSON.toJson(jsonRequest);
                EncryptedRequest encryptedRequest = new EncryptedRequest();
                encryptedRequest.setSecretKey(CLIENT_PUBLIC_KEY);
                encryptedRequest.setSignature(Base64Util.encodeToString(ECCUtil.sign(CLIENT_SIGNATURE, strData.getBytes())));
                encryptedRequest.setData(Base64Util.encodeToString(AESUtil.encrypt(strData.getBytes(), AES_KEY)));
                String strEncryptedData = GSON.toJson(encryptedRequest);
                LogUtil.d(String.format(
                        Locale.getDefault(),
                        "\n正在发出请求：url = %s\n打印请求内容：%s\n完整加密内容：%s",
                        url,
                        strData,
                        strEncryptedData
                ));
                return strEncryptedData.getBytes();
            }

            @Override
            public PartContent convertMultipartRequest(String url, String partName, Map<String, Object> requestParams) {
                PartContent partContent = new PartContent();
                partContent.setContentType("application/json");
                partContent.setContent(convertRequest(url, requestParams));
                return partContent;
            }

            @Nullable
            @Override
            public Object convertResponseBody(String url, byte[] body, Type genericType) {
                String responseData = new String(body);
                String decryptData = null;
                try {
                    DecryptedResponse decryptedResponse;
                    decryptedResponse = GSON.fromJson(responseData, DecryptedResponse.class);
                    if (decryptedResponse == null) {
                        return null;
                    }
                    String signature = decryptedResponse.getSignature();
                    if (TextUtils.isEmpty(signature)) {
                        return null;
                    }
                    String data = decryptedResponse.getData();
                    if (TextUtils.isEmpty(data)) {
                        return null;
                    }
                    byte[] originalData = AESUtil.decrypt(Base64Util.decode(data), AES_KEY);
                    if (originalData == null || originalData.length == 0) {
                        return null;
                    }
                    if (!ECCUtil.verify(SERVER_SIGNATURE, originalData, Base64Util.decode(signature))) {
                        return null;
                    }
                    decryptData = new String(originalData);
                    decryptedResponse = GSON.fromJson(decryptData, DecryptedResponse.class);
                    return decryptedResponse;
                } catch (Exception e) {
                    LogUtil.w(e.toString(), e);
                } finally {
                    LogUtil.d(String.format(
                            Locale.getDefault(),
                            "\n正在处理响应：url = %s\n打印响应内容：%s\n解密响应内容：%s",
                            url,
                            responseData,
                            decryptData
                    ));
                }
                return null;
            }
        });

        KeyPair ecKeyPair = ECCUtil.generateECCKeyPair("secp256r1");
        PublicKey clientPublicKey = ECCUtil.loadECPublicKey(Base64Util.decode("MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEK5txd+iIlJMWJNnr9Iwl/99MaOfuH050Vs3NYSCS\n" +
                "+1jpKctixugvZlNj1URhpd/jChCac8CLcW+7DdnK6nrb9A==\n"));
        PrivateKey clientPrivateKey = ECCUtil.loadECPrivateKey(Base64Util.decode("MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgcGWf6S5mZT+XsCwy0J6JLElEsn1M\n" +
                "2r/ziiYMZlKOWpehRANCAAQrm3F36IiUkxYk2ev0jCX/30xo5+4fTnRWzc1hIJL7WOkpy2LG6C9m\n" +
                "U2PVRGGl3+MKEJpzwItxb7sN2crqetv0\n"));
        PublicKey serverPublicKey = ECCUtil.loadECPublicKey(Base64Util.decode(Constants.SERVER_PUBLIC_KEY));

        CLIENT_PUBLIC_KEY = Base64Util.encodeToString(clientPublicKey.getEncoded());
        AES_KEY = AESUtil.loadKey(ECDHUtil.ecdh(clientPrivateKey, serverPublicKey));
        CLIENT_SIGNATURE = ECCUtil.loadSignature(clientPrivateKey);
        SERVER_SIGNATURE = ECCUtil.loadSignature(serverPublicKey);
    }

    public static <T> T getProxyInstance(Class<T> interfaceClass) {
        return API_PROXY.getProxyInstance(interfaceClass);
    }


}
