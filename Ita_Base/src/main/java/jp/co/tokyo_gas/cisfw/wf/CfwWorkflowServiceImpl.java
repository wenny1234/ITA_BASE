/*
 * Copyright 2015 TOKYO GAS CO.,LTD. All Rights Reserved.
 */
package jp.co.tokyo_gas.cisfw.wf;

import jp.co.tokyo_gas.aion.tgfw.workflow.service.WorkflowService;
import jp.co.tokyo_gas.cisfw.wf.dto.CfwWorkTeamInfo;
import jp.co.tokyo_gas.cisfw.wf.dto.CfwWorkflowDef;
import jp.co.tokyo_gas.cisfw.wf.dto.CfwWorkflowHistoryInfo;
import jp.co.tokyo_gas.cisfw.wf.dto.CfwWorkflowInfo;
import jp.co.tokyo_gas.cisfw.wf.dto.CfwWorkflowNodeInfo;
import jp.co.tokyo_gas.cisfw.wf.dto.CfwWorkflowSearchCondition;
import jp.co.tokyo_gas.cisfw.wf.dto.CfwWorkflowUserInfo;
import jp.co.tokyo_gas.cisfw.wf.exception.CfwWorkflowApplicationException;
import jp.co.tokyo_gas.cisfw.wf.interceptor.CfwWorkflowExceptionInterceptorAnnotation;

import java.util.List;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

/**
 * ワークフロー機能のクラスです。<br>
 *
 * @author A.Funakoshi (TDC)
 * @version 1.0.0
 */
@RequestScoped
@CfwWorkflowExceptionInterceptorAnnotation
public class CfwWorkflowServiceImpl implements CfwWorkflowService {

	/** 標準ワークフロー */
	@Inject
	protected WorkflowService workflowService;

	/** ワークフロー機能のワークフロー一覧関連の実装クラス */
	@Inject
	private CfwWorkflowServiceImplWorkflowList workflowServiceImplWorkflowList;

	/** ワークフロー機能のワークフロー機能の実装クラス */
	@Inject
	private CfwWorkflowServiceImplWorkflow workflowServiceImplWorkflow;

	/** ワークフロー機能の作業チーム機能の実装クラス */
	@Inject
	private CfwWorkflowServiceImplWorkTeam workflowServiceImplWorkTeam;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public CfwWorkflowDef getWorkflowDef(String workflowLogicId, boolean setFlag, CfwWorkflowUserInfo user)
		throws CfwWorkflowApplicationException {

		CfwWorkflowDef result = this.workflowServiceImplWorkflowList.getWorkflowDef(workflowLogicId, setFlag, user);

		return result;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public CfwWorkflowDef getWorkflowDef(String workflowLogicId, boolean setFlag)
		throws CfwWorkflowApplicationException {

		CfwWorkflowDef result = this.workflowServiceImplWorkflowList.getWorkflowDef(workflowLogicId, setFlag);

		return result;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String draft(CfwWorkflowInfo workflowInfo, CfwWorkflowUserInfo user) throws CfwWorkflowApplicationException {

		String result = this.workflowServiceImplWorkflow.draft(workflowInfo, user);

		return result;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean approve(CfwWorkflowInfo workflowInfo, String targetNodeDefId, CfwWorkflowUserInfo user)
		throws CfwWorkflowApplicationException {

		boolean result = this.workflowServiceImplWorkflow.approve(workflowInfo, targetNodeDefId, user);

		return result;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean sendback(CfwWorkflowInfo workflowInfo, String targetNodeDefId, CfwWorkflowUserInfo user)
		throws CfwWorkflowApplicationException {

		boolean result = this.workflowServiceImplWorkflow.sendback(workflowInfo, targetNodeDefId, user);

		return result;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean assign(String workflowId, CfwWorkflowNodeInfo targetNodeInfo, CfwWorkflowUserInfo targetUser,
			CfwWorkflowUserInfo user) throws CfwWorkflowApplicationException {

		boolean result = this.workflowServiceImplWorkflow.assign(workflowId, targetNodeInfo, targetUser, user);

		return result;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean delete(CfwWorkflowInfo workflowInfo, String targetNodeDefId, CfwWorkflowUserInfo user)
		throws CfwWorkflowApplicationException {

		boolean result = this.workflowServiceImplWorkflow.delete(workflowInfo, targetNodeDefId, user);

		return result;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<CfwWorkflowInfo> getWorkflowInfoList(CfwWorkflowSearchCondition condition, int maxCount)
		throws CfwWorkflowApplicationException {

		List<CfwWorkflowInfo> result = this.workflowServiceImplWorkflowList.getWorkflowInfoList(condition, maxCount);

		return result;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public CfwWorkflowInfo getWorkflowInfo(String workflowId, boolean setFlag, CfwWorkflowUserInfo user)
		throws CfwWorkflowApplicationException {

		CfwWorkflowInfo result = this.workflowServiceImplWorkflowList.getWorkflowInfo(workflowId, setFlag, user);

		return result;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<CfwWorkflowHistoryInfo> getWorkflowHistoryInfoList(String workflowId, CfwWorkflowUserInfo user)
		throws CfwWorkflowApplicationException {

		List<CfwWorkflowHistoryInfo> result = this.workflowServiceImplWorkflowList.getWorkflowHistoryInfoList(workflowId, user);

		return result;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean updateWorkTeamInfo(CfwWorkTeamInfo workTeam, boolean usableCmpCodeFlg, CfwWorkflowUserInfo user)
		throws CfwWorkflowApplicationException {

		boolean result = this.workflowServiceImplWorkTeam.updateWorkTeamInfo(workTeam, usableCmpCodeFlg, user);

		return result;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<CfwWorkTeamInfo> getWorkTeamInfoList(
		String businessTypeCode, boolean usableCmpCodeFlg, String cmpCode) throws CfwWorkflowApplicationException {

		List<CfwWorkTeamInfo> result = 
			this.workflowServiceImplWorkTeam.getWorkTeamInfoList(businessTypeCode, usableCmpCodeFlg, cmpCode);

		return result;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public CfwWorkTeamInfo getWorkTeamInfo(String workTeamId, boolean usableCmpCodeFlg, String cmpCode) 
			throws CfwWorkflowApplicationException {

		CfwWorkTeamInfo result = this.workflowServiceImplWorkTeam.getWorkTeamInfo(workTeamId, usableCmpCodeFlg, cmpCode);

		return result;
	}

	
}