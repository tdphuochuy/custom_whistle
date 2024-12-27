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
	Map<String,Map<Integer,Integer>> sequenceMap;
	public SequenceGetter()
	{
		sequenceMap = new HashMap<>();
		sessionId = getSessionId();
	}
	
	public int getSequence(String orderNum,String itemNum,String packNum)
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
            				if(content.contains(itemNum) && content.toLowerCase().contains("pmambo"))
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
            					Elements tds = tr.getElementsByTag("td");
            					String webItemNum = tds.get(2).text();
            					String webPackNum = tds.get(3).text();
            					if(webItemNum.equals(itemNum) && webPackNum.equals(packNum))
            					{
            						String lotNum = tds.get(5).text();
            						String hourSequenceText = lotNum.substring(lotNum.length() - 6);
            						int lotNumHour = Integer.valueOf(hourSequenceText.substring(0,2));
            						int lotNumSequence = Integer.valueOf(hourSequenceText.substring(2));
            						return getSequenceLocal(itemNum,packNum,lotNumHour,lotNumSequence);
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
		
		return getSequenceLocal(itemNum,packNum,getHour(),1);
	}
	
	public int getSequenceLocal(String itemNum,String packNum,int hour,int sequence)
	{
		String keyName = itemNum + packNum;
		int currentHour = getHour();
		if(!sequenceMap.keySet().contains(keyName))
		{
			if(hour != currentHour) //new hour, new itemNum
			{
				Map<Integer,Integer> map = new HashMap<>();
				map.put(currentHour, 1);
				sequenceMap.put(keyName, map);
				return 1;
			} else { //new ItemNum, same hour
				Map<Integer,Integer> map = new HashMap<>();
				map.put(currentHour, sequence + 1);
				sequenceMap.put(keyName, map);
				return sequence + 1;
			}
		} else { //if itemNum is existed
			Map<Integer,Integer> map = sequenceMap.get(keyName);
			if(hour != currentHour)
			{
				if(map.containsKey(currentHour)) //incase website slow to update
				{
					int newSequence =  map.get(currentHour) + 1;
					map.put(currentHour,newSequence);
					//sequenceMap.put(packNum + itemNum, map);
					return newSequence;
				} else { //new hour
					map.put(currentHour, 1);
					//sequenceMap.put(packNum + itemNum, map);
					return 1;
				}
			} else { //if hour == currentHour
				if(map.containsKey(hour))
				{
					if(map.get(hour) <= sequence) //missed or current sequence case
					{
						int newSequence = sequence + 1;
						map.put(hour, newSequence);
						//sequenceMap.put(packNum + itemNum, map);
						return newSequence;
					} else {  //website slow to update
						int newSequence =  map.get(hour) + 1;
						map.put(hour, newSequence);
						//sequenceMap.put(packNum + itemNum, map);
						return newSequence;
					}
				} else {
					int newSequence = sequence + 1;
					map.put(hour, newSequence);
					return newSequence;
				}
			}

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
	
	public int getHour()
	{
		LocalTime currentTime = LocalTime.now();

        // Get the current hour in 24-hour format
        int currentHour = currentTime.getHour();

        // Adjust the hour by adding 24
        if(currentHour < 3)
        {        	
        	currentHour += 24;
        }
        
        return currentHour;

	}
}