package com.poweramp.dsp.engine;

import com.poweramp.dsp.filter.BiquadFilter;
import java.util.List;

public class FrequencyResponse {

    public static final int FRS_POINTS = 200;

    public record FrsPoint(double frequency, double magnitudeDb) {}

    public static FrsPoint[] compute(List<BiquadFilter> filters, float sampleRate) {
        FrsPoint[] points = new FrsPoint[FRS_POINTS];
        double minFreq = 10.0;
        double maxFreq = sampleRate / 2.0;

        for (int i = 0; i < FRS_POINTS; i++) {
            double freq = minFreq * Math.pow(maxFreq / minFreq, (double) i / (FRS_POINTS - 1));
            double magDb = 0;

            for (BiquadFilter filter : filters) {
                if (filter != null) {
                    magDb += filter.getMagnitudeResponse(freq);
                }
            }

            points[i] = new FrsPoint(freq, magDb);
        }

        return points;
    }

    public static FrsPoint[] computeTone(BiquadFilter bassFilter, BiquadFilter trebleFilter, float sampleRate) {
        FrsPoint[] points = new FrsPoint[FRS_POINTS];
        double minFreq = 10.0;
        double maxFreq = sampleRate / 2.0;

        for (int i = 0; i < FRS_POINTS; i++) {
            double freq = minFreq * Math.pow(maxFreq / minFreq, (double) i / (FRS_POINTS - 1));
            double magDb = 0;

            if (bassFilter != null) magDb += bassFilter.getMagnitudeResponse(freq);
            if (trebleFilter != null) magDb += trebleFilter.getMagnitudeResponse(freq);

            points[i] = new FrsPoint(freq, magDb);
        }

        return points;
    }

    public static double[] normalizeToCanvas(FrsPoint[] points, double canvasWidth, double canvasHeight,
                                              double minDb, double maxDb) {
        double[] results = new double[points.length * 2];

        for (int i = 0; i < points.length; i++) {
            results[i * 2] = (i / (double) (points.length - 1)) * canvasWidth;
            double clamped = Math.max(minDb, Math.min(maxDb, points[i].magnitudeDb()));
            results[i * 2 + 1] = canvasHeight - ((clamped - minDb) / (maxDb - minDb)) * canvasHeight;
        }

        return results;
    }
}
