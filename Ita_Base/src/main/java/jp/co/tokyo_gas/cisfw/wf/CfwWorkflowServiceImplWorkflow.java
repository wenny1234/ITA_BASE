/*
 * Copyright 2015 TOKYO GAS CO.,LTD. All Rights Reserved.
 */
package jp.co.tokyo_gas.cisfw.wf;

import jp.co.tokyo_gas.aion.tgfw.parts.db.sql.annotation.FwSQLMapper;
import jp.co.tokyo_gas.aion.tgfw.workflow.dto.WfItemInfo;
import jp.co.tokyo_gas.aion.tgfw.workflow.dto.WfItemNodeInfo;
import jp.co.tokyo_gas.aion.tgfw.workflow.dto.WorkflowUserInfo;
import jp.co.tokyo_gas.aion.tgfw.workflow.service.exception.WorkflowApplicationException;
import jp.co.tokyo_gas.aion.tgfw.workflow.service.WorkflowService;
import jp.co.tokyo_gas.cisfw.converter.CfwBeanConverter;
import jp.co.tokyo_gas.cisfw.utils.CfwStringValidator;
import jp.co.tokyo_gas.cisfw.wf.constants.CfwWorkflowErrorcode;
import jp.co.tokyo_gas.cisfw.wf.constants.CfwWorkflowFunctionType;
import jp.co.tokyo_gas.cisfw.wf.constants.CfwWorkflowNodeType;
import jp.co.tokyo_gas.cisfw.wf.constants.CfwWorkflowStatus;
import jp.co.tokyo_gas.cisfw.wf.dao.dto.WfItemCurrentInfo;
import jp.co.tokyo_gas.cisfw.wf.dao.dto.WfItemCurrentInfoEnd;
import jp.co.tokyo_gas.cisfw.wf.dao.dto.WfItemDetailInfo;
import jp.co.tokyo_gas.cisfw.wf.dao.dto.WfItemDetailInfoEnd;
import jp.co.tokyo_gas.cisfw.wf.dao.dto.WfItemHistoryDetailInfo;
import jp.co.tokyo_gas.cisfw.wf.dao.dto.WfItemHistoryDetailInfoEnd;
import jp.co.tokyo_gas.cisfw.wf.dao.dto.WfItemNodeRoleExInfo;
import jp.co.tokyo_gas.cisfw.wf.dao.dto.WfItemNodeRoleExInfoEnd;
import jp.co.tokyo_gas.cisfw.wf.dao.dto.WfNodeDef;
import jp.co.tokyo_gas.cisfw.wf.dao.dto.WfNodeDetailDef;
import jp.co.tokyo_gas.cisfw.wf.dao.mapper.WfItemCurrentInfoEndMapper;
import jp.co.tokyo_gas.cisfw.wf.dao.mapper.WfItemCurrentInfoMapper;
import jp.co.tokyo_gas.cisfw.wf.dao.mapper.WfItemDetailInfoEndMapper;
import jp.co.tokyo_gas.cisfw.wf.dao.mapper.WfItemDetailInfoMapper;
import jp.co.tokyo_gas.cisfw.wf.dao.mapper.WfItemHistoryDetailInfoEndMapper;
import jp.co.tokyo_gas.cisfw.wf.dao.mapper.WfItemHistoryDetailInfoMapper;
import jp.co.tokyo_gas.cisfw.wf.dao.mapper.WfItemNodeRoleExInfoEndMapper;
import jp.co.tokyo_gas.cisfw.wf.dao.mapper.WfItemNodeRoleExInfoMapper;
import jp.co.tokyo_gas.cisfw.wf.dao.mapper.WfNodeDefMapper;
import jp.co.tokyo_gas.cisfw.wf.dao.mapper.WfNodeDetailDefMapper;
import jp.co.tokyo_gas.cisfw.wf.dao.mapper.extend.SequenceMapper;
import jp.co.tokyo_gas.cisfw.wf.dao.mapper.extend.WfItemCurrentInfoMapperEx;
import jp.co.tokyo_gas.cisfw.wf.dao.mapper.extend.WfItemDetailInfoMapperEx;
import jp.co.tokyo_gas.cisfw.wf.dao.mapper.extend.WfItemHistoryDetailInfoMapperEx;
import jp.co.tokyo_gas.cisfw.wf.dao.mapper.extend.WfItemNodeRoleExInfoMapperEx;
import jp.co.tokyo_gas.cisfw.wf.dto.CfwAuthInfo;
import jp.co.tokyo_gas.cisfw.wf.dto.CfwWorkflowInfo;
import jp.co.tokyo_gas.cisfw.wf.dto.CfwWorkflowNodeAuthDef;
import jp.co.tokyo_gas.cisfw.wf.dto.CfwWorkflowNodeDef;
import jp.co.tokyo_gas.cisfw.wf.dto.CfwWorkflowNodeInfo;
import jp.co.tokyo_gas.cisfw.wf.dto.CfwWorkflowUserInfo;
import jp.co.tokyo_gas.cisfw.wf.exception.CfwWorkflowApplicationException;
import jp.co.tokyo_gas.cisfw.wf.util.CfwWfCiriusToAIOnConverter;
import jp.co.tokyo_gas.cisfw.wf.util.CfwWfCiriusToCiriusConverter;
import jp.co.tokyo_gas.cisfw.wf.util.CfwWfTypeConverter;
import jp.co.tokyo_gas.cisfw.wf.util.CfwWorkflowChecker;
import jp.co.tokyo_gas.cisfw.wf.util.CfwWorkflowProcessValueHolder;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

/**
 * ワークフローの基本機能のクラスです。<br>
 *
 * @author A.Funakoshi (TDC)
 * @version 1.0.0
 */
@RequestScoped
public class CfwWorkflowServiceImplWorkflow {

	/** 標準WF インターフェースクラス */
	@Inject
	protected WorkflowService workflowService;

	/** ワークフロー機能のワークフロー一覧関連の実装クラス */
	@Inject
	private CfwWorkflowServiceImplWorkflowList workflowServiceImplWorkflowList;

	/** ワークフロー機能の内部共通機能の実装クラス */
	@Inject
	private CfwWorkflowServiceImplUtility workflowServiceImplUtilty;

