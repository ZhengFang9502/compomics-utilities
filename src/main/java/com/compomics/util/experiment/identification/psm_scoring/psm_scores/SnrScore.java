package com.compomics.util.experiment.identification.psm_scoring.psm_scores;

import com.compomics.util.experiment.biology.Ion;
import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.biology.ions.PeptideFragmentIon;
import com.compomics.util.experiment.identification.matches.IonMatch;
import com.compomics.util.experiment.identification.spectrum_annotation.AnnotationSettings;
import com.compomics.util.experiment.identification.spectrum_annotation.SpecificAnnotationSettings;
import com.compomics.util.experiment.identification.spectrum_annotation.spectrum_annotators.PeptideSpectrumAnnotator;
import com.compomics.util.experiment.massspectrometry.MSnSpectrum;
import com.compomics.util.experiment.massspectrometry.Peak;
import com.compomics.util.experiment.massspectrometry.SimpleNoiseDistribution;
import java.util.ArrayList;
import java.util.HashMap;
import org.apache.commons.math.MathException;
import org.apache.commons.math.util.FastMath;

/**
 * This score uses the intensity distribution of the peaks to evaluate an SNR
 * score.
 *
 * @author Marc Vaudel
 */
public class SnrScore {

    /**
     * Log10 value of the lowest limit of a double.
     */
    private static double limitLog10 = -FastMath.log10(Double.MIN_VALUE);

    /**
     * Returns the score.
     *
     * @param peptide the peptide of interest
     * @param spectrum the spectrum of interest
     * @param annotationSettings the general spectrum annotation settings
     * @param specificAnnotationSettings the annotation settings specific to
     * this PSM
     * @param peptideSpectrumAnnotator the spectrum annotator to use
     *
     * @return the score of the match
     *
     * @throws java.lang.InterruptedException exception thrown if a threading
     * error occurs
     * @throws org.apache.commons.math.MathException exception if an exception
     * occurs when calculating logs
     */
    public double getScore(Peptide peptide, MSnSpectrum spectrum, AnnotationSettings annotationSettings, SpecificAnnotationSettings specificAnnotationSettings, PeptideSpectrumAnnotator peptideSpectrumAnnotator) throws InterruptedException, MathException {
        ArrayList<IonMatch> ionMatchesList = peptideSpectrumAnnotator.getSpectrumAnnotation(annotationSettings, specificAnnotationSettings, spectrum, peptide);
        return getScore(peptide, spectrum, annotationSettings, specificAnnotationSettings, ionMatchesList);
    }

    /**
     * Returns the score.
     *
     * @param peptide the peptide of interest
     * @param spectrum the spectrum of interest
     * @param annotationSettings the general spectrum annotation settings
     * @param specificAnnotationSettings the annotation settings specific to
     * this PSM
     * @param ionMatchesList the ion matches obtained from spectrum annotation
     *
     * @return the score of the match
     *
     * @throws java.lang.InterruptedException exception thrown if a threading
     * error occurs
     * @throws org.apache.commons.math.MathException exception if an exception
     * occurs when calculating logs
     */
    public double getScore(Peptide peptide, MSnSpectrum spectrum, AnnotationSettings annotationSettings, SpecificAnnotationSettings specificAnnotationSettings, ArrayList<IonMatch> ionMatchesList) throws InterruptedException, MathException {
        HashMap<Double, ArrayList<IonMatch>> ionMatches = new HashMap<Double, ArrayList<IonMatch>>(ionMatchesList.size());
        for (IonMatch ionMatch : ionMatchesList) {
            double mz = ionMatch.peak.mz;
            ArrayList<IonMatch> peakMatches = ionMatches.get(mz);
            if (peakMatches == null) {
                peakMatches = new ArrayList<IonMatch>(1);
                ionMatches.put(mz, peakMatches);
            }
            peakMatches.add(ionMatch);
        }
        return getScore(peptide, spectrum, annotationSettings, specificAnnotationSettings, ionMatches);
    }

    /**
     * Returns the score.
     *
     * @param peptide the peptide of interest
     * @param spectrum the spectrum of interest
     * @param annotationSettings the general spectrum annotation settings
     * @param specificAnnotationSettings the annotation settings specific to
     * this PSM
     * @param ionMatches the ion matches obtained from spectrum annotation
     * indexed by mz
     *
     * @return the score of the match
     *
     * @throws java.lang.InterruptedException exception thrown if a threading
     * error occurs
     * @throws org.apache.commons.math.MathException exception if an exception
     * occurs when calculating logs
     */
    public double getScore(Peptide peptide, MSnSpectrum spectrum, AnnotationSettings annotationSettings, SpecificAnnotationSettings specificAnnotationSettings, HashMap<Double, ArrayList<IonMatch>> ionMatches) throws InterruptedException, MathException {
        
        Double pAnnotatedMinusLog = 0.0;
        Double pNotAnnotatedMinusLog = 0.0;
        HashMap<Double, Peak> peakMap = spectrum.getPeakMap();
        SimpleNoiseDistribution binnedCumulativeFunction = spectrum.getIntensityLogDistribution();
        
        for (Double mz : spectrum.getOrderedMzValues()) {
            Peak peak = peakMap.get(mz);
            double intensity = peak.intensity;
            double pMinusLog = -binnedCumulativeFunction.getBinnedCumulativeProbabilityLog(intensity);
            ArrayList<IonMatch> peakMatches = ionMatches.get(mz);
            if (peakMatches == null) {
                pNotAnnotatedMinusLog += pMinusLog;
            } else {
                for (IonMatch ionMatch : peakMatches) {
                    if (ionMatch.ion.getType() == Ion.IonType.PEPTIDE_FRAGMENT_ION) {
                        PeptideFragmentIon peptideFragmentIon = (PeptideFragmentIon) ionMatch.ion;
                        if (!peptideFragmentIon.hasNeutralLosses() && peptideFragmentIon.getNumber() >= 2) {
                            pAnnotatedMinusLog += pMinusLog;
                            break;
                        }
                    }
                }
            }
        }
        if (pAnnotatedMinusLog == 0.0) {
            return pAnnotatedMinusLog;
        }
        if (pNotAnnotatedMinusLog < limitLog10) {
            double pNotAnnotated = FastMath.pow(10, -pNotAnnotatedMinusLog);
            if (pNotAnnotated > 1.0 - Double.MIN_VALUE) {
                pNotAnnotated = 1.0 - Double.MIN_VALUE;
            }
            pNotAnnotated = 1.0 - pNotAnnotated;
            double notAnnotatedCorrection = -FastMath.log10(pNotAnnotated);
            if (notAnnotatedCorrection > pAnnotatedMinusLog) {
                notAnnotatedCorrection = pAnnotatedMinusLog;
            }
            pAnnotatedMinusLog += notAnnotatedCorrection;
        }
        return pAnnotatedMinusLog;
    }
}
