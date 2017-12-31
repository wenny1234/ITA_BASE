/*
 * Copyright 2015 TOKYO GAS CO.,LTD. All Rights Reserved.
 */
package jp.co.tokyo_gas.cisfw.wf;

import jp.co.tokyo_gas.aion.tgfw.parts.db.exception.FwSQLAlreadyModifiedException;
import jp.co.tokyo_gas.aion.tgfw.parts.db.sql.annotation.FwSQLMapper;
import jp.co.tokyo_gas.cisfw.utils.CfwDateUtils;
import jp.co.tokyo_gas.cisfw.utils.CfwStringValidator;
import jp.co.tokyo_gas.cisfw.wf.constants.CfwWorkflowErrorcode;
import jp.co.tokyo_gas.cisfw.wf.dao.dto.WfWorkteamDetailInfo;
import jp.co.tokyo_gas.cisfw.wf.dao.dto.WfWorkteamInfo;
import jp.co.tokyo_gas.cisfw.wf.dao.mapper.WfWorkteamDetailInfoMapper;
import jp.co.tokyo_gas.cisfw.wf.dao.mapper.WfWorkteamInfoMapper;
import jp.co.tokyo_gas.cisfw.wf.dao.mapper.extend.WfWorkteamDetailInfoMapperEx;
import jp.co.tokyo_gas.cisfw.wf.dao.mapper.extend.WfWorkteamInfoListMapperEx;
import jp.co.tokyo_gas.cisfw.wf.dao.mapper.extend.WfWorkteamInfoMapperEx;
import jp.co.tokyo_gas.cisfw.wf.dao.mapper.extend.WfWorkteamSearchInfoMapperEx;
import jp.co.tokyo_gas.cisfw.wf.dto.CfwWorkTeamInfo;
import jp.co.tokyo_gas.cisfw.wf.dto.CfwWorkflowUserInfo;
import jp.co.tokyo_gas.cisfw.wf.exception.CfwWorkflowApplicationException;
import jp.co.tokyo_gas.cisfw.wf.util.CfwWorkflowChecker;

import org.apache.commons.collections.CollectionUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

/**
 * 作業チーム機能のクラスです。
 *
 * @author Hiroko Mifuji(TDC)
 * @version 1.0.0
 */
@RequestScoped
public class CfwWorkflowServiceImplWorkTeam {

	/** ワークフロー機能の内部共通機能の実装クラス */
	@Inject
	private CfwWorkflowServiceImplUtility workflowServiceImplUtility;

	/** 作業チーム情報テーブル */
	@Inject
	@FwSQLMapper
	private WfWorkteamInfoMapper workteamInfoMapper;

	@Inject
	@FwSQLMapper
	private WfWorkteamInfoMapperEx workteamInfoMapperEx;

	@Inject
	@FwSQLMapper
	private WfWorkteamInfoListMapperEx workteamInfoListMapperEx;

	/** 作業チーム詳細情報テーブル */
	@Inject
	@FwSQLMapper
	private WfWorkteamDetailInfoMapper workteamDetailInfoMapper;

	/** 作業チーム詳細情報テーブル拡張 */
	@Inject
	@FwSQLMapper
	private WfWorkteamDetailInfoMapperEx workteamDetailInfoMapperEx;

	/** 作業チーム情報 */
	@Inject
	@FwSQLMapper
	private WfWorkteamSearchInfoMapperEx wfWorkteamSearchInfoMapperEx;