	/** ワークフロー機能の内部共通権限チェックのクラス */
	@Inject
	private CfwWorkflowServiceImplAuthCheckUtility workflowServiceImplAuthCheckUtility;

	/** 分岐判定持ち回りクラス */
	@Inject
	CfwWorkflowProcessValueHolder processValueHolder;

	
	/**
	 * CIRIUS用マッパー定義現行用
	 */

	/** ワークフローノード詳細定義テーブル */
	@Inject
	@FwSQLMapper
	private WfNodeDetailDefMapper nodeDetailDefMapper;
	
	/** ワークフロー詳細情報現行用テーブル */
	@Inject
	@FwSQLMapper
	private WfItemDetailInfoMapper itemDetailInfoMapper;

	/** ワークフロー現在情報現行用テーブル */
	@Inject
	@FwSQLMapper
	private WfItemCurrentInfoMapper itemCurrentInfoMapper;

	/** ノード担当グループ情報現行用テーブル */
	@Inject
	@FwSQLMapper
	private WfItemNodeRoleExInfoMapper itemNodeRoleInfoMapper;

	/** ワークフロー履歴詳細情報現行用テーブル */
	@Inject
	@FwSQLMapper
	private WfItemHistoryDetailInfoMapper itemHistoryDetailInfoMapper;

	
	/**
	 * CIRIUS用マッパー定義完了用
	 */

	/** ワークフロー詳細情報完了用テーブル */
	@Inject
	@FwSQLMapper
	private WfItemDetailInfoEndMapper itemDetailInfoEndMapper;

	/** ワークフロー現在情報完了用テーブル */
	@Inject
	@FwSQLMapper
	private WfItemCurrentInfoEndMapper itemCurrentInfoEndMapper;

	/** ノード担当グループ情報完了用テーブル */
	@Inject
	@FwSQLMapper
	private WfItemNodeRoleExInfoEndMapper itemNodeRoleInfoEndMapper;

	/** ワークフロー履歴詳細情報完了用テーブル */
	@Inject
	@FwSQLMapper
	private WfItemHistoryDetailInfoEndMapper itemHistoryDetailInfoEndMapper;

	
	/**
	 * CIRIUS用マッパー定義拡張用
	 */

	/** シーケンス取得DAO */
	@Inject
	@FwSQLMapper
	SequenceMapper sequenceMapper;

	/** ワークフロー詳細情報現行用テーブル拡張用 */
	@Inject
	@FwSQLMapper
	private WfItemDetailInfoMapperEx itemDetailInfoMapperEx;

	/** ワークフロー現在情報現行用テーブル拡張用 */
	@Inject
	@FwSQLMapper
	private WfItemCurrentInfoMapperEx itemCurrentInfoMapperEx;

	/** ノード担当グループ情報現行用テーブル拡張用 */
	@Inject
	@FwSQLMapper
	private WfItemNodeRoleExInfoMapperEx itemNodeRoleInfoMapperEx;

	/** ワークフロー履歴詳細情報テーブル拡張用 */
	@Inject
	@FwSQLMapper
	private WfItemHistoryDetailInfoMapperEx itemHistoryDetailInfoMapperEx;
	
	
	/**
	 * 標準WF用マッパー定義
	 */
	
	/** ノード定義取得DAO */
	@Inject
	@FwSQLMapper
	WfNodeDefMapper nodeDefMapper;

