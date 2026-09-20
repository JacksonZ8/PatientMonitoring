package Services;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class RespSimulatorServiceTest {

    @Test
    void returnsCorrectNumberOfSamples() {
        RespSimulatorService sim = new RespSimulatorService();

        List<Double> samples = sim.nextSamples(100, 12);

        assertEquals(100, samples.size(), "Should return exactly n samples");
    }

    @Test
    void samplesAreFiniteNumbers() {
        RespSimulatorService sim = new RespSimulatorService();

        List<Double> samples = sim.nextSamples(50, 15);

        for (double v : samples) {
            assertFalse(Double.isNaN(v), "Sample should not be NaN");
            assertFalse(Double.isInfinite(v), "Sample should not be infinite");
        }
    }

    @Test
    void signalIsNotConstant() {
        RespSimulatorService sim = new RespSimulatorService();

        List<Double> samples = sim.nextSamples(100, 10);

        double first = samples.get(0);
        boolean allSame = samples.stream().allMatch(v -> v == first);

        assertFalse(allSame, "Respiratory signal should vary over time");
    }

    @Test
    void reasonableAmplitudeRange() {
        RespSimulatorService sim = new RespSimulatorService();

        List<Double> samples = sim.nextSamples(200, 12);

        for (double v : samples) {
            assertTrue(
                    Math.abs(v) < 3.0,
                    "Respiratory signal should stay within a reasonable range"
            );
        }
    }

    @Test
    void successiveCallsAdvanceTime() {
        RespSimulatorService sim = new RespSimulatorService();

        List<Double> first = sim.nextSamples(10, 12);
        List<Double> second = sim.nextSamples(10, 12);

        // If time advanced, first value of second batch should differ
        assertNotEquals(
                first.get(0),
                second.get(0),
                "Time should advance between successive calls"
        );
    }
}
