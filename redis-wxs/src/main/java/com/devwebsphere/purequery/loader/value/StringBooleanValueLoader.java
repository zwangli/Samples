package com.devwebsphere.purequery.loader.value;

//
//This sample program is provided AS IS and may be used, executed, copied and
//modified without royalty payment by customer (a) for its own instruction and
//study, (b) in order to develop applications designed to run with an IBM
//WebSphere product, either for customer's own internal use or for redistribution
//by customer, as part of such an application, in customer's own products. "
//
//5724-J34 (C) COPYRIGHT International Business Machines Corp. 2009
//All Rights Reserved * Licensed Materials - Property of IBM
//


import com.devwebsphere.purequery.loader.BaseKeyValue;
import com.devwebsphere.purequery.loader.ValueLoader;
import com.ibm.websphere.objectgrid.plugins.LogElement;


public class StringBooleanValueLoader extends ValueLoader {

	@Override
	public BaseKeyValue<String,Boolean> getPOJOProperty(LogElement e) 
	{
		return new TemplateProperty<String, Boolean>(e);
	}

	@Override
	public Class getPOJOClass() 
	{
		return TemplateProperty.class;
		
	}
}
