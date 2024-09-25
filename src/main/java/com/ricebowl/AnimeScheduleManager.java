package com.ricebowl;

import java.time.ZonedDateTime;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import java.sql.ResultSet;
import java.sql.SQLException;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.User;


public class AnimeScheduleManager extends Timer
{
	private final long FIVE_MINUTE_DELAY = 1000 * 60 * 5;
	private final long NEXT_DAY_DELAY = 1000 * 60 * 60 * 24;
	
	private JDA bot;
	private AnimeAlertDatabase alertDB;
		
	private ConcurrentHashMap<String, TimerTask> alertTasks;
	private Timer recheckTimer;
		
	public AnimeScheduleManager(JDA bot, AnimeAlertDatabase alertDB) 
	{
		this.bot = bot;
		this.alertDB = alertDB;
		
		this.alertTasks = new ConcurrentHashMap<String, TimerTask>();
		this.recheckTimer = new Timer();
		rescheduleTasks();
	}
	
	private void rescheduleTasks() 
	{
		ResultSet alerts = alertDB.fetchAllAlerts();
		
		if(alerts != null) 
		{
			try
			{
				while(alerts.next()) 
				{
					String user = alerts.getString("user_id");
					String title = alerts.getString("title");
					
					TimerTask task = new TimerTask() 
					{
						public void run() 
						{
							NewAnimeListener listener = alertDB.fetchAlert(user, title);
							User requester = bot.retrieveUserById(user).complete();
							
							listener.checkNewAnime(requester);
							
							if(!listener.isAlerted()) 
							{
								recheckTimer.schedule(new TimerTask() 
								{
									public void run() 
									{
										listener.checkNewAnime(requester);
										
										if(listener.isAlerted()) 
										{
											listener.resetAlerted();
											alertDB.updateAlert(user, listener);
										}
									}
								}, FIVE_MINUTE_DELAY);
							}
							else 
							{
								listener.resetAlerted();
								alertDB.updateAlert(user, listener);
							}
						}
					};
					
					String hashId = user + String.valueOf(title.hashCode());
					alertTasks.put(hashId, task);
					this.schedule(task, new Date(alerts.getTimestamp("alert_time").getTime()), NEXT_DAY_DELAY);
				}
			} 
			catch (SQLException e)
			{
				e.printStackTrace();
			}
			finally 
			{
				try
				{
					alerts.close();
				}
				catch (SQLException e)
				{
					e.printStackTrace();
				}
			}
		}
	}
	
	public void addNewListener(User user, NewAnimeListener listener) 
	{
		ZonedDateTime alertTime = listener.getAlertTime();
		boolean canBeAdded = alertDB.addAlert(user.getId(), listener);
		
		if(canBeAdded) 
		{
			String userId = user.getId();
			String title = listener.getTitle();
			
			TimerTask task = new TimerTask() 
			{
				public void run() 
				{
					NewAnimeListener fetchedListener = alertDB.fetchAlert(userId, title);
					
					fetchedListener.checkNewAnime(user);
					
					if(!fetchedListener.isAlerted()) 
					{
						recheckTimer.schedule(new TimerTask() 
						{
							public void run() 
							{
								fetchedListener.checkNewAnime(user);
								
								if(listener.isAlerted()) 
								{
									fetchedListener.resetAlerted();
									alertDB.updateAlert(userId, listener);
								}
							}
						}, FIVE_MINUTE_DELAY);
					}
					else 
					{
						fetchedListener.resetAlerted();
						alertDB.updateAlert(userId, listener);
					}
				}
			};
			
			String hashId = userId + String.valueOf(title.hashCode());
			alertTasks.put(hashId, task);
			this.schedule(task, Date.from(alertTime.toInstant()), NEXT_DAY_DELAY);
		}
	}
	
	public void removeListener(String user, String title) 
	{
		String hashId = user + String.valueOf(title.hashCode());
		TimerTask task = alertTasks.get(hashId);
		
		task.cancel();
		this.purge();
		
		alertDB.removeAlert(user, title);
	}
	
}
