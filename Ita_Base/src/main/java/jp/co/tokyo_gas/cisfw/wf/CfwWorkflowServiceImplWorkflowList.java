/*
 * Copyright 2015 TOKYO GAS CO.,LTD. All Rights Reserved.
 */
package jp.co.tokyo_gas.cisfw.wf;

import jp.co.tokyo_gas.aion.tgfw.parts.db.sql.annotation.FwSQLMapper;
import jp.co.tokyo_gas.aion.tgfw.workflow.dto.WfItemInfo;
import jp.co.tokyo_gas.aion.tgfw.workflow.dto.WfItemNodeInfo;
import jp.co.tokyo_gas.aion.tgfw.workflow.dto.WfNodeDef;
import jp.co.tokyo_gas.aion.tgfw.workflow.dto.WfWorkflowDef;
import jp.co.tokyo_gas.aion.tgfw.workflow.dto.WorkflowUserInfo;
import jp.co.tokyo_gas.aion.tgfw.workflow.service.exception.WorkflowApplicationException;
import jp.co.tokyo_gas.aion.tgfw.workflow.service.WorkflowService;
import jp.co.tokyo_gas.cisfw.converter.CfwBeanConverter;
import jp.co.tokyo_gas.cisfw.utils.CfwStringValidator;
import jp.co.tokyo_gas.cisfw.wf.constants.CfwAuthType;
import jp.co.tokyo_gas.cisfw.wf.constants.CfwDetailInfoNoType;
import jp.co.tokyo_gas.cisfw.wf.constants.CfwWfSearchConditionKbn;
import jp.co.tokyo_gas.cisfw.wf.constants.CfwWorkflowErrorcode;
import jp.co.tokyo_gas.cisfw.wf.constants.CfwWorkflowNodeStatus;
import jp.co.tokyo_gas.cisfw.wf.constants.CfwWorkflowNodeType;
import jp.co.tokyo_gas.cisfw.wf.constants.CfwWorkflowOperationType;
import jp.co.tokyo_gas.cisfw.wf.constants.CfwWorkflowSortType;
import jp.co.tokyo_gas.cisfw.wf.constants.CfwWorkflowStatus;
import jp.co.tokyo_gas.cisfw.wf.dao.dto.WfItemCurrentInfo;
import jp.co.tokyo_gas.cisfw.wf.dao.dto.WfItemCurrentInfoEnd;
import jp.co.tokyo_gas.cisfw.wf.dao.dto.WfItemDetailInfo;
import jp.co.tokyo_gas.cisfw.wf.dao.dto.WfItemDetailInfoEnd;
import jp.co.tokyo_gas.cisfw.wf.dao.dto.WfNodeDetailDef;
import jp.co.tokyo_gas.cisfw.wf.dao.dto.WfWorkflowDetailDef;
import jp.co.tokyo_gas.cisfw.wf.dao.dto.WfWorkflowRoleDef;
import jp.co.tokyo_gas.cisfw.wf.dao.dto.WfWorkteamDetailInfo;
import jp.co.tokyo_gas.cisfw.wf.dao.mapper.WfItemCurrentInfoEndMapper;
import jp.co.tokyo_gas.cisfw.wf.dao.mapper.WfItemCurrentInfoMapper;
import jp.co.tokyo_gas.cisfw.wf.dao.mapper.WfItemDetailInfoEndMapper;
import jp.co.tokyo_gas.cisfw.wf.dao.mapper.WfItemDetailInfoMapper;
import jp.co.tokyo_gas.cisfw.wf.dao.mapper.WfWorkflowDefMapper;
import jp.co.tokyo_gas.cisfw.wf.dao.mapper.WfWorkflowDetailDefMapper;
import jp.co.tokyo_gas.cisfw.wf.dao.mapper.extend.WfItemHistoryInfoSearchEndMapperEx;
import jp.co.tokyo_gas.cisfw.wf.dao.mapper.extend.WfItemHistoryInfoSearchMapperEx;
import jp.co.tokyo_gas.cisfw.wf.dao.mapper.extend.WfItemNodeRoleExInfoEndMapperEx;
import jp.co.tokyo_gas.cisfw.wf.dao.mapper.extend.WfItemNodeRoleExInfoMapperEx;
import jp.co.tokyo_gas.cisfw.wf.dao.mapper.extend.WfItemSearchInfoEndMapperEx;
import jp.co.tokyo_gas.cisfw.wf.dao.mapper.extend.WfItemSearchInfoMapperEx;
import jp.co.tokyo_gas.cisfw.wf.dao.mapper.extend.WfNodeDetailDefMapperEx;
import jp.co.tokyo_gas.cisfw.wf.dao.mapper.extend.WfWorkflowRoleDefMapperEx;
import jp.co.tokyo_gas.cisfw.wf.dao.mapper.extend.WfWorkteamDetailInfoMapperEx;
import jp.co.tokyo_gas.cisfw.wf.dto.CfwAuthInfo;
import jp.co.tokyo_gas.cisfw.wf.dto.CfwDetailInfoCondition;
import jp.co.tokyo_gas.cisfw.wf.dto.CfwSearchCondition;
import jp.co.tokyo_gas.cisfw.wf.dto.CfwWfItemHistoryInfoSearch;
import jp.co.tokyo_gas.cisfw.wf.dto.CfwWfItemSearchInfo;
import jp.co.tokyo_gas.cisfw.wf.dto.CfwWorkflowDef;
import jp.co.tokyo_gas.cisfw.wf.dto.CfwWorkflowHistoryInfo;
import jp.co.tokyo_gas.cisfw.wf.dto.CfwWorkflowInfo;
import jp.co.tokyo_gas.cisfw.wf.dto.CfwWorkflowNodeAuthDef;
import jp.co.tokyo_gas.cisfw.wf.dto.CfwWorkflowNodeDef;
import jp.co.tokyo_gas.cisfw.wf.dto.CfwWorkflowNodeInfo;
import jp.co.tokyo_gas.cisfw.wf.dto.CfwWorkflowSearchCondition;
import jp.co.tokyo_gas.cisfw.wf.dto.CfwWorkflowSort;
import jp.co.tokyo_gas.cisfw.wf.dto.CfwWorkflowUserInfo;
import jp.co.tokyo_gas.cisfw.wf.exception.CfwWorkflowApplicationException;
import jp.co.tokyo_gas.cisfw.wf.util.CfwWfAIOnToCiriusConverter;
import jp.co.tokyo_gas.cisfw.wf.util.CfwWfCiriusToAIOnConverter;
import jp.co.tokyo_gas.cisfw.wf.util.CfwWfCommonUtility;
import jp.co.tokyo_gas.cisfw.wf.util.CfwWfTypeConverter;
import jp.co.tokyo_gas.cisfw.wf.util.CfwWorkflowChecker;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.math.NumberUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

/**
 * ワークフローの一覧を取得する機能のクラスです。<br>
 *
 * @author A.Funakoshi (TDC)
 * @version 1.0.0
 */
@RequestScoped
public class CfwWorkflowServiceImplWorkflowList {

	/** 標準ワークフロー */
	@Inject
	protected WorkflowService workflowService;

	/** 共通的に使用する機能のクラス */
	@Inject
	private CfwWorkflowServiceImplUtility workflowServiceImplUtility;

	/** ワークフロー機能の内部共通権限チェックのクラス */
	@Inject
	private CfwWorkflowServiceImplAuthCheckUtility workflowServiceImplAuthCheckUtility;

	/** ワークフロー詳細定義テーブル */
	@Inject
	@FwSQLMapper
	private WfWorkflowDetailDefMapper workflowDetailDefMapper;

	/** ワークフローノード詳細定義テーブル */
	@Inject
	@FwSQLMapper
	private WfNodeDetailDefMapperEx nodeDetailDefMapperEx;

	/** ノード担当グループ情報現行用テーブル */
	@Inject
	@FwSQLMapper
	private WfItemNodeRoleExInfoMapperEx itemNodeRoleInfoMapperEx;

