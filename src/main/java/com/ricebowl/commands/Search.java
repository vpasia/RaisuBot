package com.ricebowl.commands;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.ricebowl.AnimeInfoSearcher;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class Search extends Command
{
	private AnimeInfoSearcher searcher;
	private EventWaiter waiter;
	
	public Search(AnimeInfoSearcher searcher, EventWaiter waiter) 
	{
		this.name = "search";
		this.aliases = new String[] {"s"};
		this.help = "Gets information about anime. Used: !!s [query]";
		this.searcher = searcher;
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
	
	private MessageEmbed createShowEmbed(String url) throws NullPointerException
	{
		HashMap<String, String> showInfo = searcher.getShowInfo(url);
		
		if(showInfo == null) 
		{
			throw new NullPointerException();
		}
		
		EmbedBuilder infoEmbed = new EmbedBuilder();
		infoEmbed.setTitle(showInfo.get("title"));
		infoEmbed.setImage(showInfo.get("image"));
		infoEmbed.addField("Type: ", showInfo.get("type") != null ? showInfo.get("type") : "", false);
		infoEmbed.addField("Released: ", showInfo.get("released") != null ? showInfo.get("released") : "", false);
		infoEmbed.addField("Studios: ", showInfo.get("studios") != null ? showInfo.get("studios") : "", false);
		infoEmbed.addField("Status: ", showInfo.get("status") != null ? showInfo.get("status") : "", false);
		
		String plot =  showInfo.get("plot") != null ? showInfo.get("plot") : "";
		plot = plot.trim().replace("\n", "");
		plot = plot.length() <= 1024 ? plot : "Summary Exceeds Limit";
		infoEmbed.addField("Plot Summary: ", plot, false);
		
		infoEmbed.addField("Genre: ", showInfo.get("genre") != null ? showInfo.get("genre") : "", false);
		infoEmbed.addField("Duration: ", showInfo.get("duration") != null ? showInfo.get("duration") : "", false);
		infoEmbed.addField("Episodes: ", showInfo.get("episodes") != null ? showInfo.get("episodes") : "", false);
		
		return infoEmbed.build();
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
									
									event.reply(createShowEmbed(url));
									
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
					m.delete().complete();
					String url = (String)rawResults;
					
					try 
					{
						event.reply(createShowEmbed(url));
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
