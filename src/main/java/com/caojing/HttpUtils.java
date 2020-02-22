package com.caojing;

import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.*;

public class HttpUtils {

    private static final MultiThreadedHttpConnectionManager connectionManager = new MultiThreadedHttpConnectionManager();
    private static final org.apache.commons.httpclient.HttpClient client = new org.apache.commons.httpclient.HttpClient(connectionManager);
    private static final Log logger = LogFactory.getLog(HttpUtils.class);

    /**
     * get
     *
     * @param host
     * @param path
     * @param method
     * @param headers
     * @param querys
     * @return
     * @throws Exception
     */
    public static HttpResponse doGet(String host, String path, String method,
                                     Map<String, String> headers,
                                     Map<String, String> querys)
        throws Exception {
        HttpClient httpClient = wrapClient(host);

        HttpGet request = new HttpGet(buildUrl(host, path, querys));
        for (Map.Entry<String, String> e : headers.entrySet()) {
            request.addHeader(e.getKey(), e.getValue());
        }

        return httpClient.execute(request);
    }

    public static String httpGet(String url, Map<String, Object> params, Map<String, String> headers) {
        GetMethod get = new GetMethod(url);
        if (headers != null) {
            for (String key : headers.keySet()) {
                get.setRequestHeader(key, headers.get(key));
            }
        }
        org.apache.commons.httpclient.NameValuePair[] nameValuePairs = buildNameValuePairs2(params);
        if (nameValuePairs != null) {
            get.setQueryString(nameValuePairs);
        }
        try {
            client.executeMethod(get);
            if (get.getStatusCode() > 299) {
                return null;
            }
            return get.getResponseBodyAsString();
        } catch (Exception ex) {
            logger.error(url + " get request fail! ", ex);
        } finally {
            get.releaseConnection();
        }

        return null;
    }

    private static org.apache.commons.httpclient.NameValuePair[] buildNameValuePairs2(Map<String, Object> params) {
        org.apache.commons.httpclient.NameValuePair[] data = null;
        if (params != null && params.size() > 0) {
            Set<Map.Entry<String, Object>> entrySet = params.entrySet();
            data = new org.apache.commons.httpclient.NameValuePair[]{};

            List<org.apache.commons.httpclient.NameValuePair> nameValuePairs = new ArrayList<org.apache.commons.httpclient.NameValuePair>();
            for (Map.Entry<String, Object> entry : entrySet) {
                if (entry.getValue() != null) {
                    nameValuePairs.add(new org.apache.commons.httpclient.NameValuePair(entry.getKey(), String.valueOf(entry.getValue())));
                }
            }
            data = nameValuePairs.toArray(data);
        }
        return data;
    }

    /**
     * get请求url
     *
     * @param url 格式如：https://devres.alphalawyer.cn/fiscal_tax/22f95464c22240cd90b8d94a79c7a7e1
     * @return HttpResponse
     * @author CaoJing
     * @date 2019/3/16 13:12
     */
    public static HttpResponse doGet(String url) throws Exception {
        return doGet(url, null, "GET", new HashMap<>(), new HashMap<>());
    }

    /**
     * get请求url
     *
     * @param url 格式如：https://devres.alphalawyer.cn/fiscal_tax/22f95464c22240cd90b8d94a79c7a7e1
     * @return HttpResponse
     * @author CaoJing
     * @date 2019/3/16 13:12
     */
    public static HttpResponse doGet(Map<String, String> headers, String url) throws Exception {
        return doGet(url, null, "GET", headers, new HashMap<>());
    }

