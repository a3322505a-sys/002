import java.util.Locale;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.kalinisa.diatronome.Cores.AccuracyTimer;
import org.kalinisa.diatronome.Cores.SoundAnalyzer.*;

/** Diagnostic measurements; synthetic inputs are not real guitar validation. */
public class P0Probe {
    public static void main(String[] args) throws Exception {
        Locale.setDefault(Locale.ROOT);
        double[] notes = {82.406889, 110, 146.832384, 195.997718, 246.941651, 329.627557};
        for (ISoundAnalyzer analyzer : new ISoundAnalyzer[] {
                new SoundAnalyzerEnvelop(44100), new SoundAnalyzerYin(44100)}) {
            for (double note : notes) {
                for (boolean harmonics : new boolean[] {false, true}) {
                    double[] samples = new double[4096];
                    for (int i = 0; i < samples.length; i++) {
                        double phase = 2 * Math.PI * note * i / 44100;
                        samples[i] = 0.4 * Math.sin(phase);
                        if (harmonics) samples[i] += 0.24 * Math.sin(2 * phase) + 0.12 * Math.sin(3 * phase);
                    }
                    double measured = analyzer.getPitch(samples);
                    double cents = 1200 * Math.log(measured / note) / Math.log(2);
                    System.out.printf("PITCH %s %s target=%.6f measured=%.6f cents=%+.3f%n",
                        analyzer.getClass().getSimpleName(), harmonics ? "harmonics" : "sine", note, measured, cents);
                }
            }
        }
        List<Thread> probes = new ArrayList<>();
        for (int bpm : new int[] {50, 60, 120}) {
            Thread probe = new Thread(() -> {
                try {
                    int period = 60000 / bpm;
                    int count = bpm + 1;
                    long[] actual = new long[count];
                    CountDownLatch done = new CountDownLatch(count);
                    AccuracyTimer timer = new AccuracyTimer();
                    timer.scheduleAtFixedRate(new AccuracyTimer.AccuracyTimerTask() {
                        int index;
                        public void run() {
                            if (index < actual.length) {
                                actual[index++] = System.nanoTime();
                                done.countDown();
                            }
                        }
                        public void interrupt() {}
                    }, 0, period);
                    boolean completed = done.await(70, TimeUnit.SECONDS);
                    timer.cancel();
                    if (!completed) throw new IllegalStateException("Timer did not complete at " + bpm);
                    double drift = (actual[count - 1] - actual[0]) / 1e6 - (count - 1) * period;
                    double maxJitter = 0;
                    for (int i = 1; i < actual.length; i++)
                        maxJitter = Math.max(maxJitter, Math.abs((actual[i] - actual[i - 1]) / 1e6 - period));
                    System.out.printf("TIMER bpm=%d callbacks=%d duration=60s end_drift_ms=%+.3f max_interval_error_ms=%.3f%n",
                        bpm, count, drift, maxJitter);
                } catch (Exception e) { throw new RuntimeException(e); }
            });
            probes.add(probe);
            probe.start();
        }
        for (Thread probe : probes) probe.join();
    }
}
