package com.github.qyzhangaws.cloudhsm.handler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.mrgatto.enclave.handler.AbstractActionHandler;
import com.github.mrgatto.enclave.nsm.NsmClient;
import com.github.qyzhangaws.cloudhsm.Actions;
import com.github.qyzhangaws.cloudhsm.model.MyPojoData;
import com.github.qyzhangaws.cloudhsm.model.MyPojoDataResult;

@Component
public class EchoHandler extends AbstractActionHandler<MyPojoData, MyPojoDataResult> {

	@Autowired
	private NsmClient nsmClient;

	@Override
	public boolean canHandle(String action) {
		return Actions.ECHO.name().equalsIgnoreCase(action);
	}

	@Override
	public MyPojoDataResult handle(MyPojoData data) {
		String nsmModuleId = this.nsmClient.describeNsm().getModuleId();

		MyPojoDataResult result = new MyPojoDataResult();
		result.setValue("Echo from Enclave " + nsmModuleId + ": " + data.getValue());

		return result;
	}

}