	/**
	 * 作業チーム情報変更。<br>
	 * 指定した作業チームに対して、チーム名やメンバーの変更を行います。<br>
	 * メンバーリストは一旦全削除を行い、その後全登録します。<br>
	 * メンバーリストが空の場合、作業チームからメンバーを全て削除します。<br>
	 * 登録する際にメンバーが実在するかのチェックは行いません。<br>
	 *
	 * @param workTeam 作業チーム情報
	 * @param usableCmpCodeFlg 企業コード使用可否フラグ
	 * @param user 更新者ユーザー情報
	 * @return 実行結果（true:成功）
	 * @throws CfwWorkflowApplicationException ワークフローアプリケーション例外をスローします。
	 */
	protected boolean updateWorkTeamInfo(CfwWorkTeamInfo workTeam, boolean usableCmpCodeFlg, CfwWorkflowUserInfo user)
		throws CfwWorkflowApplicationException {

		// 必須チェック
		this.checkUpdateWorkTeamInfo(workTeam, usableCmpCodeFlg, user);

		// 作業チーム情報の登録済確認
		if (!this.workflowServiceImplUtility.existsWorkTeamInfo(
				workTeam.getWorkteamId(), usableCmpCodeFlg, user.getCmpCode())) {
			throw new CfwWorkflowApplicationException(CfwWorkflowErrorcode.CFWWFNF001.getWfCode(), "作業チーム情報");
		}

		try {
			// サーバー日付取得
			Date serverDate = CfwDateUtils.getServerDate();

			// 作業チーム情報DTO生成
			WfWorkteamInfo workteamInfoDto = createWfWorkTeamInfo(workTeam, user);

			// 作業チーム情報TBLから対象作業チーム情報の更新
			this.workteamInfoMapperEx.updateWithLock(workteamInfoDto);

			// 削除用の作業チーム詳細情報DTO生成
			WfWorkteamDetailInfo deleteWorkteamDetailInfo = createWfWorkTeamDetailInfoForInsert(workTeam);

			// 作業チーム詳細情報TBLから対象作業チームIDの全削除
			this.workteamDetailInfoMapperEx.delete(deleteWorkteamDetailInfo);

			// 作業チーム詳細情報TBLへ作業チーム詳細情報を登録
			if (workTeam.getMemberList() != null) {
				for (CfwWorkflowUserInfo member : workTeam.getMemberList()) {

					// 登録用の作業チーム詳細情報DTO生成
					WfWorkteamDetailInfo insertWorkteamDetailInfo = createWfWorkTeamDetailInfoForDelete(
						workTeam, user, member, serverDate);

					this.workteamDetailInfoMapper.insert(insertWorkteamDetailInfo);
				}
			}

		} catch (FwSQLAlreadyModifiedException e) {
			throw new CfwWorkflowApplicationException(e.getCause(), CfwWorkflowErrorcode.CFWWFAP001.getWfCode());
		}

		return true;
	}

