package net.runelite.client.plugins.cluescrolls;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.runelite.api.Point;

public class CoordinateClueScroll
{
	public enum Direction
	{
		NORTH(1), SOUTH(-1), EAST(1), WEST(-1);
		private int multiplier;

		private Direction(int multiplier)
		{
			this.multiplier = multiplier;
		}

		public int getMultiplier()
		{
			return multiplier;
		}
	}

	private static final int BASE_Y = 3161;
	private static final int BASE_X = 2440;
	private static final double RATIO = 1.875;

	private static final Pattern coordinatePattern = Pattern.compile(
			"([0-9]{2}) degrees ([0-9]{2}) minutes (north|south) ([0-9]{2}) degrees ([0-9]{2}) minutes (east|west)");
	private final int yMinutes;
	private final Direction yDirection;
	private final int xMinutes;
	private final Direction xDirection;

	public static CoordinateClueScroll parseCoordinateClue(String clueText)
	{
		Matcher m = coordinatePattern.matcher(clueText);
		if (m.find())
		{
			try
			{
				int yDeg = Integer.parseInt(m.group(1));
				int yMin = Integer.parseInt(m.group(2));
				String yDir = m.group(3).toUpperCase();
				int xDeg = Integer.parseInt(m.group(4));
				int xMin = Integer.parseInt(m.group(5));
				String xDir = m.group(6).toUpperCase();

				return new CoordinateClueScroll(yDeg * 60 + yMin, Direction.valueOf(yDir), xDeg * 60 + xMin,
						Direction.valueOf(xDir));
			} catch (NumberFormatException e)
			{
				return null;
			}
		} else
		{
			return null;
		}
	}
	
	public static CoordinateClueScroll calculateCoordinate(Point point) {
		Long x = Math.round((point.getX() - BASE_X) * RATIO);
		Long y = Math.round((point.getY() - BASE_Y) * RATIO);
		return new CoordinateClueScroll(Math.abs(y.intValue()), 
				y < 0 ? Direction.SOUTH : Direction.NORTH,
				Math.abs(x.intValue()), 
				x < 0 ? Direction.WEST : Direction.EAST);
	}

	public CoordinateClueScroll(int yMinutes, Direction yDirection, int xMinutes, Direction xDirection)
	{
		this.yMinutes = yMinutes;
		this.yDirection = yDirection;
		this.xMinutes = xMinutes;
		this.xDirection = xDirection;
	}

	public Point toPoint()
	{
		Long y = Math.round(BASE_Y + ((yMinutes / RATIO) * yDirection.getMultiplier()));
		Long x = Math.round(BASE_X + ((xMinutes / RATIO) * xDirection.getMultiplier()));
		
		return new Point(x.intValue(), y.intValue());
	}

	@Override
	public String toString()
	{
		int yMin = yMinutes % 60;
		int yDeg = (yMinutes - yMin) / 60;
		int xMin = xMinutes % 60;
		int xDeg = (xMinutes - xMin) / 60;

		String format = "%2d degrees %2d minutes %s %2d degrees %2d minutes %s";

		return String.format(format, yDeg, yMin, yDirection.name().toLowerCase(), xDeg, xMin,
				xDirection.name().toLowerCase());
	}
}