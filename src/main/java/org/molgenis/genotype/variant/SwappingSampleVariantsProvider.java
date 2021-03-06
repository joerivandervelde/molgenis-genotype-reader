package org.molgenis.genotype.variant;

import java.util.List;

import org.molgenis.genotype.Alleles;

public class SwappingSampleVariantsProvider implements SampleVariantsProvider
{
	private SampleVariantsProvider sampleVariantsProvider;
	private final int sampleVariantProviderUniqueId;

	public SwappingSampleVariantsProvider(SampleVariantsProvider sampleVariantsProvider)
	{
		this.sampleVariantsProvider = sampleVariantsProvider;
		sampleVariantProviderUniqueId = SampleVariantUniqueIdProvider.getNextUniqueId();
	}

	@Override
	public List<Alleles> getSampleVariants(GeneticVariant variant)
	{
		List<Alleles> alleles = sampleVariantsProvider.getSampleVariants(variant);
		for (int i = 0; i < alleles.size(); i++)
		{
			alleles.set(i, alleles.get(i).getComplement());
		}

		return alleles;
	}

	@Override
	public int cacheSize()
	{
		return 0;
	}

	@Override
	public List<Boolean> getSamplePhasing(GeneticVariant variant)
	{
		return sampleVariantsProvider.getSamplePhasing(variant);
	}
	
	public int getSampleVariantProviderUniqueId()
	{
		return sampleVariantProviderUniqueId;
	}

}