	/**
	 * 申請
	 *
	 * @param workflowInfo ワークフロー情報
	 * @param user 操作者ユーザー情報
	 * @return ワークフローID
	 * @throws CfwWorkflowApplicationException ワークフローアプリケーション例外をスローします。
	 */
	protected String draft(CfwWorkflowInfo workflowInfo, CfwWorkflowUserInfo user) throws CfwWorkflowApplicationException {

		// ワークフロー情報の必須チェック
		CfwWorkflowChecker.requiredCheckForDraft(workflowInfo);

		// 操作者ユーザー情報の必須チェック
		CfwWorkflowChecker.checkUserInfo(user);

		// 申請者の取得
		CfwWorkflowUserInfo draftUser = workflowInfo.getDraftUser();

		// ノード定義のリストを取得する
		List<CfwWorkflowNodeDef> nodeDefList = 
			this.workflowServiceImplWorkflowList.getWorkflowNodeDefList(workflowInfo.getWorkflowDefId(), draftUser);
		
		// ノード定義をMapに変換
		Map<String, CfwWorkflowNodeDef> nodeDefMap = 
			CfwWfTypeConverter.convertCfwWorkflowNodeDefListToNodeDefMap(nodeDefList);

		// ワークフロー情報のワークフローノード情報の必須チェック
		CfwWorkflowChecker.checkWorkflowNodeInfoListForDraft(workflowInfo.getNodeInfoList(), nodeDefMap);

		// ノード定義のリストから申請ノードを検索する
		String draftNodeDefId = null;
		for (CfwWorkflowNodeDef nodeDef : nodeDefList) {

			CfwWorkflowNodeType type = nodeDef.getNodeType();

			// ノード種別が申請かどうか判定
			if (CfwWorkflowNodeType.DRAFT.equals(type)) {

				draftNodeDefId = nodeDef.getNodeDefId();
				break;
			}
		}
		
		//ノード担当グループを取得する
		Map<String, List<CfwWorkflowNodeAuthDef>> nodeRoleMap = 
			CfwWfTypeConverter.convertCfwWorkflowNodeDefListToMap(nodeDefList);
		
		// ワークフローノード情報の操作ユーザーの権限チェック
		for (CfwWorkflowNodeInfo nodeInfo : workflowInfo.getNodeInfoList()) {

			CfwWorkflowUserInfo nodeUserInfo = nodeInfo.getOperationUser();
			
			//ノードにユーザが指定されていたときのみチェックを行う。
			if (nodeUserInfo != null && !CfwStringValidator.isEmpty(nodeUserInfo.getUserId())) {
				authorityCheckForDraft(nodeRoleMap.get(nodeInfo.getNodeDefId()), nodeUserInfo);
			}
		}

		// 操作者が管理者ではない場合は操作者の権限判定を行う
		if (!user.isAdminFlg()) {
			authorityCheckForDraft(nodeRoleMap.get(draftNodeDefId), user);
		}

		// 標準ワークフローの形式に変換
		WfItemInfo tgfwItemInfo =
			CfwWfCiriusToAIOnConverter.convertCfwWorkflowInfoToWorkflowItemInfo(
											workflowInfo,
											user.getUserId(),
											CfwWorkflowFunctionType.DRAFT);

		//パラメータの各ノードの企業コードおよび組織コードはノード担当グループの値で上書きを行う。
		for (WfItemNodeInfo nodeInfo : tgfwItemInfo.getItemNodeInfoList()) {
			CfwWorkflowNodeAuthDef nodeRole = nodeRoleMap.get(nodeInfo.getNodeDefId()).get(0);
			nodeInfo.setCmpCode(nodeRole.getAuthKey());
			nodeInfo.setOrgCode(nodeRole.getAuthValue());
		}

		WorkflowUserInfo tgfwUserInfo = CfwWfCiriusToAIOnConverter.convertCfwWorkflowUserInfoToWorkflowUserInfo(user);

		CfwWorkflowNodeAuthDef nodeRole = nodeRoleMap.get(draftNodeDefId).get(0);
		tgfwUserInfo.setCmpCode(nodeRole.getAuthKey());
		tgfwUserInfo.setOrgCode(nodeRole.getAuthValue());

		//分岐判定値を持ち回りクラスに設定
		processValueHolder.setProcessValue(workflowInfo.getProcessValue());

		// 標準ワークフローの起案を実行
		String workflowId = null;

		try {
			workflowId = this.workflowService.draft(tgfwItemInfo, tgfwUserInfo);

		} catch (WorkflowApplicationException e) {

			throw new CfwWorkflowApplicationException(e.getCause(), e.getErrorCode(), e.getErrorParams());
		}

		// ワークフローIDを設定
		workflowInfo.setWorkflowId(workflowId);

		// ワークフロー詳細情報DTOを作成
		WfItemDetailInfo itemDetailInfo
			= CfwWfCiriusToCiriusConverter.convertCfwWorkflowInfoToWfItemDetailInfo(workflowInfo, user.getUserId());

		// ワークフロー詳細情報DTOの登録
		this.itemDetailInfoMapper.insert(itemDetailInfo);

		// ワークフロー現在情報DTOを作成
		WfItemCurrentInfo itemCurrentInfo
			= CfwWfCiriusToCiriusConverter.convertCfwWorkflowInfoToWfItemCurrentInfo(workflowId,
				workflowInfo.getProcessValue(), workflowInfo.getComment(), nodeDefMap.get(draftNodeDefId), 0, user);

		// ワークフロー現在情報DTOの登録
		this.itemCurrentInfoMapper.insert(itemCurrentInfo);

		List<CfwWorkflowNodeInfo> workflowNodeInfoList = workflowInfo.getNodeInfoList();

		// ノード情報の数だけ繰り返す
		for (CfwWorkflowNodeInfo workflowNodeInfo : workflowNodeInfoList) {

			// ノード定義のリストより同じノード定義を判定する。
			for (CfwWorkflowNodeDef nodeDef : nodeDefList) {

				if (nodeDef.getNodeDefId().equals(workflowNodeInfo.getNodeDefId())) {

					// ワークフローIDの設定
					workflowNodeInfo.setWorkflowId(workflowId);

					// ノード担当グループ定義リストから権限情報リストを作成
					List<CfwAuthInfo> nodeAuthInfoList = CfwWfCiriusToCiriusConverter
						.convertCfwWorkflowNodeAuthDefListToCfwAuthInfoList(
							nodeDef.getNodeAuthDefList());
					
					// ノード担当グループ権限情報の登録
					this.workflowServiceImplUtilty.registerWorkflowNodeAuthGroupInfo(
							workflowId, workflowNodeInfo.getNodeDefId(), nodeAuthInfoList, user);

				}
			}

		}
		
		// 最新操作日時のワークフロー履歴詳細情報を取得
		WfItemHistoryDetailInfo historyDetailInfo = 
			this.workflowServiceImplUtilty.createWorkflowHistoryDetailInfo(workflowInfo, user, itemCurrentInfo);
		
		// ワークフロー履歴詳細情報DTOをワークフロー履歴詳細情報TBLへ登録
		itemHistoryDetailInfoMapper.insert(historyDetailInfo);

		return workflowId;
	}

