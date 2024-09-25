package com.ricebowl.commands;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;

import com.ricebowl.AnimeAlertDatabase;
import com.ricebowl.AnimeScheduleManager;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class RemoveAnime extends Command
{
	private AnimeScheduleManager manager;
	private AnimeAlertDatabase alertDB;
	private EventWaiter waiter;
	
	public RemoveAnime(AnimeScheduleManager manager, AnimeAlertDatabase alertDB, EventWaiter waiter) 
	{
		this.name = "removeanime";
		this.aliases = new String[] {"ra"};
		this.help = "Removes alert for saved anime. Used: !!ra";
		this.manager = manager;
		this.waiter = waiter;
		this.alertDB = alertDB;
	}
	
	private String createAlertString(ArrayList<String> alerts) 
	{
		String aString = "";
		
		for(int i = 0; i < alerts.size(); i++) 
		{
			aString += String.format("%d. %s \n", i + 1, alerts.get(i));
		}
		
		return aString;
	}

	@Override
	protected void execute(CommandEvent event)
	{
		ArrayList<String> alerts = alertDB.fetchUserTitles(event.getAuthor().getId());
		
		if(alerts.size() != 0) 
		{
			if(alerts.size() > 1) 
			{
				EmbedBuilder alertsEmbed = new EmbedBuilder();
				alertsEmbed.setTitle("Pick Anime: ");
				alertsEmbed.setDescription(createAlertString(alerts));
				Message aem = event.getChannel().sendMessageEmbeds(alertsEmbed.build()).complete();
				
				waiter.waitForEvent(MessageReceivedEvent.class,
						u -> u.getAuthor().equals(event.getAuthor()) 
								&& u.getChannel().equals(event.getChannel()) 
								&& !u.getMessage().equals(event.getMessage()),
						e -> 
						{
							try 
							{
								int choice = Integer.parseInt(e.getMessage().getContentRaw());
								
								if(choice <= 0 || choice > alerts.size()) 
								{
									throw new NumberFormatException();
								}
								
								String title = alerts.get(choice - 1);
								manager.removeListener(e.getAuthor().getId(), title);
								
								event.reply(String.format("Removed anime alert for %s", event.getAuthor().getAsMention()));
							}
							catch(NumberFormatException e1) 
							{
								event.reply("Invalid Input.");
								aem.delete().complete();
							}
						}, 1, TimeUnit.MINUTES, () -> 
						{
							event.reply("Timed Out.");
							aem.delete().complete();
						});
			}
			else 
			{
				manager.removeListener(event.getAuthor().getId(), alerts.get(0));
				event.reply(String.format("Removed anime alert for %s", event.getAuthor().getAsMention()));
			}	
		}
		else 
		{
			event.reply(String.format("%s has no active alerts", event.getAuthor().getAsMention()));
		}
	}

}
