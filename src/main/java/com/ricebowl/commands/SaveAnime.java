package com.ricebowl.commands;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import com.jagrosh.jdautilities.command.*;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.ricebowl.*;


public class SaveAnime extends Command
{
	private AnimeInfoSearcher searcher;
	private AnimeScheduleManager manager;
	
	private EventWaiter waiter;
	
	public SaveAnime(AnimeInfoSearcher searcher, AnimeScheduleManager manager, EventWaiter waiter) 
	{
		this.name = "saveanime";
		this.aliases = new String[] {"sa"};
		this.help = "Sets up alert for new anime episode. Used: !!saveanime [query]";
		this.searcher = searcher;
		this.manager = manager;
		this.waiter = waiter;
	}
	
	private String createResults(Elements results) 
	{
		String rString = "";
		for(int i = 0; i < results.size(); i++) 
		{
			Element tile = results.get(i).getElementsByTag("h2").first();
			rString += String.format("%d. %s \n", i + 1, tile.ownText());
		}
		
		return rString;
	}
	
	private void createAlert(String url, CommandEvent event) throws NullPointerException
	{
		HashMap<String, String> showInfo = searcher.getShowInfo(url);
		
		if(showInfo == null) 
		{
			throw new NullPointerException();
		}
		
		if(!showInfo.get("status").equals("Finished") && showInfo.get("type").equals("TV")) 
		{
			int episodeCount = Integer.parseInt(showInfo.get("episodes"));
			ZonedDateTime parsedTime = ZonedDateTime.parse(showInfo.get("alert time"), DateTimeFormatter.ISO_OFFSET_DATE_TIME);
			ZonedDateTime convertedTime = parsedTime.withZoneSameInstant(ZoneId.of("America/New_York"));
			convertedTime.plusSeconds(20);
			
			manager.addNewListener(event.getAuthor(), new SavedAnimeTask(searcher, showInfo.get("title"), url, episodeCount, convertedTime));
			
			event.reply(String.format("**Added alert for `%s`**", showInfo.get("title")));
		}
		else if(!showInfo.get("type").equals("TV")) 
		{
			event.reply(showInfo.get("title") + " is a " + showInfo.get("type"));
		}
		else if(showInfo.get("status").equals("Finished"))
		{
			event.reply(showInfo.get("title") + " is Finished.");
		}
	}
	
	@Override
	protected void execute(CommandEvent event) 
	{
		if(!event.getArgs().isBlank()) 
		{
			Message m = event.getChannel().sendMessage(String.format("Fetching **%s** . . .", event.getArgs())).complete();
			Object rawResults = searcher.search(event.getArgs());
			
			if(rawResults != null) 
			{
				if(rawResults instanceof Elements) 
				{
					Elements results = (Elements)rawResults;
					EmbedBuilder resultsEmbed = new EmbedBuilder();
					resultsEmbed.setTitle("Pick Anime: ");
					resultsEmbed.setDescription(createResults(results));
					m.delete().complete();
					Message rem = event.getChannel().sendMessageEmbeds(resultsEmbed.build()).complete();
					
					waiter.waitForEvent(MessageReceivedEvent.class,
							u -> u.getAuthor().equals(event.getAuthor()) 
									&& u.getChannel().equals(event.getChannel()) 
									&& !u.getMessage().equals(event.getMessage()),
							e -> 
							{
								try 
								{
									int choice = Integer.parseInt(e.getMessage().getContentRaw());
									
									if(choice <= 0 || choice > results.size()) 
									{
										throw new NumberFormatException();
									}
									
									Element link = results.get(choice - 1).getElementsByTag("a").first();
									String url = searcher.getBaseUrl() + link.attr("href");
									
									createAlert(url, event);
								}
								catch(NullPointerException e1) 
								{
									event.reply("Page Not Found.");
									rem.delete().complete();
								}
								catch(NumberFormatException e1) 
								{
									event.reply("Invalid Input.");
									rem.delete().complete();
								}
							}, 1, TimeUnit.MINUTES, () -> 
							{
								event.reply("Timed Out.");
								rem.delete().complete();
							});
				}
				else if(rawResults instanceof String) 
				{
					String url = (String)rawResults;
					
					try 
					{
						createAlert(url, event);
					}
					catch(NullPointerException e) 
					{
						event.reply("Page Not Found.");
					}
				}
			}
			else 
			{
				event.reply("No results found.");
			}
		}
		else 
		{
			event.reply("No query provided.");
		}
	}

}
