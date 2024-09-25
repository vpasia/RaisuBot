package com.ricebowl;

import java.io.*;

import java.util.HashMap;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

@SuppressWarnings("serial")
public class AnimeInfoSearcher implements Serializable
{
	private static final String BASE_URL = "https://animeschedule.net/";
	private static final String BASE_SEARCH_URL = "https://animeschedule.net/shows?q=";
	
	/**
	 * Searches anime query and returns results
	 * @param animeTitle the anime query
	 */
	public Object search(String animeTitle) 
	{
		String[] extraChars = new String[] {":", "(", ")"};
		for(String character : extraChars) 
		{
			animeTitle = animeTitle.replace(character, "");
		}
		
		String queryUrl = BASE_SEARCH_URL + animeTitle;
		Document page = getDocument(queryUrl);		
		Elements results = null;
		
		if(page != null)
		{
			results = page.getElementsByClass("anime-tile");
			
			if(results.size() == 0 && page.getElementById("category-title") == null) //If redirected to anime page return url
			{
				return page.location();
			}
			else if(results.size() == 0)
			{
				results = null;
			}
			
		}
		else 
		{
			System.out.println("Couldn't get page.");
		}
		
		return results;
	}
	
	/**
	 * Helper method that fetches the html page
	 * @param url the page url
	 */
	private Document getDocument(String url)
	{
		Document page = null;
		try
		{
			page = Jsoup.connect(url).get();
		} 
		catch (IOException e)
		{
			e.printStackTrace();
		}
		
		return page;
	}
	
	/**
	 * Gets show information from anime page
	 * @param url anime page url
	 */
	public HashMap<String, String> getShowInfo(String url) 
	{
		HashMap<String, String> showInfo = null;
		Document page = getDocument(url);
		
		if(page != null && page.getElementById("anime-header-main-title") != null) //Checks if page exists and is anime page
		{
			showInfo = new HashMap<String, String>();
			
			Element title = page.getElementById("anime-header-main-title");
			showInfo.put("title", title.ownText());
			
			Element poster = page.getElementById("anime-poster");
			showInfo.put("image", poster.attr("src"));
			
			Element infoSection = page.getElementById("information-section-large");
			
			Element statusInfo = infoSection.getElementsContainingOwnText("Status").first().parent();
			String status = statusInfo.getElementsByTag("div").get(1).ownText();
			showInfo.put("status", status);
			
			Element typeInfo = infoSection.getElementsContainingOwnText("Type").first().parent();
			String type = typeInfo.getElementsByTag("a").first().ownText();
			showInfo.put("type", type);	
			
			Element descriptionInfo = page.getElementById("description");
			String description = descriptionInfo.text();
			showInfo.put("plot", description);
			
			String genreString = "";
			Element genresDiv = page.getElementById("genres-wrapper");
			Elements links = genresDiv.getElementsByTag("a");
			for(Element link : links) 	
			{
				genreString += link.ownText() + ", ";
			}		
			showInfo.put("genre", genreString.substring(0, genreString.lastIndexOf(", ")));
			
			String studioString = "";
			Elements studioWrappers = page.getElementsByClass("studio-wrapper");
			for(Element wrapper : studioWrappers) 
			{
				Element span = wrapper.getElementsByTag("a").first().getElementsByTag("span").first();
				studioString += span.ownText();
			}
			int endIndex = studioString.lastIndexOf(", ") != -1 ? studioString.lastIndexOf(", ") : studioString.length();
			showInfo.put("studios", studioString.substring(0, endIndex));
			
			if(!status.equals("Upcoming")) //Check to make sure if other information are available
			{
				Element releaseInfo = infoSection.getElementsContainingOwnText("Release Date").first().parent();
				String release = releaseInfo.getElementsByTag("time").first().ownText();
				showInfo.put("released", release);
				
				Element durationInfo = infoSection.getElementsContainingOwnText("Episode Length").first().parent();
				String duration = durationInfo.getElementsByTag("div").get(1).ownText();
				showInfo.put("duration", duration);
				
				if(status.equals("Finished")) //If finished get episode info from infosection
				{
					Element episodesInfo = infoSection.getElementsContainingOwnText("Episodes").first().parent();
					String episodes = episodesInfo.getElementsByTag("div").get(1).ownText();
					showInfo.put("episodes", episodes);
				}
				else 
				{
					Element episodesInfo = page.getElementsByClass("release-time-type-text release-time-type-subs").first();
					String episodeReleaseId = "";
					
					if(episodesInfo == null) //If show doesn't have subs get raw episode info
					{	
						episodesInfo = page.getElementsByClass("release-time-type-text release-time-type-raw").first();
						episodeReleaseId = "release-time-raw";
					}
					else 
					{
						episodeReleaseId = "release-time-subs";
					}
					
					String episodes = episodesInfo.getElementsByClass("release-time-episode-number").first().ownText();
					int episodeCount = Integer.parseInt(episodes.split(" ")[1]) - 1;
					showInfo.put("episodes", episodeCount + "");
					
					Element episodeReleaseInfo = page.getElementById(episodeReleaseId);
					showInfo.put("alert time", episodeReleaseInfo.attr("datetime"));
				}
			}
		}
		else 
		{
			System.out.println("Couldn't find anime page.");
		}
		
		return showInfo;
	}
	
	public String getBaseUrl() 
	{
		return BASE_URL;
	}
}
