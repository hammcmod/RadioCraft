package com.arrl.radiocraft.common.radio.antenna.data;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Reads an .s1p file with header "# HZ S RI R 50" and provides VSWR and Smith trace helpers.
 */
public final class S1pSmith {
    // --- Small immutable complex helper
    public static final class Complex {
        public final double re;
        public final double im;
        public Complex(double re, double im) { this.re = re; this.im = im; }
        public double abs() { return Math.hypot(re, im); }
        public double angleRad() { return Math.atan2(im, re); }
        public double angleDeg() { return Math.toDegrees(angleRad()); }
        public static Complex lerp(Complex a, Complex b, double t) {
            return new Complex(a.re + (b.re - a.re) * t, a.im + (b.im - a.im) * t);
        }
        @Override public String toString() { return String.format("(%.6f, %.6f)", re, im); }
    }

    /** One row of data. */
    public static final class DataPoint {
        public final double freqHz;
        public final Complex gamma; // S11 as reflection coefficient Γ (linear)
        public DataPoint(double freqHz, Complex gamma) { this.freqHz = freqHz; this.gamma = gamma; }
    }

    private final List<DataPoint> points; // sorted by frequency ascending
    private final double z0Ohms;          // parsed from header; expected 50

    private S1pSmith(List<DataPoint> sortedPoints, double z0Ohms) {
        this.points = Collections.unmodifiableList(sortedPoints);
        this.z0Ohms = z0Ohms;
    }

    // ---------- Loading ----------

    public static S1pSmith load(Path path) throws IOException {
        List<DataPoint> pts = new ArrayList<>();
        double z0 = 50.0;
        boolean sawHeader = false;

        try (BufferedReader br = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("!")) continue;

                if (line.startsWith("#")) {
                    // Expect something like: "# HZ S RI R 50"
                    String[] toks = line.substring(1).trim().split("\\s+");
                    // Minimal sanity checks
                    if (toks.length < 5)
                        throw new IOException("Unsupported header: " + line);
                    if (!toks[0].equalsIgnoreCase("HZ"))
                        throw new IOException("Only HZ frequency units supported, got: " + toks[0]);
                    if (!toks[1].equalsIgnoreCase("S"))
                        throw new IOException("Only S-parameters supported, got: " + toks[1]);
                    if (!toks[2].equalsIgnoreCase("RI"))
                        throw new IOException("Only RI (real/imag) format supported, got: " + toks[2]);
                    if (!toks[3].equalsIgnoreCase("R"))
                        throw new IOException("Expected 'R <z0>' in header, got: " + toks[3]);
                    try { z0 = Double.parseDouble(toks[4]); } catch (NumberFormatException ignored) {}
                    sawHeader = true;
                    continue;
                }

                // Data line: freq_Hz  Re(S11)  Im(S11)
                String[] parts = line.split("\\s+");
                if (parts.length < 3) continue; // skip malformed
                double f = parseDoubleSafe(parts[0]);
                double re = parseDoubleSafe(parts[1]);
                double im = parseDoubleSafe(parts[2]);
                pts.add(new DataPoint(f, new Complex(re, im)));
            }
        }

        if (!sawHeader)
            throw new IOException("Missing header line starting with '#'.");
        if (pts.isEmpty())
            throw new IOException("No data rows found.");

        return from(pts, z0);
    }

    private static double parseDoubleSafe(String s) {
        // Some tools emit 'D' instead of 'E' exponents; normalize.
        return Double.parseDouble(s.replace('D', 'E').replace('d', 'E'));
    }

    /** Builds an {@link S1pSmith} instance from an in-memory list of data points. */
    public static S1pSmith from(List<DataPoint> dataPoints, double z0Ohms) {
        Objects.requireNonNull(dataPoints, "dataPoints");
        if (dataPoints.isEmpty()) {
            throw new IllegalArgumentException("S-parameter profile requires at least one data point.");
        }

        List<DataPoint> copy = new ArrayList<>(dataPoints);
        copy.sort(Comparator.comparingDouble(p -> p.freqHz));
        return new S1pSmith(copy, z0Ohms);
    }

    // ---------- Query: Γ and VSWR at arbitrary frequency ----------

    /** Reflection coefficient Γ at freqHz. Linearly interpolates in frequency; clamps outside range. */
    public Complex gammaAt(double freqHz) {
        int n = points.size();
        if (freqHz <= points.get(0).freqHz) return points.get(0).gamma;
        if (freqHz >= points.get(n - 1).freqHz) return points.get(n - 1).gamma;

        // Binary search for insertion point
        int lo = 0, hi = n - 1;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            double f = points.get(mid).freqHz;
            if (f < freqHz) lo = mid + 1;
            else if (f > freqHz) hi = mid - 1;
            else return points.get(mid).gamma; // exact
        }
        // hi is index of lower neighbor, lo of upper neighbor
        DataPoint p0 = points.get(hi);
        DataPoint p1 = points.get(lo);
        double t = (freqHz - p0.freqHz) / (p1.freqHz - p0.freqHz);
        return Complex.lerp(p0.gamma, p1.gamma, t);
    }

    /** VSWR at freqHz using VSWR = (1 + |Γ|) / (1 - |Γ|). Returns +Inf if |Γ| >= 1. */
    public double vswrAt(double freqHz) {
        double g = gammaAt(freqHz).abs();
        if (g >= 1.0) return Double.POSITIVE_INFINITY;
        return (1.0 + g) / (1.0 - g);
    }

    // ---------- Smith-plot helpers ----------

    /** Unmodifiable view of all data points in file order. */
    public List<DataPoint> data() { return points; }

    /** Convenience: all Γ points (Re, Im) in file order for drawing a Smith trace. */
    public List<Complex> gammaTrace() {
        List<Complex> out = new ArrayList<>(points.size());
        for (DataPoint p : points) out.add(p.gamma);
        return Collections.unmodifiableList(out);
    }

    /** Convenience: frequencies (Hz) in file order. */
    public List<Double> frequenciesHz() {
        List<Double> out = new ArrayList<>(points.size());
        for (DataPoint p : points) out.add(p.freqHz);
        return Collections.unmodifiableList(out);
    }

    /** Convenience: Γ as (mag, angleDeg) pairs in file order—handy if you plot in polar. */
    public List<double[]> gammaPolarTrace() {
        List<double[]> out = new ArrayList<>(points.size());
        for (DataPoint p : points) {
            double mag = p.gamma.abs();
            double ang = p.gamma.angleDeg();
            out.add(new double[] { mag, ang });
        }
        return Collections.unmodifiableList(out);
    }

    public double getMinFreqHz() { return points.get(0).freqHz; }
    public double getMaxFreqHz() { return points.get(points.size() - 1).freqHz; }
    public double getZ0Ohms() { return z0Ohms; }

    // ---------- Example: nearest sample (no interpolation) if you ever need it ----------
    public Complex gammaNearest(double freqHz) {
        int n = points.size();
        if (freqHz <= points.get(0).freqHz) return points.get(0).gamma;
        if (freqHz >= points.get(n - 1).freqHz) return points.get(n - 1).gamma;

        // Binary search to find neighbors, then pick nearest by |Δf|
        int lo = 0, hi = n - 1;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            double f = points.get(mid).freqHz;
            if (f < freqHz) lo = mid + 1;
            else if (f > freqHz) hi = mid - 1;
            else return points.get(mid).gamma;
        }
        DataPoint a = points.get(hi);
        DataPoint b = points.get(lo);
        return (freqHz - a.freqHz) <= (b.freqHz - freqHz) ? a.gamma : b.gamma;
    }
}
