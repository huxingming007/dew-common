package com.ecfront.dew.common;

import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.*;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.ConnectException;
import java.net.URLDecoder;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;

import static org.joox.JOOX.$;

/**
 * HTTP操作
 */
public class ApacheHttpHelper implements HttpHelper {

    private static final Logger logger = LoggerFactory.getLogger(ApacheHttpHelper.class);

    private CloseableHttpClient httpClient;
    private boolean retryAble;
    private int defaultConnectTimeoutMS;
    private int defaultSocketTimeoutMS;

    /**
     * @param maxTotal                整个连接池最大连接数
     * @param maxPerRoute             每个路由（域）的默认最大连接
     * @param defaultConnectTimeoutMS 默认连接超时时间
     * @param defaultSocketTimeoutMS  默认读取超时时间
     * @param autoRedirect            302状态下是否自动跳转
     * @param retryAble               是否重试
     */
    ApacheHttpHelper(int maxTotal, int maxPerRoute, int defaultConnectTimeoutMS, int defaultSocketTimeoutMS, boolean autoRedirect, boolean retryAble) {
        this.defaultConnectTimeoutMS = defaultConnectTimeoutMS;
        this.defaultSocketTimeoutMS = defaultSocketTimeoutMS;
        this.retryAble = retryAble;
        HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
        SSLContext sslContext = null;
        try {
            sslContext = new SSLContextBuilder().loadTrustMaterial(null, (arg0, arg1) -> true).build();
        } catch (NoSuchAlgorithmException | KeyManagementException | KeyStoreException e) {
            e.printStackTrace();
        }
        httpClientBuilder.setSSLContext(sslContext);
        Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", PlainConnectionSocketFactory.getSocketFactory())
                .register("https", new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE)).build();
        PoolingHttpClientConnectionManager connMgr = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
        connMgr.setMaxTotal(maxTotal);
        connMgr.setDefaultMaxPerRoute(maxPerRoute);
        if (autoRedirect) {
            httpClientBuilder.setRedirectStrategy(new LaxRedirectStrategy());
        }
        httpClientBuilder.setConnectionManager(connMgr);
        httpClient = httpClientBuilder.build();
    }

    @Override
    public String get(String url) throws IOException {
        return get(url, null, null, null, defaultConnectTimeoutMS, defaultSocketTimeoutMS);
    }

    @Override
    public String get(String url, Map<String, String> header) throws IOException {
        return get(url, header, null, null, defaultConnectTimeoutMS, defaultSocketTimeoutMS);
    }

    @Override
    public String get(String url, String contentType) throws IOException {
        return get(url, null, contentType, null, defaultConnectTimeoutMS, defaultSocketTimeoutMS);
    }

    @Override
    public String get(String url, Map<String, String> header, String contentType, String charset, int connectTimeoutMS, int socketTimeoutMS) throws IOException {
        return request("GET", url, null, header, contentType, charset, connectTimeoutMS, socketTimeoutMS).result;
    }

    @Override
    public ResponseWrap getWrap(String url) throws IOException {
        return getWrap(url, null, null, null, defaultConnectTimeoutMS, defaultSocketTimeoutMS);
    }

    @Override
    public ResponseWrap getWrap(String url, Map<String, String> header) throws IOException {
        return getWrap(url, header, null, null, defaultConnectTimeoutMS, defaultSocketTimeoutMS);
    }

    @Override
    public ResponseWrap getWrap(String url, String contentType) throws IOException {
        return getWrap(url, null, contentType, null, defaultConnectTimeoutMS, defaultSocketTimeoutMS);
    }

    @Override
    public ResponseWrap getWrap(String url, Map<String, String> header, String contentType, String charset, int connectTimeoutMS, int socketTimeoutMS) throws IOException {
        return request("GET", url, null, header, contentType, charset, connectTimeoutMS, socketTimeoutMS);
    }

    @Override
    public String post(String url, Object body) throws IOException {
        return post(url, body, null, null, null, defaultConnectTimeoutMS, defaultSocketTimeoutMS);
    }

    @Override
    public String post(String url, Object body, Map<String, String> header) throws IOException {
        return post(url, body, header, null, null, defaultConnectTimeoutMS, defaultSocketTimeoutMS);
    }

    @Override
    public String post(String url, Object body, String contentType) throws IOException {
        return post(url, body, null, contentType, null, defaultConnectTimeoutMS, defaultSocketTimeoutMS);
    }

    @Override
    public String post(String url, Object body, Map<String, String> header, String contentType, String charset, int connectTimeoutMS, int socketTimeoutMS) throws IOException {
        return request("POST", url, body, header, contentType, charset, connectTimeoutMS, socketTimeoutMS).result;
    }

    @Override
    public ResponseWrap postWrap(String url, Object body) throws IOException {
        return postWrap(url, body, null, null, null, defaultConnectTimeoutMS, defaultSocketTimeoutMS);
    }

    @Override
    public ResponseWrap postWrap(String url, Object body, Map<String, String> header) throws IOException {
        return postWrap(url, body, header, null, null, defaultConnectTimeoutMS, defaultSocketTimeoutMS);
    }

    @Override
    public ResponseWrap postWrap(String url, Object body, String contentType) throws IOException {
        return postWrap(url, body, null, contentType, null, defaultConnectTimeoutMS, defaultSocketTimeoutMS);
    }

    @Override
    public ResponseWrap postWrap(String url, Object body, Map<String, String> header, String contentType, String charset, int connectTimeoutMS, int socketTimeoutMS) throws IOException {
        return request("POST", url, body, header, contentType, charset, connectTimeoutMS, socketTimeoutMS);
    }

    @Override
    public String put(String url, Object body) throws IOException {
        return put(url, body, null, null, null, defaultConnectTimeoutMS, defaultSocketTimeoutMS);
    }

    @Override
    public String put(String url, Object body, Map<String, String> header) throws IOException {
        return put(url, body, header, null, null, defaultConnectTimeoutMS, defaultSocketTimeoutMS);
    }

    @Override
    public String put(String url, Object body, String contentType) throws IOException {
        return put(url, body, null, contentType, null, defaultConnectTimeoutMS, defaultSocketTimeoutMS);
    }

    @Override
    public String put(String url, Object body, Map<String, String> header, String contentType, String charset, int connectTimeoutMS, int socketTimeoutMS) throws IOException {
        return request("PUT", url, body, header, contentType, charset, connectTimeoutMS, socketTimeoutMS).result;
    }

    @Override
    public ResponseWrap putWrap(String url, Object body) throws IOException {
        return putWrap(url, body, null, null, null, defaultConnectTimeoutMS, defaultSocketTimeoutMS);
    }

    @Override
    public ResponseWrap putWrap(String url, Object body, Map<String, String> header) throws IOException {
        return putWrap(url, body, header, null, null, defaultConnectTimeoutMS, defaultSocketTimeoutMS);
    }

    @Override
    public ResponseWrap putWrap(String url, Object body, String contentType) throws IOException {
        return putWrap(url, body, null, contentType, null, defaultConnectTimeoutMS, defaultSocketTimeoutMS);
    }

    @Override
    public ResponseWrap putWrap(String url, Object body, Map<String, String> header, String contentType, String charset, int connectTimeoutMS, int socketTimeoutMS) throws IOException {
        return request("PUT", url, body, header, contentType, charset, connectTimeoutMS, socketTimeoutMS);
    }

    @Override
    public String patch(String url, Object body) throws IOException {
        return patch(url, body, null, null, null, defaultConnectTimeoutMS, defaultSocketTimeoutMS);
    }

    @Override
    public String patch(String url, Object body, Map<String, String> header) throws IOException {
        return patch(url, body, header, null, null, defaultConnectTimeoutMS, defaultSocketTimeoutMS);
    }

    @Override
    public String patch(String url, Object body, String contentType) throws IOException {
        return patch(url, body, null, contentType, null, defaultConnectTimeoutMS, defaultSocketTimeoutMS);
    }

    @Override
    public String patch(String url, Object body, Map<String, String> header, String contentType, String charset, int connectTimeoutMS, int socketTimeoutMS) throws IOException {
        return request("PATCH", url, body, header, contentType, charset, connectTimeoutMS, socketTimeoutMS).result;
    }

    @Override
    public ResponseWrap patchWrap(String url, Object body) throws IOException {
        return patchWrap(url, body, null, null, null, defaultConnectTimeoutMS, defaultSocketTimeoutMS);
    }

    @Override
    public ResponseWrap patchWrap(String url, Object body, Map<String, String> header) throws IOException {
        return patchWrap(url, body, header, null, null, defaultConnectTimeoutMS, defaultSocketTimeoutMS);
    }

    @Override
    public ResponseWrap patchWrap(String url, Object body, String contentType) throws IOException {
        return patchWrap(url, body, null, contentType, null, defaultConnectTimeoutMS, defaultSocketTimeoutMS);
    }

    @Override
    public ResponseWrap patchWrap(String url, Object body, Map<String, String> header, String contentType, String charset, int connectTimeoutMS, int socketTimeoutMS) throws IOException {
        return request("PATCH", url, body, header, contentType, charset, connectTimeoutMS, socketTimeoutMS);
    }

    @Override
    public String delete(String url) throws IOException {
        return delete(url, null, null, null, defaultConnectTimeoutMS, defaultSocketTimeoutMS);
    }

    @Override
    public String delete(String url, Map<String, String> header) throws IOException {
        return delete(url, header, null, null, defaultConnectTimeoutMS, defaultSocketTimeoutMS);
    }

    @Override
    public String delete(String url, String contentType) throws IOException {
        return delete(url, null, contentType, null, defaultConnectTimeoutMS, defaultSocketTimeoutMS);
    }

    @Override
    public String delete(String url, Map<String, String> header, String contentType, String charset, int connectTimeoutMS, int socketTimeoutMS) throws IOException {
        return request("DELETE", url, null, header, contentType, charset, connectTimeoutMS, socketTimeoutMS).result;
    }

    @Override
    public ResponseWrap deleteWrap(String url) throws IOException {
        return deleteWrap(url, null, null, null, defaultConnectTimeoutMS, defaultSocketTimeoutMS);
    }

    @Override
    public ResponseWrap deleteWrap(String url, Map<String, String> header) throws IOException {
        return deleteWrap(url, header, null, null, defaultConnectTimeoutMS, defaultSocketTimeoutMS);
    }

    @Override
    public ResponseWrap deleteWrap(String url, String contentType) throws IOException {
        return deleteWrap(url, null, contentType, null, defaultConnectTimeoutMS, defaultSocketTimeoutMS);
    }

    @Override
    public ResponseWrap deleteWrap(String url, Map<String, String> header, String contentType, String charset, int connectTimeoutMS, int socketTimeoutMS) throws IOException {
        return request("DELETE", url, null, header, contentType, charset, connectTimeoutMS, socketTimeoutMS);
    }

    @Override
    public Map<String, List<String>> head(String url) throws IOException {
        return head(url, null, null, null, defaultConnectTimeoutMS, defaultSocketTimeoutMS);
    }

    @Override
    public Map<String, List<String>> head(String url, Map<String, String> header) throws IOException {
        return head(url, header, null, null, defaultConnectTimeoutMS, defaultSocketTimeoutMS);
    }

    @Override
    public Map<String, List<String>> head(String url, String contentType) throws IOException {
        return head(url, null, contentType, null, defaultConnectTimeoutMS, defaultSocketTimeoutMS);
    }

    @Override
    public Map<String, List<String>> head(String url, Map<String, String> header, String contentType, String charset, int connectTimeoutMS, int socketTimeoutMS) throws IOException {
        return request("HEAD", url, null, header, contentType, charset, connectTimeoutMS, socketTimeoutMS).head;
    }

    @Override
    public Map<String, List<String>> options(String url) throws IOException {
        return options(url, null, null, null, defaultConnectTimeoutMS, defaultSocketTimeoutMS);
    }

    @Override
    public Map<String, List<String>> options(String url, Map<String, String> header) throws IOException {
        return options(url, header, null, null, defaultConnectTimeoutMS, defaultSocketTimeoutMS);
    }

    @Override
    public Map<String, List<String>> options(String url, String contentType) throws IOException {
        return options(url, null, contentType, null, defaultConnectTimeoutMS, defaultSocketTimeoutMS);
    }

    @Override
    public Map<String, List<String>> options(String url, Map<String, String> header, String contentType, String charset, int connectTimeoutMS, int socketTimeoutMS) throws IOException {
        return request("OPTIONS", url, null, header, contentType, charset, connectTimeoutMS, socketTimeoutMS).head;
    }

    @Override
    public ResponseWrap request(String method, String url, Object body,
                                Map<String, String> header, String contentType,
                                String charset, int connectTimeoutMS, int socketTimeoutMS) throws IOException {
        return request(method, url, body, header, contentType, charset, charset, connectTimeoutMS, socketTimeoutMS);
    }

    @Override
    public ResponseWrap request(String method, String url, Object body,
                                Map<String, String> header, String contentType,
                                String requestCharset, String responseCharset,
                                int connectTimeoutMS, int socketTimeoutMS) throws IOException {
        return request(method, url, body, header, contentType, requestCharset, responseCharset, connectTimeoutMS, socketTimeoutMS, 0);
    }

    /**
     * 发起请求
     *
     * @param method           http方法
     * @param url              请求url
     * @param body             请求体，用于post、put、patch
     *                         如果content-type是application/x-www-form-urlencoded 且 body是map时，会以form形式提交，即视为表单内容
     *                         如果content-type是xml时，body只能是Document或Xml的String格式
     *                         如果content-type是multipart/form-data时，body只能是File格式
     *                         其它情况下，body可以是任意格式
     * @param header           请求头
     * @param contentType      content-type
     * @param requestCharset   请求内容编码
     * @param responseCharset  返回内容编码，默认等于请求内容编码
     * @param connectTimeoutMS 连接超时时间
     * @param socketTimeoutMS  读取超时时间
     * @param retry            重试次数
     * @return 返回结果
     */
    private ResponseWrap request(String method, String url, Object body,
                                 Map<String, String> header, String contentType,
                                 String requestCharset, String responseCharset,
                                 int connectTimeoutMS, int socketTimeoutMS, int retry) throws IOException {
        if (header == null) {
            header = new HashMap<>();
        }
        if (body instanceof File) {
            contentType = "multipart/form-data";
        } else if (contentType == null) {
            contentType = "application/json; charset=utf-8";
        }
        if (requestCharset == null) {
            requestCharset = "UTF-8";
        }
        if (responseCharset == null) {
            responseCharset = requestCharset;
        }
        HttpRequestBase httpMethod;
        switch (method.toUpperCase()) {
            case "GET":
                httpMethod = new HttpGet(url);
                break;
            case "POST":
                httpMethod = new HttpPost(url);
                break;
            case "PUT":
                httpMethod = new HttpPut(url);
                break;
            case "DELETE":
                httpMethod = new HttpDelete(url);
                break;
            case "HEAD":
                httpMethod = new HttpHead(url);
                break;
            case "OPTIONS":
                httpMethod = new HttpOptions(url);
                break;
            case "TRACE":
                httpMethod = new HttpTrace(url);
                break;
            case "PATCH":
                httpMethod = new HttpPatch(url);
                break;
            default:
                throw new RuntimeException("The method [" + method + "] is NOT exist.");
        }
        httpMethod.setConfig(RequestConfig.custom().setSocketTimeout(socketTimeoutMS).setConnectTimeout(connectTimeoutMS).build());
        for (Map.Entry<String, String> entry : header.entrySet()) {
            httpMethod.addHeader(entry.getKey(), entry.getValue());
        }
        httpMethod.addHeader("Content-Type", contentType);
        logger.trace("HTTP [" + method + "]" + url);
        if (body != null) {
            HttpEntity entity;
            switch (contentType.toLowerCase()) {
                case "application/x-www-form-urlencoded":
                    List<NameValuePair> m = new java.util.ArrayList<>();
                    if (body instanceof Map<?, ?>) {
                        ((Map<String, String>) body).forEach((key, value) -> m.add(new BasicNameValuePair(key, value)));
                        entity = new UrlEncodedFormEntity(m, requestCharset);
                    } else if (body instanceof String) {
                        String[] items = URLDecoder.decode((String) body, requestCharset).split("&");
                        for (String item : items) {
                            String[] kv = item.split("=");
                            m.add(new BasicNameValuePair(kv[0], kv.length == 2 ? kv[1] : ""));
                        }
                        entity = new UrlEncodedFormEntity(m, requestCharset);
                    } else {
                        throw new IllegalArgumentException("The body only support Map OR String types when content type is application/x-www-form-urlencoded");
                    }
                    break;
                case "xml":
                    if (body instanceof Document) {
                        entity = new StringEntity($((Document) body).toString(), requestCharset);
                    } else if (body instanceof String) {
                        entity = new StringEntity((String) body, requestCharset);
                    } else {
                        logger.error("Not support return type [" + body.getClass().getName() + "] by xml");
                        entity = new StringEntity("", requestCharset);
                    }
                    break;
                case "multipart/form-data":
                    httpMethod.addHeader("Content-Transfer-Encoding", "binary");
                    entity = MultipartEntityBuilder.create()
                            .setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
                            .addBinaryBody(((File) body).getName(), (File) body, ContentType.APPLICATION_OCTET_STREAM, ((File) body).getName())
                            .build();
                    // delete custom value,httpclient will use like this "multipart/form-data; boundary=---------------------------7e1295335048a"
                    httpMethod.removeHeaders("Content-Type");
                    break;
                default:
                    if (body instanceof String) {
                        entity = new StringEntity((String) body, requestCharset);
                    } else if (body instanceof Integer || body instanceof Long || body instanceof Float ||
                            body instanceof Double || body instanceof BigDecimal || body instanceof Boolean) {
                        entity = new StringEntity(body.toString(), requestCharset);
                    } else if (body instanceof Date) {
                        entity = new StringEntity(((Date) body).getTime() + "", requestCharset);
                    } else {
                        entity = new StringEntity($.json.toJsonString(body), requestCharset);
                    }
            }
            ((HttpEntityEnclosingRequestBase) httpMethod).setEntity(entity);
        }
        try (CloseableHttpResponse response = httpClient.execute(httpMethod)) {
            ResponseWrap responseWrap = new ResponseWrap();
            if (!(httpMethod instanceof HttpHead || httpMethod instanceof HttpOptions)) {
                responseWrap.result = EntityUtils.toString(response.getEntity(), responseCharset);
            } else {
                responseWrap.result = "";
            }
            responseWrap.statusCode = response.getStatusLine().getStatusCode();
            String finalCharset = responseCharset;
            responseWrap.head = Arrays
                    .stream(response.getAllHeaders())
                    .collect(Collectors.groupingBy(NameValuePair::getName))
                    .entrySet()
                    .stream()
                    .collect(Collectors.toMap(Map.Entry::getKey,
                            head -> head.getValue()
                                    .stream()
                                    .map(h -> {
                                        try {
                                            return URLDecoder.decode(h.getValue(), finalCharset);
                                        } catch (UnsupportedEncodingException e) {
                                            logger.warn("HTTP [" + httpMethod.getMethod() + "] " + url + " ERROR ", e);
                                            return null;
                                        }
                                    })
                                    .collect(Collectors.toList())
                    ));
            return responseWrap;
        } catch (UnknownHostException | ConnectException | ConnectTimeoutException e) {
            // 同络错误重试5次
            if (retryAble && retry <= 5) {
                try {
                    Thread.sleep(1000 * retry);
                } catch (InterruptedException e1) {
                    Thread.currentThread().interrupt();
                    e1.printStackTrace();
                }
                logger.warn("HTTP [" + httpMethod.getMethod() + "] " + url + " ERROR. retry " + (retry + 1) + ".");
                return request(method, url, body, header, contentType, requestCharset, responseCharset, connectTimeoutMS, socketTimeoutMS, retry + 1);
            } else {
                logger.warn("HTTP [" + httpMethod.getMethod() + "] " + url + " ERROR. retry " + (retry + 1) + ".");
                throw e;
            }
        } catch (IOException e) {
            logger.warn("HTTP [" + httpMethod.getMethod() + "] " + url + " ERROR. retry " + (retry + 1) + ".");
            throw e;
        }
    }

}
