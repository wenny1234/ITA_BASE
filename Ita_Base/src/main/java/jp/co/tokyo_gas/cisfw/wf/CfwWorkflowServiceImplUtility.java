/*
 * Copyright 2015 TOKYO GAS CO.,LTD. All Rights Reserved.
 */
package jp.co.tokyo_gas.cisfw.wf;

import jp.co.tokyo_gas.aion.tgfw.parts.db.sql.annotation.FwSQLMapper;
import jp.co.tokyo_gas.aion.tgfw.workflow.dto.WfItemHistoryInfo;
import jp.co.tokyo_gas.aion.tgfw.workflow.dto.WorkflowUserInfo;
import jp.co.tokyo_gas.aion.tgfw.workflow.service.exception.WorkflowIllegalParameterException;
import jp.co.tokyo_gas.aion.tgfw.workflow.service.exception.WorkflowNotFoundException;
import jp.co.tokyo_gas.aion.tgfw.workflow.service.WorkflowService;
import jp.co.tokyo_gas.cisfw.utils.CfwStringValidator;
import jp.co.tokyo_gas.cisfw.wf.constants.CfwAuthType;
import jp.co.tokyo_gas.cisfw.wf.constants.CfwWorkflowErrorcode;
import jp.co.tokyo_gas.cisfw.wf.dao.dto.WfItemCurrentInfo;
import jp.co.tokyo_gas.cisfw.wf.dao.dto.WfItemHistoryDetailInfo;
import jp.co.tokyo_gas.cisfw.wf.dao.dto.WfItemNodeRoleExInfo;
import jp.co.tokyo_gas.cisfw.wf.dao.dto.WfWorkteamDetailInfo;
import jp.co.tokyo_gas.cisfw.wf.dao.dto.WfWorkteamInfo;
import jp.co.tokyo_gas.cisfw.wf.dao.mapper.WfItemHistoryDetailInfoMapper;
import jp.co.tokyo_gas.cisfw.wf.dao.mapper.WfItemNodeRoleExInfoMapper;
import jp.co.tokyo_gas.cisfw.wf.dao.mapper.WfWorkteamInfoMapper;
import jp.co.tokyo_gas.cisfw.wf.dao.mapper.extend.SequenceMapper;
import jp.co.tokyo_gas.cisfw.wf.dao.mapper.extend.WfItemHistoryDetailInfoMapperEx;
import jp.co.tokyo_gas.cisfw.wf.dao.mapper.extend.WfItemNodeRoleExInfoMapperEx;
import jp.co.tokyo_gas.cisfw.wf.dao.mapper.extend.WfWorkflowHistoryDetailInfoSearchMapperEx;
import jp.co.tokyo_gas.cisfw.wf.dao.mapper.extend.WfWorkteamInfoMapperEx;
import jp.co.tokyo_gas.cisfw.wf.dto.CfwAuthInfo;
import jp.co.tokyo_gas.cisfw.wf.dto.CfwWorkflowInfo;
import jp.co.tokyo_gas.cisfw.wf.dto.CfwWorkflowUserInfo;
import jp.co.tokyo_gas.cisfw.wf.exception.CfwWorkflowApplicationException;
import jp.co.tokyo_gas.cisfw.wf.util.CfwWfAIOnToCiriusConverter;
import jp.co.tokyo_gas.cisfw.wf.util.CfwWfCiriusToAIOnConverter;
import jp.co.tokyo_gas.cisfw.wf.util.CfwWfCiriusToCiriusConverter;

import java.util.List;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;


/**
 * 共通的に使用する機能のクラスです。<br>
 *
 * @author A.Funakoshi (TDC)
 * @version 1.0.0
 */
@RequestScoped
public class CfwWorkflowServiceImplUtility {

	/** 標準ワークフローのサービス */
	@Inject
	protected WorkflowService workflowService;

	/** ノード担当グループ情報テーブル */
	@Inject
	@FwSQLMapper
	private WfItemNodeRoleExInfoMapper itemNodeRoleExInfoMapper;

	/** ノード担当グループ情報テーブル拡張 現行用 */
	@Inject
	@FwSQLMapper
	private WfItemNodeRoleExInfoMapperEx itemNodeRoleInfoMapperEx;

	/** 作業チーム情報テーブル */
	@Inject
	@FwSQLMapper
	private WfWorkteamInfoMapper workteamInfoMapper;

	/** 作業チーム情報テーブル拡張　*/
	@Inject
	@FwSQLMapper
	private WfWorkteamInfoMapperEx workteamInfoMapperEx;

	/** ワークフロー履歴詳細情報テーブル */
	@Inject
	@FwSQLMapper
	private WfWorkflowHistoryDetailInfoSearchMapperEx workflowHistoryDetailInfoSearchMapper;

	/** シーケンスオブジェクト */
	@Inject
	@FwSQLMapper
	private SequenceMapper sequenceMapper;

	/** ワークフロー履歴詳細情報テーブル */
	@Inject
	@FwSQLMapper
	private WfItemHistoryDetailInfoMapper wfItemHistoryDetailInfoMapper;

