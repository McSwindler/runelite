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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.time.Duration;
import java.time.Instant;

import javax.inject.Inject;

import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.NPC;
import net.runelite.api.Perspective;
import net.runelite.api.queries.NPCQuery;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.PanelComponent;
import net.runelite.client.util.QueryRunner;

public class ClueScrollLocationOverlay extends Overlay
{
	private static final Duration WAIT_DURATION = Duration.ofMinutes(4);

	private final Client client;
	private final QueryRunner queryRunner;
	private final PanelComponent panelComponent = new PanelComponent();

	ClueScroll clue;
	CoordinateClueScroll coordinate;
	Instant clueTimeout;

	@Inject
	public ClueScrollLocationOverlay(Client client, QueryRunner queryRunner)
	{
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
		this.client = client;
		this.queryRunner = queryRunner;
	}

	@Override
	public Dimension render(Graphics2D graphics, Point parent)
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return null;
		}
		if (clue != null)
		{
			return renderNpcClue(graphics, parent);
		} else if (coordinate != null)
		{
			return renderCoordClue(graphics, parent);
		}
		return null;
	}

	public Dimension renderNpcClue(Graphics2D graphics, Point parent)
	{
		if (clue == null)
		{
			return null;
		}

		if (clueTimeout == null || Instant.now().compareTo(clueTimeout.plus(WAIT_DURATION)) >= 0)
		{
			return null;
		}

		panelComponent.getLines().clear();
		panelComponent.setTitle("Talk to:");

		NPCQuery npcQuery = new NPCQuery().idEquals(clue.getIds());
		NPC[] npcs = queryRunner.runQuery(npcQuery);
		// String debug = client.getNpcs().stream().map(n -> n.getId() +
		// n.getName()).collect(Collectors.joining(" "));
		// System.out.println(debug);
		for (NPC npc : npcs)
		{
			String text = npc.getName();

			drawNpc(graphics, npc, text);
		}

		return null;
	}

	public Dimension renderCoordClue(Graphics2D graphics, Point parent)
	{
		if (coordinate == null)
		{
			return null;
		}

		if (clueTimeout == null || Instant.now().compareTo(clueTimeout.plus(WAIT_DURATION)) >= 0)
		{
			return null;
		}

		Polygon poly = Perspective.getCanvasTilePoly(client, Perspective.worldToLocal(client, coordinate.toPoint()));
		if (poly != null)
		{
			graphics.setColor(Color.CYAN);
			graphics.setStroke(new BasicStroke(2));
			graphics.drawPolygon(poly);
			graphics.setColor(new Color(0, 0, 0, 50));
			graphics.fillPolygon(poly);
		}

		return null;
	}

	private void drawNpc(Graphics2D graphics, Actor actor, String text)
	{
		Polygon poly = actor.getCanvasTilePoly();
		if (poly != null)
		{
			graphics.setColor(Color.CYAN);
			graphics.setStroke(new BasicStroke(2));
			graphics.drawPolygon(poly);
			graphics.setColor(new Color(0, 0, 0, 50));
			graphics.fillPolygon(poly);
		}

		net.runelite.api.Point minimapLocation = actor.getMinimapLocation();
		if (minimapLocation != null)
		{
			graphics.setColor(Color.CYAN);
			graphics.fillOval(minimapLocation.getX(), minimapLocation.getY(), 5, 5);
			graphics.setColor(Color.WHITE);
			graphics.setStroke(new BasicStroke(1));
			graphics.drawOval(minimapLocation.getX(), minimapLocation.getY(), 5, 5);
		}

		net.runelite.api.Point textLocation = actor.getCanvasTextLocation(graphics, text, actor.getLogicalHeight());
		if (textLocation != null)
		{
			int x = textLocation.getX();
			int y = textLocation.getY();

			graphics.setColor(Color.BLACK);
			graphics.drawString(text, x + 1, y + 1);

			graphics.setColor(Color.CYAN);
			graphics.drawString(text, x, y);
		}
	}
}
