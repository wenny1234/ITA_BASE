/*
 * Copyright 2015 TOKYO GAS CO.,LTD. All Rights Reserved.
 */
package jp.co.tokyo_gas.cisfw.wf;

import jp.co.tokyo_gas.aion.tgfw.parts.db.sql.annotation.FwSQLMapper;
import jp.co.tokyo_gas.cisfw.converter.CfwBeanConverter;
import jp.co.tokyo_gas.cisfw.wf.constants.CfwWorkflowErrorcode;
import jp.co.tokyo_gas.cisfw.wf.dao.dto.WfItemNodeRoleExInfo;
import jp.co.tokyo_gas.cisfw.wf.dao.dto.WfItemNodeRoleExInfoEnd;
import jp.co.tokyo_gas.cisfw.wf.dao.dto.WfWorkteamDetailInfo;
import jp.co.tokyo_gas.cisfw.wf.dao.mapper.extend.WfItemNodeRoleExInfoEndMapperEx;
import jp.co.tokyo_gas.cisfw.wf.dao.mapper.extend.WfItemNodeRoleExInfoMapperEx;
import jp.co.tokyo_gas.cisfw.wf.dao.mapper.extend.WfWorkteamDetailInfoMapperEx;
import jp.co.tokyo_gas.cisfw.wf.dto.CfwAuthInfo;
import jp.co.tokyo_gas.cisfw.wf.dto.CfwWorkflowNodeAuthDef;
import jp.co.tokyo_gas.cisfw.wf.dto.CfwWorkflowUserInfo;
import jp.co.tokyo_gas.cisfw.wf.exception.CfwWorkflowApplicationException;
import jp.co.tokyo_gas.cisfw.wf.util.CfwWfCiriusToCiriusConverter;

import org.apache.commons.collections.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

/**
 * 権限（ワークフロー参照権限、ノード担当グループ権限）のチェックのクラスです。<br>
 *
 * @author Y.Furuyama (TDC)
 * @version 1.0.0
 */
@RequestScoped
public class CfwWorkflowServiceImplAuthCheckUtility {

	/** 共通的に使用する機能のクラス */
	@Inject
	private CfwWorkflowServiceImplUtility workflowServiceImplUtility;

	/** ノード担当グループ情報テーブル拡張 現行用 */
	@Inject
	@FwSQLMapper
	private WfItemNodeRoleExInfoMapperEx itemNodeRoleInfoMapperEx;

	/** ノード担当グループ情報テーブル拡張 完了用 */
	@Inject
	@FwSQLMapper
	private WfItemNodeRoleExInfoEndMapperEx itemNodeRoleInfoEndMapperEx;

	/** 作業チーム詳細情報テーブル拡張 */
	@Inject
	@FwSQLMapper
	private WfWorkteamDetailInfoMapperEx workteamDetailInfoMapperEx;


	/**
	 * 参照権限判定<br>
	 * 指定されたユーザーがワークフローに対して参照できる権限を持っているか判定します。<br>
	 *
	 * @param workflowId ワークフローID
	 * @param user ユーザー情報
	 * @return true:権限あり false:権限なし
	 * @throws CfwWorkflowApplicationException ワークフローアプリケーション例外をスローします。
	 */
	protected boolean canReadWorkflowNodeByWfItemNodeRoleExList(String workflowId, CfwWorkflowUserInfo user)
		throws CfwWorkflowApplicationException {

		// ワークフローIDからノード担当グループ情報一覧現行用を取得
		List<WfItemNodeRoleExInfo> itemNodeRoleExInfoList = this.itemNodeRoleInfoMapperEx.findByWorkflowId(workflowId);

		// 現行用を取得できなかった場合
		if (CollectionUtils.isEmpty(itemNodeRoleExInfoList)) {

			// ワークフローIDからノード担当グループ情報一覧完了用を取得
			List<WfItemNodeRoleExInfoEnd> itemNodeRoleExInfoEndList =
				this.itemNodeRoleInfoEndMapperEx.findByWorkflowId(workflowId);

			// ノード担当グループ情報一覧の完了用を現行用に形式変換して、リストに追加
			for (WfItemNodeRoleExInfoEnd wfItemNodeRoleExInfoEnd : itemNodeRoleExInfoEndList) {
				WfItemNodeRoleExInfo wfItemNodeRoleExInfo = new WfItemNodeRoleExInfo();
				CfwBeanConverter.simpleCopy(wfItemNodeRoleExInfoEnd, wfItemNodeRoleExInfo);
				itemNodeRoleExInfoList.add(wfItemNodeRoleExInfo);
			}
		}

		// 権限判定を行う
		boolean result = workflowNodeByWfItemNodeRoleExList(itemNodeRoleExInfoList, user);

		return result;
	}