	/**
	 * 承認
	 *
	 * @param workflowInfo ワークフロー情報
	 * @param targetNodeDefId 操作ノード定義ID
	 * @param user 操作者ユーザー情報
	 * @return true:成功 false:失敗
	 * @throws CfwWorkflowApplicationException ワークフローアプリケーション例外をスローします。
	 */
	protected boolean approve(CfwWorkflowInfo workflowInfo, String targetNodeDefId, CfwWorkflowUserInfo user)
		throws CfwWorkflowApplicationException {

		// ワークフロー情報、操作ノード定義IDの必須チェック
		CfwWorkflowChecker.requiredCheckForApprove(workflowInfo, targetNodeDefId);

		// 操作者ユーザー情報の必須チェック
		CfwWorkflowChecker.checkUserInfo(user);

		CfwWorkflowNodeInfo workflowNodeInfo = null;

		// 該当のノード情報を取得する
		for (CfwWorkflowNodeInfo tempNodeInfo : workflowInfo.getNodeInfoList()) {

			// ノード定義IDの比較
			if (targetNodeDefId.equals(tempNodeInfo.getNodeDefId())) {

				workflowNodeInfo = tempNodeInfo;
				break;
			}
		}

		// 対象のノード情報がなかった場合
		if (workflowNodeInfo == null) {
			throw new CfwWorkflowApplicationException(CfwWorkflowErrorcode.CFWWFNF001.getWfCode(), "ノード情報");
		}

		// ノード担当グループ情報の取得
		List<WfItemNodeRoleExInfo> nodeRoleList = getWfItemNodeRoleExInfoList(workflowNodeInfo.getWorkflowId());
		
		//　Map形式に変換
		Map<String ,List<WfItemNodeRoleExInfo>> nodeRoleMap = 
				CfwWfTypeConverter.convertWfItemNodeRoleExInfoLisToMap(nodeRoleList);

		// 承認者の取得
		CfwWorkflowUserInfo approveUserInfo = workflowNodeInfo.getOperationUser();

		// 承認者の権限チェック
		authorityCheck(nodeRoleMap.get(targetNodeDefId), approveUserInfo);

		// 管理者ではない場合は権限判定を行う
		if (!user.isAdminFlg()) {
			authorityCheck(nodeRoleMap.get(targetNodeDefId), user);
		}

		// カレントノードの情報のみを設定する
		workflowInfo.getNodeInfoList().clear();
		workflowInfo.getNodeInfoList().add(workflowNodeInfo);

		// 標準ワークフローの形式に変換
		WfItemInfo workflowItemInfo
			= CfwWfCiriusToAIOnConverter.convertCfwWorkflowInfoToWorkflowItemInfo(
											workflowInfo,
											user.getUserId(),
											CfwWorkflowFunctionType.APPROVE);

		// パラメータの企業コードおよび組織コードはノード担当グループの値で上書きを行う。
		for (WfItemNodeInfo nodeInfo : workflowItemInfo.getItemNodeInfoList()) {
			WfItemNodeRoleExInfo nodeRole = nodeRoleMap.get(nodeInfo.getNodeDefId()).get(0);
			nodeInfo.setCmpCode(nodeRole.getAuthKey());
			nodeInfo.setOrgCode(nodeRole.getAuthValue());
		}

		WorkflowUserInfo workflowUserInfo
			= CfwWfCiriusToAIOnConverter.convertCfwWorkflowUserInfoToWorkflowUserInfoAndAuth(
				user, nodeRoleMap.get(targetNodeDefId).get(0));

		// 分岐判定値を持ち回りクラスに設定
		processValueHolder.setProcessValue(workflowInfo.getProcessValue());

		// 標準ワークフローの承認を実行
		boolean result = false;
		try {

			result = this.workflowService.approveOrRedraft(workflowItemInfo, targetNodeDefId, workflowUserInfo);

		} catch (WorkflowApplicationException e) {

			throw new CfwWorkflowApplicationException(e.getCause(), e.getErrorCode(), e.getErrorParams());
		}

		// 失敗した場合(到達不能)
		if (!result) {
			throw new CfwWorkflowApplicationException(CfwWorkflowErrorcode.CFWWFCX002.getWfCode());
		}

		String userId = user.getUserId();

		// ワークフロー詳細情報DTOを作成
		WfItemDetailInfo cfwWfDetailInfo = getCfwWfDetailInfo(workflowInfo);
		WfItemDetailInfo itemDetailInfo =
			CfwWfCiriusToCiriusConverter.convertCfwWorkflowInfoToWfItemDetailInfo(cfwWfDetailInfo, workflowInfo, userId);
		
		// ノード定義を取得する
		WfNodeDetailDef param = new WfNodeDetailDef();
		param.setNodeDefId(targetNodeDefId);
		WfNodeDetailDef detailDef = nodeDetailDefMapper.find(param);

		// ワークフロー現在情報DTOを作成
		WfItemCurrentInfo itemCurrentInfo = 
				CfwWfCiriusToCiriusConverter.createCurrentInfo(
						workflowInfo.getWorkflowId(), workflowInfo.getProcessValue(), 
						detailDef, 0, user, workflowInfo.getComment());
		
		// 最新操作日時のワークフロー履歴詳細情報を取得
		WfItemHistoryDetailInfo historyDetailInfo = 
			this.workflowServiceImplUtilty.createWorkflowHistoryDetailInfo(workflowInfo, user, itemCurrentInfo);
		
		// ワークフロー履歴詳細情報DTOをワークフロー履歴詳細情報TBLへ登録
		itemHistoryDetailInfoMapper.insert(historyDetailInfo);

		// 最新の情報を取得する
		CfwWorkflowInfo newWorkflowInfo
			= this.workflowServiceImplWorkflowList.getWorkflowInfo(workflowInfo.getWorkflowId(), true, user);

		// 承認後ステータスを確認する
		// 完了だった場合 完了テーブルに登録、現在テーブルから削除
		if (CfwWorkflowStatus.COMPLETE.equals(newWorkflowInfo.getWorkflowStatus())) { 

			// ワークフロー詳細情報完了テーブルへの登録
			WfItemDetailInfoEnd itemDetailInfoEnd
					= CfwWfCiriusToCiriusConverter.convertWfItemDetailInfoToWfItemDetailInfoEnd(itemDetailInfo);
			this.itemDetailInfoEndMapper.insert(itemDetailInfoEnd);

			//  現行用テーブルからの削除
			this.itemDetailInfoMapper.delete(itemDetailInfo);

			// ワークフロー現在情報完了テーブルへの登録および現行用テーブルからの削除
			WfItemCurrentInfo currentInfo = new WfItemCurrentInfo();
			currentInfo.setItemId(workflowInfo.getWorkflowId());
			WfItemCurrentInfo wfItemCurrentInfo = this.itemCurrentInfoMapper.find(currentInfo);
			WfItemCurrentInfoEnd itemCurrentInfoEnd = CfwWfCiriusToCiriusConverter
				.convertWfItemCurrentInfoToWfItemCurrentInfoEnd(itemCurrentInfo, wfItemCurrentInfo);
			this.itemCurrentInfoEndMapper.insert(itemCurrentInfoEnd);
			this.itemCurrentInfoMapper.delete(itemCurrentInfo);

			// ノード担当グループ情報完了用の登録
			for (WfItemNodeRoleExInfo itemNodeRoleInfo : nodeRoleList) {

				WfItemNodeRoleExInfoEnd itemNodeRoleInfoEnd = CfwWfCiriusToCiriusConverter
					.convertWfItemNodeRoleInfoToWfItemNodeRoleInfoEnd(itemNodeRoleInfo);

				this.itemNodeRoleInfoEndMapper.insert(itemNodeRoleInfoEnd);
			}
			//ノード担当グループ情報現行用の削除
			this.itemNodeRoleInfoMapperEx.deleteByWorkflowId(workflowInfo.getWorkflowId());

			// ワークフロー履歴詳細情報のリストを取得
			List<WfItemHistoryDetailInfo> historyDetailInfoList = 
					this.workflowServiceImplUtilty.createWorkflowHistoryDetailInfoList(workflowInfo.getWorkflowId());

			// ワークフロー履歴詳細情報DTOをワークフロー履歴詳細情報DTO（完了）に変換
			for (WfItemHistoryDetailInfo wfItemHistoryDetailInfo : historyDetailInfoList) {
				WfItemHistoryDetailInfoEnd wfItemHistoryDetailInfoEnd = new WfItemHistoryDetailInfoEnd();
				CfwBeanConverter.simpleCopy(wfItemHistoryDetailInfo, wfItemHistoryDetailInfoEnd);
				// ワークフロー履歴詳細情報DTO（完了）をワークフロー履歴詳細情報完了用TBLへ登録
				itemHistoryDetailInfoEndMapper.insert(wfItemHistoryDetailInfoEnd);
			}
			
			// ワークフロー履歴詳細情報現行用TBLから削除
			itemHistoryDetailInfoMapperEx.deleteByWorkflowId(workflowInfo.getWorkflowId());

		} else {
			// 完了ではない場合はデータの更新

			// ワークフロー詳細情報の更新
			this.itemDetailInfoMapperEx.extendOfUpdate(itemDetailInfo);

			// ワークフロー現在情報の更新
			this.itemCurrentInfoMapperEx.extendOfUpdate(itemCurrentInfo);

		}

		return true;
	}

