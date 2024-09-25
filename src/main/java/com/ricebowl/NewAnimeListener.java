package com.ricebowl;

import java.io.Serializable;

import java.time.ZonedDateTime;

import net.dv8tion.jda.api.entities.User;

public interface NewAnimeListener extends Serializable
{
	public void checkNewAnime(User requester);
	public String getTitle();
	public boolean isAlerted();
	public void resetAlerted();
	public ZonedDateTime getAlertTime();
}
