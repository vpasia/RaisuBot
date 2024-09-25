package com.ricebowl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class AnimeAlertDatabase
{
	private Connection conn;
	private Timer pingTimer;
	
	public AnimeAlertDatabase(String user, String password) 
	{
		pingTimer = new Timer();
		
		try 
		{
			Class.forName("com.mysql.cj.jdbc.Driver");
			conn = DriverManager.getConnection("jdbc:mysql://192.168.1.194:2435/rice", user, password);
			
			TimerTask pingTask = new TimerTask() 
			{
				public void run() 
				{
					try
					{
						Statement ping = conn.createStatement();
						ping.executeQuery("do 1");
					} 
					catch (SQLException e)
					{
						e.printStackTrace();
					}
					
				}
			};
			
			pingTimer.schedule(pingTask, 300000);
		}
		catch(ClassNotFoundException | SQLException e) 
		{
			e.printStackTrace();
		}
	}
	
	public ResultSet fetchAllAlerts() 
	{
		ResultSet result = null;
		Statement query = null;
		try 
		{
			query = conn.createStatement();
			result = query.executeQuery("SELECT * FROM anime_alerts");
		}
		catch(SQLException e) 
		{
			e.printStackTrace();
		}
		
		return result;
	}
	
	public ArrayList<String> fetchUserTitles(String user)
	{
		ArrayList<String> titles = new ArrayList<String>();
		ResultSet result = null;
		PreparedStatement query = null;
		
		try 
		{
			query = conn.prepareStatement("SELECT title FROM anime_alerts WHERE user_id = ?");
			query.setString(1, user);
			
			result = query.executeQuery();
			
			while(result.next()) 
			{
				titles.add(result.getString(1));
			}
		}
		catch(SQLException e) 
		{
			e.printStackTrace();
		}
		
		return titles;
	}
	
	public NewAnimeListener fetchAlert(String user, String title) 
	{
		ResultSet result = null;
		PreparedStatement query = null;
		NewAnimeListener listener = null;
		try 
		{	
			query = conn.prepareStatement("SELECT listener FROM anime_alerts WHERE title = ? AND user_id = ?");
			query.setString(1, String.valueOf(title.hashCode()));		
			query.setString(2, user);
			result = query.executeQuery();
			
			if(result.next()) 
			{
				byte[] buffer = result.getBytes(1);
				
				if(buffer != null) 
				{
					ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(buffer));
					listener = (NewAnimeListener) ois.readObject();
				}
			}
		}
		catch(SQLException | IOException | ClassNotFoundException e) 
		{
			e.printStackTrace();
		}
		finally 
		{
			try
			{
				if(result != null) result.close();
				if(query != null) query.close();
			} 
			catch (SQLException e)
			{
				e.printStackTrace();
			}
		}
		
		return listener;
	}
	
	public boolean addAlert(String user, NewAnimeListener listener) 
	{
		boolean canBeAdded = true;
		PreparedStatement insertStatement = null;
		
		try 
		{			
			insertStatement = conn.prepareStatement("INSERT INTO anime_alerts VALUES(?, ?, ?, ?)");
			insertStatement.setString(1, String.valueOf(listener.getTitle().hashCode()));
			insertStatement.setString(2, user);
			insertStatement.setObject(3, listener);
			insertStatement.setTimestamp(4, Timestamp.from(listener.getAlertTime().toInstant()));
			insertStatement.executeUpdate();
		}
		catch(SQLException e) 
		{
			if(e.getMessage().toLowerCase().contains("duplicate entry")) 
			{
				canBeAdded = false;
			}
			else 
			{
				e.printStackTrace();
			}
		}
		finally 
		{
			try
			{
				if(insertStatement != null) insertStatement.close();
			} 
			catch (SQLException e)
			{
				e.printStackTrace();
			}	
		}
		
		return canBeAdded;
	}
	
	public void updateAlert(String user, NewAnimeListener listener) 
	{
		PreparedStatement updateStatement = null;
		
		try 
		{			
			updateStatement = conn.prepareStatement("UPDATE anime_alerts SET alert_time = ?, listener = ? WHERE title = ? AND user_id = ?");
			updateStatement.setTimestamp(1, Timestamp.from(listener.getAlertTime().toInstant()));
			updateStatement.setObject(2, listener);
			updateStatement.setString(3, String.valueOf(listener.getTitle().hashCode()));
			updateStatement.setString(4, user);
			updateStatement.executeUpdate();
		}
		catch(SQLException e) 
		{
			e.printStackTrace();
		}
		finally 
		{
			try
			{
				if(updateStatement != null) updateStatement.close();
			} 
			catch (SQLException e)
			{
				e.printStackTrace();
			}	
		}
	}
	
	public void removeAlert(String user, String title) 
	{
		PreparedStatement deleteStatement = null;
		
		try 
		{
			deleteStatement = conn.prepareStatement("DELETE FROM anime_alerts WHERE title = ? AND user_id = ?");
			deleteStatement.setString(1, String.valueOf(title.hashCode()));
			deleteStatement.setString(2, user);
			deleteStatement.executeUpdate();
		}
		catch(SQLException e) 
		{
			e.printStackTrace();
		}
		finally 
		{
			try 
			{
				if(deleteStatement != null) deleteStatement.close();
			}
			catch(SQLException e) 
			{
				e.printStackTrace();
			}
		}
	}
}