	/**
	 * 実行権限判定<br>
	 * 指定されたユーザーがノードに対して実行できる権限を持っているか判定します。<br>
	 * 申請で使用します。<br>
	 *
	 * @param authDefList ノード担当グループ定義リスト
	 * @param user ユーザー情報
	 * @return True：権限あり False：権限なし
	 * @throws CfwWorkflowApplicationException ワークフローアプリケーション例外をスローします。
	 */
	protected boolean canWriteWorkflowNodeByWfNodeAuthDefList(List<CfwWorkflowNodeAuthDef> authDefList, CfwWorkflowUserInfo user)
		throws CfwWorkflowApplicationException {

		List<WfItemNodeRoleExInfo> nodeAuthList = new ArrayList<WfItemNodeRoleExInfo>();

		for (CfwWorkflowNodeAuthDef authDef :authDefList) {
			// ノード担当グループ定義からノード担当グループ情報現行用DTOに形式変換（権限キー, 権限値のみ）
			WfItemNodeRoleExInfo authGroupInfo = new WfItemNodeRoleExInfo();
			authGroupInfo.setAuthKey(authDef.getAuthKey());
			authGroupInfo.setAuthValue(authDef.getAuthValue());
			nodeAuthList.add(authGroupInfo);
		}

		// 権限判定を行う
		return  workflowNodeByWfItemNodeRoleExList(nodeAuthList, user);
	}

	/**
	 * 実行権限判定<br>
	 * 指定されたユーザーがノードに対して実行できる権限を持っているか判定します。<br>
	 * 承認、差戻し、削除、担当者割振で使用します。<br>
	 *
	 * @param authDefList ノード担当グループ情報DTOリスト
	 * @param user ユーザー情報
	 * @return True：権限あり False：権限なし
	 * @throws CfwWorkflowApplicationException ワークフローアプリケーション例外をスローします。
	 */
	protected boolean canWriteWorkflowNodeByWfItemNodeRoleExInfoList(List<WfItemNodeRoleExInfo> authDefList, CfwWorkflowUserInfo user)
		throws CfwWorkflowApplicationException {

		// 権限判定を行う
		return  workflowNodeByWfItemNodeRoleExList(authDefList, user);
	}

	/**
	 * 権限判定処理<br>
	 * 指定されたユーザーがワークフローに対して参照できる権限を持っているか判定します。<br>
	 * ユーザーの権限にはユーザーが所属している作業チームを追加します。<br>
	 *
	 * @param itemNodeRoleExInfoList ノード担当グループ情報のリスト
	 * @param user 実行者ユーザー情報
	 * @return True：権限あり False：権限なし
	 * @throws CfwWorkflowApplicationException ワークフローアプリケーション例外をスローします。
	 */
	private boolean workflowNodeByWfItemNodeRoleExList(
		List<WfItemNodeRoleExInfo> itemNodeRoleExInfoList, CfwWorkflowUserInfo user) throws CfwWorkflowApplicationException {

		// ノード担当グループ情報のリストの必須チェック
		if (CollectionUtils.isEmpty(itemNodeRoleExInfoList)) {
			throw new CfwWorkflowApplicationException(CfwWorkflowErrorcode.CFWWFIP001.getWfCode(), "ノード担当グループ情報");
		}

		// 管理者フラグを確認する
		if (user.isAdminFlg()) {
			return true;
		}

		// ユーザーIDから作業チーム一覧を取得
		WfWorkteamDetailInfo workteamDetailInfo = workflowServiceImplUtility.createWfWorkflowDetailInfo(user);
		List<WfWorkteamDetailInfo> detailList = this.workteamDetailInfoMapperEx.findByWorkteamDetailInfo(workteamDetailInfo);

		// 作業チーム一覧の形式を変換
		List<CfwAuthInfo> workTeamAuthList =
			CfwWfCiriusToCiriusConverter.convertWfWorkteamDetailInfoListToCfwAuthInfoList(detailList);

		// ノード担当グループ情報のリストから権限情報のリストを取得
		List<CfwAuthInfo> workflowAuthList =
			CfwWfCiriusToCiriusConverter.convertWfItemNodeRoleExInfoListToCfwAuthInfoList(itemNodeRoleExInfoList);

		// 比較用の権限情報のリストを作成
		List<CfwAuthInfo> userAuthList = user.getAuthInfoList();
		workTeamAuthList.addAll(userAuthList);

		// workflowAuthListの権限情報がworkTeamAuthListの権限情報に一件でも存在するか確認
		for (CfwAuthInfo targetAuthInfo : workflowAuthList) {
			for (CfwAuthInfo userAuthInfo : workTeamAuthList) {
				if (targetAuthInfo.getAuthKey().equals(userAuthInfo.getAuthKey())
					&& targetAuthInfo.getAuthValue().equals(userAuthInfo.getAuthValue())) {
					return true;
				}
			}
		}

		return false;
	}
}
