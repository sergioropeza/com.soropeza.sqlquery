/******************************************************************************
 * Product: iDempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 1999-2012 ComPiere, Inc. All Rights Reserved.                *
 * This program is free software, you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY, without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program, if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * ComPiere, Inc., 2620 Augustine Dr. #245, Santa Clara, CA 95054, USA        *
 * or via info@compiere.org or http://www.compiere.org/license.html           *
 *****************************************************************************/
/** Generated Model - DO NOT CHANGE */
package com.soropeza.webui.apps.model;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.Properties;
import org.compiere.model.*;
import org.compiere.util.Env;
import org.compiere.util.KeyNamePair;

/** Generated Model for SOP_SQLQueryHistory
 *  @author iDempiere (generated)
 *  @version Release 10 - $Id$ */
@org.adempiere.base.Model(table="SOP_SQLQueryHistory")
public class X_SOP_SQLQueryHistory extends PO implements I_SOP_SQLQueryHistory, I_Persistent
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20261013L;

    /** Standard Constructor */
    public X_SOP_SQLQueryHistory (Properties ctx, int SOP_SQLQueryHistory_ID, String trxName)
    {
      super (ctx, SOP_SQLQueryHistory_ID, trxName);
      /** if (SOP_SQLQueryHistory_ID == 0)
        {
			setAD_User_ID (0);
// @#AD_User_ID@
			setSOP_SQLQueryHistory_ID (0);
			setSQLStatement (null);
        } */
    }

    /** Standard Constructor */
    public X_SOP_SQLQueryHistory (Properties ctx, int SOP_SQLQueryHistory_ID, String trxName, String ... virtualColumns)
    {
      super (ctx, SOP_SQLQueryHistory_ID, trxName, virtualColumns);
      /** if (SOP_SQLQueryHistory_ID == 0)
        {
			setAD_User_ID (0);
// @#AD_User_ID@
			setSOP_SQLQueryHistory_ID (0);
			setSQLStatement (null);
        } */
    }

    /** Standard Constructor */
    public X_SOP_SQLQueryHistory (Properties ctx, String SOP_SQLQueryHistory_UU, String trxName)
    {
      super (ctx, SOP_SQLQueryHistory_UU, trxName);
      /** if (SOP_SQLQueryHistory_UU == null)
        {
			setAD_User_ID (0);
// @#AD_User_ID@
			setSOP_SQLQueryHistory_ID (0);
			setSQLStatement (null);
        } */
    }

    /** Standard Constructor */
    public X_SOP_SQLQueryHistory (Properties ctx, String SOP_SQLQueryHistory_UU, String trxName, String ... virtualColumns)
    {
      super (ctx, SOP_SQLQueryHistory_UU, trxName, virtualColumns);
      /** if (SOP_SQLQueryHistory_UU == null)
        {
			setAD_User_ID (0);
// @#AD_User_ID@
			setSOP_SQLQueryHistory_ID (0);
			setSQLStatement (null);
        } */
    }

    /** Load Constructor */
    public X_SOP_SQLQueryHistory (Properties ctx, ResultSet rs, String trxName)
    {
      super (ctx, rs, trxName);
    }

    /** AccessLevel
      * @return 7 - System - Client - Org
      */
    protected int get_AccessLevel()
    {
      return accessLevel.intValue();
    }

    /** Load Meta Data */
    protected POInfo initPO (Properties ctx)
    {
      POInfo poi = POInfo.getPOInfo (ctx, Table_ID, get_TrxName());
      return poi;
    }

    public String toString()
    {
      StringBuilder sb = new StringBuilder ("X_SOP_SQLQueryHistory[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	@Deprecated(since="13") // use better methods with cache
	public org.compiere.model.I_AD_User getAD_User() throws RuntimeException
	{
		return (org.compiere.model.I_AD_User)MTable.get(getCtx(), org.compiere.model.I_AD_User.Table_ID)
			.getPO(getAD_User_ID(), get_TrxName());
	}

	/** Set User/Contact.
		@param AD_User_ID User within the system - Internal or Business Partner Contact
	*/
	public void setAD_User_ID (int AD_User_ID)
	{
		if (AD_User_ID < 1)
			set_ValueNoCheck (COLUMNNAME_AD_User_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_AD_User_ID, Integer.valueOf(AD_User_ID));
	}

	/** Get User/Contact.
		@return User within the system - Internal or Business Partner Contact
	  */
	public int getAD_User_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_AD_User_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

    /** Get Record ID/ColumnName
        @return ID/ColumnName pair
      */
    public KeyNamePair getKeyNamePair()
    {
        return new KeyNamePair(get_ID(), String.valueOf(getAD_User_ID()));
    }

	/** Set Elapsed Time ms.
		@param ElapsedTimeMS Elapsed Time in milli seconds
	*/
	public void setElapsedTimeMS (BigDecimal ElapsedTimeMS)
	{
		set_ValueNoCheck (COLUMNNAME_ElapsedTimeMS, ElapsedTimeMS);
	}

	/** Get Elapsed Time ms.
		@return Elapsed Time in milli seconds
	  */
	public BigDecimal getElapsedTimeMS()
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_ElapsedTimeMS);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set No. of Records.
		@param SOP_NoOfRecords Number of records returned or affected by the statement
	*/
	public void setSOP_NoOfRecords (int SOP_NoOfRecords)
	{
		set_ValueNoCheck (COLUMNNAME_SOP_NoOfRecords, Integer.valueOf(SOP_NoOfRecords));
	}

	/** Get No. of Records.
		@return Number of records returned or affected by the statement
	  */
	public int getSOP_NoOfRecords()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_SOP_NoOfRecords);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set SQL Query History.
		@param SOP_SQLQueryHistory_ID SQL Query History
	*/
	public void setSOP_SQLQueryHistory_ID (int SOP_SQLQueryHistory_ID)
	{
		if (SOP_SQLQueryHistory_ID < 1)
			set_ValueNoCheck (COLUMNNAME_SOP_SQLQueryHistory_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_SOP_SQLQueryHistory_ID, Integer.valueOf(SOP_SQLQueryHistory_ID));
	}

	/** Get SQL Query History.
		@return SQL Query History	  */
	public int getSOP_SQLQueryHistory_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_SOP_SQLQueryHistory_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set SQL Query History UU.
		@param SOP_SQLQueryHistory_UU SQL Query History UU
	*/
	public void setSOP_SQLQueryHistory_UU (String SOP_SQLQueryHistory_UU)
	{
		set_Value (COLUMNNAME_SOP_SQLQueryHistory_UU, SOP_SQLQueryHistory_UU);
	}

	/** Get SQL Query History UU.
		@return SQL Query History UU	  */
	public String getSOP_SQLQueryHistory_UU()
	{
		return (String)get_Value(COLUMNNAME_SOP_SQLQueryHistory_UU);
	}

	/** Set SQL Expression/Statement.
		@param SQLStatement SQL Expression/Statement
	*/
	public void setSQLStatement (String SQLStatement)
	{
		set_ValueNoCheck (COLUMNNAME_SQLStatement, SQLStatement);
	}

	/** Get SQL Expression/Statement.
		@return SQL Expression/Statement	  */
	public String getSQLStatement()
	{
		return (String)get_Value(COLUMNNAME_SQLStatement);
	}
}