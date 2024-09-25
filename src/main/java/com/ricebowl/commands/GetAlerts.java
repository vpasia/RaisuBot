package com.ricebowl.commands;

import java.util.ArrayList;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

import com.ricebowl.*;

import net.dv8tion.jda.api.EmbedBuilder;

public class GetAlerts extends Command
{
	private AnimeAlertDatabase alertDB;
	
	public GetAlerts(AnimeAlertDatabase alertDB) 
	{
		this.name = "getalerts";
		this.aliases = new String[] {"ga"};
		this.help = "Gets User's alerts. Used: !!ga";
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
			EmbedBuilder alertsEmbed = new EmbedBuilder();
			alertsEmbed.setTitle(String.format("%s's Alerts", event.getAuthor().getName()));
			alertsEmbed.setDescription(createAlertString(alerts));
			event.getChannel().sendMessageEmbeds(alertsEmbed.build()).complete();
		}
		else 
		{
			event.reply(String.format("%s has no active alerts", event.getAuthor().getAsMention()));
		}

	}
}