	/**
	 * 作業チーム情報一覧取得。<br>
	 * 作業チームIDと作業チーム名の一覧を取得します。<br>
	 * 作業チーム情報のメンバーのリストはnullのままで返します。<br>
	 * 検索条件に合致する情報が存在しない場合は、結果件数0件で正常終了します。<br>
	 *
	 * @param businessTypeCode 業務種別コード
	 * @param usableCmpCodeFlg 企業コード使用可否フラグ
	 * @param cmpCode 企業コード
	 * @return 作業チーム情報のリスト
	 * @throws CfwWorkflowApplicationException ワークフローアプリケーション例外をスローします。
	 */
	protected List<CfwWorkTeamInfo> getWorkTeamInfoList(String businessTypeCode, boolean usableCmpCodeFlg, String cmpCode)
		throws CfwWorkflowApplicationException {

		if (usableCmpCodeFlg) {
			// 企業コードの必須チェック
			if (CfwStringValidator.isEmpty(cmpCode)) {
				throw new CfwWorkflowApplicationException(CfwWorkflowErrorcode.CFWWFIP001.getWfCode(), "企業コード");
			}
		}

		// DBから【作業チーム情報DTO】のリストを取得
		List<WfWorkteamInfo> wfWorkteamInfoList = workteamInfoListMapperEx.find(businessTypeCode, usableCmpCodeFlg, cmpCode);

		// 【作業チーム情報DTO】のリストを【作業チーム情報】のリストに変換
		List<CfwWorkTeamInfo> resList = new ArrayList<>();

		for (WfWorkteamInfo res : wfWorkteamInfoList) {

			// 作業チーム情報DTOを生成
			CfwWorkTeamInfo cfwWorkTeamInfoDto = new CfwWorkTeamInfo();

			// 作業チームID
			cfwWorkTeamInfoDto.setWorkteamId(res.getWorkteamId());
			// 作業チーム名
			cfwWorkTeamInfoDto.setWorkteamName(res.getWorkteamName());
			// 楽観ロック用バージョン
			cfwWorkTeamInfoDto.setLockVersion(res.getUpdateDatetime());
			// メンバーリスト
			cfwWorkTeamInfoDto.setMemberList(null);
			// 最終更新日時
			cfwWorkTeamInfoDto.setLastUpdateDatetime(res.getUpdateDatetime());
			// 最終更新者ユーザーID
			cfwWorkTeamInfoDto.setLastUpdateUserId(res.getUpdateUserId());
			// 最終更新者氏名
			cfwWorkTeamInfoDto.setLastUpdateUserName(res.getUpdateUserName());
			// 最終更新者企業コード
			cfwWorkTeamInfoDto.setLastUpdateCmpCode(res.getUpdateUserCmpCode());
			// 最終更新者企業名
			cfwWorkTeamInfoDto.setLastUpdateCmpName(res.getUpdateUserCmpName());
			// 最終更新者所属コード
			cfwWorkTeamInfoDto.setLastUpdateOrgCode(res.getUpdateUserOrgCode());
			// 最終更新者所属名
			cfwWorkTeamInfoDto.setLastUpdateOrgName(res.getUpdateUserOrgName());
			// リストに格納
			resList.add(cfwWorkTeamInfoDto);
		}
		return resList;
	}

	/**
	 * 作業チーム情報詳細を取得します。<br>
	 * 作業チームIDで指定した作業チーム情報を取得します。<br>
	 * 作業チーム情報のメンバーのリストも取得して返します。<br>
	 * 検索条件に合致する情報が存在しない場合は、結果件数0件で正常終了します。<br>
	 *
	 * @param workTeamId 作業チームID
	 * @param usableCmpCodeFlg 企業コード使用可否フラグ
	 * @param cmpCode 企業コード
	 * @return 作業チーム情報
	 * @throws CfwWorkflowApplicationException ワークフローアプリケーション例外をスローします。
	 */
	protected CfwWorkTeamInfo getWorkTeamInfo(String workTeamId, boolean usableCmpCodeFlg, String cmpCode)
			throws CfwWorkflowApplicationException {

		// 作業チームIDの必須チェック
		if (CfwStringValidator.isEmpty(workTeamId)) {
			throw new CfwWorkflowApplicationException(CfwWorkflowErrorcode.CFWWFIP001.getWfCode(), "作業チームID");
		}

		// 企業コードの必須チェック
		if (usableCmpCodeFlg && CfwStringValidator.isEmpty(cmpCode)) {
			throw new CfwWorkflowApplicationException(CfwWorkflowErrorcode.CFWWFIP001.getWfCode(), "企業コード");
		}

		if (!usableCmpCodeFlg) {
			cmpCode = null;
		}

		// DBから【作業チーム情報DTO】を取得
		CfwWorkTeamInfo cfwWorkTeamInfo = wfWorkteamSearchInfoMapperEx.getWorkTeamInfo(workTeamId, cmpCode);

		// 該当する作業チームが存在しない
		if (cfwWorkTeamInfo == null) {
			throw new CfwWorkflowApplicationException(CfwWorkflowErrorcode.CFWWFNF001.getWfCode(), "作業チーム情報");
		}

		// 該当するメンバーリストが存在しない
		//    (LEFT JOINのため、メンバーが存在しない場合はメンバーリスト情報がNULLのデータが1件作成される)
		//    (メンバリストのユーザIDは作業チーム詳細情報テーブルの主キーのため、値がNULLとなるのはデータが無い場合)
		if ((cfwWorkTeamInfo.getMemberList().size() == 1)
			&& (CfwStringValidator.isEmpty(cfwWorkTeamInfo.getMemberList().get(0).getUserId()))) {

			// メンバーリストを０件リストにする
			cfwWorkTeamInfo.getMemberList().clear();
		}

		return cfwWorkTeamInfo;
	}

