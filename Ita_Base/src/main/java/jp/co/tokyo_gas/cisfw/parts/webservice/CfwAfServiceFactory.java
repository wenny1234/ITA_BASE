/*
 * Copyright 2015 TOKYO GAS CO.,LTD. All Rights Reserved.
 */
package jp.co.tokyo_gas.cisfw.parts.webservice;

import jp.co.tokyo_gas.aion.tgfw.parts.ws.client.FwWSClientFactory;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.xml.ws.Service;

/**
 * サービスエンドポイントを取得するためのファクトリクラスのスタブ
 * 
 * @author IBM
 * @version 1.0.0
 */
@Dependent
public class CfwAfServiceFactory {

//	@Inject
//	private transient FwWSClientFactory factory;
	
//	/**
//	 * サービスエンドポイントインターフェースを生成のスタブ
//	 * @param serviceClass サービスクラス
//	 * @param serviceEndpointInterface サービスエンドポイントインタフェース
//	 * @param <T> サービスクラスの型
//	 * @param <K> サービスエンドポイントの型
//	 * @return サービスエンドポイントインターフェース
//	 */
//	public <T extends Service, K> K createSEI(Class<T> serviceClass, Class<K> serviceEndpointInterface) {
//		K endPoint = factory.createSEI(serviceClass, serviceEndpointInterface);
//		return endPoint;
//	}
}