	/** ノード担当グループ情報完了用テーブル */
	@Inject
	@FwSQLMapper
	private WfItemNodeRoleExInfoEndMapperEx itemNodeRoleInfoEndMapperEx;

	/** ワークフロー詳細情報現行用 */
	@Inject
	@FwSQLMapper
	private WfItemDetailInfoMapper itemDetailInfoMapper;

	/** ワークフロー詳細情報完了用 */
	@Inject
	@FwSQLMapper
	private WfItemDetailInfoEndMapper itemDetailInfoEndMapper;

	/** ワークフロー役割定義テーブル */
	@Inject
	@FwSQLMapper
	private WfWorkflowRoleDefMapperEx workflowRoleDefMapperEx;

	/** ワークフロー現在情報現行用テーブル */
	@Inject
	@FwSQLMapper
	private WfItemCurrentInfoMapper itemCurrentInfoMapper;

	/** ワークフロー現在情報完了用テーブル */
	@Inject
	@FwSQLMapper
	private WfItemCurrentInfoEndMapper itemCurrentInfoEndMapper;

	/** 作業チーム詳細情報テーブル */
	@Inject
	@FwSQLMapper
	private WfWorkteamDetailInfoMapperEx workteamDetailInfoMapperEx;

	/** ワークフロー情報一覧取得（現行用） */
	@Inject
	@FwSQLMapper
	private WfItemSearchInfoMapperEx itemSearchInfoMapperEx;

	/** ワークフロー情報一覧取得（完了用） */
	@Inject
	@FwSQLMapper
	private WfItemSearchInfoEndMapperEx itemSearchInfoEndMapperEx;

	/** ワークフロー定義テーブル */
	@Inject
	@FwSQLMapper
	private WfWorkflowDefMapper workflowDefMapper;

	/** ワークフロー履歴一覧（現行用） */
	@Inject
	@FwSQLMapper
	WfItemHistoryInfoSearchMapperEx wfItemwHistoryInfoSearchMapperEx;

	/** ワークフロー履歴一覧（完了用） */
	@Inject
	@FwSQLMapper
	WfItemHistoryInfoSearchEndMapperEx wfItemwHistoryInfoSearchEndMapperEx;

	/**
	 * ワークフロー定義情報を取得します。
	 *
	 * @param workflowLogicId ワークフロー論理ID
	 * @param setFlag セット取得フラグ
	 * @param user 申請者ユーザー情報
	 * @return ワークフロー定義
	 * @throws CfwWorkflowApplicationException ワークフローアプリケーション例外をスローします。
	 */
	protected CfwWorkflowDef getWorkflowDef(String workflowLogicId, boolean setFlag,
		CfwWorkflowUserInfo user) throws CfwWorkflowApplicationException {

		// ワークフロー論理ID必須チェック
		if (CfwStringValidator.isEmpty(workflowLogicId)) {
			throw new CfwWorkflowApplicationException(CfwWorkflowErrorcode.CFWWFIP001.getWfCode(),
				"ワークフロー論理ID");
		}

		// 権限情報必須チェック
		CfwWorkflowChecker.checkUserInfo(user);

		// 標準ワークフローからワークフロー定義の取得
		WfWorkflowDef workflowDef = null;

		try {

			workflowDef = this.workflowService.findWorkflowDefinition(workflowLogicId, setFlag,
				"dummy", "0");

		} catch (WorkflowApplicationException e) {

			throw new CfwWorkflowApplicationException(e.getCause(), e.getErrorCode(),
				e.getErrorParams());
		}

		WfWorkflowDetailDef workflowDetailDef = new WfWorkflowDetailDef();
		workflowDetailDef.setWorkflowDefId(workflowDef.getWorkflowDefId());

		// ワークフロー詳細定義DTOの取得
		workflowDetailDef = this.workflowDetailDefMapper.find(workflowDetailDef);

		List<WfNodeDetailDef> nodeDetailDefList = null;

		// セット取得フラグがTrueの場合
		if (setFlag) {

			// ノード定義IDのリストを作成
			List<String> nodeDefIdList = new ArrayList<String>();

			for (WfNodeDef nodeDef : workflowDef.getNodeDefList()) {

				nodeDefIdList.add(nodeDef.getNodeDefId());
			}

			// ワークフローノード詳細定義DTOのリストを取得
			nodeDetailDefList = this.nodeDetailDefMapperEx.findByNodeDefIdList(nodeDefIdList);
		}

		// ワークフロー定義の変換
		CfwWorkflowDef wfDef = this.convertWfWorkflowDefToCfwWorkflowDef(workflowDef,
			workflowDetailDef, nodeDetailDefList);

		// ワークフロー役割定義の取得
		List<WfWorkflowRoleDef> workflowRoleDefList = this
			.getWorkflowRoleDefList(wfDef.getWorkflowDefId(), user);
		if (workflowRoleDefList.isEmpty()) {
			throw new CfwWorkflowApplicationException(CfwWorkflowErrorcode.CFWWFNF001.getWfCode(),
				"ワークフロー役割定義");
		}

		if (wfDef.getNodeDefList() != null) {

			String draftUserAuthKey = workflowRoleDefList.get(0).getAuthKey();
			String draftUserAuthValue = workflowRoleDefList.get(0).getAuthValue();

			// 該当するノードに実行権限を付加する
			for (CfwWorkflowNodeDef workflowNodeDef : wfDef.getNodeDefList()) {

				for (WfWorkflowRoleDef workflowRoleDef : workflowRoleDefList) {

					if (workflowNodeDef.getNodeName().equals(workflowRoleDef.getRoleName())) {

						CfwWorkflowNodeAuthDef workflowNodeAuthDef = new CfwWorkflowNodeAuthDef();
						workflowNodeAuthDef.setNodeRoleDefId(null);
						workflowNodeAuthDef.setWorkflowDefId(workflowNodeDef.getWorkflowDefId());
						workflowNodeAuthDef.setNodeDefId(workflowNodeDef.getNodeDefId());

						workflowNodeAuthDef.setAuthKey(workflowRoleDef.getAuthKey());
						workflowNodeAuthDef.setAuthValue(workflowRoleDef.getAuthValue());

						workflowNodeDef.getNodeAuthDefList().add(workflowNodeAuthDef);

						// ロール情報に格納されてある権限情報は申請者と同じ。一時的に格納。
						draftUserAuthKey = workflowRoleDef.getDraftUserAuthKey();
						draftUserAuthValue = workflowRoleDef.getDraftUserAuthValue();
						break;
					}
				}
			}

			// 申請者の権限を作る
			for (CfwWorkflowNodeDef workflowNodeDef : wfDef.getNodeDefList()) {
				if (workflowNodeDef.getNodeType() == CfwWorkflowNodeType.DRAFT) {

					CfwWorkflowNodeAuthDef workflowNodeAuthDef = new CfwWorkflowNodeAuthDef();
					workflowNodeAuthDef.setNodeRoleDefId(null);
					workflowNodeAuthDef.setWorkflowDefId(workflowNodeDef.getWorkflowDefId());
					workflowNodeAuthDef.setNodeDefId(workflowNodeDef.getNodeDefId());
					workflowNodeAuthDef.setAuthKey(draftUserAuthKey);
					workflowNodeAuthDef.setAuthValue(draftUserAuthValue);

					workflowNodeDef.getNodeAuthDefList().add(workflowNodeAuthDef);
					WfWorkflowRoleDef wfWorkflowRoleDef = new WfWorkflowRoleDef();
					wfWorkflowRoleDef.setAuthKey(draftUserAuthKey);
					wfWorkflowRoleDef.setAuthValue(draftUserAuthValue);
					workflowRoleDefList.add(wfWorkflowRoleDef);
					break;
				}
			}
		}

		return wfDef;
	}

