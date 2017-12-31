package weld.util.sample;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;

import jp.co.tokyo_gas.aion.tgfw.parts.db.sql.annotation.FwSQLTransactional;
import weld.util.DBunitBackup;
import weld.util.DBunitPostRestore;
import weld.util.DBunitPreRestore;
import weld.util.DBunitSaveResult;
import weld.util.WeldMocks;
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
@RunWith(WeldMockRunner.class)
public class WeldMockSample2 {

	/** テスト対象. */
	@InjectMocks
	WeldJunitSampleTarget target;

	@Inject
	@WeldMocks
	WeldJunitSampleTarget rTarget;

	@Test
	@FwSQLTransactional
	// 「@DBunitBackup」 :: バックアップ用のファイルを作成します。
	// 「file」 :: バックアップする先のファイル名でパスは自プロジェクトからの相対パスで指定します
	// テスト用のデータを作成する目的でも使用できます。
	// 「tables」 :: バックアップを取得するテーブル名をString[]で指定します
	@DBunitBackup(file = "TestData/tmp.xml", tables = { "SVMSTKR" })

	// 「@DBunitPreRestore」/「@DBunitPostRestore」 ::
	// 実行前、もしくは実行後にファイルからテーブルをリストアします
	// 「file」 :: リストアに使用するファイル名でパスは自プロジェクトからの相対パスで指定します
	// 「@DBunitBackup」と組み合わせて復元したり、テスト用データをロードしたりできます
	@DBunitPreRestore(file = "TestData/WeldMockSample_test_normal2.xml")
	// @DBunitPostRestore(file = "TestData/tmp.xml")

	// 「@DBunitCompare」 :: 実行後のDBの内容と指定されたファイルの内容を比較
	// 「file」 :: 比較元のファイル名でパスは自プロジェクトからの相対パスで指定します
	// 「tables」 :: 比較対象のテーブル名をString[]で指定します
	// @DBunitCompare(file = "TestData/result.xml", tables = {
	// @DBunitTable(name = "MTNRHTF", sort = { "SYABAN_NO" }, ignore = {
	// "REC_UPD_JNJ_NO" }) })

	// 「@DBunitSaveResult」 :: 実行結果用のファイルを生成します。
	// 「file」 :: セーブする先のファイル名でパスは自プロジェクトからの相対パスで指定します
	// 「tables」 :: セーブするテーブル名をString[]形式で指定します
	@DBunitSaveResult(file = "TestData/SaveResult_WeldMockSample2_test_normal1.xml", tables = { "SVMSTKR", "KGKCKIY" })

	@DBunitPostRestore(file = "TestData/tmp.xml")

	public void test_normal1() {
		final BigDecimal mtstNo = new BigDecimal(1234);
		final BigDecimal svkyNo = new BigDecimal(5678);
		System.err.println("calling method2..." + target);

		rTarget.method2(mtstNo,svkyNo);

	}

	// @Test
	@FwSQLTransactional
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