    /**
     * post form
     *
     * @param host
     * @param path
     * @param headers
     * @param querys
     * @param bodys
     * @return
     * @throws Exception
     */
    public static HttpResponse doPost(String host, String path,
                                      Map<String, String> headers,
                                      Map<String, String> querys,
                                      Map<String, String> bodys)
        throws Exception {
        HttpClient httpClient = wrapClient(host);

        HttpPost request = new HttpPost(buildUrl(host, path, querys));
        for (Map.Entry<String, String> e : headers.entrySet()) {
            request.addHeader(e.getKey(), e.getValue());
        }

        if (bodys != null) {
            List<NameValuePair> nameValuePairList = new ArrayList<NameValuePair>();

            for (String key : bodys.keySet()) {
                nameValuePairList.add(new BasicNameValuePair(key, bodys.get(key)));
            }
            UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(nameValuePairList, "utf-8");
            formEntity.setContentType("application/x-www-form-urlencoded; charset=UTF-8");
            request.setEntity(formEntity);
        }

        return httpClient.execute(request);
    }

    /**
     * Post String
     *
     * @param host
     * @param path
     * @param headers
     * @param querys
     * @param body
     * @return
     * @throws Exception
     */
    public static HttpResponse doPost(String host, String path,
                                      Map<String, String> headers,
                                      Map<String, String> querys,
                                      String body)
        throws Exception {
        HttpClient httpClient = wrapClient(host);

        HttpPost request = new HttpPost(buildUrl(host, path, querys));
        for (Map.Entry<String, String> e : headers.entrySet()) {
            request.addHeader(e.getKey(), e.getValue());
        }

        if (StringUtils.isNotBlank(body)) {
            request.setEntity(new StringEntity(body, "utf-8"));
        }

        return httpClient.execute(request);
    }

    /**
     * Post stream
     *
     * @param host
     * @param path
     * @param headers
     * @param querys
     * @param body
     * @return
     * @throws Exception
     */
    public static HttpResponse doPost(String host, String path,
                                      Map<String, String> headers,
                                      Map<String, String> querys,
                                      byte[] body)
        throws Exception {
        HttpClient httpClient = wrapClient(host);

        HttpPost request = new HttpPost(buildUrl(host, path, querys));
        for (Map.Entry<String, String> e : headers.entrySet()) {
            request.addHeader(e.getKey(), e.getValue());
        }

        if (body != null) {
            request.setEntity(new ByteArrayEntity(body));
        }

        return httpClient.execute(request);
    }

    public static String httpPostJson(String url, String json, Map<String, String> headers) {
        return httpPostJson(url, json, null, false, headers);
    }

    public static String httpPostJson(String url, String json) {
        return httpPostJson(url, json, null, false);
    }

    public static String httpPostJson(String url, String json, String token) {
        return httpPostJson(url, json, token, false);
    }

    public static String httpPostJson(String url, String json, String token, boolean keepLive) {
        return httpPostJson(url, json, token, keepLive, null);
    }