	/**
	 * 作業チーム情報変更機能のパラメーターの必須チェックを行います。<br>
	 *
	 * @param workTeamInfo 作業チーム情報
	 * @param usableCmpCodeFlg 企業コード使用可否フラグ
	 * @param userInfo 更新者ユーザー情報
	 * @throws CfwWorkflowApplicationException ワークフローアプリケーション例外をスローします。
	 */
	private void checkUpdateWorkTeamInfo(CfwWorkTeamInfo workTeamInfo, boolean usableCmpCodeFlg, CfwWorkflowUserInfo userInfo)
		throws CfwWorkflowApplicationException {

		// 作業チーム情報
		if (workTeamInfo == null) {
			throw new CfwWorkflowApplicationException(CfwWorkflowErrorcode.CFWWFIP001.getWfCode(), "作業チーム情報");
		}

		// 更新者ユーザー情報
		if (userInfo == null) {
			throw new CfwWorkflowApplicationException(CfwWorkflowErrorcode.CFWWFIP001.getWfCode(), "更新者ユーザー情報");
		}

		/**
		 * 作業チーム情報の必須チェック
		 */
		// 作業チームID
		if (CfwStringValidator.isEmpty(workTeamInfo.getWorkteamId())) {
			throw new CfwWorkflowApplicationException(CfwWorkflowErrorcode.CFWWFIP001.getWfCode(), "作業チーム情報.作業チームID");
		}

		// 作業チーム名
		if (CfwStringValidator.isEmpty(workTeamInfo.getWorkteamName())) {
			throw new CfwWorkflowApplicationException(CfwWorkflowErrorcode.CFWWFIP001.getWfCode(), "作業チーム情報.作業チーム名");
		}

		// 楽観ロック用バージョン
		if (!CfwWorkflowChecker.checkCfwLockVersion(workTeamInfo.getLockVersion())) {
			throw new CfwWorkflowApplicationException(CfwWorkflowErrorcode.CFWWFIP001.getWfCode(), "作業チーム情報.楽観ロック用バージョン");
		}

		// メンバー情報存在確認
		if (CollectionUtils.isNotEmpty(workTeamInfo.getMemberList())) {

			// 存在する場合
			List<CfwWorkflowUserInfo> memberList = workTeamInfo.getMemberList();
			for (CfwWorkflowUserInfo cfwWorkflowUserInfo : memberList) {

				// ユーザーID
				if (CfwStringValidator.isEmpty(cfwWorkflowUserInfo.getUserId())) {
					throw new CfwWorkflowApplicationException(
						CfwWorkflowErrorcode.CFWWFIP001.getWfCode(), "作業チーム情報.メンバー情報.ユーザーID");
				}

				// 企業コード
				if (CfwStringValidator.isEmpty(cfwWorkflowUserInfo.getCmpCode())) {
					throw new CfwWorkflowApplicationException(
						CfwWorkflowErrorcode.CFWWFIP001.getWfCode(), "作業チーム情報.メンバー情報.企業コード");
				}

				// 所属コード
				if (CfwStringValidator.isEmpty(cfwWorkflowUserInfo.getOrgCode())) {
					throw new CfwWorkflowApplicationException(
						CfwWorkflowErrorcode.CFWWFIP001.getWfCode(), "作業チーム情報.メンバー情報.所属コード");
				}
			}
		}

		/**
		 * 更新者ユーザー情報の必須チェック
		 */
		// ユーザーID
		if (CfwStringValidator.isEmpty(userInfo.getUserId())) {
			throw new CfwWorkflowApplicationException(CfwWorkflowErrorcode.CFWWFIP001.getWfCode(), "更新者ユーザー情報.ユーザーID");
		}

		// 企業コード
		if (CfwStringValidator.isEmpty(userInfo.getCmpCode())) {
			throw new CfwWorkflowApplicationException(CfwWorkflowErrorcode.CFWWFIP001.getWfCode(), "更新者ユーザー情報.企業コード");
		}
	}

