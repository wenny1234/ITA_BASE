package weld.util.sample;

import static org.junit.Assert.assertEquals;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import jp.co.tokyo_gas.aion.tgfw.parts.db.sql.annotation.FwSQLTransactional;
import jp.co.tokyo_gas.cisfw.sfw.annotation.CfwAfComponentInterface;
import weld.util.DBunitBackup;
import weld.util.DBunitCompare;
import weld.util.DBunitPostRestore;
import weld.util.DBunitPreRestore;
import weld.util.DBunitTable;
import weld.util.WeldMockExRunner;
import weld.util.WeldMocks;
import weld.util.WeldMockUtil;
import weld.util.WeldRunner;
import weld.util.WeldMockRunner;

/**
 * @author Sogo
 * 
 *         TGFWのspをJUnit上で特殊なコーティングなしにテストするためのサンプルプログラム
 *         (1)@FwSQLTransactionalを指定しないとエラーもなにも出力されず、単にDAOが実行されない -->
 *         付け忘れる可能性が高いが、「必ず付ける」でも運用的には不可能ではない
 *         ただし、複雑なテストをした場合にはエラーが出ないと動かない原因が分からないため、エラー検知等のFW拡張を要望したい
 */
@Dependent
@RunWith(WeldMockExRunner.class)
public class WeldMockExSample {

	/** テスト対象. */
	@InjectMocks
	WeldJunitSampleTarget target;

	@Inject
	@WeldMocks
	WeldJunitSampleTarget rTarget;

	@Mock
	private CXXDA01644Mapper cXXDA01644Mapper;

	@Test
	@FwSQLTransactional
	// 「@DBunitBackup」 :: バックアップ用のファイルを作成します。
	// 「file」 :: バックアップする先のファイル名でパスは自プロジェクトからの相対パスで指定します
	// テスト用のデータを作成する目的でも使用できます。
	// 「tables」 :: バックアップを取得するテーブル名をString[]で指定します
	@DBunitBackup(file = "TestData/tmp.xml", tables = { "MNZKIY" })

	// 「@DBunitPreRestore」/「@DBunitPostRestore」 ::
	// 実行前、もしくは実行後にファイルからテーブルをリストアします
	// 「file」 :: リストアに使用するファイル名でパスは自プロジェクトからの相対パスで指定します
	// 「@DBunitBackup」と組み合わせて復元したり、テスト用データをロードしたりできます
	@DBunitPreRestore(file = "TestData/WeldMockSample_test_normal1.xml")
	@DBunitPostRestore(file = "TestData/tmp.xml")

	// 「@DBunitCompare」 :: 実行後のDBの内容と指定されたファイルの内容を比較
	// 「file」 :: 比較元のファイル名でパスは自プロジェクトからの相対パスで指定します
	// 「tables」 :: 比較対象のテーブル名をString[]で指定します
	// @DBunitCompare(file = "TestData/result.xml", tables = {
	// @DBunitTable(name = "MTNRHTF", sort = { "SYABAN_NO" }, ignore = {
	// "REC_UPD_JNJ_NO" }) })

	public void test_normal1() {
		final String OP_YMD = "20170702";
		System.err.println("calling method..." + target);

		ArgumentCaptor<CXXDA01644InDaoDTO> cap = ArgumentCaptor.forClass(CXXDA01644InDaoDTO.class);
		CXXDA01644OutDaoDTO out = new CXXDA01644OutDaoDTO();
		out.setSyokyNo("1");
		// Mockito.when(cXXDA01644Mapper.find(cap.capture())).thenReturn(out);

//		WeldMockUtil.mockThenReal(cXXDA01644Mapper, "find", cap);
		WeldMockUtil wmu = new WeldMockUtil(rTarget);
		wmu.mockThenReal(cXXDA01644Mapper, "find", cap);
//		wmu.chgInject(cXXDA01644Mapper);
		// Mockito.when(cXXDA01644Mapper.find(cap.capture()))
		// .thenAnswer(new Answer<Object>() {
		// public Object answer(InvocationOnMock invocation) throws Throwable {
		// return ((CXXDA01644Mapper)wmu.useInject(cXXDA01644Mapper)).find(
		// (CXXDA01644InDaoDTO) invocation.getArguments()[0]);
		// }
		// });
		// target.method(OP_YMD);
		CXXDA01644OutDaoDTO ret = rTarget.method(OP_YMD);
		wmu.restoreInjects();

		// System.out.println("OpYmd = " + cap.getValue().getOpYmd());
		// assertEquals(OP_YMD, cap.getValue().getOpYmd());
		System.out.println("SyokyNo = " + ret.getSyokyNo());
		assertEquals("111", ret.getSyokyNo());
	}

	// @Test
//	@FwSQLTransactional
	// @DBunitBackup(file = "TestData/tmp.xml", tables = { "MNZKIY" })
	// @DBunitPreRestore(file = "TestData/testData.xml")
	// @DBunitPostRestore(file = "TestData/tmp.xml")
	// @DBunitCompare(file = "TestData/unmatch.xml", tables = {
	// @DBunitTable(name = "MTNRHTF", sort = { "SYABAN_NO" }, ignore = {
	// "REC_UPD_JNJ_NO" }) })
	public void test_normal2() {
		// target.method("111");
	}
}