    public static String httpPostJson(String url, String json, String token, boolean keepLive, Map<String, String> headers) {
        PostMethod post = new PostMethod(url);
        if (keepLive) {
            post.setRequestHeader("Connection", "Keep-Alive");
        }
        if (Objects.nonNull(headers)) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                post.setRequestHeader(entry.getKey(), entry.getValue());
            }
        }
        if (Objects.nonNull(token)) {
            post.setRequestHeader("token", token);
        }
        try {
            post.setRequestEntity(new StringRequestEntity(json, "application/json", "UTF-8"));
            client.executeMethod(post);
            if (post.getStatusCode() > 299) {
                logger.error(url + " post request fail! ");
                return null;
            }
            return post.getResponseBodyAsString();
        } catch (Exception ex) {
            logger.error(url + " post request fail! " + ex.getMessage());
            throw new RuntimeException("", ex);
        } finally {
            post.releaseConnection();
        }
    }


    /**
     * Put String
     *
     * @param host
     * @param path
     * @param method
     * @param headers
     * @param querys
     * @param body
     * @return
     * @throws Exception
     */
    public static HttpResponse doPut(String host, String path, String method,
                                     Map<String, String> headers,
                                     Map<String, String> querys,
                                     String body)
        throws Exception {
        HttpClient httpClient = wrapClient(host);

        HttpPut request = new HttpPut(buildUrl(host, path, querys));
        for (Map.Entry<String, String> e : headers.entrySet()) {
            request.addHeader(e.getKey(), e.getValue());
        }

        if (StringUtils.isNotBlank(body)) {
            request.setEntity(new StringEntity(body, "utf-8"));
        }

        return httpClient.execute(request);
    }

    /**
     * Put stream
     *
     * @param host
     * @param path
     * @param method
     * @param headers
     * @param querys
     * @param body
     * @return
     * @throws Exception
     */
    public static HttpResponse doPut(String host, String path, String method,
                                     Map<String, String> headers,
                                     Map<String, String> querys,
                                     byte[] body)
        throws Exception {
        HttpClient httpClient = wrapClient(host);

        HttpPut request = new HttpPut(buildUrl(host, path, querys));
        for (Map.Entry<String, String> e : headers.entrySet()) {
            request.addHeader(e.getKey(), e.getValue());
        }

        if (body != null) {
            request.setEntity(new ByteArrayEntity(body));
        }

        return httpClient.execute(request);
    }

    /**
     * Delete
     *
     * @param host
     * @param path
     * @param method
     * @param headers
     * @param querys
     * @return
     * @throws Exception
     */
    public static HttpResponse doDelete(String host, String path, String method,
                                        Map<String, String> headers,
                                        Map<String, String> querys)
        throws Exception {
        HttpClient httpClient = wrapClient(host);

        HttpDelete request = new HttpDelete(buildUrl(host, path, querys));
        for (Map.Entry<String, String> e : headers.entrySet()) {
            request.addHeader(e.getKey(), e.getValue());
        }

        return httpClient.execute(request);
    }

    private static String buildUrl(String host, String path, Map<String, String> querys) throws UnsupportedEncodingException {
        StringBuilder sbUrl = new StringBuilder();
        sbUrl.append(host);
        if (!StringUtils.isBlank(path)) {
            sbUrl.append(path);
        }
        if (null != querys) {
            StringBuilder sbQuery = new StringBuilder();
            for (Map.Entry<String, String> query : querys.entrySet()) {
                if (0 < sbQuery.length()) {
                    sbQuery.append("&");
                }
                if (StringUtils.isBlank(query.getKey()) && !StringUtils.isBlank(query.getValue())) {
                    sbQuery.append(query.getValue());
                }
                if (!StringUtils.isBlank(query.getKey())) {
                    sbQuery.append(query.getKey());
                    if (!StringUtils.isBlank(query.getValue())) {
                        sbQuery.append("=");
                        sbQuery.append(URLEncoder.encode(query.getValue(), "utf-8"));
                    }
                }
            }
            if (0 < sbQuery.length()) {
                sbUrl.append("?").append(sbQuery);
            }
        }

        return sbUrl.toString();
    }

    private static HttpClient wrapClient(String host) {
        HttpClient httpClient = new DefaultHttpClient();
        if (host.startsWith("https://")) {
            sslClient(httpClient);
        }

        return httpClient;
    }

    private static void sslClient(HttpClient httpClient) {
        try {
            SSLContext ctx = SSLContext.getInstance("TLS");
            X509TrustManager tm = new X509TrustManager() {
                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                @Override
                public void checkClientTrusted(X509Certificate[] xcs, String str) {

                }

                @Override
                public void checkServerTrusted(X509Certificate[] xcs, String str) {

                }
            };
            ctx.init(null, new TrustManager[]{tm}, null);
            SSLSocketFactory ssf = new SSLSocketFactory(ctx);
            ssf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
            ClientConnectionManager ccm = httpClient.getConnectionManager();
            SchemeRegistry registry = ccm.getSchemeRegistry();
            registry.register(new Scheme("https", 443, ssf));
        } catch (KeyManagementException ex) {
            throw new RuntimeException(ex);
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException(ex);
        }
    }
}