	/**
	 * ワークフロー定義情報(ノード担当グループ定義なし)を取得します。
	 * ノード担当グループ定義リストにはnullを設定します。
	 * 
	 * @param workflowLogicId ワークフロー論理ID
	 * @param setFlag セット取得フラグ
	 * @return ワークフロー定義
	 * @throws CfwWorkflowApplicationException ワークフローアプリケーション例外をスローします。
	 */
	protected CfwWorkflowDef getWorkflowDef(String workflowLogicId, boolean setFlag)
		throws CfwWorkflowApplicationException {

		// ワークフロー論理ID必須チェック
		if (CfwStringValidator.isEmpty(workflowLogicId)) {
			throw new CfwWorkflowApplicationException(CfwWorkflowErrorcode.CFWWFIP001.getWfCode(),
				"ワークフロー論理ID");
		}

		// 標準ワークフローからワークフロー定義の取得
		WfWorkflowDef workflowDef = null;

		try {

			workflowDef = this.workflowService.findWorkflowDefinition(workflowLogicId, setFlag,
				"dummy", "0");

		} catch (WorkflowApplicationException e) {

			throw new CfwWorkflowApplicationException(e.getCause(), e.getErrorCode(),
				e.getErrorParams());
		}

		WfWorkflowDetailDef workflowDetailDef = new WfWorkflowDetailDef();
		workflowDetailDef.setWorkflowDefId(workflowDef.getWorkflowDefId());

		// ワークフロー詳細定義DTOの取得
		workflowDetailDef = this.workflowDetailDefMapper.find(workflowDetailDef);

		List<WfNodeDetailDef> nodeDetailDefList = null;

		// セット取得フラグがTrueの場合
		if (setFlag) {

			// ノード定義IDのリストを作成
			List<String> nodeDefIdList = new ArrayList<String>();

			for (WfNodeDef nodeDef : workflowDef.getNodeDefList()) {

				nodeDefIdList.add(nodeDef.getNodeDefId());
			}

			// ワークフローノード詳細定義DTOのリストを取得
			nodeDetailDefList = this.nodeDetailDefMapperEx.findByNodeDefIdList(nodeDefIdList);
		}

		// ワークフロー定義の変換
		CfwWorkflowDef wfDef = this.convertWfWorkflowDefToCfwWorkflowDef(workflowDef,
			workflowDetailDef, nodeDetailDefList);

		if (wfDef.getNodeDefList() != null) {
			// ノード担当グループ定義リストにnullを設定
			for (CfwWorkflowNodeDef workflowNodeDef : wfDef.getNodeDefList()) {
				workflowNodeDef.setNodeAuthDefList(null);
			}
		}
		return wfDef;
	}
	
	/**
	 * ワークフロー役割定義を取得します。<br>
	 *
	 * ユーザーの組織情報と作業チームで検索し、複数ヒットした場合は例外をします。<br>
	 *
	 * @param workflowDefId ワークフロー定義ID
	 * @param userInfo ユーザー情報
	 * @return ワークフロー役割定義のリスト
	 * @throws CfwWorkflowApplicationException ワークフローアプリケーション例外をスローします。
	 */
	protected List<WfWorkflowRoleDef> getWorkflowRoleDefList(String workflowDefId,
		CfwWorkflowUserInfo userInfo) throws CfwWorkflowApplicationException {

		// 必須チェック
		if (CfwStringValidator.isEmpty(workflowDefId)) {

			return null;
		}

		// 必須チェック
		if (userInfo == null) {

			return null;
		}

		// 有効なデータが有るかどうかのフラグ
		boolean flag = false;

		// 最初はユーザーの所属組織で検索する
		List<CfwAuthInfo> list = userInfo.getAuthInfoList();
		String userAuthKey = null;
		String userAuthValue = null;
		// リストのサイズは一つなのでリストの最後のデータを検索キーにします。
		for (CfwAuthInfo info : list) {
			userAuthKey = info.getAuthKey();
			userAuthValue = info.getAuthValue();
		}

		// ワークフロー役割定義を取得
		List<WfWorkflowRoleDef> workflowRoleDefList = this.workflowRoleDefMapperEx
			.findByWorkflowDefIdAndUserAuth(workflowDefId, userAuthKey, userAuthValue);

		if (workflowRoleDefList.size() > 0) {

			flag = true;
		}

		// 次にユーザーが所属している作業チーム情報を取得する
		CfwWorkflowUserInfo copyUserInfo = new CfwWorkflowUserInfo();
		CfwBeanConverter.simpleCopy(userInfo, copyUserInfo);
		WfWorkteamDetailInfo workteamDetailInfo = workflowServiceImplUtility
			.createWfWorkflowDetailInfo(copyUserInfo);

		// ユーザーIDから作業チームの一覧を取得する
		List<WfWorkteamDetailInfo> workteamDetailInfoList = this.workteamDetailInfoMapperEx
			.findByWorkteamDetailInfo(workteamDetailInfo);

		// 作業チームが無かったら所属組織の情報を返す
		if (workteamDetailInfoList.size() != 0) {

			// 作業チームがあったら
			for (WfWorkteamDetailInfo detailInfo : workteamDetailInfoList) {

				// 作業チーム情報を検索条件にする
				userAuthKey = CfwAuthType.WORKTEAM.getValue();
				userAuthValue = detailInfo.getWorkteamId();

				// ワークフロー役割定義を取得
				List<WfWorkflowRoleDef> workflowRoleDefList2 = this.workflowRoleDefMapperEx
					.findByWorkflowDefIdAndUserAuth(workflowDefId, userAuthKey, userAuthValue);

				// 既にデータが有った場合は例外を返す
				if (flag == true) {

					if (workflowRoleDefList2.size() > 0) {

						throw new CfwWorkflowApplicationException(
							CfwWorkflowErrorcode.CFWWFNF001.getWfCode(), "ワークフロー役割定義");
					}
				} else {
					// データが無かった場合はデータをセットする
					workflowRoleDefList = workflowRoleDefList2;
					flag = true;
				}
			}
		}

		return workflowRoleDefList;
	}

	/**
	 * ワークフロー定義とワークフロー詳細定義とノード詳細定義リストをワークフロー定義に変換します。
	 *
	 * @param workflowDef ワークフロー定義
	 * @param workflowDetailDef ワークフロー詳細定義
	 * @param nodeDetailDefList ノード詳細定義リスト
	 * @return ワークフロー定義
	 */
	protected CfwWorkflowDef convertWfWorkflowDefToCfwWorkflowDef(WfWorkflowDef workflowDef,
		WfWorkflowDetailDef workflowDetailDef, List<WfNodeDetailDef> nodeDetailDefList) {

		// 必須チェック
		if (workflowDef == null) {

			return null;
		}

		// 必須チェック
		if (workflowDetailDef == null) {

			return null;
		}

		CfwWorkflowDef wfDef = new CfwWorkflowDef();
		wfDef.setWorkflowDefId(workflowDef.getWorkflowDefId());
		wfDef.setWorkflowLogicId(workflowDef.getWorkflowId());
		wfDef.setWorkflowName(workflowDef.getWorkflowName());
		wfDef.setVersion(workflowDef.getVersion());
		wfDef.setStartDate(workflowDef.getStartDate());
		wfDef.setEndDate(workflowDef.getEndDate());
		wfDef.setBusinessInfo(workflowDef.getBusinessInfo());
		wfDef.setBusinessTypeCode(workflowDetailDef.getBusinessTypeCode());
		wfDef.setBusinessTypeName(workflowDetailDef.getBusinessTypeName());
		wfDef.setDetailInfo1(workflowDetailDef.getDetailInfo1());
		wfDef.setDetailInfo2(workflowDetailDef.getDetailInfo2());
		wfDef.setDetailInfo3(workflowDetailDef.getDetailInfo3());
		wfDef.setDetailInfo4(workflowDetailDef.getDetailInfo4());
		wfDef.setDetailInfo5(workflowDetailDef.getDetailInfo5());
		wfDef.setDetailInfo6(workflowDetailDef.getDetailInfo6());
		wfDef.setDetailInfo7(workflowDetailDef.getDetailInfo7());
		wfDef.setDetailInfo8(workflowDetailDef.getDetailInfo8());
		wfDef.setDetailInfo9(workflowDetailDef.getDetailInfo9());
		wfDef.setDetailInfo10(workflowDetailDef.getDetailInfo10());

		List<CfwWorkflowNodeDef> workflowNodeDefList = this
			.convertWfNodedefListToCfwWorkflowNodeDefList(workflowDef.getNodeDefList(),
				nodeDetailDefList);

		wfDef.setNodeDefList(workflowNodeDefList);

		return wfDef;
	}
	
