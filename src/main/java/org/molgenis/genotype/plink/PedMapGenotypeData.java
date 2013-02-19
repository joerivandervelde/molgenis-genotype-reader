package org.molgenis.genotype.plink;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.samtools.util.BlockCompressedInputStream;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.molgenis.genotype.GenotypeDataException;
import org.molgenis.genotype.GenotypeDataIndex;
import org.molgenis.genotype.IndexedGenotypeData;
import org.molgenis.genotype.Sample;
import org.molgenis.genotype.Sequence;
import org.molgenis.genotype.annotation.Annotation;
import org.molgenis.genotype.tabix.TabixIndex;
import org.molgenis.genotype.tabix.TabixSequence;
import org.molgenis.genotype.variant.GeneticVariant;
import org.molgenis.genotype.variant.SampleVariantsProvider;
import org.molgenis.util.plink.datatypes.Biallele;
import org.molgenis.util.plink.datatypes.MapEntry;
import org.molgenis.util.plink.datatypes.PedEntry;
import org.molgenis.util.plink.drivers.PedFileDriver;
import org.molgenis.util.plink.readers.MapFileReader;

public class PedMapGenotypeData extends IndexedGenotypeData implements SampleVariantsProvider
{
	public static final char SEPARATOR_MAP = '	';
	private static final char NULL_VALUE = '0';
	private static final Logger LOG = Logger.getLogger(PedMapGenotypeData.class);
	private final GenotypeDataIndex dataIndex;
	private final File pedFile;
	private final char pedFileSeparator;
	private Map<Integer, List<Biallele>> sampleAllelesBySnpIndex = new HashMap<Integer, List<Biallele>>();
	private List<GeneticVariant> snps = new ArrayList<GeneticVariant>(1000000);
	private Map<String, GeneticVariant> snpById = new HashMap<String, GeneticVariant>(1000000);
	private Map<String, Integer> snpIndexById = new HashMap<String, Integer>(1000000);

	public PedMapGenotypeData(File bzipMapFile, File mapIndexFile, File pedFile, char pedFileSeparator)
	{
		this.pedFile = pedFile;
		this.pedFileSeparator = pedFileSeparator;

		MapFileReader mapFileReader = null;
		PedFileDriver pedFileDriver = null;
		try
		{
			pedFileDriver = new PedFileDriver(pedFile, pedFileSeparator);
			loadSampleBialleles(pedFileDriver);

			mapFileReader = new MapFileReader(new BlockCompressedInputStream(bzipMapFile), SEPARATOR_MAP);
			loadSnps(mapFileReader);
			dataIndex = new TabixIndex(mapIndexFile, bzipMapFile, new PedMapVariantLineMapper(this));
		}
		catch (IOException e)
		{
			throw new GenotypeDataException("IOException creating TabixIndex", e);
		}
		finally
		{
			IOUtils.closeQuietly(pedFileDriver);
			IOUtils.closeQuietly(mapFileReader);
		}

	}

	private void loadSampleBialleles(PedFileDriver pedFileDriver)
	{
		int count = 0;
		for (PedEntry entry : pedFileDriver)
		{
			int index = 0;
			for (Biallele biallele : entry)
			{
				List<Biallele> biallelesForSnp = sampleAllelesBySnpIndex.get(index);
				if (biallelesForSnp == null)
				{
					biallelesForSnp = new ArrayList<Biallele>();
					sampleAllelesBySnpIndex.put(index, biallelesForSnp);
				}

				biallelesForSnp.add(biallele);
				index++;
			}

			LOG.info("Loaded [" + (++count) + "] samples");
			System.out.println("Loaded [" + count + "] samples");
		}

		LOG.info("Total [" + count + "] samples");
		System.out.println("Total [" + count + "] samples");
	}