	/** ワークフロー履歴詳細情報テーブル拡張用 */
	@Inject
	@FwSQLMapper
	private WfItemHistoryDetailInfoMapperEx wfItemHistoryDetailInfoMapperEx;

	/**
	 * 作業チーム情報存在確認<br>
	 * 指定された作業チーム情報が登録されているか確認します。<br>
	 *
	 * @param workTeamId 作業チームコード
	 * @param usableCmpCodeFlg 企業コード使用可否フラグ
	 * @param lastUpdateCmpCode 最終更新者企業コード
	 * @return True：存在する False：存在しない
	 * @throws CfwWorkflowApplicationException ワークフローアプリケーション例外をスローします。
	 */
	protected boolean existsWorkTeamInfo(String workTeamId, boolean usableCmpCodeFlg, String lastUpdateCmpCode)
		throws CfwWorkflowApplicationException {

		/**
		 * SQLの実行
		 */
		// パラメーター生成
		WfWorkteamInfo workteamInfo = new WfWorkteamInfo();
		workteamInfo.setWorkteamId(workTeamId);

		// 企業コード使用可否フラグ判定
		if (usableCmpCodeFlg) {
			// パラメーターに企業コードを追加
			workteamInfo.setUpdateUserCmpCode(lastUpdateCmpCode);
			// 作業チームTBLから対象作業チーム情報を取得
			workteamInfo = this.workteamInfoMapperEx.find(workteamInfo);
		} else {
			// 作業チームTBLから対象作業チーム情報を取得
			workteamInfo = this.workteamInfoMapper.find(workteamInfo);
		}

		// 結果確認
		if (workteamInfo != null) {
			// 作業チーム情報が取得できた場合
			return true;
		} else {
			// 作業チーム情報が取得できない場合
			return false;
		}
	}

	/**
	 * 作業チーム詳細情報を作成します。
	 * @param user ユーザー情報
	 * @return 作業チーム詳細情報
	 * @throws CfwWorkflowApplicationException ワークフローアプリケーション例外をスローします。
	 */
	protected WfWorkteamDetailInfo createWfWorkflowDetailInfo(CfwWorkflowUserInfo user) throws CfwWorkflowApplicationException {

		String cmpCode = user.getAuthInfoList().get(0).getAuthKey();
		String orgCode = user.getAuthInfoList().get(0).getAuthValue();

		WfWorkteamDetailInfo workteamDetailInfo = new WfWorkteamDetailInfo();

		String[] auth = convertAuthType(cmpCode,orgCode);
		workteamDetailInfo.setCmpCode(auth[0]);
		workteamDetailInfo.setOrgCode(auth[1]);

		// ユーザーIDの設定
		workteamDetailInfo.setUserId(user.getUserId());

		return workteamDetailInfo;
	}

	/**
	 * 企業コード（権限種別）と所属コード（権限種別）を元に企業コードと所属コードを作成します。
	 *
	 * @param cmpCode 企業コード（権限種別）
	 * @param orgCode 所属コード（権限種別）
	 * @return String[0]:企業コード String[1]:所属コード
	 * @throws CfwWorkflowApplicationException ワークフローアプリケーション例外をスローします。
	 */
	protected String[] convertAuthType(String cmpCode, String orgCode) throws CfwWorkflowApplicationException {

		String[] re = new String[2];
		// 企業コード、所属コード];を設定
		if (CfwAuthType.ARTIS.getValue().equals(cmpCode)) {
			// ARTISの場合
			re[0] = orgCode.substring(0, 8);
			re[1] = orgCode.substring(8, 17);
		} else if (CfwAuthType.NWCODE.getValue().equals(cmpCode)) {
			// NW箇所の場合
			re[0] = orgCode.substring(0, 8);
			re[1] = orgCode.substring(8, 15);
		} else {
			// 作業チーム、職制、ユーザーIDの場合
			throw new CfwWorkflowApplicationException(
				CfwWorkflowErrorcode.CFWWFIP007.getWfCode(), "企業コード:" + cmpCode);
		}
		return re;
	}