	/**
	 * ノード定義リストとノード詳細定義リストをワークフローノード定義リストに変換します。
	 *
	 * @param nodeDefList ノード定義リスト
	 * @param nodeDetailDefList ノード詳細定義リスト
	 * @return ワークフローノード定義リスト
	 */
	protected List<CfwWorkflowNodeDef> convertWfNodedefListToCfwWorkflowNodeDefList(
		List<WfNodeDef> nodeDefList, List<WfNodeDetailDef> nodeDetailDefList) {

		// 必須チェック
		if (nodeDefList == null) {

			return null;
		}

		List<CfwWorkflowNodeDef> workflowNodeDefList = new ArrayList<CfwWorkflowNodeDef>();

		for (WfNodeDef nodeDef : nodeDefList) {

			CfwWorkflowNodeDef workflowNodeDef = new CfwWorkflowNodeDef();
			workflowNodeDef.setWorkflowDefId(nodeDef.getWorkflowDefId());
			workflowNodeDef.setNodeDefId(nodeDef.getNodeDefId());
			workflowNodeDef.setNodeLogicId(nodeDef.getNodeId());
			workflowNodeDef.setNodeName(nodeDef.getNodeName());
			workflowNodeDef
				.setNodeType(CfwWorkflowNodeType.getCfwWorkflowNodeType(nodeDef.getNodeType()));
			workflowNodeDef.setRoleName(nodeDef.getRoleName());
			workflowNodeDef.setGroupApproveFlg(
				CfwWfTypeConverter.convertStringToBoolean(nodeDef.getReceptFlg()));
			workflowNodeDef.setSkipEnableFlg(
				CfwWfTypeConverter.convertStringToBoolean(nodeDef.getSkipEnableFlg()));
			workflowNodeDef.setSendbackEnableFlg(
				CfwWfTypeConverter.convertStringToBoolean(nodeDef.getSendbackEnableFlg()));
			workflowNodeDef.setPullbackEnableFlg(
				CfwWfTypeConverter.convertStringToBoolean(nodeDef.getPullbackEnableFlg()));
			workflowNodeDef.setDeleteEnableFlg(
				CfwWfTypeConverter.convertStringToBoolean(nodeDef.getDeleteEnableFlg()));
			workflowNodeDef.setProcessId(nodeDef.getProcessId());

			// 該当するワークフロー詳細定義を取得
			if (nodeDetailDefList != null) {
				for (WfNodeDetailDef nodeDetailDef : nodeDetailDefList) {

					if (nodeDef.getNodeDefId().equals(nodeDetailDef.getNodeDefId())) {

						workflowNodeDef.setSendbackNodeDefId(nodeDetailDef.getSendbackNodeDefId());
						workflowNodeDef
							.setCommonApproveStatus(nodeDetailDef.getCommonApproveStatus());
						workflowNodeDef
							.setCommonSendbackStatus(nodeDetailDef.getCommonSendbackStatus());
						workflowNodeDef
							.setCommonDeleteStatus(nodeDetailDef.getCommonDeleteStatus());
						workflowNodeDef
							.setOriginalApproveStatus(nodeDetailDef.getOriginalApproveStatus());
						workflowNodeDef
							.setOriginalSendbackStatus(nodeDetailDef.getOriginalSendbackStatus());
						workflowNodeDef
							.setOriginalDeleteStatus(nodeDetailDef.getOriginalDeleteStatus());
						workflowNodeDef.setButtonName(nodeDetailDef.getButtonName());
						workflowNodeDef.setLumpWorkFlg(CfwWfTypeConverter
							.convertStringToBoolean(nodeDetailDef.getLumpWorkFlg()));
						workflowNodeDef.setChangeWorkerFlg(CfwWfTypeConverter
							.convertStringToBoolean(nodeDetailDef.getChangeWorkerFlg()));
						workflowNodeDef.setDetailInfo1(nodeDetailDef.getDetailInfo1());
						workflowNodeDef.setDetailInfo2(nodeDetailDef.getDetailInfo2());
						workflowNodeDef.setDetailInfo3(nodeDetailDef.getDetailInfo3());
						workflowNodeDef.setDetailInfo4(nodeDetailDef.getDetailInfo4());
						workflowNodeDef.setDetailInfo5(nodeDetailDef.getDetailInfo5());
						break;
					}
				}
			}

			List<CfwWorkflowNodeAuthDef> nodeAuthDefList = new ArrayList<CfwWorkflowNodeAuthDef>();
			workflowNodeDef.setNodeAuthDefList(nodeAuthDefList);

			workflowNodeDefList.add(workflowNodeDef);
		}

		return workflowNodeDefList;
	}

	/**
	 * ワークフローノード定義一覧を取得します。
	 *
	 * @param workflowDefId ワークフロー論理ID
	 * @param user 申請者ユーザー情報
	 * @return ワークフローノード定義のリスト
	 * @throws CfwWorkflowApplicationException ワークフローアプリケーション例外をスローします。
	 */
	protected List<CfwWorkflowNodeDef> getWorkflowNodeDefList(String workflowDefId,
		CfwWorkflowUserInfo user) throws CfwWorkflowApplicationException {

		// ノード定義一覧照会を実行
		List<WfNodeDef> nodeDefList = null;

		try {

			nodeDefList = this.workflowService.findNodeDefinitionList(workflowDefId, null, null);

		} catch (WorkflowApplicationException e) {

			throw new CfwWorkflowApplicationException(e.getCause(), e.getErrorCode(),
				e.getErrorParams());
		}

		List<String> nodeDefIdList = new ArrayList<String>();

		for (WfNodeDef nodeDef : nodeDefList) {

			nodeDefIdList.add(nodeDef.getNodeDefId());
		}

		// ワークフローノード詳細定義DTOのリストを取得
		List<WfNodeDetailDef> nodeDetailDefList = this.nodeDetailDefMapperEx
			.findByNodeDefIdList(nodeDefIdList);

		// ワークフローノード詳細定義リストの存在確認
		if (CollectionUtils.isEmpty(nodeDetailDefList)) {
			throw new CfwWorkflowApplicationException(CfwWorkflowErrorcode.CFWWFNF001.getWfCode(),
				"ワークフローノード詳細定義");
		}

		// 形式変換
		List<CfwWorkflowNodeDef> workflowNodeDefList = this
			.convertWfNodedefListToCfwWorkflowNodeDefList(nodeDefList, nodeDetailDefList);

		// ワークフロー役割定義の取得
		List<WfWorkflowRoleDef> workflowRoleDefList = this.getWorkflowRoleDefList(workflowDefId,
			user);
		if (workflowRoleDefList.isEmpty()) { // getWorkflowRoleDefListのパラメーターは必ず設定されるためnullの返却はなく、nullのチェックは不要。
			throw new CfwWorkflowApplicationException(CfwWorkflowErrorcode.CFWWFNF001.getWfCode(),
				"ワークフロー役割定義");
		}

		// ワークフロー役割定義をキーを役割名としたMapに変換
		Map<String, WfWorkflowRoleDef> workflowRoleDefMap = new HashMap<String, WfWorkflowRoleDef>();
		for (WfWorkflowRoleDef rolDef : workflowRoleDefList) {
			workflowRoleDefMap.put(rolDef.getRoleName(), rolDef);
		}

		// 該当するノードに実行権限を付加する
		for (CfwWorkflowNodeDef workflowNodeDef : workflowNodeDefList) {

			CfwWorkflowNodeAuthDef workflowNodeAuthDef = new CfwWorkflowNodeAuthDef();
			workflowNodeAuthDef.setNodeRoleDefId(null);
			workflowNodeAuthDef.setWorkflowDefId(workflowNodeDef.getWorkflowDefId());
			workflowNodeAuthDef.setNodeDefId(workflowNodeDef.getNodeDefId());

			WfWorkflowRoleDef workflowRoleDef = workflowRoleDefMap
				.get(workflowNodeDef.getRoleName());

			if (workflowRoleDef != null) {
				workflowNodeAuthDef.setAuthKey(workflowRoleDef.getAuthKey());
				workflowNodeAuthDef.setAuthValue(workflowRoleDef.getAuthValue());

			} else {
				// 役割定義とマッチしないのは申請ノードのみである。申請ノードの場合は、申請ユーザの権限キーおよび権限値を設定する。
				workflowNodeAuthDef.setAuthKey(workflowRoleDefList.get(0).getDraftUserAuthKey());
				workflowNodeAuthDef
					.setAuthValue(workflowRoleDefList.get(0).getDraftUserAuthValue());
			}

			workflowNodeDef.getNodeAuthDefList().add(workflowNodeAuthDef);
		}

		return workflowNodeDefList;
	}

