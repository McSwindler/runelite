/*
 * Copyright (c) 2016-2017, Seth <Sethtroll3@gmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */
package net.runelite.client.plugins.cluescrolls;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collection;

import javax.inject.Inject;

import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.task.Schedule;
import net.runelite.client.ui.overlay.Overlay;

@PluginDescriptor(name = "Clue scroll")
public class ClueScrollPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private ClueScrollOverlay clueScrollOverlay;

	@Inject
	private ClueScrollLocationOverlay clueScrollLocationOverlay;

	// @Override
	// public ClueScrollOverlay getOverlay()
	// {
	// return clueScrollOverlay;
	// }

	@Override
	public Collection<Overlay> getOverlays()
	{
		return Arrays.asList(clueScrollOverlay, clueScrollLocationOverlay);
	}

	@Schedule(period = 600, unit = ChronoUnit.MILLIS)
	public void checkForClues()
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}

		Widget clueScroll = client.getWidget(WidgetInfo.CLUE_SCROLL_TEXT);

		if (clueScroll == null)
		{
			return;
		}

		// remove line breaks and also the rare occasion where there are double
		// line breaks
		String rawText = clueScroll.getText().replaceAll("<br>", " ").replaceAll("  ", " ").toLowerCase();
		ClueScroll clue = ClueScroll
				.forText(rawText);
		System.out.println(client.getLocalPlayer().getWorldLocation());
		if (clue == null)
		{
			CoordinateClueScroll coordClue = CoordinateClueScroll.parseCoordinateClue(rawText);
			if (coordClue != null){
				clueScrollLocationOverlay.coordinate = coordClue;
				clueScrollLocationOverlay.clueTimeout = Instant.now();
			}
			
//			CoordinateClueScroll location = CoordinateClueScroll.calculateCoordinate(client.getLocalPlayer().getWorldLocation());
//			System.out.println(location.toString());
			// clueScrollOverlay.clue = null;
			return;
		}

		if (clue.getType() == ClueScrollType.EMOTE)
		{
			clueScrollOverlay.clue = clue;

			clueScrollOverlay.clueTimeout = Instant.now();
			
			clueScrollLocationOverlay.clue = null;
			clueScrollLocationOverlay.coordinate = null;

			return;
		} else if (clue.getType() == ClueScrollType.CIPHER)
		{
			clueScrollOverlay.clue = clue;
			clueScrollOverlay.clueTimeout = Instant.now();

			clueScrollLocationOverlay.clue = clue;
			clueScrollLocationOverlay.clueTimeout = Instant.now();
			
			clueScrollLocationOverlay.coordinate = null;
		} else
		{
			clueScrollOverlay.clue = null;
			clueScrollLocationOverlay.clue = null;
			clueScrollLocationOverlay.coordinate = null;
		}

		// check for <col=ffffff> which tells us if the string has already been
		// built
		if (clueScroll.getText().contains("<col=ffffff>"))
		{
			return;
		}

		// Build widget text
		StringBuilder clueText = new StringBuilder();

		clueText.append(clueScroll.getText()).append("<br><br>");

		if (clue.getNpc() != null)
		{
			clueText.append("<col=ffffff>Talk to: ").append(clue.getNpc()).append("<br>");
		}

		if (clue.getLocation() != null)
		{
			clueText.append("<col=ffffff>Location: ").append(clue.getLocation()).append("<br>");
		}

		if (clue.getAnswer() != null)
		{
			clueText.append("<col=ffffff>Answer: ").append(clue.getAnswer());
		}

		clueScroll.setText(clueText.toString());

	}
}
