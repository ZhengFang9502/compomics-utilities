package com.compomics.util.experiment.identification.protein_sequences.digestion.iterators;

import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.identification.protein_sequences.digestion.ProteinIteratorUtils;
import com.compomics.util.experiment.identification.protein_sequences.digestion.PeptideWithPosition;
import com.compomics.util.experiment.identification.protein_sequences.digestion.SequenceIterator;

/**
 * Iterator for no digestion.
 *
 * @author Marc Vaudel
 */
public class NoDigestionIterator implements SequenceIterator {

    /**
     * Utilities classes for the digestion.
     */
    private ProteinIteratorUtils proteinIteratorUtils;
    /**
     * The peptide with position inferred from the sequence.
     */
    private PeptideWithPosition peptideWithPosition = null;

    /**
     * Constructor.
     *
     * @param proteinIteratorUtils utils for the creation of the peptides
     * @param sequence the sequence to iterate
     * @param massMin the minimal mass of a peptide
     * @param massMax the maximal mass of a peptide
     */
    public NoDigestionIterator(ProteinIteratorUtils proteinIteratorUtils, String sequence, Double massMin, Double massMax) {

        this.proteinIteratorUtils = proteinIteratorUtils;
        setPeptide(sequence, massMin, massMax);

    }

    @Override
    public PeptideWithPosition getNextPeptide() {
        PeptideWithPosition result = peptideWithPosition;
        peptideWithPosition = null;
        return result;
    }

    /**
     * Makes a peptide from the given sequence without digestion and saves it as
     * attribute. The sequence should not contain ambiguous amino acids.
     * Peptides are filtered according to the given masses. Filters are ignored
     * if null.
     *
     * @param sequence the amino acid sequence
     * @param massMin the minimal mass
     * @param massMax the maximal mass
     */
    private void setPeptide(String sequence, Double massMin, Double massMax) {

        Peptide peptide = proteinIteratorUtils.getPeptideFromProtein(sequence.toCharArray(), sequence, 0, massMin, massMax);

        if (peptide != null
                && (massMin == null || peptide.getMass() >= massMin)
                && (massMax == null || peptide.getMass() <= massMax)) {
            peptideWithPosition = new PeptideWithPosition(peptide, 0);
        }
    }
}