	/**
	 * ワークフロー情報一覧を取得します。
	 *
	 * @param condition 一覧取得検索条件
	 * @param maxCount 最大件数
	 * @return ワークフロー情報のリスト
	 * @throws CfwWorkflowApplicationException ワークフローアプリケーション例外をスローします。
	 */
	protected List<CfwWorkflowInfo> getWorkflowInfoList(CfwWorkflowSearchCondition condition,
		int maxCount) throws CfwWorkflowApplicationException {

		// 必須チェック
		if (condition == null) {
			throw new CfwWorkflowApplicationException(CfwWorkflowErrorcode.CFWWFIP001.getWfCode(),
				"一覧取得検索条件情報");
		}

		// 必須チェック
		if (CfwStringValidator.isEmpty(condition.getTargetUserId())) {
			throw new CfwWorkflowApplicationException(CfwWorkflowErrorcode.CFWWFIP001.getWfCode(),
				"指定ユーザーID");
		}

		// 必須チェック
		if (CfwStringValidator.isEmpty(condition.getTargetUserCmpCode())) {
			throw new CfwWorkflowApplicationException(CfwWorkflowErrorcode.CFWWFIP001.getWfCode(),
				"指定ユーザー企業コード");
		}

		// 必須チェック
		if (CfwStringValidator.isEmpty(condition.getTargetUserOrgCode())) {
			throw new CfwWorkflowApplicationException(CfwWorkflowErrorcode.CFWWFIP001.getWfCode(),
				"指定ユーザー所属コード");
		}

		// 必須チェック
		if (CfwStringValidator.isEmpty(condition.getWorkflowSearchConditionKbn())) {
			throw new CfwWorkflowApplicationException(CfwWorkflowErrorcode.CFWWFIP001.getWfCode(),
				"ワークフロー検索方法区分");
		}

		// 最大件数が0件以下 または 501件以上の場合エラー
		if ((maxCount < 1) || (500 < maxCount)) {
			throw new CfwWorkflowApplicationException(CfwWorkflowErrorcode.CFWWFIP006.getWfCode(),
				"最大件数", Integer.toString(maxCount));
		}

		// ワークフロー検索方法区分が想定外の値の場合エラー
		boolean checkConditionKbn = false;
		for (CfwWfSearchConditionKbn conditionKbn : CfwWfSearchConditionKbn.values()) {
			if (condition.getWorkflowSearchConditionKbn().equals(conditionKbn.getValue())) {
				checkConditionKbn = true;
				break;
			}
		}
		if (!checkConditionKbn) {
			throw new CfwWorkflowApplicationException(CfwWorkflowErrorcode.CFWWFIP007.getWfCode(),
				"ワークフロー検索方法区分");
		}

		// ワークフロー情報一覧照会検索条件を検索条件に変換
		CfwSearchCondition searchCondition = this
			.convertCfwWorkflowSearchConditionToSearchCondition(condition);

		// SQL検索用の最大件数を作成
		int maxCountTmp = maxCount + 1;

		// ワークフロー情報一覧（現行用）を取得
		List<CfwWfItemSearchInfo> itemSearchInfoList = this.itemSearchInfoMapperEx
			.searchItemInfo(searchCondition, maxCountTmp);

		// ワークフロー情報一覧（現行用）取得関数の結果のリストをワークフロー情報のリストに変換
		List<CfwWorkflowInfo> workflowInfoList = this
			.convertWfItemSearchInfoListToCfwWorkflowInfoList(itemSearchInfoList);

		// ワークフロー情報一覧（完了用）の取得件数を作成
		maxCountTmp = maxCountTmp - itemSearchInfoList.size();

		// ワークフロー検索方法区分が"1"（カレントノード検索）以外かつワークフロー情報一覧（完了用）の取得件数が1件以上の場合
		if (!(searchCondition.getWorkflowSearchConditionKbn()
			.equals(CfwWfSearchConditionKbn.CURRENT_NODE_SEARCH.getValue())) && maxCountTmp > 0) {

			// ワークフロー情報一覧（完了用）を取得
			List<CfwWfItemSearchInfo> itemSearchInfoEndList = this.itemSearchInfoEndMapperEx
				.searchItemInfo(searchCondition, maxCountTmp);

			// ワークフロー情報一覧（完了用）取得関数の結果のリストをワークフロー情報のリストに変換
			List<CfwWorkflowInfo> workflowInfoEndList = this
				.convertWfItemSearchInfoListToCfwWorkflowInfoList(itemSearchInfoEndList);

			// ワークフロー情報一覧（現行用）にワークフロー情報一覧（完了用）を追加をする
			workflowInfoList.addAll(workflowInfoEndList);

		}

		return workflowInfoList;
	}

	/**
	 * ワークフロー情報一覧照会検索条件を検索条件に変換します。<br>
	 *
	 * @param condition ワークフロー情報一覧照会検索条件
	 * @return 検索条件
	 * @throws CfwWorkflowApplicationException ワークフローアプリケーション例外をスローします。
	 */
	protected CfwSearchCondition convertCfwWorkflowSearchConditionToSearchCondition(
		CfwWorkflowSearchCondition condition) throws CfwWorkflowApplicationException {

		CfwSearchCondition searchConditionOrg = new CfwSearchCondition();

		searchConditionOrg.setTargetUserId(condition.getTargetUserId());
		searchConditionOrg.setTargetUserCmpCode(condition.getTargetUserCmpCode());
		searchConditionOrg.setTargetUserOrgCode(condition.getTargetUserOrgCode());
		searchConditionOrg.setWorkflowSearchConditionKbn(condition.getWorkflowSearchConditionKbn());
		searchConditionOrg.setSortList(condition.getSortList());
		searchConditionOrg.setDetailInfoConditionList(condition.getDetailInfoConditionList());
		searchConditionOrg.setCommonStatus(condition.getCommonStatus());
		searchConditionOrg.setOriginalStatusList(condition.getOriginalStatusList());

		// 企業コードを作業チーム企業コードと作業チーム所属コードに設定
		CfwWorkflowServiceImplUtility util = new CfwWorkflowServiceImplUtility();
		String[] authType = util.convertAuthType(condition.getTargetUserCmpCode(),
			condition.getTargetUserOrgCode());

		searchConditionOrg.setTeamUserCmpCode(authType[0]);
		searchConditionOrg.setTeamUserOrgCode(authType[1]);

		// ソート条件リストと詳細情報検索条件リストをSQL用に編集
		CfwSearchCondition searchCondition = this
			.convertCfwWorkflowSearchConditionForSearchItemInfo(searchConditionOrg);

		return searchCondition;
	}

