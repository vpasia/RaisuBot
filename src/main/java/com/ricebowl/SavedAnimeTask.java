package com.ricebowl;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;

import net.dv8tion.jda.api.entities.User;

@SuppressWarnings("serial")
public class SavedAnimeTask implements NewAnimeListener
{
	private String title;
	private String url;
	private AnimeInfoSearcher infoSearcher;
	
	private int currentEpisodeCount;
	private boolean alerted;
	private ZonedDateTime alertTime;
	
	public SavedAnimeTask(AnimeInfoSearcher infoSearcher, String title, String url, int count, ZonedDateTime alertTime) 
	{
		this.title = title;
		this.url = url;
		this.infoSearcher = infoSearcher;
		this.currentEpisodeCount = count;
		this.alerted = false;
		this.alertTime = alertTime;
	}

	@Override
	public void checkNewAnime(User requester)
	{
		HashMap<String, String> showInfo = infoSearcher.getShowInfo(url);
		
		if(showInfo != null) 
		{
			int newEpisodeCount = Integer.parseInt(showInfo.get("episodes"));
			
			if(newEpisodeCount > currentEpisodeCount) 
			{
				alertUser(requester, showInfo.get("title"), Integer.parseInt(showInfo.get("episodes")));
				currentEpisodeCount = newEpisodeCount;
				alerted = true;
				
				ZonedDateTime parsedTime = ZonedDateTime.parse(showInfo.get("alert time"), DateTimeFormatter.ISO_OFFSET_DATE_TIME);
				alertTime = parsedTime.withZoneSameInstant(ZoneId.of("America/New_York"));
				alertTime.plusSeconds(20);
			}
		}
		
	}
	
	@Override
	public String getTitle() 
	{
		return title;
	}
	
	@Override
	public boolean isAlerted() 
	{
		return alerted;
	}
	
	@Override
	public void resetAlerted() 
	{
		alerted = false;
	}
	
	@Override
	public ZonedDateTime getAlertTime()
	{
		return alertTime;
	}
	
	private void alertUser(User requester, String title, int newEpisode) 
	{
		requester.openPrivateChannel().queue((channel) -> 
		{
			String message = String.format("**%s released new episode: %d**", title, newEpisode);
			channel.sendMessage(message).queue();
		});
	}

	
}
