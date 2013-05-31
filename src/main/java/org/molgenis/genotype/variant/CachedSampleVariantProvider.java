package org.molgenis.genotype.variant;

import java.util.List;

import org.molgenis.genotype.Alleles;
import org.molgenis.genotype.util.Cache;

/**
 * Cached sample variant provider to prevent reloading a SNPs that is accessed
 * multiple times in a sort periode.
 * 
 * @author Patrick Deelen
 * 
 */
public class CachedSampleVariantProvider implements SampleVariantsProvider
{

	private final SampleVariantsProvider sampleVariantProvider;
	private final Cache<GeneticVariant, List<Alleles>> cache;
	private final int cacheSize;

	public CachedSampleVariantProvider(SampleVariantsProvider sampleVariantProvider, int cacheSize)
	{
		this.sampleVariantProvider = sampleVariantProvider;
		this.cache = new Cache<GeneticVariant, List<Alleles>>(cacheSize);
		this.cacheSize = cacheSize;
	}

	@Override
	public List<Alleles> getSampleVariants(GeneticVariant variant)
	{
		if (cache.containsKey(variant))
		{
			return cache.get(variant);
		}
		else
		{
			List<Alleles> variantAlleles = sampleVariantProvider.getSampleVariants(variant);
			cache.put(variant, variantAlleles);
			return variantAlleles;
		}
	}

	@Override
	public int cacheSize()
	{
		return cacheSize;
	}
}