	/**
	 * ワークフロー情報一覧取得関数の結果リストをワークフロー情報リストに変換します。<br>
	 *
	 * @param itemSearchInfoList ワークフロー情報一覧取得関数の結果のリスト
	 * @return ワークフロー情報のリスト
	 */
	protected List<CfwWorkflowInfo> convertWfItemSearchInfoListToCfwWorkflowInfoList(
		List<CfwWfItemSearchInfo> itemSearchInfoList) {

		// 必須チェック
		if (itemSearchInfoList == null) {

			return null;
		}

		List<CfwWorkflowInfo> workflowInfoList = new ArrayList<CfwWorkflowInfo>();

		for (CfwWfItemSearchInfo itemSearchInfo : itemSearchInfoList) {

			if (itemSearchInfo != null) {

				CfwWorkflowInfo workflowInfo = this
					.convertWfItemSearchInfoToCfwWorkflowInfo(itemSearchInfo);

				workflowInfoList.add(workflowInfo);
			}
		}

		return workflowInfoList;
	}

	/**
	 * ワークフロー情報一覧取得関数の結果をワークフロー情報に変換します。<br>
	 *
	 * @param itemSearchInfo ワークフロー情報一覧取得関数の結果
	 * @return ワークフロー情報
	 */
	protected CfwWorkflowInfo convertWfItemSearchInfoToCfwWorkflowInfo(
		CfwWfItemSearchInfo itemSearchInfo) {

		if (itemSearchInfo == null) {

			return null;
		}

		CfwWorkflowInfo workflowInfo = new CfwWorkflowInfo();

		workflowInfo.setWorkflowId(itemSearchInfo.getItemId());

		if (NumberUtils.isNumber(itemSearchInfo.getItemStatus())) {

			workflowInfo.setWorkflowStatus(CfwWorkflowStatus
				.getCfwWorkflowStatus(Integer.parseInt(itemSearchInfo.getItemStatus())));
		}

		CfwWorkflowUserInfo draftUser = new CfwWorkflowUserInfo();
		draftUser.setUserId(itemSearchInfo.getDraftUserId());
		draftUser.setUserName(itemSearchInfo.getDraftUserName());
		draftUser.setCmpCode(itemSearchInfo.getDraftUserCmpCode());
		draftUser.setCmpName(itemSearchInfo.getDraftUserCmpName());
		draftUser.setOrgCode(itemSearchInfo.getDraftUserOrgCode());
		draftUser.setOrgName(itemSearchInfo.getDraftUserOrgName());

		workflowInfo.setDraftUser(draftUser);

		workflowInfo.setComment(itemSearchInfo.getComment());
		workflowInfo.setDetailInfo1(itemSearchInfo.getDetailInfo1());
		workflowInfo.setDetailInfo2(itemSearchInfo.getDetailInfo2());
		workflowInfo.setDetailInfo3(itemSearchInfo.getDetailInfo3());
		workflowInfo.setDetailInfo4(itemSearchInfo.getDetailInfo4());
		workflowInfo.setDetailInfo5(itemSearchInfo.getDetailInfo5());
		workflowInfo.setDetailInfo6(itemSearchInfo.getDetailInfo6());
		workflowInfo.setDetailInfo7(itemSearchInfo.getDetailInfo7());
		workflowInfo.setDetailInfo8(itemSearchInfo.getDetailInfo8());
		workflowInfo.setDetailInfo9(itemSearchInfo.getDetailInfo9());
		workflowInfo.setDetailInfo10(itemSearchInfo.getDetailInfo10());
		workflowInfo.setDetailInfo11(itemSearchInfo.getDetailInfo11());
		workflowInfo.setDetailInfo12(itemSearchInfo.getDetailInfo12());
		workflowInfo.setDetailInfo13(itemSearchInfo.getDetailInfo13());
		workflowInfo.setDetailInfo14(itemSearchInfo.getDetailInfo14());
		workflowInfo.setDetailInfo15(itemSearchInfo.getDetailInfo15());
		workflowInfo.setDetailInfo16(itemSearchInfo.getDetailInfo16());
		workflowInfo.setDetailInfo17(itemSearchInfo.getDetailInfo17());
		workflowInfo.setDetailInfo18(itemSearchInfo.getDetailInfo18());
		workflowInfo.setDetailInfo19(itemSearchInfo.getDetailInfo19());
		workflowInfo.setDetailInfo20(itemSearchInfo.getDetailInfo20());
		workflowInfo.setDetailInfo21(itemSearchInfo.getDetailInfo21());
		workflowInfo.setDetailInfo22(itemSearchInfo.getDetailInfo22());
		workflowInfo.setDetailInfo23(itemSearchInfo.getDetailInfo23());
		workflowInfo.setDetailInfo24(itemSearchInfo.getDetailInfo24());
		workflowInfo.setDetailInfo25(itemSearchInfo.getDetailInfo25());
		workflowInfo.setDetailInfo26(itemSearchInfo.getDetailInfo26());
		workflowInfo.setDetailInfo27(itemSearchInfo.getDetailInfo27());
		workflowInfo.setDetailInfo28(itemSearchInfo.getDetailInfo28());
		workflowInfo.setDetailInfo29(itemSearchInfo.getDetailInfo29());
		workflowInfo.setDetailInfo30(itemSearchInfo.getDetailInfo30());
		workflowInfo.setDetailInfo31(itemSearchInfo.getDetailInfo31());
		workflowInfo.setDetailInfo32(itemSearchInfo.getDetailInfo32());
		workflowInfo.setDetailInfo33(itemSearchInfo.getDetailInfo33());
		workflowInfo.setDetailInfo34(itemSearchInfo.getDetailInfo34());
		workflowInfo.setDetailInfo35(itemSearchInfo.getDetailInfo35());
		workflowInfo.setDetailInfo36(itemSearchInfo.getDetailInfo36());
		workflowInfo.setDetailInfo37(itemSearchInfo.getDetailInfo37());
		workflowInfo.setDetailInfo38(itemSearchInfo.getDetailInfo38());
		workflowInfo.setDetailInfo39(itemSearchInfo.getDetailInfo39());
		workflowInfo.setDetailInfo40(itemSearchInfo.getDetailInfo40());
		workflowInfo.setCommonStatus(itemSearchInfo.getCommonStatus());
		workflowInfo.setOriginalStatus(itemSearchInfo.getOriginalStatus());

		if (NumberUtils.isNumber(itemSearchInfo.getProcessValue())) {
			workflowInfo.setProcessValue(Integer.parseInt(itemSearchInfo.getProcessValue()));
		}

		workflowInfo.setLockVersion(itemSearchInfo.getLockVersion());

		CfwWorkflowUserInfo updateUserinfo = new CfwWorkflowUserInfo();
		updateUserinfo.setUserId(itemSearchInfo.getUpdateUserId());
		updateUserinfo.setUserName(itemSearchInfo.getUpdateUserName());
		updateUserinfo.setCmpCode(itemSearchInfo.getUpdateUserCmpCode());
		updateUserinfo.setCmpName(itemSearchInfo.getUpdateUserCmpName());
		updateUserinfo.setOrgCode(itemSearchInfo.getUpdateUserOrgCode());
		updateUserinfo.setOrgName(itemSearchInfo.getUpdateUserOrgName());

		workflowInfo.setLastUpdateUser(updateUserinfo);

		return workflowInfo;
	}

