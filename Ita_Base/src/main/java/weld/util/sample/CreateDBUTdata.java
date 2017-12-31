package weld.util.sample;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import weld.util.DBunitBackup;
import weld.util.WeldMockExRunner;

import javax.enterprise.context.Dependent;

@Dependent
@RunWith(WeldMockExRunner.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CreateDBUTdata {

	@Test
	@DBunitBackup(file = "TestData/tmp.xml", tables = { "NTKNSD", "NTREKI" })
	public void create() {
		System.out.println("create...");
	}
}
