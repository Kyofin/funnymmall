package com.gec.dao;

import com.gec.mmall.dao.ShippingMapper;
import com.gec.mmall.pojo.Shipping;
import com.gec.test.TestBase;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class DaoTest extends TestBase {

	@Autowired
	ShippingMapper shippingMapper;

	@Test
	public void test(){
		Shipping shipping = new Shipping();
		shippingMapper.insert(shipping);
		System.out.println(shipping.getId());
	}
}