	/**
	 * ワークフロー情報を取得します。<br>
	 * ワークフローIDで指定したワークフロー情報を取得します。<br>
	 *
	 * @param workflowId ワークフローID
	 * @param setFlag セット取得フラグ
	 * @param user 検索者ユーザー情報
	 * @return ワークフロー情報
	 * @throws CfwWorkflowApplicationException ワークフローアプリケーション例外をスローします。
	 */
	protected CfwWorkflowInfo getWorkflowInfo(String workflowId, boolean setFlag,
		CfwWorkflowUserInfo user) throws CfwWorkflowApplicationException {

		// ワークフローIDの必須チェック
		if (CfwStringValidator.isEmpty(workflowId)) {
			throw new CfwWorkflowApplicationException(CfwWorkflowErrorcode.CFWWFIP001.getWfCode(),
				"ワークフローID");
		}

		// ユーザー情報の必須チェック
		CfwWorkflowChecker.checkUserInfo(user);

		// 管理者フラグチェック
		if (!user.isAdminFlg()) {
			boolean authCheck = workflowServiceImplAuthCheckUtility
				.canReadWorkflowNodeByWfItemNodeRoleExList(workflowId, user);

			if (!authCheck) {
				// 権限がなかった場合、業務例外
				throw new CfwWorkflowApplicationException(
					CfwWorkflowErrorcode.CFWWFNF001.getWfCode(), "ユーザー権限情報");
			}
		}

		// ユーザー情報を標準ワークフローのユーザー情報に変換
		WorkflowUserInfo wfUserInfo = CfwWfCiriusToAIOnConverter
			.convertCfwWorkflowUserInfoToWorkflowUserInfo(user);

		// 標準ワークフローの案件情報を取得
		WfItemInfo wfItemInfo = null;

		try {

			wfItemInfo = workflowService.findItemInfo(workflowId, true, wfUserInfo);

		} catch (WorkflowApplicationException e) {

			throw new CfwWorkflowApplicationException(e.getCause(), e.getErrorCode(),
				e.getErrorParams());
		}

		// DBからワークフロー定義を取得
		jp.co.tokyo_gas.cisfw.wf.dao.dto.WfWorkflowDef workflowDef = new jp.co.tokyo_gas.cisfw.wf.dao.dto.WfWorkflowDef();
		workflowDef.setWorkflowDefId(wfItemInfo.getWorkflowDefId());
		workflowDef = workflowDefMapper.find(workflowDef);

		// トランザクションテーブルのDtoを生成
		// ワークフロー詳細情報
		WfItemDetailInfo itemDetailInfo = new WfItemDetailInfo();
		// ワークフロー現在情報
		WfItemCurrentInfo itemCurrentInfo = new WfItemCurrentInfo();

		// 完了用テーブル取得フラグ
		boolean endFlg = false;

		// ワークフロー詳細情報現行用を取得
		itemDetailInfo.setItemId(workflowId);
		itemDetailInfo = itemDetailInfoMapper.find(itemDetailInfo);

		// ワークフロー詳細情報現行用がnullの場合、トランザクションテーブルの情報を完了用から取得
		if (itemDetailInfo == null) {

			// 完了用テーブル取得フラグをtrueに設定
			endFlg = true;

			// ワークフロー詳細情報完了用を取得
			WfItemDetailInfoEnd itemDetailInfoEnd = new WfItemDetailInfoEnd();
			itemDetailInfoEnd.setItemId(workflowId);
			itemDetailInfoEnd = itemDetailInfoEndMapper.find(itemDetailInfoEnd);

			// ワークフロー詳細情報完了用データを現行用に変換
			itemDetailInfo = new WfItemDetailInfo();
			CfwBeanConverter.simpleCopy(itemDetailInfoEnd, itemDetailInfo);

			// ワークフロー現在情報完了用を取得
			WfItemCurrentInfoEnd itemCurrentInfoEnd = new WfItemCurrentInfoEnd();
			itemCurrentInfoEnd.setItemId(workflowId);
			itemCurrentInfoEnd = itemCurrentInfoEndMapper.find(itemCurrentInfoEnd);

			// ワークフロー現在情報完了用データを現行用に変換
			CfwBeanConverter.simpleCopy(itemCurrentInfoEnd, itemCurrentInfo);

			// ワークフロー詳細情報現行用がnullではない場合、トランザクションテーブルの情報を現行用から取得
		} else {

			// ワークフロー現在情報現行用を取得
			itemCurrentInfo.setItemId(workflowId);
			itemCurrentInfo = itemCurrentInfoMapper.find(itemCurrentInfo);
		}

		List<CfwWorkflowNodeInfo> cfwWorkflowNodeInfoList = null;

		// 引数のセット取得フラグがtrueの場合、ワークフローノード情報のリストを取得する。
		if (setFlag) {
		// ワークフローノード情報のリストを取得
			cfwWorkflowNodeInfoList = CfwWfAIOnToCiriusConverter
			.convertWfItemNodeInfoListToCfwWorkflowNodeInfoList(wfItemInfo.getItemNodeInfoList());
		}

		boolean nodeUpdateEnableFlg = false;

		// カレントノード更新可否フラグを判定
		// 完了用テーブル取得フラグがtrueの場合、判定を行わない。
		if (!endFlg) {
			nodeUpdateEnableFlg = checkNodeUpdateEnableFlg(user, wfItemInfo.getItemNodeInfoList());
		}

		// ワークフロー情報を取得
		CfwWorkflowInfo workflowInfo = CfwWfAIOnToCiriusConverter
			.convertWfWorkflowInfoToCfwWorkflowInfo(wfItemInfo, itemDetailInfo,
				cfwWorkflowNodeInfoList, itemCurrentInfo, workflowDef, nodeUpdateEnableFlg);

		return workflowInfo;
	}

	/**
	 * ワークフロー履歴一覧を取得します。<br>
	 * ワークフローIDで指定したワークフロー履歴情報の一覧を「操作日時」の降順で取得します。<br>
	 * 検索条件に合致するワークフロー履歴情報が存在しない場合は、結果件数0件で正常終了します。<br>
	 *
	 * @param workflowId ワークフローID
	 * @param user 検索者ユーザー情報
	 * @return ワークフロー履歴情報のリスト
	 * @throws CfwWorkflowApplicationException ワークフローアプリケーション例外をスローします。
	 */
	protected List<CfwWorkflowHistoryInfo> getWorkflowHistoryInfoList(String workflowId,
		CfwWorkflowUserInfo user) throws CfwWorkflowApplicationException {

		// ワークフローIDの必須チェック
		if (CfwStringValidator.isEmpty(workflowId)) {
			throw new CfwWorkflowApplicationException(CfwWorkflowErrorcode.CFWWFIP001.getWfCode(),
				"ワークフローID");
		}

		// ユーザー情報の必須チェック
		CfwWorkflowChecker.checkUserInfo(user);

		// 参照権限チェック
		if (!user.isAdminFlg()) {
			boolean res = workflowServiceImplAuthCheckUtility
				.canReadWorkflowNodeByWfItemNodeRoleExList(workflowId, user);

			if (!res) {
				// 権限がなかった場合、業務例外
				throw new CfwWorkflowApplicationException(
					CfwWorkflowErrorcode.CFWWFNF001.getWfCode(), "ユーザー権限情報");
			}
		}

		// ワークフロー履歴情報（現行用）を取得する
		List<CfwWfItemHistoryInfoSearch> itemHistoryInfoSearchList = wfItemwHistoryInfoSearchMapperEx
			.searchItemHistoryInfo(workflowId);

		// ワークフロー履歴情報（現行用）が取得できない場合、ワークフロー履歴情報（完了用）を取得する
		if (itemHistoryInfoSearchList.isEmpty()) {
			itemHistoryInfoSearchList = wfItemwHistoryInfoSearchEndMapperEx
				.searchItemHistoryInfoEnd(workflowId);
		}

		// ワークフロー履歴情報のリスト
		List<CfwWorkflowHistoryInfo> resList = new ArrayList<>();

		// ワークフロー履歴一覧をワークフロー履歴情報に変換する
		for (CfwWfItemHistoryInfoSearch itemHistoryInfoSearch : itemHistoryInfoSearchList) {
			CfwWorkflowHistoryInfo workflowHistoriinfo = this
				.convertCfwWfItemHistoryInfoSearchToCfwWorkflowHistoryInfo(itemHistoryInfoSearch);
			resList.add(workflowHistoriinfo);
		}

		return resList;
	}