	/**
	 * 作業チーム情報変更機能の作業チーム詳細情報登録で使用するパラメーターを生成します。
	 *
	 * @param workTeam 作業チーム情報
	 * @param user 更新者ユーザー情報
	 * @param member メンバー情報
	 * @param serverDate システム日付
	 * @return 作業チーム詳細情報
	 */
	private WfWorkteamDetailInfo createWfWorkTeamDetailInfoForDelete(CfwWorkTeamInfo workTeam,
		CfwWorkflowUserInfo user, CfwWorkflowUserInfo member, Date serverDate) {
		WfWorkteamDetailInfo insertWorkteamDetailInfo = new WfWorkteamDetailInfo();

		insertWorkteamDetailInfo.setWorkteamId(workTeam.getWorkteamId());
		insertWorkteamDetailInfo.setUserId(member.getUserId());
		insertWorkteamDetailInfo.setUserName(member.getUserName());
		insertWorkteamDetailInfo.setCmpName(member.getCmpName());
		insertWorkteamDetailInfo.setCmpCode(member.getCmpCode());
		insertWorkteamDetailInfo.setOrgName(member.getOrgName());
		insertWorkteamDetailInfo.setOrgCode(member.getOrgCode());
		insertWorkteamDetailInfo.setInsertDatetime(serverDate);
		insertWorkteamDetailInfo.setInsertUserId(user.getUserId());
		insertWorkteamDetailInfo.setUpdateDatetime(serverDate);
		insertWorkteamDetailInfo.setUpdateUserId(user.getUserId());
		return insertWorkteamDetailInfo;
	}

	/**
	 * 作業チーム情報変更機能の作業チーム詳細情報削除で使用するパラメーターを生成します。
	 *
	 * @param workTeam 作業チーム情報
	 * @return 作業チーム詳細情報
	 */
	private WfWorkteamDetailInfo createWfWorkTeamDetailInfoForInsert(CfwWorkTeamInfo workTeam) {
		WfWorkteamDetailInfo deleteWorkteamDetailInfo = new WfWorkteamDetailInfo();
		deleteWorkteamDetailInfo.setWorkteamId(workTeam.getWorkteamId());
		return deleteWorkteamDetailInfo;
	}

	/**
	 * 作業チーム情報変更機能の作業チーム情報更新で使用するパラメーターを生成します。
	 *
	 * @param workTeam 作業チーム情報
	 * @param user 更新者ユーザー情報
	 * @return 作業チーム情報
	 */
	private WfWorkteamInfo createWfWorkTeamInfo(CfwWorkTeamInfo workTeam,
		CfwWorkflowUserInfo user) {
		WfWorkteamInfo workteamInfoDto = new WfWorkteamInfo();
		workteamInfoDto.setWorkteamId(workTeam.getWorkteamId());
		workteamInfoDto.setWorkteamName(workTeam.getWorkteamName());
		workteamInfoDto.setUpdateUserName(user.getUserName());
		workteamInfoDto.setUpdateUserCmpCode(user.getCmpCode());
		workteamInfoDto.setUpdateUserCmpName(user.getCmpName());
		workteamInfoDto.setUpdateUserOrgCode(user.getOrgCode());
		workteamInfoDto.setUpdateUserOrgName(user.getOrgName());
		workteamInfoDto.setUpdateDatetime((Date) workTeam.getLockVersion());
		workteamInfoDto.setUpdateUserId(user.getUserId());
		return workteamInfoDto;
	}
}
