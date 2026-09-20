package Services;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RespSimulatorService {

    private double t = 0;
    private final Random r = new Random();

    public List<Double> nextSamples(int n, double rr) {

        List<Double> data = new ArrayList<>();
        double freq = rr / 60.0;

        for (int i = 0; i < n; i++) {
            t += 0.02;

            double resp =
                    Math.sin(2 * Math.PI * freq * t) +
                            Math.sin(2 * Math.PI * freq * t * 0.2) * 0.2 +
                            r.nextGaussian() * 0.05;

            data.add(resp);
        }
        return data;
    }
}