	/**
	 * 差戻し。<br>
	 * ワークフローを差し戻します。<br>
	 *
	 * @param workflowInfo ワークフロー情報
	 * @param targetNodeDefId 操作ノード定義ID
	 * @param user 操作者ユーザー情報
	 * @return true：成功
	 * @throws CfwWorkflowApplicationException ワークフローアプリケーション例外をスローします。
	 */
	protected boolean sendback(CfwWorkflowInfo workflowInfo, String targetNodeDefId, CfwWorkflowUserInfo user)
		throws CfwWorkflowApplicationException {

		// ワークフロー情報、操作ノード定義IDの必須チェック
		CfwWorkflowChecker.requiredCheckForSendback(workflowInfo, targetNodeDefId);

		// 操作者ユーザー情報の必須チェック
		CfwWorkflowChecker.checkUserInfo(user);

		// パラメーターの操作ノード定義IDから差戻し先ノード定義IDを取得する
		WfNodeDetailDef param = new WfNodeDetailDef();
		param.setNodeDefId(targetNodeDefId);
		WfNodeDetailDef detailDef = this.nodeDetailDefMapper.find(param);

		// 対象のノード情報がなかった場合
		if (detailDef == null) {
			throw new CfwWorkflowApplicationException(CfwWorkflowErrorcode.CFWWFNF001.getWfCode(), "ワークフローノード詳細定義");
		}
		
		String sendbackNodeDefId = detailDef.getSendbackNodeDefId();
		
		// ノード担当グループ情報の取得
		List<WfItemNodeRoleExInfo> nodeRoleList = getWfItemNodeRoleExInfoList(workflowInfo.getWorkflowId());
		
		//　Map形式に変換
		Map<String ,List<WfItemNodeRoleExInfo>> nodeRoleMap
			= CfwWfTypeConverter.convertWfItemNodeRoleExInfoLisToMap(nodeRoleList);

		// ワークフロー情報から引数の操作ノード定義IDで指定されたノードのノード担当グループ情報のリストを取得
		// ワークフローノード情報
		CfwWorkflowNodeInfo workflowNodeInfo = null;
		
		// 該当のノード情報を取得する
		for (CfwWorkflowNodeInfo tempNodeInfo : workflowInfo.getNodeInfoList()) {

			// ノード定義IDの比較
			if (targetNodeDefId.equals(tempNodeInfo.getNodeDefId())) {

				workflowNodeInfo = tempNodeInfo;
				break;
			}
		}

		// 対象のノード情報がなかった場合
		if (workflowNodeInfo == null) {
			throw new CfwWorkflowApplicationException(CfwWorkflowErrorcode.CFWWFNF001.getWfCode(), "対象のノード情報");
		}

		// 実行権限のチェック
		if (!user.isAdminFlg()) {
			authorityCheck(nodeRoleMap.get(workflowNodeInfo.getNodeDefId()), user);
		}

		// ユーザー情報を標準ワークフローのユーザー情報に変換
		WorkflowUserInfo workflowUserInfo = CfwWfCiriusToAIOnConverter.convertCfwWorkflowUserInfoToWorkflowUserInfoAndAuth(
			user,nodeRoleMap.get(workflowNodeInfo.getNodeDefId()).get(0));

		// 差戻しを実行
		boolean result = false;
		try {

			result = workflowService.sendback(
					workflowInfo.getWorkflowId(),
					targetNodeDefId,
					sendbackNodeDefId,
					workflowUserInfo,
					StringUtils.EMPTY, 
					workflowInfo.getLockVersion());

		} catch (WorkflowApplicationException e) {

			throw new CfwWorkflowApplicationException(e.getCause(), e.getErrorCode(), e.getErrorParams());
		}

		// 失敗した場合(到達不能)
		if (!result) {
			throw new CfwWorkflowApplicationException(CfwWorkflowErrorcode.CFWWFCX002.getWfCode());
		}

		// ワークフロー拡張部分を更新
		this.updateWorkflowExpansionAndAuthInfo(workflowInfo, targetNodeDefId, 1, user);

		return true;
	}

