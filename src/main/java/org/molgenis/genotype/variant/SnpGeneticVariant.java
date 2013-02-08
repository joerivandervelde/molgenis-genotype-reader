package org.molgenis.genotype.variant;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class SnpGeneticVariant extends AbstractGeneticVariant
{
	private final char refAllele;
	private final char[] snpAlleles;
	private char minorAllele = '\0';
	private float minorAlleleFreq = 0;
	private final List<List<Character>> sampleSnpVariants;

	public SnpGeneticVariant(List<String> ids, String sequenceName, int startPos, char[] snpAlleles, char refAllele,
			List<List<Character>> sampleSnpVariants, Map<String, ?> annotationValues, Integer stopPos,
			List<String> altDescriptions, List<String> altTypes)
	{
		super(ids, sequenceName, startPos, annotationValues, stopPos, altDescriptions, altTypes);
		this.refAllele = refAllele;
		this.snpAlleles = snpAlleles.clone();
		this.sampleSnpVariants = sampleSnpVariants;
	}

	@Override
	public List<String> getAlleles()
	{
		List<String> alleles = new ArrayList<String>(snpAlleles.length);

		for (char snpAllele : snpAlleles)
		{
			alleles.add(Character.toString(snpAllele));
		}

		return Collections.unmodifiableList(alleles);
	}

	@Override
	public String getRefAllele()
	{
		return Character.toString(refAllele);
	}

	public char getSnpRefAlelle()
	{
		return refAllele;
	}

	public char[] getSnpAlleles()
	{
		return snpAlleles.clone();
	}

	@Override
	public Integer getStopPos()
	{
		return null;
	}

	public char getMinorSnpAllele()
	{
		if (minorAllele == '\0')
		{
			deterimeSnpMinorAllele();
		}
		return minorAllele;
	}

	@Override
	public String getMinorAllele()
	{
		if (minorAllele == '\0')
		{
			deterimeSnpMinorAllele();
		}
		return Character.toString(minorAllele);
	}

	@Override
	public float getMinorAlleleFrequency()
	{
		if (minorAllele == '\0')
		{
			deterimeSnpMinorAllele();
		}
		return minorAlleleFreq;
	}

	/**
	 * Determine the minor allele and its frequency and fill in these values
	 */
	private void deterimeSnpMinorAllele()
	{
		throw new UnsupportedOperationException("Not yet implemented");
	}

	@Override
	public List<List<String>> getSampleVariants()
	{
		List<List<String>> sampleVariants = new ArrayList<List<String>>();

		for (List<Character> snpVariants : getSampleSnpVariants())
		{
			List<String> variants = new ArrayList<String>(snpVariants.size());
			for (Character snpVariant : snpVariants)
			{
				variants.add(snpVariant.toString());
			}

			sampleVariants.add(variants);
		}

		return sampleVariants;
	}

	public List<List<Character>> getSampleSnpVariants()
	{
		return sampleSnpVariants;
	}

}
