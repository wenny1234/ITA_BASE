package weld.util.sample;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;

import jp.co.tokyo_gas.aion.tgfw.parts.db.sql.annotation.FwSQLTransactional;
import jp.co.tokyo_gas.cisfw.sfw.annotation.CfwAfComponentInterface;
import weld.util.DBunitBackup;
import weld.util.DBunitCompare;
import weld.util.DBunitPostRestore;
import weld.util.DBunitPreRestore;
import weld.util.DBunitTable;
import weld.util.WeldRunner;

/**
 * @author Sogo
 * 
 *         TGFWのspをJUnit上で特殊なコーティングなしにテストするためのサンプルプログラム
 *         (1)@FwSQLTransactionalを指定しないとエラーもなにも出力されず、単にDAOが実行されない -->
 *         付け忘れる可能性が高いが、「必ず付ける」でも運用的には不可能ではない
 *         ただし、複雑なテストをした場合にはエラーが出ないと動かない原因が分からないため、エラー検知等のFW拡張を要望したい
 */
@Dependent
//@RunWith(WeldRunner.class)
public class WeldJunitSample2 {

	/** テスト対象. */
	@Inject
	WeldJunitSampleTarget target;
	
	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
	}
	
	@After
	public void tearDown() {
	}
	
//	@Test
	@FwSQLTransactional
	// 「@DBunitBackup」 :: バックアップ用のファイルを作成します。
	// 「file」 :: バックアップする先のファイル名でパスは自プロジェクトからの相対パスで指定します
	// テスト用のデータを作成する目的でも使用できます。
	// 「tables」 :: バックアップを取得するテーブル名をString[]で指定します
//	@DBunitBackup(file = "TestData/tmp.xml", tables = { "MNZKIY" })

	// 「@DBunitPreRestore」/「@DBunitPostRestore」 ::
	// 実行前、もしくは実行後にファイルからテーブルをリストアします
	// 「file」 :: リストアに使用するファイル名でパスは自プロジェクトからの相対パスで指定します
	// 「@DBunitBackup」と組み合わせて復元したり、テスト用データをロードしたりできます
//	@DBunitPreRestore(file = "TestData/testData.xml")
//	@DBunitPostRestore(file = "TestData/tmp.xml")

	// 「@DBunitCompare」 :: 実行後のDBの内容と指定されたファイルの内容を比較
	// 「file」 :: 比較元のファイル名でパスは自プロジェクトからの相対パスで指定します
	// 「tables」 :: 比較対象のテーブル名をString[]で指定します
//	@DBunitCompare(file = "TestData/result.xml", tables = { 
//			@DBunitTable(name = "MTNRHTF", sort = { "SYABAN_NO" }, ignore = { "REC_UPD_JNJ_NO" }) })

	public void test_normal1() {
		target.method("111");
	}
	
//	@Test
	@FwSQLTransactional
//	@DBunitBackup(file = "TestData/tmp.xml", tables = { "MNZKIY" })
//	@DBunitPreRestore(file = "TestData/testData.xml")
//	@DBunitPostRestore(file = "TestData/tmp.xml")
//	@DBunitCompare(file = "TestData/unmatch.xml", tables = { 
//			@DBunitTable(name = "MTNRHTF", sort = { "SYABAN_NO" }, ignore = { "REC_UPD_JNJ_NO" }) })
	public void test_normal2() {
//		target.method("111");
	}
}
