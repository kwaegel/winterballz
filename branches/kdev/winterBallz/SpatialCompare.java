package winterBallz;


public class SpatialCompare implements java.util.Comparator<SpatialRect> {

	@Override
	public int compare(SpatialRect o1, SpatialRect o2) {

		if (o1.getType() == SpatialRect.Type.RABBIT)
		{
			return Integer.MIN_VALUE;
		}
		else if (o2.getType() == SpatialRect.Type.RABBIT)
		{
			return Integer.MAX_VALUE;
		}
		else
		{
			return o2.y - o1.y;
		}
	}

}
