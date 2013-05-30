package org.molgenis.genotype;

import static org.testng.AssertJUnit.assertEquals;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class AlleleTest
{
	@BeforeMethod
	public void beforeMethod()
	{
	}

	@Test
	public void createString()
	{
		Allele testAllele = Allele.create("AAA");

		assertEquals(testAllele.getAlleleAsString(), "AAA");
		assertEquals((byte) testAllele.getAlleleAsSnp(), -1);

	}

	@Test
	public void getComplement()
	{
		assertEquals(Allele.A_ALLELE.getComplement().getAlleleAsString(), "T");
	}

	@Test
	public void getSnpAllele()
	{
		assertEquals(Allele.A_ALLELE.getAlleleAsSnp(), 'A');
	}

	@Test
	public void getStringAllele()
	{
		assertEquals(Allele.A_ALLELE.getAlleleAsString(), "A");
	}

	@Test
	public void isSnpAllele()
	{
		assertEquals(Allele.A_ALLELE.isSnpAllele(), true);
	}

}
