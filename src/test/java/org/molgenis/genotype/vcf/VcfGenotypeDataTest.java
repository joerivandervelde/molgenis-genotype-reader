package org.molgenis.genotype.vcf;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.molgenis.genotype.ResourceTest;
import org.molgenis.genotype.Sequence;
import org.molgenis.genotype.annotation.Annotation;
import org.molgenis.genotype.annotation.VcfAnnotation;
import org.molgenis.genotype.variant.GeneticVariant;
import org.molgenis.genotype.variant.ListVariantHandler;
import org.molgenis.genotype.variant.SnpGeneticVariant;
import org.molgenis.genotype.variant.VariantHandler;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class VcfGenotypeDataTest extends ResourceTest
{
	private VcfGenotypeData genotypeData;

	@BeforeClass
	public void beforeClass() throws IOException
	{
		genotypeData = new VcfGenotypeData(getTestVcfGz(), getTestVcfGzTbi());
	}

	@AfterClass
	public void afterClass() throws IOException
	{
		genotypeData.close();
	}

	@Test
	public void getSeqNames()
	{
		List<String> seqNames = genotypeData.getSeqNames();
		assertNotNull(seqNames);
		assertEquals(seqNames.size(), 3);
		assertEquals(seqNames.get(0), "1");
		assertEquals(seqNames.get(1), "2");
		assertEquals(seqNames.get(2), "3");
	}

	@Test
	public void getVariantAnnotationsMap()
	{
		Map<String, Annotation> annotations = genotypeData.getVariantAnnotationsMap();
		assertNotNull(annotations);
		assertEquals(annotations.size(), 21);

		Annotation annotation = annotations.get("NS");
		assertNotNull(annotation);
		assertEquals(annotation.getId(), "NS");
		assertTrue(annotation instanceof VcfAnnotation);
	}

	@Test
	public void testGetSequences()
	{
		List<Sequence> sequences = genotypeData.getSequences();
		assertNotNull(sequences);
		assertEquals(sequences.size(), 3);
	}

	@Test
	public void testGetSequenceByName()
	{
		Sequence sequence = genotypeData.getSequenceByName("2");
		assertNotNull(sequence);
		assertEquals(sequence.getName(), "2");

		ListVariantHandler handler = new ListVariantHandler();
		sequence.variants(handler);

		List<GeneticVariant> variants = handler.getVariants();
		assertNotNull(variants);
		assertEquals(variants.size(), 1);
		GeneticVariant variant = variants.get(0);
		assertEquals(variant.getPrimaryVariantId(), "rs4908464");
		assertEquals(variant.getStartPos(), 7569187);
		assertEquals(variant.getRefAllele(), "G");
		assertEquals(variant.getSequenceName(), "2");
		assertTrue(variant instanceof SnpGeneticVariant);

		List<String> alleles = variant.getAlleles();
		assertNotNull(alleles);
		assertEquals(alleles.size(), 2);
		assertEquals(alleles.get(0), "G");
		assertEquals(alleles.get(1), "C");

		List<String> sampleVariants = variant.getSampleVariants();
		assertNotNull(sampleVariants);
		assertEquals(sampleVariants.size(), 2);
		assertEquals(sampleVariants.get(0), "C");
		assertEquals(sampleVariants.get(0), "C");
	}

	@Test
	public void testGetSequenceByNameNotExisting()
	{
		assertNull(genotypeData.getSequenceByName("Bogus"));
	}

	@Test
	public void testGetVariant()
	{
		List<GeneticVariant> variants = genotypeData.getVariantsByPos("1", 565286);
		assertNotNull(variants);
		assertEquals(variants.size(), 1);
		assertEquals(variants.get(0).getPrimaryVariantId(), "rs1578391");

		assertNotNull(genotypeData.getVariantsByPos("bogus", 8));
		assertTrue(genotypeData.getVariantsByPos("bogus", 8).isEmpty());
	}

	@Test
	public void testSeqVariants()
	{
		CountingVariantHandler handler = new CountingVariantHandler();
		genotypeData.seqVariants("1", handler);
		assertEquals(handler.count, 6);

		handler = new CountingVariantHandler();
		genotypeData.seqVariants("2", handler);
		assertEquals(handler.count, 1);

		handler = new CountingVariantHandler();
		genotypeData.seqVariants("x", handler);
		assertEquals(handler.count, 0);
	}

	@Test
	public void testGetSampleGeneticVariants()
	{
		List<String> variants = genotypeData.getSampleGeneticVariants("1", 3172273);
		assertNotNull(variants);
		assertEquals(variants.size(), 2);
		assertEquals(variants.get(0), "C");
		assertEquals(variants.get(1), "C");

		variants = genotypeData.getSampleGeneticVariants("1", 1);
		assertNotNull(variants);
		assertTrue(variants.isEmpty());
	}

	@Test
	public void testGeneticVariantAnnotations()
	{
		// NS=1;DP=4;AF=1.000;ANNOT=INT;GI=PRDM16;TI=NM_022114.3;PI=NP_071397.3
		List<GeneticVariant> variants = genotypeData.getVariantsByPos("1", 3171929);
		assertNotNull(variants);
		assertEquals(variants.size(), 1);

		GeneticVariant variant = variants.get(0);
		assertNotNull(variant.getAnnotationValues());
		assertEquals(variant.getAnnotationValues().size(), 7);

		Object annotationValue = variant.getAnnotationValues().get("NS");
		assertNotNull(annotationValue);
		assertEquals(annotationValue, Integer.valueOf(1));

		annotationValue = variant.getAnnotationValues().get("AF");
		assertNotNull(annotationValue);
		assertTrue(annotationValue instanceof List);
		@SuppressWarnings("unchecked")
		List<Float> floats = (List<Float>) annotationValue;
		assertEquals(floats.size(), 1);
		assertEquals(floats.get(0).floatValue(), 1.0, 0.001);

		annotationValue = variant.getAnnotationValues().get("ANNOT");
		assertNotNull(annotationValue);
		assertTrue(annotationValue instanceof List);
		@SuppressWarnings("unchecked")
		List<String> strings = (List<String>) annotationValue;
		assertEquals(strings.size(), 1);
		assertEquals(strings.get(0), "INT");
	}

	@Test
	public void testStopPos()
	{
		List<GeneticVariant> variants = genotypeData.getVariantsByPos("1", 565286);
		assertNotNull(variants);
		assertEquals(variants.size(), 1);
		assertNull(variants.get(0).getStopPos());

		variants = genotypeData.getVariantsByPos("3", 7569);
		assertNotNull(variants);
		assertEquals(variants.size(), 1);
		assertEquals(variants.get(0).getStopPos(), Integer.valueOf(321887));
	}

	@Test
	public void testAlt()
	{
		List<GeneticVariant> variants = genotypeData.getVariantsByPos("3", 7569);
		assertNotNull(variants);
		assertEquals(variants.size(), 1);

		GeneticVariant variant = variants.get(0);

		assertNotNull(variant.getAltTypes());
		assertEquals(variant.getAltTypes().size(), 1);
		assertEquals(variant.getAltTypes().get(0), "DEL");

		assertNotNull(variant.getAltDescriptions());
		assertEquals(variant.getAltDescriptions().size(), 1);
		assertEquals(variant.getAltDescriptions().get(0), "Deletion");
	}

	private static class CountingVariantHandler implements VariantHandler
	{
		private int count = 0;

		@Override
		public boolean handle(GeneticVariant variant)
		{
			count++;
			return true;
		}
	}
}