	/**
	 * 担当者割振。<br>
	 * グループ承認が指定されているノードに対し、グループに所属するユーザーを割り当てます。<br>
	 *
	 * @param workflowId ワークフローID
	 * @param targetNodeInfo 操作ノード情報
	 * @param targetUser 割振り先ユーザー情報
	 * @param user 操作者ユーザー情報
	 * @return true：成功
	 * @throws CfwWorkflowApplicationException ワークフローアプリケーション例外をスローします。
	 */
	protected boolean assign(String workflowId, CfwWorkflowNodeInfo targetNodeInfo, CfwWorkflowUserInfo targetUser,
		CfwWorkflowUserInfo user) throws CfwWorkflowApplicationException {

		// ワークフローIDのチェック(必須チェック)
		if (CfwStringValidator.isEmpty(workflowId)) {
			throw new CfwWorkflowApplicationException(CfwWorkflowErrorcode.CFWWFIP001.getWfCode(), "ワークフローID");
		}

		// 操作ノード情報のチェック
		CfwWorkflowChecker.requiredCheckForAssign(targetNodeInfo);

		// 割振り先ユーザー情報(必須チェック)
		CfwWorkflowChecker.checkUserInfo(targetUser);

		// 操作者ユーザー情報(必須チェック)
		CfwWorkflowChecker.checkUserInfo(user);

		// ノード担当グループ情報の取得
		List<WfItemNodeRoleExInfo> nodeRoleList = getWfItemNodeRoleExInfoList(workflowId);
		
		//　Map形式に変換
		Map<String ,List<WfItemNodeRoleExInfo>> nodeRoleMap =
			CfwWfTypeConverter.convertWfItemNodeRoleExInfoLisToMap(nodeRoleList);

		// 割振り先ユーザーの権限チェック
		boolean result = workflowServiceImplAuthCheckUtility.canWriteWorkflowNodeByWfItemNodeRoleExInfoList(
				nodeRoleMap.get(targetNodeInfo.getNodeDefId()), targetUser);

		// 権限が無かった場合
		if (!result) {
			throw new CfwWorkflowApplicationException(CfwWorkflowErrorcode.CFWWFAP102.getWfCode());
		}

		// 管理者ではない場合は権限判定を行う
		if (!user.isAdminFlg()) {
			authorityCheck(nodeRoleMap.get(targetNodeInfo.getNodeDefId()), user);
		}

		// ユーザー情報を標準ワークフローのユーザー情報に変換
		WorkflowUserInfo workflowUserInfo =
			CfwWfCiriusToAIOnConverter.convertCfwWorkflowUserInfoToWorkflowUserInfoAndAuth(
				user, nodeRoleMap.get(targetNodeInfo.getNodeDefId()).get(0));

		// 割振先ユーザー情報を標準ワークフローのユーザー情報に変換(但し、組織および権限については、ノード担当グループの情報で上書く）
		WorkflowUserInfo workflowTargetUserInfo =
			CfwWfCiriusToAIOnConverter.convertCfwWorkflowUserInfoToWorkflowUserInfoAndAuth(
				targetUser,nodeRoleMap.get(targetNodeInfo.getNodeDefId()).get(0));

		// 担当者割振を実行
		try {

			result = this.workflowService.assign(
									workflowId, 
									targetNodeInfo.getNodeDefId(), 
									workflowTargetUserInfo, 
									workflowUserInfo, 
									targetNodeInfo.getLockVersion());

		} catch (WorkflowApplicationException e) {

			throw new CfwWorkflowApplicationException(e.getCause(), e.getErrorCode(), e.getErrorParams());
		}

		// 失敗した場合(到達不能)
		if (!result) {
			throw new CfwWorkflowApplicationException(CfwWorkflowErrorcode.CFWWFCX002.getWfCode());
		}

		return result;
	}

