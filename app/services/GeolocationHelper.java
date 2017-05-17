package services;

import akka.japi.Pair;

public class GeolocationHelper {
	/**
	 * The roughly radius of  ideal earth model is 6372.797 kilometers
	 * x km / 6372.797 km => y radian (i.e.) (弧度) (x inputed from getRange method below.)
	 * y * (180/PI) => z degree (角度)
	 * i.e. x * rangeConst = z degree corresponding to x for earth sphere
	 */
	public static double rangeConst = 180 / Math.PI / 6372.797;

	// Input x <= rangeVariable (see above.)
	private static double getRange(int rangeVariable) {

		if (rangeVariable <= 0)
			return 0;
		return rangeConst * rangeVariable;
	}

	public static Pair<Double, Double> getLatitudeRange(double latitude) {
		return getLatitudeRange(latitude, 10);
	}

	public static Pair<Double, Double> getLongitudeRange(double latitude, double longitude) {
		return getLatitudeRange(longitude, 10);
	}

	public static Pair<Double, Double> getLatitudeRange(double latitude, int rangeVariable) {
		double range = getRange(rangeVariable);
		
		//Linear approximate, suppose the related area is a plane
		//range is in degree, so (degree - degree) and (degree + degree)
		return new Pair<Double, Double>(latitude - range, latitude + range);
	}

	public static Pair<Double, Double> getLongitudeRange(double latitude, double longitude, int rangeVariable) {
		double range = getRange(rangeVariable);
		//Linear approximate, suppose the related area is a plane
		//latitude is a degree while cosine requires a radian value so convert it
		
		
		//range is a degree(angle) value based on the radius of radius of earth. 
		//For a particular latitude(纬度) cycle, the range value at that latitude
		//should be linear approximately equal to the original range value divided by the cosine value of the latitude.
		//(i.e. think about ratio of radius of particular latitude radius / radius of earth)

		//ingR is the range variable projection in a particular latitude
		double ingR = range / Math.cos(latitude * Math.PI / 180.0);
		return new Pair<Double, Double>(longitude - ingR, longitude + ingR);
	}

}