	/**
	 * ノード担当グループ情報設定.<br>
	 * ノードにノード担当グループ情報を設定します。<br>
	 * 一旦該当ノード担当グループ情報を全て削除して、全て登録し直します。<br>
	 * 権限情報のリストがnullの場合は削除だけを行います。<br>
	 *
	 * @param workflowId ワークフローID
	 * @param nodeDefId ノード定義ID
	 * @param authList 権限情報のリスト
	 * @param user ユーザー情報
	 * @return True:成功 False:失敗
	 * @throws CfwWorkflowApplicationException ワークフローアプリケーション例外をスローします。
	 */
	protected boolean registerWorkflowNodeAuthGroupInfo(String workflowId, String nodeDefId, List<CfwAuthInfo> authList,
		CfwWorkflowUserInfo user) throws CfwWorkflowApplicationException {

		// 必須チェック
		if (CfwStringValidator.isEmpty(workflowId)) {
			throw new CfwWorkflowApplicationException(CfwWorkflowErrorcode.CFWWFIP001.getWfCode(), "ワークフローID");
		}

		// 必須チェック
		if (CfwStringValidator.isEmpty(nodeDefId)) {
			throw new CfwWorkflowApplicationException(CfwWorkflowErrorcode.CFWWFIP001.getWfCode(), "ノード定義ID");
		}

		// 必須チェック
		if (authList == null) {
			throw new CfwWorkflowApplicationException(CfwWorkflowErrorcode.CFWWFIP001.getWfCode(), "ノード担当グループ情報.権限情報");
		}

		for (CfwAuthInfo authInfo : authList) {

			if (authInfo == null) {
				throw new CfwWorkflowApplicationException(CfwWorkflowErrorcode.CFWWFIP001.getWfCode(), "ノード担当グループ情報.権限情報");
			}

			if (CfwStringValidator.isEmpty(authInfo.getAuthKey())) {
				throw new CfwWorkflowApplicationException(CfwWorkflowErrorcode.CFWWFIP001.getWfCode(), "ノード担当グループ情報.権限キー");
			}

			if (CfwStringValidator.isEmpty(authInfo.getAuthValue())) {
				throw new CfwWorkflowApplicationException(CfwWorkflowErrorcode.CFWWFIP001.getWfCode(), "ノード担当グループ情報.権限値");
			}
		}

		// DBから削除
		this.itemNodeRoleInfoMapperEx.deleteByWorkflowIdAndNodeDefId(workflowId, nodeDefId);

		for (CfwAuthInfo authInfo : authList) {

			// ノード担当グループ情報IDの設定
			String nodeRoleInfoId = this.sequenceMapper.sequenceNodeGroupInfoId();

			WfItemNodeRoleExInfo itemNodeRoleInfo
				= CfwWfCiriusToCiriusConverter.convertCfwAuthInfoToWfItemNodeRoleInfo(authInfo, nodeRoleInfoId,
					workflowId, nodeDefId, user.getUserId());
			this.itemNodeRoleExInfoMapper.insert(itemNodeRoleInfo);
		}

		return true;
	}


	/**
	 * 最新操作日時のワークフロー履歴詳細情報を作成します。
	 *
	 * @param cfwWorkflowInfo ワークフロー情報
	 * @param cfwWorkflowuserInfo ユーザー情報
	 * @param wfItemCurrentInfo ワークフロー現在情報
	 * @return ワークフロー履歴詳細情報
	 * @throws CfwWorkflowApplicationException ワークフローアプリケーション例外をスローします。
	 */
	protected WfItemHistoryDetailInfo createWorkflowHistoryDetailInfo(
		CfwWorkflowInfo cfwWorkflowInfo, CfwWorkflowUserInfo cfwWorkflowuserInfo, WfItemCurrentInfo wfItemCurrentInfo)
			throws CfwWorkflowApplicationException {

		// 標準ワークフローのユーザー情報生成
		WorkflowUserInfo tgfwUserInfo =
			CfwWfCiriusToAIOnConverter.convertCfwWorkflowUserInfoToWorkflowUserInfo(cfwWorkflowuserInfo);

		// 標準ワークフローの案件履歴一覧照会を実行
		List<WfItemHistoryInfo> historyInfoList = null;
		try {

			historyInfoList = workflowService.findItemHistoryInfoList(cfwWorkflowInfo.getWorkflowId(), tgfwUserInfo);

			if (historyInfoList == null) {
				// ワークフロー履歴情報が取得できない場合
				throw new CfwWorkflowApplicationException(CfwWorkflowErrorcode.CFWWFNF001.getWfCode(), "ワークフロー履歴情報");
			}

		} catch (WorkflowIllegalParameterException e) {
			// 標準での必須チェックエラーの場合
			throw new CfwWorkflowApplicationException(e.getCause(), e.getErrorCode(), e.getErrorParams());
		} catch (WorkflowNotFoundException e) {
			// 案件情報が存在しない場合
			throw new CfwWorkflowApplicationException(e.getCause(), e.getErrorCode(), e.getErrorParams());
		}

		// ワークフロー履歴詳細情報生成
		// ※標準ワークフローから降順にソートしたリストが渡される為、0を指定し最新の履歴を取得
		WfItemHistoryDetailInfo historyDetailInfo =
			CfwWfAIOnToCiriusConverter.convertWfItemHistoryInfoToWfItemHistoryDetailInfo(
				historyInfoList.get(0), cfwWorkflowInfo, cfwWorkflowuserInfo, wfItemCurrentInfo);

		return historyDetailInfo;
	}

	/**
	 * ワークフロー履歴詳細情報のリストを作成します。
	 *
	 * @param workflowId ワークフローID
	 * @return ワークフロー履歴詳細情報リスト
	 */
	protected List<WfItemHistoryDetailInfo> createWorkflowHistoryDetailInfoList(String workflowId) {

		// ワークフロー履歴詳細情報DTOのリストを取得
		List<WfItemHistoryDetailInfo> wfItemHistoryDetailInfoList = wfItemHistoryDetailInfoMapperEx.findByWorkflowId(workflowId);

		return wfItemHistoryDetailInfoList;
	}
}