	/**
	 * 削除
	 *
	 * @param workflowInfo ワークフロー情報
	 * @param targetNodeDefId 操作ノード定義ID
	 * @param user 操作者ユーザー情報
	 * @return true:成功 false:失敗
	 * @throws CfwWorkflowApplicationException ワークフローアプリケーション例外をスローします。
	 */
	protected boolean delete(CfwWorkflowInfo workflowInfo, String targetNodeDefId, CfwWorkflowUserInfo user)
		throws CfwWorkflowApplicationException {

		// ワークフロー情報、操作ノード定義IDの必須チェック
		CfwWorkflowChecker.requiredCheckForDelete(workflowInfo, targetNodeDefId);

		// 操作者ユーザー情報の必須チェック
		CfwWorkflowChecker.checkUserInfo(user);

		// ノードが削除可能かどうかのチェック
		WfNodeDef nodeDef = getWfNodeDef(targetNodeDefId);
		if (!nodeDef.getDeleteEnableFlg().equals("1")) {
			throw new CfwWorkflowApplicationException(CfwWorkflowErrorcode.CFWWFCX005.getWfCode());
		}

		// ノード担当グループ情報の取得
		List<WfItemNodeRoleExInfo> nodeRoleList = getWfItemNodeRoleExInfoList(workflowInfo.getWorkflowId());
		
		//　Map形式に変換
		Map<String, List<WfItemNodeRoleExInfo>> nodeRoleMap =
			CfwWfTypeConverter.convertWfItemNodeRoleExInfoLisToMap(nodeRoleList);

		// 管理者ではない場合は権限判定を行う
		if (!user.isAdminFlg()) {
			authorityCheck(nodeRoleMap.get(targetNodeDefId), user);
		}

		// 標準ワークフローの形式に変換
		WorkflowUserInfo workflowUserInfo =
			CfwWfCiriusToAIOnConverter.convertCfwWorkflowUserInfoToWorkflowUserInfoAndAuth(
				user,nodeRoleMap.get(targetNodeDefId).get(0));

		// 標準ワークフローの削除を実行
		boolean result = false;
		try {

			result = this.workflowService.delete(
											workflowInfo.getWorkflowId(),
											targetNodeDefId,
											workflowUserInfo,
											StringUtils.EMPTY, 
											workflowInfo.getLockVersion());

		} catch (WorkflowApplicationException e) {

			throw new CfwWorkflowApplicationException(e.getCause(), e.getErrorCode(), e.getErrorParams());
		}

		// 失敗した場合(到達不能)
		if (!result) {

			// 標準ワークフローでエラー
			throw new CfwWorkflowApplicationException(CfwWorkflowErrorcode.CFWWFCX002.getWfCode());
		}

		// ワークフロー詳細情報DTOを作成
		WfItemDetailInfo cfwWfDetailInfo = getCfwWfDetailInfo(workflowInfo);
		WfItemDetailInfo itemDetailInfo =
			CfwWfCiriusToCiriusConverter.convertCfwWorkflowInfoToWfItemDetailInfo(
				cfwWfDetailInfo, workflowInfo, user.getUserId());

		WfItemDetailInfoEnd itemDetailInfoEnd
			= CfwWfCiriusToCiriusConverter.convertWfItemDetailInfoToWfItemDetailInfoEnd(itemDetailInfo);

		// ワークフロー詳細情報DTOの登録
		this.itemDetailInfoEndMapper.insert(itemDetailInfoEnd);
		// ワークフロー詳細情報DTOを削除
		this.itemDetailInfoMapper.delete(itemDetailInfo);

		// ワークフロー現在情報DTOを作成
		WfNodeDetailDef detailDefParam = new WfNodeDetailDef();
		detailDefParam.setNodeDefId(targetNodeDefId);
		WfNodeDetailDef nodeDetailDef = nodeDetailDefMapper.find(detailDefParam);
		
		// ワークフロー現在情報から分岐判定値を取得
		WfItemCurrentInfo currentInfo = new WfItemCurrentInfo();
		currentInfo.setItemId(workflowInfo.getWorkflowId());
		WfItemCurrentInfo wfItemCurrentInfo = this.itemCurrentInfoMapper.find(currentInfo);
		int processValue = Integer.parseInt(wfItemCurrentInfo.getProcessValue());

		WfItemCurrentInfo itemCurrentInfo = CfwWfCiriusToCiriusConverter.createCurrentInfo(workflowInfo.getWorkflowId(),
			processValue, nodeDetailDef, 2, user, workflowInfo.getComment());

		WfItemCurrentInfoEnd itemCurrentInfoEnd
			= CfwWfCiriusToCiriusConverter.convertWfItemCurrentInfoToWfItemCurrentInfoEnd(itemCurrentInfo,wfItemCurrentInfo);

		// ワークフロー現在情報の登録
		this.itemCurrentInfoEndMapper.insert(itemCurrentInfoEnd);

		// ワークフロー現在情報の削除
		this.itemCurrentInfoMapper.delete(itemCurrentInfo);

		// ノード担当グループ情報現行用TBLからノード担当グループ情報完了用TBLへ登録
		for (WfItemNodeRoleExInfo itemNodeRoleInfo : nodeRoleList) {

			WfItemNodeRoleExInfoEnd itemNodeRoleInfoEnd = CfwWfCiriusToCiriusConverter
				.convertWfItemNodeRoleInfoToWfItemNodeRoleInfoEnd(itemNodeRoleInfo);

			this.itemNodeRoleInfoEndMapper.insert(itemNodeRoleInfoEnd);
		}
		this.itemNodeRoleInfoMapperEx.deleteByWorkflowId(workflowInfo.getWorkflowId());
		
		// 最新操作日時のワークフロー履歴詳細情報を取得
		WfItemHistoryDetailInfo historyDetailInfo = 
			this.workflowServiceImplUtilty.createWorkflowHistoryDetailInfo(workflowInfo, user, itemCurrentInfo);
		
		// ワークフロー履歴詳細情報DTOをワークフロー履歴詳細情報DTO（完了）に変換
		WfItemHistoryDetailInfoEnd historyDetailInfoEnd = new WfItemHistoryDetailInfoEnd();
		CfwBeanConverter.simpleCopy(historyDetailInfo, historyDetailInfoEnd);
		
		// 最新操作日時のワークフロー履歴詳細情報をワークフロー履歴詳細情報完了用TBLへ登録
		itemHistoryDetailInfoEndMapper.insert(historyDetailInfoEnd);

		// ワークフロー履歴詳細情報のリストを取得
		List<WfItemHistoryDetailInfo> historyDetailInfoList = 
				this.workflowServiceImplUtilty.createWorkflowHistoryDetailInfoList(workflowInfo.getWorkflowId());

		// ワークフロー履歴詳細情報DTOをワークフロー履歴詳細情報DTO（完了）に変換
		for (WfItemHistoryDetailInfo wfItemHistoryDetailInfo : historyDetailInfoList) {
			WfItemHistoryDetailInfoEnd wfItemHistoryDetailInfoEnd = new WfItemHistoryDetailInfoEnd();
			CfwBeanConverter.simpleCopy(wfItemHistoryDetailInfo, wfItemHistoryDetailInfoEnd);
			// ワークフロー履歴詳細情報DTO（完了）をワークフロー履歴詳細情報完了用TBLへ登録
			itemHistoryDetailInfoEndMapper.insert(wfItemHistoryDetailInfoEnd);
		}
		
		// ワークフロー履歴詳細情報現行用TBLから削除
		itemHistoryDetailInfoMapperEx.deleteByWorkflowId(workflowInfo.getWorkflowId());

		return true;
	}

	/**
	 * ワークフロー拡張情報更新
	 *
	 * @param workflowInfo ワークフロー情報
	 * @param user 更新者ユーザー情報
	 * @return true：成功
	 * @throws CfwWorkflowApplicationException ワークフローアプリケーション例外をスローします。
	 */
	protected boolean updateWorkflowExtension(CfwWorkflowInfo workflowInfo, CfwWorkflowUserInfo user)
		throws CfwWorkflowApplicationException {

		// ワークフロー詳細情報DTOを取得
		WfItemDetailInfo cfwWfDetailInfo = getCfwWfDetailInfo(workflowInfo);
		WfItemDetailInfo itemDetailInfo =
			CfwWfCiriusToCiriusConverter.convertCfwWorkflowInfoToWfItemDetailInfo(
				cfwWfDetailInfo, workflowInfo, user.getUserId());
		
		// ワークフロー詳細情報DTOの更新
		this.itemDetailInfoMapperEx.extendOfUpdate(itemDetailInfo);

		return true;
	}