	/**
	 * ワークフロー情報一覧取得時の検索条件をSQLに編集します。
	 *
	 * @param condition 検索条件
	 * @return 編集後 検索条件
	 * @throws CfwWorkflowApplicationException ワークフローアプリケーション例外をスローします。
	 */
	private CfwSearchCondition convertCfwWorkflowSearchConditionForSearchItemInfo(
		CfwSearchCondition condition) throws CfwWorkflowApplicationException {

		CfwSearchCondition targetCondition = (CfwSearchCondition) CfwWfCommonUtility
			.deepCopy(condition);
		List<CfwDetailInfoCondition> targetDetailInfoConditionList = targetCondition
			.getDetailInfoConditionList();
		List<CfwWorkflowSort> targetSortList = targetCondition.getSortList();

		// 詳細情報検索条件リストの設定
		if (targetDetailInfoConditionList != null) {
			targetCondition.setDetailInfoConditionList(
				convertDetailInfoCondition(targetDetailInfoConditionList));
		}

		// ソート条件リストの設定
		if (targetSortList != null) {
			targetCondition.setSortList(convertSortList(targetSortList));
		}

		return targetCondition;
	}

	/**
	 * ワークフロー情報一覧取得時の詳細情報検索条件リストをSQLパラメータ用に編集します。
	 *
	 * @param detailInfoConditionList 詳細情報検索条件リスト
	 * @return 編集後 検索条件
	 * @throws CfwWorkflowApplicationException ワークフローアプリケーション例外をスローします。
	 */
	private List<CfwDetailInfoCondition> convertDetailInfoCondition(
		List<CfwDetailInfoCondition> detailInfoConditionList)
		throws CfwWorkflowApplicationException {

		// 詳細情報検索条件リストの設定
		for (CfwDetailInfoCondition targetDetailInfoCondition : detailInfoConditionList) {
			String detailInfoNo = targetDetailInfoCondition.getDetailInfoNo();
			if (!CfwStringValidator.isEmpty(detailInfoNo)) {
				String column = CfwDetailInfoNoType.convertDetailInfoNoToColumn(detailInfoNo);
				if (CfwStringValidator.isEmpty(column)) {
					throw new CfwWorkflowApplicationException(
						CfwWorkflowErrorcode.CFWWFIP007.getWfCode(), "詳細情報検索条件リスト:" + detailInfoNo);
				}
				targetDetailInfoCondition.setDetailInfoNo(column);
			}
		}
		return detailInfoConditionList;
	}

	/**
	 * ワークフロー情報一覧取得時のソート条件リストをSQLパラメータ用に編集します。
	 *
	 * @param sortList ソート条件リスト
	 * @return 編集後 検索条件
	 * @throws CfwWorkflowApplicationException ワークフローアプリケーション例外をスローします。
	 */
	private List<CfwWorkflowSort> convertSortList(List<CfwWorkflowSort> sortList)
		throws CfwWorkflowApplicationException {

		// ソート条件リストの設定
		for (CfwWorkflowSort targetSort : sortList) {
			String name = targetSort.getName();
			if (!CfwStringValidator.isEmpty(name)) {
				String column = CfwWorkflowSortType.convertSortKeyToColumn(name);
				if (CfwStringValidator.isEmpty(column)) {
					throw new CfwWorkflowApplicationException(
						CfwWorkflowErrorcode.CFWWFIP007.getWfCode(), "ソート条件リスト:" + name);
				}
				targetSort.setName(column);
			}
		}
		return sortList;
	}

	/**
	 * 指定されたユーザーがカレントノードを更新可能かをチェックします。
	 *
	 * @param user ユーザー情報
	 * @param cfwWorkflowNodeInfoList ワークフローノード情報
	 * @return 指定されたユーザーがカレントノード場合はTRUE
	 */
	private boolean checkNodeUpdateEnableFlg(CfwWorkflowUserInfo user,
		List<WfItemNodeInfo> itemNodeInfoList) throws CfwWorkflowApplicationException {

		for (WfItemNodeInfo itemNodeInfo : itemNodeInfoList) {
			// 該当ノードがカレントノードかを判定
			if (itemNodeInfo.getNodeStatus()
				.equals(CfwWorkflowNodeStatus.WAIT_FOR_APPROVAL.getValue())
				|| itemNodeInfo.getNodeStatus().equals(CfwWorkflowNodeStatus.SENDBACK.getValue())) {

				// 指定されたユーザー情報の権限情報と該当ノードの権限が一致するか判定
				if (user.getAuthInfoList().get(0).getAuthKey().equals(itemNodeInfo.getCmpCode())
					&& user.getAuthInfoList().get(0).getAuthValue()
						.equals(itemNodeInfo.getOrgCode())) {
					return true;

					// ユーザー情報の権限情報と該当ノードのノード担当グループ情報が一致しない場合
				} else {

					// 指定されたユーザー情報が作業チームに登録されているかチェック
					WfWorkteamDetailInfo workteamDetailInfo = this.workflowServiceImplUtility
						.createWfWorkflowDetailInfo(user);

					// ユーザーIDから作業チームの一覧を取得する
					List<WfWorkteamDetailInfo> detailList = this.workteamDetailInfoMapperEx
						.findByWorkteamDetailInfo(workteamDetailInfo);

					for (WfWorkteamDetailInfo wt : detailList) {
						if (CfwAuthType.WORKTEAM.getValue().equals(itemNodeInfo.getCmpCode())
							&& wt.getWorkteamId().equals(itemNodeInfo.getOrgCode())) {
							return true;
						}
					}
					return false;
				}
			}
		}
		return false;
	}

	/**
	 * ワークフロー履歴一覧取得結果DTOをワークフロー履歴情報DTOに変換します。
	 *
	 * @param itemHistoryInfoSearch ワークフロー履歴情報一覧DTO
	 * @return ワークフロー履歴情報DTO
	 */
	private CfwWorkflowHistoryInfo convertCfwWfItemHistoryInfoSearchToCfwWorkflowHistoryInfo(
		CfwWfItemHistoryInfoSearch itemHistoryInfoSearch) {

		CfwWorkflowHistoryInfo workflowHistoryInfo = new CfwWorkflowHistoryInfo();

		// ワークフローID
		workflowHistoryInfo.setWorkflowId(itemHistoryInfoSearch.getWorkflowId());
		// ノード定義ID
		workflowHistoryInfo.setNodeDefId(itemHistoryInfoSearch.getNodeDefId());
		// 操作種別
		workflowHistoryInfo.setOperationType(CfwWorkflowOperationType
			.getCfwWorkflowOperationType(itemHistoryInfoSearch.getOperationType()));
		// 操作日時
		workflowHistoryInfo.setOperationDatetime(itemHistoryInfoSearch.getOperationDatetime());

		// ユーザー情報
		CfwWorkflowUserInfo user = new CfwWorkflowUserInfo();
		// ユーザーID
		user.setUserId(itemHistoryInfoSearch.getUserId());
		// ユーザー名
		user.setUserName(itemHistoryInfoSearch.getUserName());
		// 企業名
		user.setCmpName(itemHistoryInfoSearch.getCmpName());
		// 企業コード
		user.setCmpCode(itemHistoryInfoSearch.getCmpCode());
		// 所属組織名
		user.setOrgName(itemHistoryInfoSearch.getOrgName());
		// 組織コード
		user.setOrgCode(itemHistoryInfoSearch.getOrgCode());
		workflowHistoryInfo.setUserInfo(user);

		// 役割
		workflowHistoryInfo.setRoleName(itemHistoryInfoSearch.getRoleName());
		// コメント
		workflowHistoryInfo.setComment(itemHistoryInfoSearch.getComment());
		// ワークフロー独自ステータス
		workflowHistoryInfo.setOriginalStatus(itemHistoryInfoSearch.getOriginalStatus());

		return workflowHistoryInfo;
	}
}