	private void loadSnps(MapFileReader reader)
	{
		int index = 0;
		for (MapEntry entry : reader)
		{
			List<String> ids = Collections.singletonList(entry.getSNP());
			String sequenceName = entry.getChromosome();
			int startPos = (int) entry.getBpPos();
			String refAllele = null;// Unknown for ped/map
			Map<String, ?> annotationValues = Collections.emptyMap();
			Integer stopPos = null;
			List<String> altDescriptions = Collections.emptyList();
			List<String> altTypes = Collections.emptyList();

			List<Biallele> sampleAlleles = sampleAllelesBySnpIndex.get(index);
			List<String> alleles = new ArrayList<String>(2);
			for (Biallele biallele : sampleAlleles)
			{
				String allele1 = biallele.getAllele1() == NULL_VALUE ? null : biallele.getAllele1() + "";
				if ((allele1 != null) && !alleles.contains(allele1))
				{
					alleles.add(allele1);
				}

				String allele2 = biallele.getAllele2() == NULL_VALUE ? null : biallele.getAllele2() + "";
				if ((allele2 != null) && !alleles.contains(allele2))
				{
					alleles.add(allele2);
				}
			}

			GeneticVariant snp = new GeneticVariant(ids, sequenceName, startPos, alleles, refAllele, annotationValues,
					stopPos, altDescriptions, altTypes, this, GeneticVariant.Type.SNP);

			snps.add(snp);
			snpById.put(snp.getPrimaryVariantId(), snp);
			snpIndexById.put(snp.getPrimaryVariantId(), index);
			index++;

			if ((index % 1000) == 0)
			{
				LOG.info("Loaded [" + index + "] snps");
				System.out.println("Loaded [" + index + "] snps");
			}
		}

		LOG.info("Total [" + index + "] snps");
		System.out.println("Total [" + index + "] snps");
	}

	@Override
	public List<Sequence> getSequences()
	{
		List<String> seqNames = getSeqNames();

		List<Sequence> sequences = new ArrayList<Sequence>(seqNames.size());
		for (String seqName : seqNames)
		{
			sequences.add(new TabixSequence(seqName, null, dataIndex));
		}

		return sequences;
	}

	@Override
	public List<Sample> getSamples()
	{
		PedFileDriver pedFileDriver = null;

		try
		{
			pedFileDriver = new PedFileDriver(pedFile, pedFileSeparator);
			List<Sample> samples = new ArrayList<Sample>();
			for (PedEntry pedEntry : pedFileDriver)
			{
				samples.add(new Sample(pedEntry.getIndividual(), pedEntry.getFamily(), Collections
						.<String, Object> emptyMap()));
			}

			return samples;
		}
		finally
		{
			IOUtils.closeQuietly(pedFileDriver);
		}
	}

	@Override
	public List<GeneticVariant> getVariants()
	{
		return snps;
	}

	@Override
	public GeneticVariant getVariantById(String primaryVariantId)
	{
		return snpById.get(primaryVariantId);
	}

	@Override
	public List<List<String>> getSampleVariants(GeneticVariant variant)
	{
		if (variant.getPrimaryVariantId() == null)
		{
			throw new IllegalArgumentException("Not a snp, missing primaryVariantId");
		}

		Integer index = snpIndexById.get(variant.getPrimaryVariantId());

		if (index == null)
		{
			throw new IllegalArgumentException("Unknown primaryVariantId [" + variant.getPrimaryVariantId() + "]");
		}

		List<Biallele> bialleles = sampleAllelesBySnpIndex.get(index);
		List<List<String>> sampleVariants = new ArrayList<List<String>>(bialleles.size());
		for (Biallele biallele : bialleles)
		{
			String allele1 = biallele.getAllele1() == NULL_VALUE ? null : biallele.getAllele1() + "";
			String allele2 = biallele.getAllele1() == NULL_VALUE ? null : biallele.getAllele2() + "";
			sampleVariants.add(Arrays.asList(allele1, allele2));
		}

		return sampleVariants;
	}

	@Override
	protected GenotypeDataIndex getIndex()
	{
		return dataIndex;
	}

	@Override
	protected Map<String, Annotation> getVariantAnnotationsMap()
	{
		return Collections.emptyMap();
	}

}