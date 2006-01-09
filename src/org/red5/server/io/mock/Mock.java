package org.red5.server.io.mock;

import java.util.Iterator;
import java.util.List;

import org.red5.server.io.DataTypes;

public class Mock {
	
	public static final byte TYPE_END_OF_OBJECT = (byte) (DataTypes.CUSTOM_MOCK_MASK + 0x01);
	public static final byte TYPE_END_OF_ARRAY = (byte) (DataTypes.CUSTOM_MOCK_MASK + 0x02);
	public static final byte TYPE_ELEMENT_SEPARATOR = (byte) (DataTypes.CUSTOM_MOCK_MASK +0x03);
	public static final byte TYPE_PROPERTY_SEPARATOR = (byte) (DataTypes.CUSTOM_MOCK_MASK +0x04);
	public static final byte TYPE_ITEM_SEPARATOR = (byte) (DataTypes.CUSTOM_MOCK_MASK +0x05);
	public static final byte TYPE_END_OF_LIST = (byte) (DataTypes.CUSTOM_MOCK_MASK +0x06);
	
	public static String toStringValue(byte dataType){
		
		switch(dataType){
			case TYPE_END_OF_OBJECT: return "End of Object";
			case TYPE_END_OF_ARRAY: return "End of Array";
			case TYPE_ELEMENT_SEPARATOR: return ",";
			case TYPE_ITEM_SEPARATOR: return ",";
			case TYPE_PROPERTY_SEPARATOR: return "::";
		}
		
		return "MOCK[" + (dataType - DataTypes.CUSTOM_MOCK_MASK)+"]";
	
	}
	
	public static String listToString(List list){
		StringBuffer sb = new StringBuffer();
		Iterator it = list.iterator();
		while(it.hasNext()){
			Object val = it.next();
			if(val instanceof Byte){
				byte type = ((Byte) val).byteValue();
				if(type < DataTypes.CUSTOM_MOCK_MASK) 
					sb.append(DataTypes.toStringValue(type));
				else sb.append(toStringValue(type));
			} else {
				if( val != null) sb.append( val.getClass().getName() );
				sb.append( " { " );
				sb.append( val==null ? null : val.toString() );
				sb.append( " } " );
			}
			sb.append(" | ");
		}
		return sb.toString();
	}	
}
