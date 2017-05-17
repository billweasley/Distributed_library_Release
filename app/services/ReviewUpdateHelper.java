package services;

public class ReviewUpdateHelper {
	// consider confidence level as 95%, Z and T are statistics specific values
	private static final double Z = 1.96;
	private static final double T = 2.0423;
	
	//note: numberOfRatings is updated value (After value);
	
	/*
	 * Take an owner's record and pass in the number of praise and the total
	 * number of ratings, then use calculate the wilson score interval value.
	 * Equation as follows: =(p+z^2/2n-z√((p(1-p)+z^2/4n)/n))/(1+z^2/n)
	 */
	public static double updateWilsonConfidence(double numberOfPraise, double numberOfRatings) {
		if (numberOfRatings <= 0)
			return 0;
		double fractionOfPraise, width, average, lowerBound, confidence;
		fractionOfPraise = numberOfPraise / numberOfRatings;
		confidence = numberOfRatings <= 30 ? T : Z;
		double tmp = (fractionOfPraise * (1 - fractionOfPraise)) / numberOfRatings
				+ Math.pow(confidence, 2) / (4 * Math.pow(numberOfRatings, 2));
		width = (2 * confidence * Math.sqrt(tmp)) / (1 + Math.pow(confidence, 2) / numberOfRatings);
		average = (fractionOfPraise + Math.pow(confidence, 2) / (numberOfRatings * 2))
				/ (1 + Math.pow(confidence, 2) / numberOfRatings);
		lowerBound = average - width / 2;
		return lowerBound;
	}

	public static double updateReview(double oldRateValue, double newRateValue, double numberOfRatings) {

		if (oldRateValue <= 0 || newRateValue < 0 || numberOfRatings <= 0)
			return 0;
		
		return (oldRateValue * (numberOfRatings-1) + newRateValue) / (numberOfRatings);

	}

}
