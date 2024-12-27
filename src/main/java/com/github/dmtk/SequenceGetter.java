package com.github.dmtk;

import java.io.IOException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SequenceGetter{
	String sessionId = "";
	String username = "pmambo";
	String pass = "4292";
	Map<String,List<String>> sequenceMap;
	public SequenceGetter()
	{
		sequenceMap = new HashMap<>();
		sessionId = getSessionId();
	}
	
	public String getSequence(String orderNum,String item,String pack)
	{
		OkHttpClient client = new OkHttpClient();
		FormBody formBody = new FormBody.Builder()
                .add("fileName", "reports/SingleOrderProductionViewer.txt")
                .add("Order", orderNum)
                .add("submit1", "Go")
                .add("r", "XMLReport")
                .add("f", "n")
                .add("session", sessionId)
                .build();
		
		Request request = new Request.Builder()
                .url("http://whistleclient/cgi-bin/yield/") // Example URL
                .post(formBody)
                .build();
		
		try (
				Response response = client.newCall(request).execute()) {
            if (response.code() == 200) {
            	String body = response.body().string();
            	List<String> blacklist = new ArrayList<>();
            	Document doc = Jsoup.parse(body);
            	Element bodyElement = doc.body();
            	Element inputElement = bodyElement.select("[name=unnamed]").first();
            	for(Element table : inputElement.getElementsByTag("table"))
            	{
            		if(table.html().toLowerCase().contains(username.toLowerCase()))
            		{
            			Elements elements = table.getElementsByTag("tr");
            			for(int i = elements.size() - 1; i >= 0;i--)
            			{
            				Element tr = elements.get(i);
            				String content = tr.html();
            				if(content.contains(item) && content.toLowerCase().contains("pmambo"))
            				{
            					String trackingNum = tr.getElementsByTag("a").get(0).text();
            					if(content.contains("#ff0000"))
            					{
            						blacklist.add(trackingNum);
            						continue;
            					}
            					if(blacklist.contains(trackingNum))
            					{
            						continue;
            					}
            					Elements tds = tr.getElementsByAttribute("td");
            					String itemNum = tds.get(2).text();
            					String packNum = tds.get(3).text();
            					if(itemNum.equals(item) && packNum.equals(pack))
            					{
            						return tds.get(5).text();
            					}
            					

            				}
            			}
            			break;
            		}
            	}
            } else {
            	System.out.println(response.code());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
		
		return getSequenceLocal(item);
	}
	
	public String getSequenceLocal(String item)
	{
		String hour = getHour();
		if(!sequenceMap.keySet().contains(item))
		{
			List<String> list = new ArrayList<>();
			list.add(hour + "1001");
			sequenceMap.put(item, list);
			return "1";
		} else {
			
		}
	}
	
	public String getSessionId()
	{
		OkHttpClient client = new OkHttpClient();
		FormBody formBody = new FormBody.Builder()
                .add("user", username)
                .add("pass", pass)
                .add("submit1", "Go")
                .add("r", "login")
                .add("f", "n")
                .build();
		
		Request request = new Request.Builder()
                .url("http://whistleclient/cgi-bin/yield/") // Example URL
                .post(formBody)
                .build();
		
		try (
				Response response = client.newCall(request).execute()) {
            if (response.code() == 200) {
            	String body = response.body().string();
            	String session = body.substring(body.indexOf("?session=")+9,body.indexOf("&cmp="));
            	return session;
            } else {
            	System.out.println(response.code());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
		
		return null;
	}
	
	public static String getHour()
	{
		LocalTime currentTime = LocalTime.now();

        // Get the current hour in 24-hour format
        int currentHour = currentTime.getHour();

        // Adjust the hour by adding 24
        if(currentHour < 3)
        {        	
        	currentHour += 24;
        }
        
        return String.valueOf(currentHour);

	}
}