	/**
	 * ワークフロー拡張情報更新.<br>
	 *　CIRIUSで追加したテーブルの更新を行います。
	 *
	 * @param workflowInfo ワークフロー情報
	 * @param targetNodeDefId 対象ノード定義ID
	 * @param action 操作 0:承認 1:差戻し 2:削除
	 * @param user ユーザ情報
	 * @return 成否
	 * @throws CfwWorkflowApplicationException ワークフローアプリケーション例外をスローします。
	 */
	protected boolean updateWorkflowExpansionAndAuthInfo(
		CfwWorkflowInfo workflowInfo, String targetNodeDefId, int action, CfwWorkflowUserInfo user)
			throws CfwWorkflowApplicationException {

		// ワークフローを更新する
		this.updateWorkflowExtension(workflowInfo, user);

		// ワークフロー現在情報を更新する
		// ノード定義を取得する
		WfNodeDetailDef param = new WfNodeDetailDef();
		param.setNodeDefId(targetNodeDefId);
		WfNodeDetailDef detailDef = nodeDetailDefMapper.find(param);

		// ワークフロー現在情報DTOを作成
		WfItemCurrentInfo itemCurrentInfo
			= CfwWfCiriusToCiriusConverter.createCurrentInfo(workflowInfo.getWorkflowId(),
				workflowInfo.getProcessValue(), detailDef, action, user, workflowInfo.getComment());

		this.itemCurrentInfoMapperEx.updateForBranch(itemCurrentInfo);
		
		// 最新操作日時のワークフロー履歴詳細情報を取得
		WfItemHistoryDetailInfo historyDetailInfo = 
			this.workflowServiceImplUtilty.createWorkflowHistoryDetailInfo(workflowInfo, user, itemCurrentInfo);
		
		// ワークフロー履歴詳細情報DTOをワークフロー履歴詳細情報TBLへ登録
		itemHistoryDetailInfoMapper.insert(historyDetailInfo);

		return true;
	}

	/**
	 * 実行権限判定処理を呼び出します。
	 * 
	 * @param wfItemNodeRoleExInfoList ノード担当グループ情報リスト
	 * @param userInfo ユーザー情報
	 * @throws CfwWorkflowApplicationException ワークフローアプリケーション例外をスローします。
	 */
	private void authorityCheck(List<WfItemNodeRoleExInfo> wfItemNodeRoleExInfoList, CfwWorkflowUserInfo userInfo)
		throws CfwWorkflowApplicationException {
		
		boolean result = false;
		result = this.workflowServiceImplAuthCheckUtility
			.canWriteWorkflowNodeByWfItemNodeRoleExInfoList(wfItemNodeRoleExInfoList, userInfo);
		
		if (!result) {
			throw new CfwWorkflowApplicationException(CfwWorkflowErrorcode.CFWWFAP101.getWfCode());
		}
	}

	/**
	 * 実行権限判定処理を呼び出します。(申請用)
	 * 
	 * @param cfwWorkflowNodeAuthDefList ノード担当グループ定義リスト
	 * @param userInfo ユーザー情報
	 * @throws CfwWorkflowApplicationException ワークフローアプリケーション例外をスローします。
	 */
	private void authorityCheckForDraft(List<CfwWorkflowNodeAuthDef> cfwWorkflowNodeAuthDefList, CfwWorkflowUserInfo userInfo)
		throws CfwWorkflowApplicationException {
		
		boolean result = false;
		
		result = this.workflowServiceImplAuthCheckUtility
			.canWriteWorkflowNodeByWfNodeAuthDefList(cfwWorkflowNodeAuthDefList, userInfo);
		
		// 権限が無かった場合
		if (!result) {
			throw new CfwWorkflowApplicationException(CfwWorkflowErrorcode.CFWWFAP101.getWfCode());
		}
	}
	
	/**
	 * 指定したワークフローIDでワークフロー詳細情報現行用を取得します。
	 *
	 * @param workflowInfo ワークフロー情報
	 * @return ワークフロー詳細情報現行用
	 * @throws CfwWorkflowApplicationException ワークフローアプリケーション例外をスローします。
	 */
	private WfItemDetailInfo getCfwWfDetailInfo(CfwWorkflowInfo workflowInfo) throws CfwWorkflowApplicationException {

		WfItemDetailInfo retInfo = null;
		WfItemDetailInfo itemDetailInfo = new WfItemDetailInfo();
		itemDetailInfo.setItemId(workflowInfo.getWorkflowId());

		retInfo = itemDetailInfoMapper.find(itemDetailInfo);

		if (retInfo == null) {
			throw new CfwWorkflowApplicationException(CfwWorkflowErrorcode.CFWWFNF001.getWfCode(), "ワークフロー詳細情報現行用");
		}

		return retInfo;
	}

	/**
	 * 指定したワークフローIDでノード担当グループ情報現行用を取得します。
	 * 
	 * @param workflowId ワークフローID
	 * @return ノード担当グループ情報リスト
	 * @throws CfwWorkflowApplicationException ワークフローアプリケーション例外をスローします。
	 */
	private List<WfItemNodeRoleExInfo> getWfItemNodeRoleExInfoList(String workflowId) throws CfwWorkflowApplicationException {
		
		List<WfItemNodeRoleExInfo> nodeRoleList = new ArrayList<WfItemNodeRoleExInfo>();
		
		nodeRoleList = this.itemNodeRoleInfoMapperEx.findByWorkflowId(workflowId);
		
		if (CollectionUtils.isEmpty(nodeRoleList)) {
			throw new CfwWorkflowApplicationException(CfwWorkflowErrorcode.CFWWFNF001.getWfCode(), "ノード担当グループ情報");
		}
		
		return nodeRoleList;
	}

	/**
	 * 指定したノード定義IDでノード定義を取得します。
	 * 
	 * @param targetNodeDefId ノード定義ID
	 * @return ノード定義
	 * @throws CfwWorkflowApplicationException ワークフローアプリケーション例外をスローします。
	 */
	private WfNodeDef getWfNodeDef(String nodeDefId) throws CfwWorkflowApplicationException {
		
		WfNodeDef nodeDefParam = new WfNodeDef();
		nodeDefParam.setNodeDefId(nodeDefId);
		
		WfNodeDef nodeDef = null;
		nodeDef = nodeDefMapper.find(nodeDefParam);
		
		if (nodeDef == null) {
			throw new CfwWorkflowApplicationException(CfwWorkflowErrorcode.CFWWFNF001.getWfCode(), "ノード定義");
		}
		
		return nodeDef;
	}
	
}
