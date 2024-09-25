package com.ricebowl;

import javax.security.auth.login.LoginException;

import com.jagrosh.jdautilities.command.*;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.ricebowl.commands.*;

import net.dv8tion.jda.api.*;
import net.dv8tion.jda.api.requests.GatewayIntent;

import java.io.IOException;
import java.io.FileInputStream;
import java.io.DataInputStream;
import java.io.File;

public class RaisuBot
{	
	public static String[] readSecrets(String fileName) 
	{
		String[] secrets = null;
		FileInputStream fis = null;
		DataInputStream dis = null;
		
		try 
		{
			File f = new File(fileName);
			
			if(f.exists()) 
			{
				fis = new FileInputStream(f);
				dis = new DataInputStream(fis);
				
				String line = dis.readUTF();
				secrets = line.split("\n");
			}
		}
		catch(IOException e) 
		{
			e.printStackTrace();
		}
		finally 
		{
			
			try
			{
				if(dis != null) dis.close();
				if(fis != null) fis.close();
			} 
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
		
		return secrets;
	}
	
	public static void main(String[] args) 
	{
		String[] secrets = readSecrets("raisu_secrets.sec");
		
		System.out.println(secrets);
		
		/*
		
		JDA bot = JDABuilder.createDefault(secrets[0])
				.enableIntents(GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_MEMBERS, GatewayIntent.MESSAGE_CONTENT)
				.build();
		
		CommandClientBuilder builder = new CommandClientBuilder();
		
		String[] login = secrets[1].split(" ");
		AnimeAlertDatabase alertDb = new AnimeAlertDatabase(login[0] , login[1]);
		AnimeScheduleManager manager = new AnimeScheduleManager(bot, alertDb);
		AnimeInfoSearcher infoSearcher = new AnimeInfoSearcher();
		
		EventWaiter searchWaiter = new EventWaiter();
		EventWaiter saveWaiter = new EventWaiter();
		EventWaiter removeWaiter = new EventWaiter();
		
		builder.setPrefix("!!");
		builder.setOwnerId("937551376215601252");
		builder.setHelpWord("help");
		
		builder.addCommand(new Search(infoSearcher, searchWaiter));
		builder.addCommand(new SaveAnime(infoSearcher, manager, saveWaiter));
		builder.addCommand(new RemoveAnime(manager, alertDb, removeWaiter));
		builder.addCommand(new GetAlerts(alertDb));
		
		CommandClient client = builder.build();
		
		bot.addEventListener(client);
		bot.addEventListener(searchWaiter);
		bot.addEventListener(saveWaiter);
		bot.addEventListener(removeWaiter);
		*/
	}
}
