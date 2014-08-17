/*
 * Computoser is a music-composition algorithm and a website to present the results
 * Copyright (C) 2012-2014  Bozhidar Bozhanov
 *
 * Computoser is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Computoser is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Computoser.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.music.tools;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

import com.google.common.collect.Sets;
import com.google.common.io.CharStreams;
import com.google.common.io.Files;

public class SongDBDownloader {
    public static void main(String[] args) throws Exception {
        HttpClient client = new DefaultHttpClient();

//        HttpHost proxy = new HttpHost("localhost", 8888);
//        client.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);

        HttpContext ctx = new BasicHttpContext();

        HttpUriRequest req = new HttpGet("http://www.hooktheory.com/analysis/view/the-beatles/i-want-to-hold-your-hand");
        client.execute(req, ctx);
        req.abort();

        List<String> urls = getSongUrls("http://www.hooktheory.com/analysis/browseSearch?sQuery=&sOrderBy=views&nResultsPerPage=525&nPage=1", client, ctx);
        List<List<? extends NameValuePair>> paramsList = new ArrayList<>(urls.size());
        for (String songUrl : urls) {
            paramsList.addAll(getSongParams(songUrl, client, ctx));
        }
        int i = 0;
        for (List<? extends NameValuePair> params : paramsList) {

            HttpPost request = new HttpPost("http://www.hooktheory.com/songs/getXML");

            request.setHeader("User-Agent",
                    "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:15.0) Gecko/20100101 Firefox/15.0.1");
            request.setHeader("Accept",
                    "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
            request.setHeader("Accept-Encoding", "gzip, deflate");
            request.setHeader("Accept-Language", "en,en-us;q=0.7,bg;q=0.3");
            request.setHeader("Content-Type", "application/x-www-form-urlencoded");
            request.setHeader("Origin", "http://www.hooktheory.com");
            request.setHeader("Referer", URLEncoder.encode("http://www.hooktheory.com/swf/DNALive Version 1.0.131.swf", "utf-8"));

            HttpEntity entity = new UrlEncodedFormEntity(params);
            request.setEntity(entity);

            try {
                HttpResponse response = client.execute(request, ctx);
                if (response.getStatusLine().getStatusCode() == 200) {
                    InputStream is = response.getEntity().getContent();
                    String xml = CharStreams.toString(new InputStreamReader(is));
                    is.close();
                    Files.write(xml, new File("c:/tmp/musicdb/" + i + ".xml"), Charset.forName("utf-8"));
                } else {
                    System.out.println(response.getStatusLine());
                    System.out.println(params);
                }
                i++;
                request.abort();
            } catch (Exception ex) {
                System.out.println(params);
                ex.printStackTrace();
            }
        }
    }

    private static List<List<NameValuePair>> getSongParams(String songUrl, HttpClient client, HttpContext ctx) throws IOException {
        String html = getResponseAsString(songUrl, client, ctx);
        List<List<NameValuePair>> result = new ArrayList<>();
        Set<String> fields = Sets.newHashSet("username", "artist", "song",
                "section", "revision", "HTID", "sCSRFToken");
        for (String field : fields) {
            Pattern pattern = Pattern.compile("'" + field + "':'(.+)'");
            Matcher m = pattern.matcher(html);
            int i = 0;
            while (m.find()) {
                // supporting multiple instances of the flash client per page.
                List<NameValuePair> httpParams;
                if (result.size() - 1 < i) {
                    httpParams = new ArrayList<NameValuePair>();
                    result.add(httpParams);
                } else {
                    httpParams = result.get(i);
                }
                if (field.equals("sCSRFToken")) {
                    String token = m.group(1);
                    httpParams.add(new BasicNameValuePair("YII_CSRF_TOKEN", token));
                } else {
                    httpParams.add(new BasicNameValuePair(field, m.group(1)));
                }
                i++;
            }
        }

        return result;
    }

    private static List<String> getSongUrls(String list, HttpClient client, HttpContext ctx) throws IOException {
        String html = getResponseAsString(list, client, ctx);
        // Right, NEVER use regex for html parsing. Only this time :)
        Pattern pattern = Pattern.compile("href=\"([\\w/\\-:\\.]+)\" ");
        Matcher m = pattern.matcher(html);
        List<String> result = new ArrayList<>();
        while (m.find()) {
            result.add(m.group(1));
        }

        return result;
    }

    private static String getResponseAsString(String urlString, HttpClient client, HttpContext ctx) throws IOException {
        HttpUriRequest req = new HttpGet(urlString);
        InputStream is = client.execute(req, ctx).getEntity().getContent();
        String result = CharStreams.toString(new InputStreamReader(is));
        is.close();
        req.abort();
        return result;
    }
}
